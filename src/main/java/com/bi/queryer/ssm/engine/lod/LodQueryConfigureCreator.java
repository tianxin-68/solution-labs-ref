package com.bi.queryer.ssm.engine.lod;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalAggConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryFilter;
import com.bi.queryer.ssm.engine.config.QueryResult;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.lod.calc.LodCalcManager;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:54 2023-12-18
 * @Description lod查询配置构建器：将前端过来的查询配置构造为lod引擎可用的查询配置
 **/
public class LodQueryConfigureCreator {
    private QueryConfigure config ;

    private QueryContext cxt;

    private LodQueryConfigure lodQueryConfig ;

    // 所有lod字段（含lod二次计算字段）
    private List<QueryField> lodFields;

    // lod指标前端传入的维度
    private Map<String, List<QueryField>> lodSupportDimMap = new HashMap<>();

    // 所有lod的二次计算字段
    private List<QueryField> lodCalcFields ;

    private LodCalcManager lodCalcMgr = null;

    public LodQueryConfigureCreator(QueryConfigure config, QueryContext cxt) {
        this.config = config;
        this.cxt = cxt;
    }

    /**
     * 将lod字段拆分出来并按其维度相同进行合并
     * @return LodQueryConfigure
     */
    public LodQueryConfigure create(){
        lodQueryConfig = new LodQueryConfigure(config);

        // 从主配置中识别出含分析指标计算字段
        List<QueryField> measures = new ArrayList<>(config.getResult().getMeasures().size());
        List<QueryField> analysisCalcFields = new ArrayList<>(8);
        for (QueryField f : config.getResult().getMeasures()) {
            if (f.isAnalysisCalc()) {
                analysisCalcFields.add(f);
            } else {
                measures.add(f);
            }
        }
        lodQueryConfig.setAnalysisCalcFields(analysisCalcFields);

        lodFields = measures.stream().filter(f-> f.getCustomFieldConfigure() != null)
                .filter(f-> CustomFieldType.isLod(f.getCustomFieldConfigure().getType())).collect(Collectors.toList());

        //对lod字段的维度进行标准化
        normalizeLodFieldDimList();

        lodCalcFields = lodFields.stream()
                        .filter(f-> CustomFieldType.get(f.getCustomFieldConfigure().getType()) == CustomFieldType.LOD_CALC)
                        .collect(Collectors.toList());

        lodCalcMgr = new LodCalcManager(lodQueryConfig.getRawConfig(), lodCalcFields);

        /**主视图配置*/
        LodQueryConfigureItem mainItem = this.createMainLodQueryConfigureItem(analysisCalcFields);
        lodQueryConfig.add(mainItem);

        lodCalcMgr.setMainItem(mainItem);

        /**子视图配置*/
        int index = 0;
        for(QueryField lodField : lodFields){
            if(CustomFieldType.LOD_CALC == CustomFieldType.get(lodField.getCustomFieldConfigure().getType())){
                // 参与四则运算的lod字段不需要构建配置项
                continue;
            }
            index++;
            LodQueryConfigureItem subItem = this.createSubLodQueryConfigureItem(mainItem, lodField, LodConsts.CONFIG_CODE_PREFIX + index);
            lodQueryConfig.add(subItem);
        }

        /**还原*/
        lodCalcMgr.restore();

        return lodQueryConfig;
    }

    /**
     * 对lod字段的维度进行标准化
     *  1 维度按主视图维度排序
     *  2 如果选择自动支持模版中的维度，则需要去掉模版中隐藏的字段
     */
    public void normalizeLodFieldDimList() {

        List<QueryField> mainDimFieldList = new ArrayList<>();
        mainDimFieldList.addAll(this.config.getResult().getRowDimensions());
        mainDimFieldList.addAll(this.config.getResult().getColDimensions());

        for (QueryField lodField : lodFields) {
            LodUIConfigure lodConfig = lodField.getCustomFieldConfigure().getLodConfig();
            List<QueryField> lodDimensionList = new ArrayList<>();
            List<QueryField> rawLodDimensionList = lodConfig.getDimensionList();
            lodSupportDimMap.computeIfAbsent(lodField.getCode(), (k) -> new ArrayList<>()).addAll(rawLodDimensionList);
            for (QueryField qf : mainDimFieldList) {

                if (!Enabled.value(qf.getIsShow())) {
                    continue;
                }

                //计算维度，没有传递code，需要通过id判断
                if (qf.isCalc()) {
                    if (rawLodDimensionList.stream().anyMatch(f -> f.getId().equals(qf.getId()))) {
                        lodDimensionList.add(qf.clone());
                    }
                } else {
                    if (rawLodDimensionList.contains(qf)) {
                        lodDimensionList.add(qf.clone());
                    }
                }
            }

            //将计算维度的原子字段 克隆，避免导出死循环
            for (QueryField qf : lodDimensionList) {
                if (CollUtil.isEmpty(qf.getCalcAtomFields())) {
                    continue;
                }

                Set<QueryField> calcAtomFields = new HashSet<>();
                for (QueryField calcAtomField : qf.getCalcAtomFields()) {
                    calcAtomFields.add(calcAtomField.clone());
                }

                qf.setCalcAtomFields(calcAtomFields);
            }

            lodConfig.setDimensionList(lodDimensionList);
        }
    }

    /**
     * 创建主视图查询配置
     * @return
     */
    protected LodQueryConfigureItem createMainLodQueryConfigureItem(List<QueryField> analysisCalcFields){
        QueryConfigure mainConfig = this.config.clone();
        mainConfig.load();

        boolean isAppend = lodCalcMgr.setLodCalcAnalysisConfigs(mainConfig, true);
        if(isAppend){
            mainConfig.post();
        }

        // 从主配置中移除lod字段
        mainConfig.getResult().remove(lodFields, QueryArea.Measure);
        mainConfig.getResult().remove(analysisCalcFields, QueryArea.Measure);

        // 移除lod的同环比指标
        List<QueryField> measures = mainConfig.getResult().getMeasures();
        List<QueryField> lodAnalysisFields = new ArrayList<>();
        for(QueryField measure : measures){
            AnalysisItemConfig itemConfig = measure.getAnalysisConfig();
            if(itemConfig != null){
                for(QueryField lodField : lodFields){
                    if(lodField.getId().equals(itemConfig.getMeasureId())){
                        lodAnalysisFields.add(measure);
                    }
                }
            }
        }
        mainConfig.getResult().remove(lodAnalysisFields, QueryArea.Measure);

        // 设置排序模式为row_number
        mainConfig.getSettings().setSortMode(SortMode.ROW_NUMBER);
        // 继承是否排序
        mainConfig.getSettings().setNeedSort(config.getSettings().getNeedSort());
        mainConfig.getSettings().setEnableCreateAllTableBySameCodeOpt(this.config.getSettings().isEnableCreateAllTableBySameCodeOpt());

        LodQueryConfigureItem mainItem = new LodQueryConfigureItem(LodConsts.CONFIG_CODE_MAIN, mainConfig);

        return mainItem;
    }

    /**
     * 创建子视图查询配置
     * @return
     */
    protected LodQueryConfigureItem createSubLodQueryConfigureItem(LodQueryConfigureItem mainItem, QueryField lodField, String lodCode){
        QueryConfigure queryConfigure = this.createQueryConfigure(lodField);
        LodQueryConfigureItem subItem = new LodQueryConfigureItem(lodCode, queryConfigure);
        subItem.setLodField(lodField);
        this.setAggByDimension(mainItem, subItem);
        // 设置最外层聚合方式：默认、日均
        subItem.setAggExpressionType(AggExpressionType.get(lodField.getAggExpressionType()));

        // 子视图不需要排序
        queryConfigure.getSettings().setNeedSort(false);

        // 设置排序模式为row_number
        queryConfigure.getSettings().setSortMode(SortMode.ROW_NUMBER);

        return subItem;
    }

    protected QueryConfigure createQueryConfigure(QueryField lodField) {
        QueryConfigure subConfig = this.config.clone();
        subConfig.load();

        // 结果
        QueryResult queryResult = subConfig.getResult();
        queryResult.getFields().clear();
        queryResult.getRowDimensions().clear();
        queryResult.getColDimensions().clear();
        queryResult.getMeasures().clear();

        // 过滤：只保留维度，去掉指标过滤
        QueryFilter queryFilter = subConfig.getFilter();
        List<QueryField> filterFields = queryFilter.getFields().stream().filter(f-> f.isDimension()).collect(Collectors.toList());
        setLodDateRange(lodField, filterFields);
        queryFilter.setFields(filterFields);

        subConfig.setSettings(this.config.getSettings().clone());

        QueryField mainCommonDateField = this.config.getResultCommonDateField();

        // lod配置
        LodUIConfigure lodConfigure = lodField.getCustomFieldConfigure().getLodConfig();

        //列维度
        List<QueryField> colFields = this.config.getResult().getColDimensions();

        // 添加维度
        List<QueryField> lodDimensions = lodConfigure.getDimensionList();
        for(QueryField dim : lodDimensions){
            QueryField lodDimension = dim.clone();
            lodDimension.init();
            lodDimension.setIsResult(true);

            QueryArea dimQueryArea = QueryArea.RowDimension;
            if(colFields.contains(dim)){
                dimQueryArea = QueryArea.ColumnDimension;
            }

            lodDimension.setQueryArea(dimQueryArea);
            lodDimension.setRawQueryArea(dimQueryArea);

            // 若主无日期子有公共日期时，子公共日期无效
            if(lodDimension.isCommonDate() && mainCommonDateField == null){
                continue;
            }

            // 若是公共日期，需设置其日期粒度
            if(lodDimension.isCommonDate() && mainCommonDateField != null){
                if(DateGranularity.get(lodConfigure.getDateGranularity()) == DateGranularity.AUTO) {
                    lodDimension.setQueryDateGranularity(mainCommonDateField.getQueryDateGranularity());
                }else {
                    lodDimension.setQueryDateGranularity(lodConfigure.getDateGranularity());
                }
                lodDimension.setIsAggQuery(mainCommonDateField.getIsAggQuery());
            }

            queryResult.add(lodDimension, dimQueryArea);
        }

        // 添加指标
        lodConfigure = lodField.getCustomFieldConfigure().getLodConfig();
        MetaField meta = SSDMetaCacheManager.getField(lodConfigure.getMeasureId());
        if(meta != null){
            QueryField measureField = lodField.clone();
            CustomFieldConfigure customFieldConfigure = measureField.getCustomFieldConfigure();
            customFieldConfigure.setLodConfig(new LodUIConfigure());
            customFieldConfigure.setType(CustomFieldType.COMMON.getCode());
            String expression = String.format("[%s]", meta.getId());
            customFieldConfigure.setExpression(expression);
            List<CustomFieldExpressionIdMapping> expressionIdMapping = new ArrayList<>();
            CustomFieldExpressionIdMapping mapping = new CustomFieldExpressionIdMapping();
            mapping.setId(meta.getId());
            mapping.setTitle(meta.getTitle());
            expressionIdMapping.add(mapping);

            customFieldConfigure.setExpressionIdMapping(expressionIdMapping);

            //customFieldConfigure.setDecimal(FieldDataType.getType(meta.getDataType()) == FieldDataType.Integer ? 0 : 2);
            customFieldConfigure.setDecimal(FieldUtil.getPrecision(meta));
            customFieldConfigure.setRatio(meta.getShowFormatExpression().contains("%"));

            measureField.init();
            measureField.setIsResult(true);
            measureField.setQueryArea(QueryArea.Measure);
            measureField.setRawQueryArea(QueryArea.Measure);

            // 此处不做默认值设置：默认值在最后根据维度的匹配关系设置
            //measureField.setAggExpressionType(lodConfigure.getAggExpressionType());

            queryResult.add(measureField, QueryArea.Measure);

            // 必须添加附加字段
            queryResult.addAdditionalField();
        }
        // 设置生效的过滤条件
        setValidFilters(subConfig, lodSupportDimMap.get(lodField.getCode()));

        //lod贡献度设置
        setLodCtrConfig(subConfig,lodField);

        // lod计算字段设置附加分析配置
        lodCalcMgr.setLodCalcAnalysisConfigs(subConfig, false);

        subConfig.getSettings().setIsLodQuery(Enabled.YES.getId());

        // 调用post:添加分析相关字段
        subConfig.post();

        // 添加分析字段code添加到acl中
        List<QueryField> allFields = subConfig.getAllFields();
        Map<String, String> aclCodes = this.cxt.getAclFields();
        for(QueryField qf : allFields){
            if(Enabled.isTrue(qf.getIsAnalysis())
                    && aclCodes.containsKey(qf.getAnalysisConfig().getMeasureCode())
                    && !aclCodes.containsKey(qf.getCode())){
                aclCodes.put(qf.getCode(), qf.getCode());
            }
        }

        return subConfig;
    }

    /**
     * 如果lod配置日期范围不为空，则改写过滤条件中的时间范围
     * @param lodField
     * @param filterFields
     */
    public void setLodDateRange(QueryField lodField,List<QueryField> filterFields){

        List<String> dateRange = lodField.getCustomFieldConfigure().getLodConfig().getDateRange();
        if(CollUtil.isNotEmpty(dateRange)) {

            if (dateRange.size() == 2) {
                for (QueryField field : filterFields) {
                    if (!field.isCommonDate()) {
                        continue;
                    }

                    if (CollUtil.isEmpty(field.getValues())) {
                        continue;
                    }

                    if (field.getValues().size() != 2) {
                        continue;
                    }

                    field.getValues().get(0).setId(dateRange.get(0));
                    field.getValues().get(1).setId(dateRange.get(1));
                }
            }
        }
    }

    // lod贡献度设置，需要设置实际值
    public void setLodCtrConfig(QueryConfigure subConfig,QueryField lodField) {

        // 同环比
        AnalysisTotalAggConfig totalAggConfig = subConfig.getAnalysis().getTotal().getAggConfig();
        AnalysisThbConfig thbConfig = subConfig.getAnalysis().getThb();
        if (thbConfig != null && thbConfig.isActive()) {
            List<AnalysisThbItemConfig> thbItemConfigs = thbConfig.getItems();

            for (AnalysisThbItemConfig itemConfig : thbItemConfigs) {
                List<String> measureIdList = itemConfig.getMeasureIdList();

                //兼容出占比同环比，不出原始同环比的场景
                if(CollUtil.isNotEmpty(itemConfig.getZbThbConfigs())){
                    for(AnalysisZbThbConfig zbThbConfig : itemConfig.getZbThbConfigs()){
                        measureIdList.add(zbThbConfig.getMeasureId());
                    }
                }

                if (BIUtil.isEmpty(measureIdList)) {
                    continue;
                }
                // lod计算字段配置了同环比，但原子字段不在分析配置中，则添加其相关配置
                if (measureIdList.contains(BIConsts.ALL_MEASURE) || measureIdList.contains(lodField.getId())) {

                    //配置了贡献率，没出实际值
                    if (itemConfig.getCalcTypes().contains(AnalysisCalcType.CONTRIBUTION_RATE.getCode())) {
                        if (!itemConfig.getCalcTypes().contains(AnalysisCalcType.REAL_VALUE.getCode())) {
                            // lod计算字段配置了同环比，但计算类型没有实际值，则添加其相关配置
                            itemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());

                        }
                    } else if (CollUtil.isNotEmpty(itemConfig.getZbThbConfigs())) {
                        // 配置了占比同环比,没有配置实际值
                        if (!itemConfig.getCalcTypes().contains(AnalysisCalcType.REAL_VALUE.getCode())) {
                            // lod计算字段配置了同环比，但计算类型没有实际值，则添加其相关配置
                            itemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());

                        }
                    } else if (totalAggConfig.isActive()) {
                        // 配置了自定义的聚合方式,没有配置实际值
                        if (!itemConfig.getCalcTypes().contains(AnalysisCalcType.REAL_VALUE.getCode())) {
                            // lod计算字段配置了同环比，但计算类型没有实际值，则添加其相关配置
                            itemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());

                        }
                    }
                }

            }

        }

        // 自定义对比
        AnalysisCompareConfig compareConfig = subConfig.getAnalysis().getCompare();
        if (compareConfig != null && compareConfig.isActive()) {
            List<AnalysisCompareItemConfig> compareItemConfigs = compareConfig.getItems();
            for (AnalysisCompareItemConfig itemConfig : compareItemConfigs) {
                List<String> measureIdList = itemConfig.getMeasureIdList();
                if (BIUtil.isEmpty(measureIdList)) {
                    continue;
                }
                // lod计算字段配置了自定义对比，但原子字段不在分析配置中，则添加其相关配置
                if (measureIdList.contains(BIConsts.ALL_MEASURE) || measureIdList.contains(lodField.getId())) {

                    //配置了贡献率，没出实际值
                    if (itemConfig.getCalcTypes().contains(AnalysisCalcType.CONTRIBUTION_RATE.getCode())) {
                        if (!itemConfig.getCalcTypes().contains(AnalysisCalcType.REAL_VALUE.getCode())) {
                            // lod计算字段配置了同环比，但计算类型没有实际值，则添加其相关配置
                            itemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());

                        }
                    } else if (CollUtil.isNotEmpty(itemConfig.getZbThbConfigs())) {
                        // 配置了占比自定义对比,没有配置实际值
                        if (!itemConfig.getCalcTypes().contains(AnalysisCalcType.REAL_VALUE.getCode())) {
                            // lod计算字段配置了同环比，但计算类型没有实际值，则添加其相关配置
                            itemConfig.getCalcTypes().add(AnalysisCalcType.REAL_VALUE.getCode());

                        }
                    }
                }

            }
        }

    }

    /**
     * 设置只对lod生效的过滤条件
     * @param subConfig
     */
    private void setValidFilters(QueryConfigure subConfig, List<QueryField> filterScopes) {
        // 全局过滤条件, 即表格原本过滤条件
        QueryFilter queryFilter = subConfig.getGlobalFilter();
        List<QueryField> filterFields = subConfig.getFilter().getFields();
        if (queryFilter == null || BIUtil.isEmpty(queryFilter.getFields())) {
            // 表格lod查询
            queryFilter = new QueryFilter();
        } else {
            // 表头和趋势图查询
            List<QueryField> globalFilterFields = queryFilter.getFields();
            List<String> lodDimFields = new ArrayList<>();
            if (filterScopes != null) {
                for (QueryField qf : filterScopes) {
                    if (!Enabled.YES.getId().equals(qf.getIsShow())) {
                        // 不展示不生效
                        continue;
                    }
                    if (StringUtils.isNotEmpty(qf.getId())) {
                        lodDimFields.add(qf.getId());
                    }
                    if (StringUtils.isNotEmpty(qf.getCode())) {
                        lodDimFields.add(qf.getCode());
                    }
                }
            }
            filterFields = filterFields.stream()
                    .filter(v -> globalFilterFields.contains(v) || lodDimFields.contains(v.getId()) || lodDimFields.contains(v.getCode()))
                    .collect(Collectors.toList());
        }
        // lod视图筛选只对lod配置的维度生效
        List<String> lodDimScopes;
        if (filterScopes == null) {
            lodDimScopes = null;
        } else {
            lodDimScopes = new ArrayList<>();
            for (QueryField qf : filterScopes) {
                if (StringUtils.isNotEmpty(qf.getId())) {
                    lodDimScopes.add(qf.getId());
                }
                if (StringUtils.isNotEmpty(qf.getCode())) {
                    lodDimScopes.add(qf.getCode());
                }
            }
        }

        //非agent的查询，过滤lod支持的维度
        //agent的查询暂时没有构造适配的维度集合
        if(!Enabled.value(subConfig.getSettings().getIsAgentQuery()) ){
            filterFields = filterFields.stream().filter(v -> lodDimScopes == null || lodDimScopes.contains(v.getId()) || lodDimScopes.contains(v.getCode()))
                    .collect(Collectors.toList());
        }

        //lod字段+指标筛选查询异常，此处将指标过滤排除
        filterFields = filterFields.stream().filter(QueryField::isDimension).collect(Collectors.toList());
        queryFilter.setFields(filterFields);
        subConfig.setFilter(queryFilter);
    }

    /**
     * 通过维度信息设置字段或查询配置项的聚合相关信息
     * 与主配置对比
     * - 若主配置查询维度包含子配置查询维度，则子配置维度不用再次聚合，否则子配置需按相同维度再次聚合
     * @param mainItem
     * @param subItem
     */
    protected void setAggByDimension(LodQueryConfigureItem mainItem, LodQueryConfigureItem subItem){
        // 主视图
        List<QueryField> mainDimensions = mainItem.getConfig().getResult().getRowDimensions();
        mainDimensions.addAll(mainItem.getConfig().getResult().getColDimensions());

        // 子视图
        List<QueryField> subDimensions = subItem.getConfig().getResult().getRowDimensions();
        subDimensions.addAll(subItem.getConfig().getResult().getColDimensions());

        // 若主维度包含lod维度且日期粒度一致：则lod不需再次聚合
        if(mainDimensions.containsAll(subDimensions)) {
            subItem.setAggToMainLevel(false);
            subItem.getAggToMainLevelDimensions().addAll(subDimensions);
        }else {
            List<QueryField> sameDimensions = subDimensions.stream()
                    .filter(f -> mainDimensions.contains(f))
                    .collect(Collectors.toList());
            subItem.getAggToMainLevelDimensions().addAll(sameDimensions);
            subItem.setAggToMainLevel(true);
        }

        // 若无任何交叉维度，默认子视图中指标二次聚合方式为sum否则avg
        if(BIUtil.isNotEmpty(subItem.getConfig().getResult().getMeasures())) {
            QueryField measureField = subItem.getConfig().getResult().getMeasures().get(0);
            if(BIUtil.isNotEmpty(measureField.getAggExpressionType())){
                return;
            }
            String avgType = BIUtil.isEmpty(subItem.getAggToMainLevelDimensions()) ? AggExpressionType.Sum.getCode() : AggExpressionType.Avg.getCode();
            measureField.setAggExpressionType(avgType);
        }
    }

    public LodCalcManager getLodCalcMgr() {
        return lodCalcMgr;
    }

    public void setLodCalcMgr(LodCalcManager lodCalcMgr) {
        this.lodCalcMgr = lodCalcMgr;
    }
}
