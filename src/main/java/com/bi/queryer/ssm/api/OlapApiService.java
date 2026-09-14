package com.bi.queryer.ssm.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.entity.*;
import com.bi.queryer.ssm.api.vo.req.*;
import com.bi.queryer.ssm.api.vo.rsp.*;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.config.QueryAnalysis;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.engine.prepare.PrepareQueryEngine;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.mgr.fieldDef.model.ManualIndexWhitePaperEntity;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.promotion.model.PromotionCfg;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.ssm.query.template.view.TemplateViewService;
import com.bi.queryer.ssm.query.template.view.event.TemplateViewUpdatedEvent;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewConfigPortraitRsp;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.ssm.util.CsvUtil;
import com.bi.queryer.ssm.util.OssUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.ssm.util.TextToZipUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserService;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.encryption.AES;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OlapApiService {

    /** hostname 归属用户 Redis 缓存 key 前缀，完整 key = 前缀 + 设备 tag */
    private static final String HOSTNAME_OWNER_KEY_PREFIX = "ssm:hostname:owner:";

    /** 单个 filter 的 values 最大条数（不含），上限 1W */
    private static final int OLAP_FILTER_VALUES_MAX_SIZE = 10000;

    private static final ExecutorService validateLogPool = Executors.newFixedThreadPool(1);

    @Autowired
    private BaseDao dao;

    @Autowired
    private UserService userService;

    /**
     * 查询多维数据
     * @param queryOlapDataReq
     * @return
     */
    public String queryOlapData(QueryOlapDataReq queryOlapDataReq,String olapApiKey,String olapApiUserName, String hostname, String accessToken, HttpServletRequest request){

        System.out.println("queryOlapDataReq查询入参:" + JSONObject.toJSONString(queryOlapDataReq));
        log.info("queryOlapDataReq查询入参:" + JSONObject.toJSONString(queryOlapDataReq));

        String result = "";

        //构建查询引擎
        QueryEngine engine = buildOlapQueryEngine(queryOlapDataReq, null, olapApiKey, olapApiUserName, hostname, accessToken, request);

        //多维数据查询
        ResultDataSet dataSet = getQueryDataSet(engine);
        // 测试联调：C4 指标结果脱敏
        OlapApiTestJointDesensitizer.applyIfNeeded(dataSet, engine, olapApiKey);

        //数据集转化
        CsvUtil.CsvDataset csvDataset = CsvUtil.toCsv(dataSet, queryOlapDataReq.getDataSet().getCsvDelimiter());
        if(queryOlapDataReq.getDataSet().isToFile()){
            result = csvToFileAndUpload(csvDataset.csvContent, queryOlapDataReq.getDataSet().isZipFile());
        }else {
            result = csvDataset.csvContent;
        }
        UserManager.remove();
        dataSet = null;
        return result;
    }


    /** genAI_feature/olap_api_v2_start */
    /**
     * 与 {@link #queryOlapData} 相同查询逻辑，额外返回列元数据（白皮书释义）及小计/同环比说明。
     */
    public OlapDatasetAndMetadataData queryOlapDataWithMetadata(QueryOlapDataReq queryOlapDataReq, String olapApiKey, String olapApiUserName, String hostname, String accessToken, HttpServletRequest request) {

        System.out.println("queryOlapDataWithMetadata查询入参:" + JSONObject.toJSONString(queryOlapDataReq));
        log.info("queryOlapDataWithMetadata查询入参:" + JSONObject.toJSONString(queryOlapDataReq));

        OlapDatasetAndMetadataData result = new OlapDatasetAndMetadataData();

        //构建查询引擎
        QueryEngine engine = buildOlapQueryEngine(queryOlapDataReq, result, olapApiKey, olapApiUserName, hostname, accessToken, request);
        ResultDataSet dataSet = getQueryDataSet(engine);
        // 测试联调：C4 指标结果脱敏
        OlapApiTestJointDesensitizer.applyIfNeeded(dataSet, engine, olapApiKey);
        CsvUtil.CsvDataset csvDataset = CsvUtil.toCsv(dataSet, queryOlapDataReq.getDataSet().getCsvDelimiter());
        if (queryOlapDataReq.getDataSet().isToFile()) {
            csvDataset.csvDatasetUrl = csvToFileAndUpload(csvDataset.csvContent, queryOlapDataReq.getDataSet().isZipFile());
            // 返回的数据为数据文件url
            result.setDataset(csvDataset.csvDatasetUrl);
        } else {
            result.setDataset(csvDataset.csvContent);
        }
        result.setMetadata(buildMetadata(engine, dataSet));
        result.setViewDimensions(buildViewDimensions(engine, dataSet));
        result.setViewMetrics(buildViewMetrics(engine));
        result.setViewFilters(buildViewFilters(engine));
        result.setRemark(buildQueryRemark(engine));

        //判断是不是准实时数据集
        //准实时数据集返回数据更新时间
        String datasetId = engine.getConfig().getSettings().getDatasetId();
        if(SSDUtil.isNearRealTimeDataset(datasetId)){
            result.setDataLastUpdateTime(dataSet.getDataUpdateTime());
        }

        UserManager.remove();
        dataSet = null;
        return result;
    }

    /**
     *  "metadata": [
     *             {
     *                 "columnName": "日期",
     *                 "columnType": "维度",
     *                 "columnDesc": ""
     *             },
     *             {
     *                 "columnName": "业务线",
     *                 "columnType": "维度",
     *                 "columnDesc": "在研发商品库中维护的商品ID（PID）的独立运营单元。旨在通过集中化的管理和运营，更有效的销售这些产品，以满足市场需求并实现企业的业务目标和利润增长。",
     *                 "": ""
     *             },
     *             {
     *                 "columnName": "支付GMV_B2",
     *                 "columnType": "原子指标",
     *                 "columnDesc": "在计算指标时，限制订单已支付、未取消且非测试订单；按照订单创建时间统计；限制计入BI Queryer成交额的订单渠道和订单类型（完整对应关系可见订单渠道维表和订单类型维表），统计订单的税前券后实付金额"
     *             }
     *         ],
     * @param engine
     * @param dataSet
     * @return
     */
    public List<OlapDatasetColumnMetadata> buildMetadata(QueryEngine engine, ResultDataSet dataSet) {
        Map<String, String> whitePaperDescMap = getWhitePaperDescMap(engine.getConfig());
        Map<String, OlapDatasetColumnMetadata> metadataMap = new HashMap<>();
        fillOlapDatasetColumnMetadataMap(engine.getConfig(), whitePaperDescMap, metadataMap);
        return orderOlapMetadataByFlatColumns(metadataMap, CsvUtil.flattenColumns(dataSet.getColumns()));
    }

    /**
     * 无查询结果集时，与 {@link #buildMetadata(QueryEngine, ResultDataSet)} 共用首段 map 构建逻辑；
     * 列顺序与列名取自 {@link QueryConfigure#getResult()} 中可见、非附加字段，与视图配置画像等场景对齐。
     */
    public List<Pair<QueryField, OlapDatasetColumnMetadata>> buildMetadataOrderedByResultFieldPairs(QueryConfigure queryConfigure) {
        Map<String, String> whitePaperDescMap = getWhitePaperDescMap(queryConfigure);
        Map<String, OlapDatasetColumnMetadata> metadataMap = new HashMap<>();
        fillOlapDatasetColumnMetadataMap(queryConfigure, whitePaperDescMap, metadataMap);
        return orderOlapMetadataByResultFieldPairs(metadataMap, queryConfigure);
    }

    /**
     * 首段：按 {@link QueryConfigure#getAllFields()} 填充 code → 列元数据（与 Olap 导出 metadata 规则一致）。
     */
    public void fillOlapDatasetColumnMetadataMap(QueryConfigure queryConfigure,
                                                 Map<String, String> whitePaperDescMap,
                                                 Map<String, OlapDatasetColumnMetadata> metadataMap) {
        for (QueryField field : queryConfigure.getAllFields()) {

            OlapDatasetColumnMetadata metadata = new OlapDatasetColumnMetadata();

            if (Enabled.value(field.getIsAnalysis())) {
                metadata.setColumnType("指标");

                String measureCode = field.getAnalysisConfig() != null ? field.getAnalysisConfig().getMeasureCode() : null;
                OlapDatasetColumnMetadata measureMetadata = measureCode != null ? metadataMap.get(measureCode) : null;

                if (measureMetadata != null) {
                    metadata.setColumnDesc(measureMetadata.getColumnDesc());
                    metadata.setAggregationType(measureMetadata.getAggregationType());
                }

            } else {
                MetaField metaField = field.getMeta();
                if (metaField == null) {
                    continue;
                }

                boolean isCrossModelMeasure = FieldType.CROSS_MODEL_MEASURE == FieldType.get(field.getFieldType());

                metadata.setColumnType(Enabled.value(metaField.getIsMeasure()) ? "指标" : "维度");

                String kpiNo = isCrossModelMeasure ? metaField.getCode() : metaField.getKpiNo();
                metadata.setColumnDesc(whitePaperDescMap.get(kpiNo));

                if (Enabled.value(metaField.getIsCommonDate())) {
                    metadata.setColumnDesc("日期");
                }

                AggExpressionType aggExpressionType = AggExpressionType.get(metaField.getAggExpression());
                metadata.setAggregationType(aggExpressionType.getCode());

                boolean isLod = field.getCustomFieldConfigure() != null
                        && CustomFieldType.LOD == CustomFieldType.get(field.getCustomFieldConfigure().getType());

                if (field.isCustom() && !isLod && !isCrossModelMeasure && field.getCustomFieldConfigure() != null) {
                    String expression = field.getCustomFieldConfigure().getExpression();
                    if (expression != null && field.getCustomFieldConfigure().getExpressionIdMapping() != null) {
                        for (CustomFieldExpressionIdMapping mapping : field.getCustomFieldConfigure().getExpressionIdMapping()) {
                            //兼容title为空的场景
                            if (StrUtil.isNotEmpty(mapping.getTitle())) {
                                expression = expression.replaceAll(mapping.getId(), mapping.getTitle());
                                if (mapping.getCode() != null) {
                                    expression = expression.replaceAll(mapping.getCode(), mapping.getTitle());
                                }
                            }
                        }
                    }
                    if (CollUtil.isNotEmpty(field.getCusCalcDependFields()) && expression != null) {
                        for (QueryField queryField : field.getCusCalcDependFields()) {
                            expression = expression.replaceAll(queryField.getId(), queryField.getTitle());
                            expression = expression.replaceAll(queryField.getCode(), queryField.getTitle());
                        }
                    }
                    metadata.setColumnDesc(expression);
                } else if (isLod && field.getCustomFieldConfigure() != null
                        && CollUtil.isNotEmpty(field.getCustomFieldConfigure().getExpressionIdMapping())) {
                    CustomFieldExpressionIdMapping expressionIdMapping = field.getCustomFieldConfigure().getExpressionIdMapping().get(0);
                    MetaField lodMetaField = SSDMetaCacheManager.getField(expressionIdMapping.getId());
                    if (lodMetaField != null) {
                        metadata.setColumnDesc(whitePaperDescMap.get(lodMetaField.getKpiNo()));
                        metadata.setAggregationType(AggExpressionType.get(lodMetaField.getAggExpression()).getCode());
                    }
                }
            }

            metadataMap.put(field.getCode(), metadata);
        }
    }

    private List<OlapDatasetColumnMetadata> orderOlapMetadataByFlatColumns(Map<String, OlapDatasetColumnMetadata> metadataMap,
                                                                             List<CsvUtil.FlatColumn> flatColumns) {
        List<OlapDatasetColumnMetadata> metadataList = new ArrayList<>();
        for (CsvUtil.FlatColumn flatColumn : flatColumns) {
            String code = flatColumn.getCode();
            code = code.split("_" + BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
            OlapDatasetColumnMetadata metadata = metadataMap.get(code);
            if (metadata == null) {
                metadata = new OlapDatasetColumnMetadata();
                metadata.setColumnName(flatColumn.getFlatTitle());
            }
            metadata.setColumnName(flatColumn.getFlatTitle());
            metadataList.add(metadata);
        }
        return metadataList;
    }

    private List<Pair<QueryField, OlapDatasetColumnMetadata>> orderOlapMetadataByResultFieldPairs(
            Map<String, OlapDatasetColumnMetadata> metadataMap, QueryConfigure queryConfigure) {
        List<Pair<QueryField, OlapDatasetColumnMetadata>> out = new ArrayList<>();
        for (QueryField resultField : queryConfigure.getResult().getFields()) {
            if (resultField.isAppend()) {
                continue;
            }
            if (!Enabled.value(resultField.getIsShow())) {
                continue;
            }
            String mapCode = resultField.getCode();
            if (StrUtil.isNotEmpty(mapCode) && mapCode.contains(BIConsts.COLUMN_DIM_FIELD_SUFFIX)) {
                mapCode = mapCode.split("_" + BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
            }
            OlapDatasetColumnMetadata om = mapCode != null ? metadataMap.get(mapCode) : null;
            if (om == null) {
                om = new OlapDatasetColumnMetadata();
            }
            if (StrUtil.isEmpty(om.getColumnType())) {
                MetaField mf = resultField.getMeta();
                if (mf != null) {
                    om.setColumnType(Enabled.value(mf.getIsMeasure()) ? "指标" : "维度");
                }
            }
            om.setColumnName(resolveResultFieldColumnTitle(resultField));
            out.add(Pair.of(resultField, om));
        }
        return out;
    }

    private static String resolveResultFieldColumnTitle(QueryField field) {
        if (StrUtil.isNotBlank(field.getTitle())) {
            return field.getTitle().trim();
        }
        if (StrUtil.isNotBlank(field.getName())) {
            return field.getName().trim();
        }
        return StrUtil.emptyToDefault(field.getCode(), field.getId());
    }

    /**
     * 查询维度与结果集列对应关系：请求中 queryDimensions 优先（保持顺序）；未传时取视图行维度 + 列维度。
     * isInDataSet 与 metadata / dataset 表头一致（展平列名，含多级表头分段匹配）。
     */
    public List<OlapViewDimensionItem> buildViewDimensions(QueryEngine engine, ResultDataSet dataSet) {
        Set<String> datasetFlatTitles = collectDatasetFlatTitles(dataSet);
        List<OlapViewDimensionItem> list = new ArrayList<>();
        LinkedHashMap<String, String> orderedDimIdToTitle = new LinkedHashMap<>();
        UIQueryConfigure rawConfig = JSONObject.parseObject(engine.getConfig().getTemplateEntity().getConfig(), UIQueryConfigure.class);
        if (rawConfig == null) {
            return list;
        }
        for (UIQueryField f : rawConfig.getResult().getRowDimensions()) {
            collectDimensionTitleWithId(f, orderedDimIdToTitle);
        }
        for (UIQueryField f : rawConfig.getResult().getColDimensions()) {
            collectDimensionTitleWithId(f, orderedDimIdToTitle);
        }
        for (Map.Entry<String, String> entry : orderedDimIdToTitle.entrySet()) {
            String title = entry.getValue();
            OlapViewDimensionItem item = new OlapViewDimensionItem();
            item.setColumnName(title);
            item.setInDataSet(CollUtil.contains(datasetFlatTitles, title));
            item.setSensitiveLevel(getFieldSensitiveLevel(entry.getKey()));
            list.add(item);
        }
        return list;
    }

    /**
     * 查询指标与结果集列对应关系：请求中 queryMetrics 优先（保持顺序）；未传时取视图指标列表。
     * isInDataSet 通过 isShow 判断（queryMetrics 会驱动指标显隐）。
     */
    public List<OlapViewMetricItem> buildViewMetrics(QueryEngine engine) {
        List<OlapViewMetricItem> list = new ArrayList<>();
        UIQueryConfigure rawConfig = JSONObject.parseObject(engine.getConfig().getTemplateEntity().getConfig(), UIQueryConfigure.class);
        if (rawConfig == null) {
            return list;
        }
        for (UIQueryField f : rawConfig.getResult().getMeasures()) {
            if (f == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(f.getCode())) {
                continue;
            }
            OlapViewMetricItem item = new OlapViewMetricItem();

            String title = StrUtil.isNotEmpty(f.getDisplayTitle()) ? f.getDisplayTitle() : f.getTitle();
            item.setColumnName(title);
            item.setInDataSet(Enabled.value(f.getIsShow()));
            item.setSensitiveLevel(getFieldSensitiveLevel(f.getId()));
            list.add(item);
        }
        return list;
    }

    /**
     * 查询筛选项：请求中 filters 优先（保持顺序，仅含 values 非空的项）；未传时取视图筛选区中有筛选值的字段。
     */
    public List<OlapViewFilterItem> buildViewFilters(QueryEngine engine) {
        List<OlapViewFilterItem> list = new ArrayList<>();

        UIQueryConfigure rawConfig = JSONObject.parseObject(engine.getConfig().getTemplateEntity().getConfig(), UIQueryConfigure.class);
        if (rawConfig == null || CollUtil.isEmpty(rawConfig.getFilter())) {
            return list;
        }
        for (UIQueryField f : rawConfig.getFilter()) {
            if (f == null) {
                continue;
            }

            //日期不返回
            if(f.isCommonDate()){
                continue;
            }

            String title = StrUtil.isNotEmpty(f.getDisplayTitle()) ? f.getDisplayTitle() : f.getTitle();
            if (StrUtil.isBlank(title)) {
                continue;
            }
            OlapViewFilterItem item = new OlapViewFilterItem();
            item.setColumnName(title.trim());
            list.add(item);
        }
        return list;
    }

    private void collectDimensionTitle(UIQueryField f, LinkedHashSet<String> out) {
        if (f == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(f.getCode())) {
            return;
        }
        String title = f.getTitle();
        if (StrUtil.isNotBlank(title)) {
            out.add(title.trim());
        }
    }

    private String getFieldSensitiveLevel(String fieldId) {
        MetaField metaField = SSDMetaCacheManager.getField(fieldId);
        return metaField != null && StrUtil.isNotBlank(metaField.getSensitiveLevel())
                ? metaField.getSensitiveLevel() : "未知";
    }

    private void collectDimensionTitleWithId(UIQueryField f, LinkedHashMap<String, String> out) {
        if (f == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(f.getCode())) {
            return;
        }
        String title = f.getTitle();
        if (StrUtil.isNotBlank(title)) {
            out.putIfAbsent(f.getId(), title.trim());
        }
    }

    private Set<String> collectDatasetFlatTitles(ResultDataSet dataSet) {
        Set<String> titles = new HashSet<>();
        if (dataSet == null || CollUtil.isEmpty(dataSet.getColumns())) {
            return titles;
        }
        for (CsvUtil.FlatColumn c : CsvUtil.flattenColumns(dataSet.getColumns())) {
            if (c != null && StrUtil.isNotBlank(c.getFlatTitle())) {
                titles.add(c.getFlatTitle().trim());
            }
        }
        return titles;
    }

    /**
     * 获取所有白皮书的描述信息
     * "白皮书编码": "白皮书描述"
     * @return
     */
    public Map<String, String> getWhitePaperDescMap(QueryEngine engine) {
        return getWhitePaperDescMap(engine.getConfig());
    }

    /**
     * 获取所有白皮书的描述信息（与 Olap 列元数据、视图配置画像等共用）。
     */
    public Map<String, String> getWhitePaperDescMap(QueryConfigure configure) {
        Map<String, String> whitePaperDescMap = new HashMap<>();

        List<String> whitePaperCodes = new ArrayList<>();

        for (QueryField field : configure.getAllFields()) {
            MetaField metaField = field.getMeta();
            if (metaField == null) {
                continue;
            }

            whitePaperCodes.add(metaField.getKpiNo());
            whitePaperCodes.add(metaField.getCode());
        }

        if (CollUtil.isEmpty(whitePaperCodes)) {
            return whitePaperDescMap;
        }

        whitePaperCodes = whitePaperCodes.stream().filter(StrUtil::isNotEmpty).distinct().collect(Collectors.toList());

        List<ManualIndexWhitePaperEntity> whitePaperList = (List<ManualIndexWhitePaperEntity>) dao.queryObjectList(
                "fieldDef.getWhitePaperListByCodes", whitePaperCodes, DataSourceType.Data_Studio);
        if (CollUtil.isNotEmpty(whitePaperList)) {
            for (ManualIndexWhitePaperEntity whitePaper : whitePaperList) {
                whitePaperDescMap.put(whitePaper.getIndexNo(), whitePaper.getInterpretation());
            }
        }

        return whitePaperDescMap;
    }

    /**
     * "remark": {
     *             "说明1": "dataset中包含了列小计、列总计。小计/总计是对维度的聚合数据，若要分析场景只需要明细数据时，需要排除此类数据。",
     *             "说明2": {
     *                 "环比": "(当前期-上期)/当前期",
     *                 "周同比": "(当前期-上周同期)/上周同期",
     *                 "年同(-1)": "(当前期-去年同期)/去年同期"
     *             }
     *         }
     * @param engine
     * @return
     */
    public Map<String, Object> buildQueryRemark(QueryEngine engine) {

        Map<String, Object> remark = new LinkedHashMap<>();

        //判断是否有列小计和列总计
        QueryAnalysis queryAnalysis = engine.getConfig().getAnalysis();
        AnalysisTotalConfig totalConfig = queryAnalysis.getTotal();
        if (totalConfig.isActive()) {

            Long count = totalConfig.getItems().stream()
                    .filter(item -> AnalysisTotalType.COL_SUBTOTAL == item.getTotalType() || AnalysisTotalType.COL_TOTAL == item.getTotalType())
                    .count();

            if (count > 0) {
                remark.put("注意1", "数据集列小计、列总计。小计/总计是对维度的聚合数据，若要分析场景只需要明细数据时，需要排除此类数据；");
            }
        }

        //处理同环比
        Set<AnalysisCalcMode> calcModes = new LinkedHashSet<>();
        for (QueryField measureField : engine.getConfig().getResult().getMeasures()) {

            if (Enabled.value(measureField.getIsAnalysis()) &&
                    (AnalysisCalcMode.get(measureField.getAnalysisConfig().getCalcMode()).isCompare()
                            || AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(measureField.getAnalysisConfig().getCalcMode())
                            || AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(measureField.getAnalysisConfig().getCalcMode())
                    )
            ) {
                AnalysisCalcMode calcMode = measureField.getAnalysisConfig().getRawThbCalcMode();
                if (calcMode.isCompare()) {
                    if (AnalysisCalcMode.CUSTOM_COMPARE != calcMode) {
                        calcModes.add(calcMode);
                    }
                }
            }
        }

        if (CollUtil.isNotEmpty(calcModes)) {
            Map<String, String> calcModeMap = new LinkedHashMap<>();
            for (AnalysisCalcMode calcMode : calcModes) {
                calcModeMap.put(calcMode.getDesc(), calcMode.getFormula());
            }

            if (remark.isEmpty()) {
                remark.put("注意1", calcModeMap);
            } else {
                remark.put("注意2", calcModeMap);
            }
        }

        int size = remark.size() + 1;
        remark.put("注意" + size, "对非累加型指标（如比率、占比、去重计数等）做累加统计时，不能直接求和，必须先询问用户采用何种业务逻辑（例如取最后值、加权平均、不累加等）。");

        return remark;
    }


    public QueryEngine buildOlapQueryEngine(QueryOlapDataReq queryOlapDataReq, OlapDatasetAndMetadataData result,String olapApiKey,String olapApiUserName, String hostname, String accessToken, HttpServletRequest request) {

        assertOlapApiServerAllowed();

        //1 参数校验
        validate(queryOlapDataReq, olapApiKey, olapApiUserName, hostname, accessToken, request);

        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getTplByViewId", queryOlapDataReq.getTemplateViewId());
        if (templateViewEntity == null) {
            throw new BIException(String.format("视图id：%s不存在！", queryOlapDataReq.getTemplateViewId()));
        }

        if (result != null) {
            result.setViewName(templateViewEntity.getViewName());
            result.setTemplateName(templateViewEntity.getTplName());
            result.setViewUrl(String.format("https://ssd-admin.example.com/ssm/#/view/%s", templateViewEntity.getViewId()));
        }

        //2 调用前端node接口获取queryConfig
        String queryConfig = getQueryConfig(queryOlapDataReq, templateViewEntity);
        JSONObject queryObject = JSONObject.parseObject(queryConfig);
        String config = queryObject.getString("config");

        //2.1 校验 queryDimensions、queryMetrics、filters 的 fieldName 是否存在于模版视图
        validateQueryFields(queryOlapDataReq, config);

        //3 参数替换
//        queryConfig = normalizeConfig(config, queryOlapDataReq, olapApiKey);
        OlapApiQueryConfigureNormalizer normalizer = new OlapApiQueryConfigureNormalizer(config, queryOlapDataReq, olapApiKey, hostname);
        UIQueryConfigure uiQueryConfigure = normalizer.normalize();
        queryConfig = JSONObject.toJSONString(uiQueryConfigure);
        queryObject.put("config", queryConfig);
        queryObject.put("type", QuerySourceType.OLAP_API.getCode());

        //将用户信息注入当前线程中
        User user = userService.queryByName(queryOlapDataReq.getUserName());
        if (user == null) {
            throw new BIException("用户" + queryOlapDataReq.getUserName() + "不存在！");
        }
        UserManager.set(user);
        assertHasNonLodVisibleMeasure(uiQueryConfigure, queryOlapDataReq);
        QueryEngine engine = createQueryEngine(queryObject);
        return engine;
    }

    /**
     * 可见指标不能仅有 LOD：至少需有一个普通（非 LOD）指标，否则主视图无法查询。
     */
    private void assertHasNonLodVisibleMeasure(UIQueryConfigure uiQueryConfigure,QueryOlapDataReq queryOlapDataReq) {

        // 没有传可用指标不校验
        if(CollUtil.isEmpty(queryOlapDataReq.getQueryMetrics())){
            return;
        }

        if (uiQueryConfigure == null || uiQueryConfigure.getResult() == null) {
            return;
        }
        boolean hasVisibleMeasure = false;
        boolean hasNonLodVisibleMeasure = false;
        for (UIQueryField measure : uiQueryConfigure.getResult().getMeasures()) {
            if (measure == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(measure.getCode())) {
                continue;
            }
            if (!Enabled.value(measure.getIsShow())) {
                continue;
            }
            hasVisibleMeasure = true;
            if (measure.isLodField()) {
                continue;
            }
            if (!measure.isCalcField()) {
                hasNonLodVisibleMeasure = true;
                break;
            }
            // 计算字段：依赖字段中至少有一个非 LOD（含 lod 四则，type=common 时 isLodField 为 false）
            if (hasNonLodCalcDependency(measure)) {
                hasNonLodVisibleMeasure = true;
                break;
            }
        }
        if (hasVisibleMeasure && !hasNonLodVisibleMeasure) {
            throw new BIException("主视图中没有指标，无法进行查询，请添加普通指标！");
        }
    }

    private boolean hasNonLodCalcDependency(UIQueryField calcMeasure) {
        List<String> calcRefFieldIdList = calcMeasure.getCalcRefFieldIdList();
        if (CollUtil.isEmpty(calcRefFieldIdList)) {
            return true;
        }
        String lodIdentifier = CustomFieldType.LOD.getIdentifier();
        for (String calcRefFieldId : calcRefFieldIdList) {
            if (StrUtil.isNotEmpty(calcRefFieldId) && !calcRefFieldId.contains(lodIdentifier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 传入完整 UI 查询 config，返回 ResultDataSet
     */
    public ResultDataSet queryOlapDataByConfig(QueryOlapByConfigReq req) throws IOException {

        /**
         * 前端传递的config需要解密
         */
        String config = SSDUtil.decryptTplConfig(req.getConfig());

        JSONObject queryObject = new JSONObject();
        queryObject.put("config", config);
        queryObject.put("type", QuerySourceType.CHAT.getCode());

        //查询模板视图，补充入参，记录到查询日志中
        if(StrUtil.isNotEmpty(req.getViewId())) {
            //查询视图
            TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", req.getViewId());
            if (templateViewEntity != null) {
                queryObject.put("id", templateViewEntity.getTplId());
                queryObject.put("viewId", templateViewEntity.getViewId());
            }
        }

        //查询多维数据
        QueryEngine engine = createQueryEngine(queryObject);
        ResultDataSet dataSet = getQueryDataSet(engine);

        CsvUtil.CsvDataset csvDataset = CsvUtil.toCsv(dataSet,'\t');

        //进行zip压缩
        csvDataset.csvDatasetUrl = this.csvToFileAndUpload(csvDataset.csvContent, true);
        /*
        String fileName = Guid.id();
        byte[] csvToZip = CsvToZipUtil.csvToZip(csvDataset.csvContent, fileName + ".csv");
        String csvDatasetUrl = OssUtil.upload(csvToZip, fileName, ".zip", "bigdata/agent_new/dataset");
        csvDataset.csvDatasetUrl = csvDatasetUrl;
         */

        //取dataset前1000行，上传到OSS，作为预览的数据集
        if(CollUtil.isNotEmpty(dataSet.getRows())) {
            ResultDataSet previewDataSet = new ResultDataSet();
            previewDataSet.setColumns(dataSet.getColumns());
            int end = Math.min(dataSet.getRows().size(), 1000);
            previewDataSet.setRows(dataSet.getRows().subList(0, end));

            CsvUtil.CsvDataset previewCsvDataset = CsvUtil.toCsv(previewDataSet,'\t');
            String previewFileName = Guid.id();
            String previewCsvDatasetUrl = OssUtil.upload(previewCsvDataset.csvContent.getBytes(StandardCharsets.UTF_8), previewFileName, ".csv", "bigdata/agent_new/dataset");
            csvDataset.previewCsvDatasetUrl = previewCsvDatasetUrl;
        }

        csvDataset.csvContent = "";
        dataSet.getProperties().put("csvDataset", csvDataset);
        dataSet.setRows(new ArrayList<>());
        return dataSet;
    }

    private void assertOlapApiServerAllowed() {
        String serverIP = BIUtil.getServerIP();
        String olapServerIPs = SC.v("ssm.olap.api.server.iptable", "");
        if (StrUtil.isNotEmpty(olapServerIPs)) {
            List<String> olapApiServerIpList = Arrays.asList(olapServerIPs.split(","));
            if (!olapApiServerIpList.contains(serverIP)) {
                throw new BIException("多维API调用Base URL:https://ssm-server.example.com已停用，请修改为https://olap-api.example.com");
            }
        }
    }


    public void validate(QueryOlapDataReq queryOlapDataReq,String olapApiKey,String olapApiUserName, String hostname, String accessToken, HttpServletRequest request){

        if(StrUtil.isEmpty(olapApiKey)){
            throw new BIException("请求头中olap_api_key不能为空！");
        }

        //校验入参
        if(StrUtil.isEmpty(queryOlapDataReq.getTemplateViewId())){
            throw new BIException("模板视图ID：templateViewId为必填项！");
        }

        validateFilterValuesSize(queryOlapDataReq);

        //查询olap_api_key是否存在
        //优先查缓存
        OlapApiKeyEntity olapApiKeyEntity = OlapApiManager.get(olapApiKey);
        if(olapApiKeyEntity == null) {
            olapApiKeyEntity = (OlapApiKeyEntity)dao.queryObject("ssm.olap.api.queryByApiKey",olapApiKey);
            if (olapApiKeyEntity != null) {
                OlapApiManager.put(olapApiKey, olapApiKeyEntity);
            }
        }
        if(olapApiKeyEntity == null) {
            throw new BIException(String.format("olap_api_key:%s不存在！", olapApiKey));
        }

        // 内网域名校验
        validateIntranetDomain(olapApiKeyEntity, request);

        // 测试联调场景：强制限制 topN
        applyTestJointTopN(olapApiKeyEntity, queryOlapDataReq);

        //校验api_key的应用平台
        if(StrUtil.isNotEmpty(olapApiKeyEntity.getApiKeyPlatform())){
            List<String> apiKeyPlatformList = Arrays.asList(olapApiKeyEntity.getApiKeyPlatform().split(","));
            if(!apiKeyPlatformList.contains("ssm")){
                throw new BIException(String.format("olap_api_key:%s无调用多维API的权限！", BIUtil.desensitizeGuid32(olapApiKey)));
            }
        }

        //如果配置了校验请求头里的用户,将header里的用户赋予username
        if(Enabled.value(olapApiKeyEntity.getIsCheckHeaderUsername())) {

            if (StrUtil.isEmpty(olapApiUserName)) {
                throw new BIException("请求头中" + BIConsts.OLAP_API_USER_NAME + "不能为空！");
            }

            String userName = olapApiUserName.split("@")[0];
            queryOlapDataReq.setUserName(userName);
        }

        // 巡检用户豁免；否则按 api_key 的 isQueryDateRequired 决定是否校验时间粒度与时间范围
        if (!BIUtil.isApiUser(queryOlapDataReq.getUserName())
                && Enabled.value(olapApiKeyEntity.getIsQueryDateRequired())) {
            if (StrUtil.isEmpty(queryOlapDataReq.getQueryDate().getGranularity())) {
                throw new BIException("时间粒度：granularity为必填项！");
            }

            validateQueryDate(queryOlapDataReq);
        }

        //配置了校验视图id，查询api_key关联的视图id
        if(Enabled.value(olapApiKeyEntity.getIsCheckBizId())) {

            if (StrUtil.isNotEmpty(olapApiKeyEntity.getCheckBizIdType())) {
                List<String> checkBizTypeList = Arrays.asList(olapApiKeyEntity.getCheckBizIdType().split(","));
                if (checkBizTypeList.contains("ssm")) {
                    List<String> bizIdList = (List<String>) dao.queryObjectList("ssm.olap.api.queryBizIdListByApiKey", olapApiKey);
                    if (!bizIdList.contains(queryOlapDataReq.getTemplateViewId())) {
                        throw new BIException(String.format("api_key:%s没有视图id：%s的权限！", BIUtil.desensitizeGuid32(olapApiKey), queryOlapDataReq.getTemplateViewId()));
                    }
                }
            }

        }

        if(StrUtil.isEmpty(queryOlapDataReq.getUserName())){
            throw new BIException("域账号：userName为必填项！");
        }

        // username兼容：含有AES:则解密出域账号
        String userName = queryOlapDataReq.getUserName();
        boolean isAESUserName = false;
        if (userName.contains(SSDUtil.encryptStr)) {
            try {
                String encryptedUserName = userName.replace(SSDUtil.encryptStr, "");
                byte[] keyByte = SSDUtil.keyByteStr.getBytes("utf-8");
                String parsedUserName = new String(AES.decrypt(encryptedUserName, keyByte), "utf-8");
                if (StrUtil.isEmpty(parsedUserName)) {
                    throw new BIException("userName不合法！");
                }
                queryOlapDataReq.setUserName(parsedUserName);
            } catch (BIException e) {
                throw e;
            } catch (Exception e) {
                throw new BIException("userName不合法！");
            }

            isAESUserName = true;
        }

        //正常传递的userName才需校验
        if(!isAESUserName) {
            List<String> apiKeyUserList = (List<String>) dao.queryObjectList("ssm.olap.api.queryUserListByApiKey", olapApiKey);
            if (!apiKeyUserList.contains(queryOlapDataReq.getUserName())) {
                //如果用户含有all，则不校验权限
                if (!apiKeyUserList.contains("all")) {
                    throw new BIException(String.format("%s没有权限使用api_key:%s", queryOlapDataReq.getUserName(), BIUtil.desensitizeGuid32(olapApiKey)));
                }
            }
        }

        // 日期维度和日期查询维度冲突
        String aggregate = queryOlapDataReq.getQueryDate().getAggregate();
        Set<String> queryDimensionSet = getQueryDimensions(queryOlapDataReq);
        if (aggregate != null && !queryDimensionSet.isEmpty()) {
            if ("true".equals(aggregate) && queryDimensionSet.contains("日期")) {
                throw new BIException("queryDate.aggregate=true表示结果不需要\"日期\"维度，但queryDimensions中有\"日期\"维度。建议queryDate.aggregate=false");
            }

            if ("false".equals(aggregate) && !queryDimensionSet.contains("日期")) {
                throw new BIException("queryDate.aggregate=false表示结果需要\"日期\"维度，但queryDimensions中没有\"日期\"维度。建议queryDate.aggregate=true");
            }
        }

        // hostname + accessToken 校验
        validateHostnameToken(olapApiKeyEntity, hostname, accessToken, queryOlapDataReq.getUserName());

    }

    /**
     * 校验 filters 中每个筛选项的 values 条数必须小于 1W。
     */
    private void validateFilterValuesSize(QueryOlapDataReq queryOlapDataReq) {
        if (queryOlapDataReq == null || CollUtil.isEmpty(queryOlapDataReq.getFilters())) {
            return;
        }
        for (QueryOlapDataQueryFilterReq filterReq : queryOlapDataReq.getFilters()) {
            if (filterReq == null || CollUtil.isEmpty(filterReq.getValues())) {
                continue;
            }
            int valueSize = filterReq.getValues().size();
            if (valueSize >= OLAP_FILTER_VALUES_MAX_SIZE) {
                String fieldName = StrUtil.isBlank(filterReq.getFieldName())
                        ? "未知字段" : filterReq.getFieldName().trim();
                throw new BIException(String.format(
                        "filters中【%s】的values条数必须小于%d，当前为%d！",
                        fieldName, OLAP_FILTER_VALUES_MAX_SIZE, valueSize));
            }
        }
    }

    /**
     * 测试联调场景（is_test_joint=1）：将 topN 限制为 SC 配置上限（默认 10）
     */
    private void applyTestJointTopN(OlapApiKeyEntity entity, QueryOlapDataReq queryOlapDataReq) {
        if (!Enabled.value(entity.getIsTestJoint())) {
            return;
        }
        int maxRow = Integer.parseInt(SC.v("olap.api.test.joint.max.row.num", "10"));
        Integer topN = queryOlapDataReq.getTopN();
        if (topN == null || topN <= 0 || topN > maxRow) {
            queryOlapDataReq.setTopN(maxRow);
        }
    }

    /**
     * 若 api_key 开启内网域名校验，则请求 Host 必须包含 SC 配置的内网域名（不校验端口）
     */
    private void validateIntranetDomain(OlapApiKeyEntity entity, HttpServletRequest request) {
        if (!Enabled.value(entity.getIsCheckIntranetDomain())) {
            return;
        }
        if (request == null) {
            throw new BIException("开启内网域名校验时请求上下文不能为空！");
        }
        String allowedDomain = SC.v("olap.api.intranet.domain", "olap-api.example.com");
        String host = buildRequestHost(request);
        if (!host.contains(allowedDomain)) {
            throw new BIException( "当前 api_key 仅允许在IDC内网访问使用。");
        }
    }

    /**
     * 取当前请求的 Host（不含 scheme、不保证含 port，经过反向代理时端口可能被透传丢失）
     */
    private String buildRequestHost(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (StrUtil.isEmpty(host)) {
            host = request.getServerName();
        }
        return host;
    }

    private void validateHostnameToken(OlapApiKeyEntity entity, String hostname, String accessToken, String username) {
        // step2: 系统级 key 跳过
        if (Enabled.value(entity.getIsSystem())) {
            return;
        }

        validateHostnameAccessToken(hostname, accessToken, username);
    }

    /**
     * 校验 hostname + accessToken 与用户的绑定关系，不依赖具体的 api_key（无系统级 key 跳过逻辑）。
     *
     * @param hostname    请求头中的设备码
     * @param accessToken 请求头中的 accessToken
     * @param username    当前请求的域账号
     */
    private void validateHostnameAccessToken(String hostname, String accessToken, String username) {
        // step0: 白名单用户跳过校验
        String whitelistStr = SC.v("olap.hostname.validate.whitelist", "");
        if (StrUtil.isNotEmpty(whitelistStr)) {
            List<String> whitelist = Arrays.asList(whitelistStr.split(","));
            if (whitelist.contains(username)) {
                return;
            }
        }

        // step0.5: 黑名单用户强制走后续校验逻辑（忽略总体开关）
        String blacklistStr = SC.v("olap.hostname.validate.blacklist", "contributor");
        boolean inBlacklist = StrUtil.isNotEmpty(blacklistStr)
                && Arrays.asList(blacklistStr.split(",")).contains(username);

        if (!inBlacklist) {
            // step1: 总体开关
            if (!"true".equals(SC.v("olap.hostname.validate.enabled", "true"))) {
                return;
            }
        }

        String tips = SC.v("olap.hostname.validate.tips", "");
        if (StrUtil.isEmpty(hostname)) {
            throw new BIException("设备码不能为空！" + tips);
        }

        // step3: 解密 accessToken
        if (StrUtil.isEmpty(accessToken)) {
            throw new BIException("accessToken不能为空！" + tips);
        }
        AccessTokenPayload payload;
        try {
            String json = AES.decryptAccessToken(accessToken);
            payload = JSONObject.parseObject(json, AccessTokenPayload.class);
        } catch (Exception e) {
            e.printStackTrace();
            asyncInsertValidateFailLog("accessToken_decrypt", accessToken, null, e.getMessage(), username);
            throw new BIException("accessToken解密失败！原因：" + e.getMessage() + "。" + tips);
        }

        // step4: 校验 hostname
        if (!hostname.equals(payload.getHostname())) {
            asyncInsertValidateFailLog("hostname_match", hostname, payload.getHostname(),
                    "入参设备码与系统获取的设备码不一致！", username);
            throw new BIException("入参设备码与系统获取的设备码不一致！" + tips);
        }

        // step5: 校验过期
        String expireSecondsStr = SC.v("olap.hostname.access.token.expire.seconds", "300");
        long expireSeconds = Long.parseLong(expireSecondsStr);
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long createTimeMs = sdf.parse(payload.getCreateTime()).getTime();
            long nowMs = System.currentTimeMillis();
            if (nowMs - createTimeMs > expireSeconds * 1000) {
                asyncInsertValidateFailLog("accessToken_expire", payload.getCreateTime(), null,
                        "accessToken已过期！", username);
                throw new BIException("accessToken已过期！" + tips);
            }
        } catch (BIException e) {
            throw e;
        } catch (Exception e) {
            asyncInsertValidateFailLog("accessToken_time_format", accessToken, null,
                    "accessToken时间格式异常！", username);
            throw new BIException("accessToken时间格式异常！" + tips);
        }

        // step6: 通过资产接口校验 hostname 是否归属于当前用户
        validateHostnameOwner(payload.getHostname(), username);
    }

    /**
     * 校验 hostname 对应的设备是否归属于当前用户。
     *
     * 流程：
     * 1. 将 hostname 去掉 .local 后缀得到设备 tag
     * 2. 优先查询 Redis 缓存，命中则直接使用缓存值
     * 3. 缓存未命中时，调用资产接口获取设备归属用户
     * 4. 资产未分配时，回退查询本地 openapi_user_hostname_mapping 缓存
     * 5. 资产命中或 mapping 命中后写入 Redis（TTL 1 小时）；两边都未命中才写空防穿透
     * 6. 校验归属用户与入参 username 是否一致，不一致则抛出异常
     *
     * 注意：接口调用失败时不写缓存，下次请求仍会重试接口。
     *
     * @param hostname 请求头中的 hostname（如 LZ-L259040.local）
     * @param username 当前请求的域账号
     */
    private void validateHostnameOwner(String hostname, String username) {
        String tag = hostname.replace(".local", "");
        String redisKey = HOSTNAME_OWNER_KEY_PREFIX + tag;

        // step1: 从 Redis 读取
        String assignedUsername = RedisCacheManager.get(redisKey);

        // RedisCacheManager.get 在 key 不存在时返回字符串 "null"，需防御判断
        if ("null".equals(assignedUsername) || assignedUsername == null) {
            // step2: 调用资产接口
            String hardwareUrl = SC.v("olap.hostname.hardware.url", "https://it-zichan.example.com/api/v1/hardware/bytag/") + tag;
            try {
                Map<String, String> hardwareHeaders = new HashMap<>();
                hardwareHeaders.put("Authorization", SC.v("olap.hostname.hardware.token", ""));
                String hardwareResp = HttpUtil.doGet(hardwareUrl, null, "application/json", hardwareHeaders, 60000);
                JSONObject hardwareJson = JSONObject.parseObject(hardwareResp);
                if (hardwareJson == null) {
                    throw new BIException("设备码资产校验失败：接口返回为空！");
                }
                JSONObject assignedTo = hardwareJson.getJSONObject("assigned_to");
                assignedUsername = assignedTo != null ? assignedTo.getString("username") : "";

                // step3: 资产未分配时回退本地扩展映射缓存；结果统一写 Redis（空值防穿透）
                if (StrUtil.isEmpty(assignedUsername) && OlapApiManager.hasUserHostnameMapping(username, tag)) {
                    assignedUsername = username;
                }
                RedisCacheManager.setAsync(redisKey, assignedUsername, 1, TimeUnit.HOURS);
            } catch (BIException e) {
                throw e;
            } catch (Exception e) {
                throw new BIException("设备码资产校验异常：" + e.getMessage());
            }
        }

        // step4: 校验归属
        if (!username.equals(assignedUsername)) {
            String remark = String.format("设备码校验异常：设备[%s]未分配给用户[%s]！", tag, username);
            asyncInsertValidateFailLog("hostname_owner", username, assignedUsername, remark, username);
            throw new BIException(remark);
        }
    }

    /**
     * 校验 hostname 与用户的绑定关系后，返回用户绑定的 api_key（不含 user_name=all 的公共 key）。
     * 用户绑定了多个 api_key 时，取任意一个。
     *
     * @param userName    域账号
     * @param hostname    请求头中的设备码
     * @param accessToken 请求头中的 accessToken
     * @return 用户绑定的 api_key，未绑定则返回 null
     */
    public String getUserApiKey(String userName, String hostname, String accessToken) {
        if (StrUtil.isEmpty(userName)) {
            throw new BIException("域账号不能为空！");
        }

        validateHostnameAccessToken(hostname, accessToken, userName);

        return OlapApiManager.getApiKeyByUserName(userName);
    }

    private void asyncInsertValidateFailLog(String validateType, String requestValue,
                                            String actualValue, String remark, String createdBy) {
        validateLogPool.execute(() -> {
            try {
                SsmOpenapiValidateFailLog validateFailLog = new SsmOpenapiValidateFailLog();
                validateFailLog.setValidateType(validateType);
                validateFailLog.setRequestValue(requestValue);
                validateFailLog.setActualValue(actualValue);
                validateFailLog.setRemark(remark);
                validateFailLog.setCreatedBy(createdBy);
                dao.insert("ssm.openapi.validate.insertFailLog", validateFailLog);
            } catch (Exception ex) {
                log.warn("asyncInsertValidateFailLog failed", ex);
            }
        });
    }

    private void validateQueryDate(QueryOlapDataReq req) {
        QueryOlapDataQueryDateReq queryDate = req.getQueryDate();
        List<String> values = queryDate.getValues();
        String type = queryDate.getType();

        if (CollUtil.isEmpty(values) || values.stream().allMatch(StrUtil::isBlank)) {
            throw new BIException("时间范围：values为必填项！");
        }

        if ("biz".equals(type)) {
            DateGranularity dateGranularity = DateGranularity.get(queryDate.getGranularity());
            if (DateGranularity.DAY != dateGranularity) {
                throw new BIException("业务日历（type=biz）仅支持日粒度（granularity=d）！");
            }
            for (String v : values) {
                if (v.contains("所有阶段")) {
                    continue;
                }
                PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(v);
                if (promotionCfg == null) {
                    throw new BIException("业务日历values在系统配置中不存在，应为 <年份>-<大促活动>-<大促阶段>，如：2025-双十一-开门红。实际值：" + v);
                }
            }
            // 展开"所有阶段"：替换为该年份+大促活动下的所有阶段标识
            List<String> expanded = new ArrayList<>();
            for (String v : values) {
                String[] parts = v.split("-", 3);
                if (parts.length == 3 && "所有阶段".equals(parts[2])) {
                    int promoYear;
                    try {
                        promoYear = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException e) {
                        throw new BIException("业务日历 values 年份格式错误：" + v);
                    }
                    List<String> stageIdentifiers = PromotionManager.getStageIdentifiersByYearAndName(promoYear, parts[1]);
                    if (CollUtil.isEmpty(stageIdentifiers)) {
                        throw new BIException("业务日历未找到年份=" + parts[0] + "、大促活动=" + parts[1] + " 的阶段配置！");
                    }
                    for (String sid : stageIdentifiers) {
                        expanded.add(sid);
                    }
                } else {
                    expanded.add(v);
                }
            }

            //去重
            if (CollUtil.isNotEmpty(expanded)) {
                expanded = expanded.stream().distinct().collect(Collectors.toList());
            }

            queryDate.setValues(expanded);
        } else {
            if (values.size() != 2) {
                throw new BIException("时间范围：values有且仅有2个元素！");
            }
        }
    }

    /**
     * 调用前端node服务，获取查询的queryConfig
     * @param queryOlapDataReq
     * @param templateViewEntity
     * @return
     */
    public String getQueryConfig(QueryOlapDataReq queryOlapDataReq,TemplateViewEntity templateViewEntity){

        Map<String,String> paramMap = new HashMap<>();

        paramMap.put("token", UserTokenManager.createByUserName(queryOlapDataReq.getUserName()));
        paramMap.put("queryTplId",templateViewEntity.getTplId());
        paramMap.put("viewId",templateViewEntity.getViewId());
        paramMap.put("dateGranularity",queryOlapDataReq.getQueryDate().getGranularity());

        //允许发送过滤条件为空的过滤条件
        paramMap.put("sendFilterWhenEmpty","1");

        String url = SC.v("ssm.get.query.config.url","http://screenshot.example.com:9010/helper/getTemplateQueryConfig");
        String response = HttpUtil.doPost(url, paramMap,"application/json", null, 5 * 60 * 1000);
        JSONObject data = JSON.parseObject(response);

        if(data == null){
            throw new BIException("获取查询配置失败！请联系数据产品技术支持");
        }

        JSONObject dataObject = data.getJSONObject("data");
        if(!data.getBoolean("success") || dataObject ==null){
            throw new BIException(String.format("获取查询配置失败！异常原因：%s。请联系数据产品技术支持",data.getString("message")));
        }

        return dataObject.getString("queryConfig");
    }

    private Set<String> getQueryDimensions(QueryOlapDataReq req) {
        if (CollUtil.isEmpty(req.getQueryDimensions())) {
            return Collections.emptySet();
        }
        Set<String> names = req.getQueryDimensions().stream()
                .map(QueryOlapDataQueryDimensionReq::getFieldName)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
        return Collections.unmodifiableSet(names);
    }

    /**
     * 校验 queryDimensions、queryMetrics、filters 的 fieldName 是否存在于模版视图配置中。
     */
    private void validateQueryFields(QueryOlapDataReq queryOlapDataReq, String config) {
        UIQueryConfigure uiQueryConfigure = JSONObject.parseObject(config, UIQueryConfigure.class);
        Set<String> rowColDimensionNames = collectRowColDimensionNames(uiQueryConfigure);
        Set<String> measureNames = collectMeasureNames(uiQueryConfigure);

        List<String> errorParts = new ArrayList<>();
        LinkedHashSet<String> notExistInQueryDimensions = collectNotExistFieldNames(
                collectQueryDimensionFieldNames(queryOlapDataReq), rowColDimensionNames);
        if (CollUtil.isNotEmpty(notExistInQueryDimensions)) {
            errorParts.add(String.format("queryDimensions中的【%s】",
                    String.join(",", notExistInQueryDimensions)));
        }

        LinkedHashSet<String> notExistInQueryMetrics = collectNotExistFieldNames(
                collectQueryMetricFieldNames(queryOlapDataReq), measureNames);
        if (CollUtil.isNotEmpty(notExistInQueryMetrics)) {
            errorParts.add(String.format("queryMetrics中的【%s】",
                    String.join(",", notExistInQueryMetrics)));
        }

        LinkedHashSet<String> notExistInFilters = collectNotExistFieldNames(
                collectFilterFieldNames(queryOlapDataReq), rowColDimensionNames);
        if (CollUtil.isNotEmpty(notExistInFilters)) {
            errorParts.add(String.format("filters中的【%s】",
                    String.join(",", notExistInFilters)));
        }

        if (CollUtil.isNotEmpty(errorParts)) {
            throw new BIException(String.format("查询失败。原因：入参错误,%s字段在模版视图中不存在",
                    String.join("、", errorParts)));
        }
    }

    private LinkedHashSet<String> collectNotExistFieldNames(List<String> fieldNames, Set<String> rowColDimensionNames) {
        LinkedHashSet<String> notExistFieldNames = new LinkedHashSet<>();
        for (String fieldName : fieldNames) {
            String trimmed = fieldName.trim();
            if (!rowColDimensionNames.contains(trimmed)) {
                notExistFieldNames.add(trimmed);
            }
        }
        return notExistFieldNames;
    }

    private List<String> collectQueryDimensionFieldNames(QueryOlapDataReq queryOlapDataReq) {
        List<String> fieldNames = new ArrayList<>();
        if (CollUtil.isEmpty(queryOlapDataReq.getQueryDimensions())) {
            return fieldNames;
        }
        for (QueryOlapDataQueryDimensionReq dimensionReq : queryOlapDataReq.getQueryDimensions()) {
            if (dimensionReq != null && StrUtil.isNotBlank(dimensionReq.getFieldName())) {
                fieldNames.add(dimensionReq.getFieldName().trim());
            }
        }
        return fieldNames;
    }

    private List<String> collectQueryMetricFieldNames(QueryOlapDataReq queryOlapDataReq) {
        List<String> fieldNames = new ArrayList<>();
        if (CollUtil.isEmpty(queryOlapDataReq.getQueryMetrics())) {
            return fieldNames;
        }
        for (QueryOlapDataQueryMetricReq metricReq : queryOlapDataReq.getQueryMetrics()) {
            if (metricReq != null && StrUtil.isNotBlank(metricReq.getFieldName())) {
                fieldNames.add(metricReq.getFieldName().trim());
            }
        }
        return fieldNames;
    }

    private List<String> collectFilterFieldNames(QueryOlapDataReq queryOlapDataReq) {
        List<String> fieldNames = new ArrayList<>();
        if (CollUtil.isEmpty(queryOlapDataReq.getFilters())) {
            return fieldNames;
        }
        for (QueryOlapDataQueryFilterReq filterReq : queryOlapDataReq.getFilters()) {
            if (filterReq != null && StrUtil.isNotBlank(filterReq.getFieldName())) {
                fieldNames.add(filterReq.getFieldName().trim());
            }
        }
        return fieldNames;
    }

    private Set<String> collectRowColDimensionNames(UIQueryConfigure uiQueryConfigure) {
        Set<String> names = new HashSet<>();
        if (uiQueryConfigure == null || uiQueryConfigure.getResult() == null) {
            return names;
        }
        for (UIQueryField field : uiQueryConfigure.getResult().getRowDimensions()) {
            addRowColDimensionName(names, field);
        }
        for (UIQueryField field : uiQueryConfigure.getResult().getColDimensions()) {
            addRowColDimensionName(names, field);
        }
        for (UIQueryField field : uiQueryConfigure.getFilter()) {
            addRowColDimensionName(names, field);
        }
        return names;
    }

    private void addRowColDimensionName(Set<String> names, UIQueryField field) {
        if (field == null) {
            return;
        }
        if (StrUtil.isNotBlank(field.getTitle())) {
            names.add(field.getTitle().trim());
        }
        if (StrUtil.isNotBlank(field.getDisplayTitle())) {
            names.add(field.getDisplayTitle().trim());
        }
    }

    private Set<String> collectMeasureNames(UIQueryConfigure uiQueryConfigure) {
        Set<String> names = new HashSet<>();
        if (uiQueryConfigure == null || uiQueryConfigure.getResult() == null) {
            return names;
        }
        for (UIQueryField field : uiQueryConfigure.getResult().getMeasures()) {
            if (field == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(field.getCode())) {
                continue;
            }
            addRowColDimensionName(names, field);
        }
        return names;
    }

    /**
     * 构建查询引擎
     * @param queryObject
     * @return
     */
    public  QueryEngine createQueryEngine(JSONObject queryObject) {

        SSDQueryTemplate queryTemplate = JSONObject.parseObject(JSONObject.toJSONString(queryObject), SSDQueryTemplate.class);
        QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
        queryConfigure.load();

        queryConfigure.getSettings().setAclCheck(true);
        queryConfigure.getSettings().setEnableCreateAllTableBySameCodeOpt(true);
        queryConfigure.getSettings().setQuerySource(QuerySourceType.OLAP_API.getCode());

        QueryContext cxt = new QueryContext();
        cxt.setQueryParamString(JSONObject.toJSONString(queryObject));

        // 预处理：正式数据查询前置数据查询
        PrepareQueryEngine prepareQueryEngine = QueryFactory.createPrepareEngine(queryConfigure, cxt);
        cxt.setPrepareQueryResult(prepareQueryEngine.execute());

        QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt);
        DataSourceRouter.setEnable(queryConfigure.getSettings().isEnableDataSourceRoute());

        // 设置当前引擎查询默认数据源
        DataSourceRouter.setQueryEngineDefaultDataSource(engine);
        return engine;
    }


    /**
     * 获取查询结果集
     * @param engine
     * @return
     */
    public ResultDataSet getQueryDataSet(QueryEngine engine) {

        ResultDataSet resultDataSet = new ResultDataSet();

        try {
            ResponseMessage responseMessage = engine.execute();
            if (!responseMessage.getSuccess()) {
                throw new BIException(responseMessage.getMessage());
            }
            resultDataSet = (ResultDataSet) responseMessage.getData();

        } catch (Exception e) {
            e.printStackTrace();
            throw new BIException("查询多维数据异常" + e.getMessage());
        }

        return resultDataSet;
    }

    public List<AgentUserViewEntity> queryViewListByUserName(String userName) {
        List<AgentUserViewEntity> viewList = (List<AgentUserViewEntity>) dao.queryObjectList("ssm.agent.user.view.queryViewListByUserName", userName);
        if (viewList == null || viewList.isEmpty()) {
            return viewList;
        }
        List<String> viewIds = viewList.stream().map(AgentUserViewEntity::getViewId).collect(java.util.stream.Collectors.toList());
        List<AgentUserViewConfigEntity> configRows = (List<AgentUserViewConfigEntity>) dao.queryObjectList("ssm.agent.user.view.queryConfigByViewIds", viewIds);
        // 按 viewId 分组，再按 columnArea 分类
        java.util.Map<String, java.util.Map<String, List<AgentUserViewConfigItem>>> configMap = new java.util.HashMap<>();
        if (configRows != null) {
            for (AgentUserViewConfigEntity row : configRows) {
                configMap
                    .computeIfAbsent(row.getViewId(), k -> new java.util.HashMap<>())
                    .computeIfAbsent(row.getColumnArea(), k -> new java.util.ArrayList<>())
                    .add(new AgentUserViewConfigItem(row.getColumnName()));
            }
        }
        for (AgentUserViewEntity view : viewList) {
            java.util.Map<String, List<AgentUserViewConfigItem>> areaMap = configMap.getOrDefault(view.getViewId(), java.util.Collections.emptyMap());
            AgentUserViewConfigRsp rsp = new AgentUserViewConfigRsp();
            rsp.setViewDimensions(areaMap.getOrDefault("Dimension", java.util.Collections.emptyList()));
            rsp.setViewMetrics(areaMap.getOrDefault("Measure", java.util.Collections.emptyList()));
            rsp.setViewFilters(areaMap.getOrDefault("Filter", java.util.Collections.emptyList()));
            view.setConfig(rsp);
        }
        return viewList;
    }

    /**
     * 视图配置画像接口鉴权：校验 olap_api_key 及 x-username 与 api_key 绑定关系，不校验视图权限
     *
     * @param olapApiKey       请求头 olap_api_key
     * @param olapApiUserName  请求头 x-username
     * @param viewId           视图 ID
     * @return 解析后的域账号
     */
    public String validateViewConfigPortrait(String olapApiKey, String olapApiUserName, String viewId) {
        if (StrUtil.isEmpty(olapApiKey)) {
            throw new BIException("请求头中olap_api_key不能为空！");
        }
        if (StrUtil.isEmpty(viewId)) {
            throw new BIException("视图ID：viewId为必填项！");
        }
        if (StrUtil.isEmpty(olapApiUserName)) {
            throw new BIException("请求头中" + BIConsts.OLAP_API_USER_NAME + "不能为空！");
        }

        OlapApiKeyEntity olapApiKeyEntity = OlapApiManager.get(olapApiKey);
        if (olapApiKeyEntity == null) {
            olapApiKeyEntity = (OlapApiKeyEntity) dao.queryObject("ssm.olap.api.queryByApiKey", olapApiKey);
            if (olapApiKeyEntity != null) {
                OlapApiManager.put(olapApiKey, olapApiKeyEntity);
            }
        }
        if (olapApiKeyEntity == null) {
            throw new BIException(String.format("olap_api_key:%s不存在！", olapApiKey));
        }

        if (StrUtil.isNotEmpty(olapApiKeyEntity.getApiKeyPlatform())) {
            List<String> apiKeyPlatformList = Arrays.asList(olapApiKeyEntity.getApiKeyPlatform().split(","));
            if (!apiKeyPlatformList.contains("ssm")) {
                throw new BIException(String.format("olap_api_key:%s无调用多维API的权限！",
                        BIUtil.desensitizeGuid32(olapApiKey)));
            }
        }

        String userName = BIUtil.resolveUserName(olapApiUserName);
        if (StrUtil.isEmpty(userName)) {
            throw new BIException("请求头中" + BIConsts.OLAP_API_USER_NAME + "不能为空！");
        }

        List<String> apiKeyUserList = (List<String>) dao.queryObjectList("ssm.olap.api.queryUserListByApiKey",
                olapApiKey);
        if (!apiKeyUserList.contains(userName) && !apiKeyUserList.contains("all")) {
            throw new BIException(String.format("%s没有权限使用api_key:%s", userName,
                    BIUtil.desensitizeGuid32(olapApiKey)));
        }

        return userName;
    }

    /**
     * 获取视图的配置画像
     * @param viewId
     * @param userName
     * @return
     */
    public TemplateViewConfigPortraitRsp getViewConfigPortrait(String viewId,String userName) {
        //将用户信息注入当前线程中
        User user = userService.queryByName(userName);
        if (user == null) {
            throw new BIException("用户" + userName + "不存在！");
        }
        UserManager.set(user);
        TemplateViewService templateViewService = (TemplateViewService)SpringContextUtil.getBean("templateViewService");
        TemplateViewConfigPortraitRsp result = templateViewService.getViewConfigPortrait(viewId);
        UserManager.remove();
        return result;
    }

    /**
     * 监听视图更新事件，复用 syncViewConfig 逻辑
     */
    @EventListener
    public void onTemplateViewUpdated(TemplateViewUpdatedEvent event) {
        syncViewConfig(event.getViewId());
    }

    /**
     * 若 viewId 在 ssm_user_agent_view 中，则解析视图 tplConfig，
     * 按 Dimension / Measure / Filter 三个区域重建 ssm_user_agent_view_config。
     */
    public void syncViewConfig(String viewId) {
        if (StrUtil.isBlank(viewId)) {
            return;
        }
        // 检查 viewId 是否在 ssm_user_agent_view 中（is_active = 1）
        Integer count = (Integer) dao.queryObject("ssm.agent.user.view.existsViewInAgentView", viewId);
        if (count == null || count == 0) {
            return;
        }
        // 获取视图实体（含 tplConfig）
        TemplateViewService templateViewService = (TemplateViewService) SpringContextUtil.getBean("templateViewService");
        TemplateViewEntity viewEntity = templateViewService.getByViewId(viewId);
        if (viewEntity == null || StrUtil.isBlank(viewEntity.getTplConfig())) {
            return;
        }
        // 获取模板，构建字段 Map，格式化 config（与 getViewConfigPortrait 保持一致）
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("templateId", viewEntity.getTplId());
        SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);
        if (tpl == null) {
            return;
        }
        Map<String, MetaField> fieldCodeMap = new HashMap<>();
        Map<String, MetaField> fieldIdMap = new HashMap<>();
        QueryFieldService queryFieldService = (QueryFieldService) SpringContextUtil.getBean("queryFieldService");
        List<?> treeNodes = queryFieldService.buildFieldTree(CategoryType.Front, tpl.getDatasetId());
        SSDUtil.fetchFieldFromTreeNodes(treeNodes, fieldCodeMap, fieldIdMap);
        String normalizedConfig = SSDUtil.normalizeConfig(viewEntity.getTplConfig(), fieldCodeMap, fieldIdMap);

        UIQueryConfigure rawConfig = JSONObject.parseObject(normalizedConfig, UIQueryConfigure.class);
        if (rawConfig == null || rawConfig.getResult() == null) {
            return;
        }

        List<AgentUserViewConfigEntity> newConfigs = new ArrayList<>();

        // 维度（行维度 + 列维度）
        List<UIQueryField> allDimensions = new ArrayList<>();
        if (CollUtil.isNotEmpty(rawConfig.getResult().getRowDimensions())) {
            allDimensions.addAll(rawConfig.getResult().getRowDimensions());
        }
        if (CollUtil.isNotEmpty(rawConfig.getResult().getColDimensions())) {
            allDimensions.addAll(rawConfig.getResult().getColDimensions());
        }
        for (UIQueryField f : allDimensions) {
            if (f == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(f.getCode())) {
                continue;
            }
            String title = StrUtil.isNotEmpty(f.getDisplayTitle()) ? f.getDisplayTitle() : f.getTitle();
            if (StrUtil.isBlank(title)) {
                continue;
            }
            AgentUserViewConfigEntity cfg = new AgentUserViewConfigEntity();
            cfg.setPkid(Guid.id());
            cfg.setViewId(viewId);
            cfg.setColumnName(title.trim());
            cfg.setColumnArea("Dimension");
            newConfigs.add(cfg);
        }

        // 指标
        if (CollUtil.isNotEmpty(rawConfig.getResult().getMeasures())) {
            for (UIQueryField f : rawConfig.getResult().getMeasures()) {
                if (f == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(f.getCode())) {
                    continue;
                }
                String title = StrUtil.isNotEmpty(f.getDisplayTitle()) ? f.getDisplayTitle() : f.getTitle();
                if (StrUtil.isBlank(title)) {
                    continue;
                }
                AgentUserViewConfigEntity cfg = new AgentUserViewConfigEntity();
                cfg.setPkid(Guid.id());
                cfg.setViewId(viewId);
                cfg.setColumnName(title.trim());
                cfg.setColumnArea("Measure");
                newConfigs.add(cfg);
            }
        }

        // 筛选
        if (CollUtil.isNotEmpty(rawConfig.getFilter())) {
            for (UIQueryField f : rawConfig.getFilter()) {
                if (f == null || f.isCommonDate()) {
                    continue;
                }
                String title = StrUtil.isNotEmpty(f.getDisplayTitle()) ? f.getDisplayTitle() : f.getTitle();
                if (StrUtil.isBlank(title)) {
                    continue;
                }
                AgentUserViewConfigEntity cfg = new AgentUserViewConfigEntity();
                cfg.setPkid(Guid.id());
                cfg.setViewId(viewId);
                cfg.setColumnName(title.trim());
                cfg.setColumnArea("Filter");
                newConfigs.add(cfg);
            }
        }

        if (newConfigs.isEmpty()) {
            return;
        }
        // 重建：先删后插
        dao.delete("ssm.agent.user.view.deleteConfigByViewId", viewId);
        dao.insert("ssm.agent.user.view.batchInsertConfig", newConfigs);
    }

    /**
     * csv字符串转文件并上传到oss
     * @param csvContent
     * @return
     * @throws IOException
     */
    protected String csvToFileAndUpload(String csvContent, boolean isZip) {
        if(BIUtil.isEmpty(csvContent)){
            return "";
        }
        //进行zip压缩
        String fileName = Guid.id();
        String csvDatasetUrl = "";
        if(isZip) {
            byte[] csvToZip = null;
            try {
                csvToZip = TextToZipUtil.toZip(csvContent, fileName + ".csv");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            csvDatasetUrl = OssUtil.upload(csvToZip, fileName, ".zip", "bigdata/agent_new/dataset");
        }else {
            csvDatasetUrl = OssUtil.upload(csvContent.getBytes(StandardCharsets.UTF_8), fileName, ".csv", "bigdata/agent_new/dataset");
        }
        return csvDatasetUrl;
    }

}
