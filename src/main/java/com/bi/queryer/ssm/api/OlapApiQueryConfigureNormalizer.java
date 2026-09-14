package com.bi.queryer.ssm.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.enums.FilterType;
import com.bi.queryer.ssm.api.enums.OlapApiCompareCalcContentType;
import com.bi.queryer.ssm.api.entity.OlapApiKeyEntity;
import com.bi.queryer.ssm.api.vo.req.QueryOlapDataAnalysisCompareReq;
import com.bi.queryer.ssm.api.vo.req.QueryOlapDataQueryDimensionReq;
import com.bi.queryer.ssm.api.vo.req.QueryOlapDataQueryMetricReq;
import com.bi.queryer.ssm.api.vo.req.QueryOlapDataQueryFilterReq;
import com.bi.queryer.ssm.api.vo.req.QueryOlapDataReq;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.initializer.AnalysisMeasureInitializer;
import com.bi.queryer.ssm.engine.chart.ChartQueryConfigureBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.CustomIdGenerator;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 14:24 2026/4/28
 * @Description TODO
 **/
public class OlapApiQueryConfigureNormalizer {
    private QueryOlapDataReq queryOlapDataReq ;
    private String config ;
    private String olapApiKey ;
    private String hostname ;
    public OlapApiQueryConfigureNormalizer(String config, QueryOlapDataReq queryOlapDataReq, String olapApiKey, String hostname){
        this.config = config;
        this.queryOlapDataReq = queryOlapDataReq;
        this.olapApiKey = olapApiKey;
        this.hostname = hostname;
    }

    /**
     * 参数替换
     * @return
     */
    public UIQueryConfigure normalize(){

        UIQueryConfigure uiQueryConfigure = JSONObject.parseObject(config, UIQueryConfigure.class);

        applyQueryDimensionsVisibility(uiQueryConfigure, queryOlapDataReq);
        applyQueryMetricsVisibility(uiQueryConfigure, queryOlapDataReq);

        //修改过滤时间字段
        for(UIQueryField uiQueryField : uiQueryConfigure.getFilter()){
            if(!uiQueryField.isCommonDate()){
                continue;
            }

            //queryDate入参为空不处理
            if(!queryOlapDataReq.hasQueryDate()) {
                continue;
            }

            String granularity = queryOlapDataReq.getQueryDate().getGranularity();
            // 周/月粒度下游按 yyyyWW、yyyyMM（均为6位无横线）解析（WeekDateUtil按下标substring、DateUtil.parse按该pattern），
            // 若values带横线（如2024-26、2024-01）会导致substring错位或parse失败，此处需去除横线
            boolean stripDash = DateGranularity.WEEK.getCode().equalsIgnoreCase(granularity)
                    || DateGranularity.MONTH.getCode().equalsIgnoreCase(granularity);

            List<FieldValue> valueList = new ArrayList<>();
            for(String value : queryOlapDataReq.getQueryDate().getValues()){
                String normalizedValue = stripDash && StrUtil.isNotEmpty(value) ? value.replace("-", "") : value;
                FieldValue fieldValue = new FieldValue();
                fieldValue.setId(normalizedValue);
                fieldValue.setTitle(normalizedValue);
                fieldValue.setFilterShowType(FieldFilterType.DateRange.getCode());
                valueList.add(fieldValue);
            }
            uiQueryField.setValues(valueList);
            uiQueryField.setQueryDateGranularity(granularity);
        }

        //修改筛选条件
        if(CollUtil.isNotEmpty(queryOlapDataReq.getFilters())) {

            for (QueryOlapDataQueryFilterReq queryOlapDataQueryFilterReq : queryOlapDataReq.getFilters()) {
                if (CollUtil.isEmpty(queryOlapDataQueryFilterReq.getValues())) {
                    continue;
                }

                UIQueryField uiQueryField = null;

                //在查询条件里，如果字段不存在，则忽略
                Optional<UIQueryField> optionalUIQueryField = uiQueryConfigure.getFilter().stream()
                        .filter(uiField -> uiField.getTitle().equals(queryOlapDataQueryFilterReq.getFieldName())
                                ||
                                uiField.getDisplayTitle().equals(queryOlapDataQueryFilterReq.getFieldName())
                        )
                        .findAny();
                if (optionalUIQueryField.isPresent()) {
                    uiQueryField = optionalUIQueryField.get();
                }

                //在查询条件中不存在，从维度中再查找一次
                if (uiQueryField == null) {
                    Optional<UIQueryField> queryFieldOptional = uiQueryConfigure.getResult().getAllFields().stream()
                            .filter(qf -> qf.getTitle().equalsIgnoreCase(queryOlapDataQueryFilterReq.getFieldName())
                                    ||
                                    qf.getDisplayTitle().equalsIgnoreCase(queryOlapDataQueryFilterReq.getFieldName())
                            )
                            .findAny();

                    //构建新的查询过滤字段
                    if (queryFieldOptional.isPresent()) {
                        uiQueryField = JSONObject.parseObject(JSON.toJSONString(queryFieldOptional.get()), UIQueryField.class);
                        applyOlapApiFilterValueTypeForNewFilter(uiQueryField, queryOlapDataQueryFilterReq.getFilterType());
                        uiQueryConfigure.getFilter().add(uiQueryField);
                    }

                }

                if (uiQueryField == null) {
                    continue;
                }

                applyOlapApiFilterValueType(uiQueryField, queryOlapDataQueryFilterReq.getFilterType());

                //公共日期字段在此处不处理
                if (uiQueryField.isCommonDate()) {
                    continue;
                }

                List<FieldValue> valueList = new ArrayList<>();
                for (Object value : queryOlapDataQueryFilterReq.getValues()) {
                    String valueStr = BIUtil.nvl(value, "");
                    FieldValue fieldValue = new FieldValue();
                    fieldValue.setId(valueStr);
                    fieldValue.setTitle(valueStr);
                    valueList.add(fieldValue);
                }

                //部分场景，表格的值与维度下拉筛选的值不一致。例如：布尔筛选
                MetaField metaField = SSDMetaCacheManager.getField(uiQueryField.getId());
                if (metaField != null) {
                    QueryField queryField = new QueryField(metaField);
                    queryField.setValues(valueList);
                    valueList = ChartQueryConfigureBuilder.transferFilterValues(queryField, valueList);
                }

                uiQueryField.setValues(valueList);
            }
        }

        //修改时间聚合
        String aggregate = queryOlapDataReq.getQueryDate().getAggregate();
        if ("true".equals(aggregate) || "false".equals(aggregate)) {
            int isAggQuery = "true".equals(aggregate) ? 1 : 0;
            setQueryDateAggregate(uiQueryConfigure, isAggQuery);
        }

        //如果入参的时间粒度为空，则使用默认时间粒度
        //此处将入参中的时间粒度设置为默认时间粒度，便于后续分析配置的修改及setting的修改使用
        if(queryOlapDataReq.getQueryDate().isEmpty()){
            queryOlapDataReq.getQueryDate().setGranularity(uiQueryConfigure.getSetting().getDateGranularity());
        }

        // 修改分析配置
        this.normalizeAnalysisConfig(uiQueryConfigure);

        uiQueryConfigure.getSetting().setDateGranularity(queryOlapDataReq.getQueryDate().getGranularity());
        uiQueryConfigure.getSetting().setOlapApiKey(olapApiKey);
        // 将 key 是否系统级写入 setting，供 QuerySessionManager 判断是否加分布式限流锁
        OlapApiKeyEntity olapApiKeyEntity = OlapApiManager.get(olapApiKey);
        if (olapApiKeyEntity != null && Enabled.value(olapApiKeyEntity.getIsSystem())) {
            uiQueryConfigure.getSetting().setIsSystemOlapApiKey(Enabled.YES.getId());
        } else {
            uiQueryConfigure.getSetting().setIsSystemOlapApiKey(Enabled.NO.getId());
        }
        uiQueryConfigure.getSetting().setHostname(hostname);
        uiQueryConfigure.getSetting().setResponseFormat(QueryResponseFormat.MAP.getCode());
        if (queryOlapDataReq.getTopN() != null && queryOlapDataReq.getTopN() > 0) {
            uiQueryConfigure.getSetting().setApiQueryRowLimit(queryOlapDataReq.getTopN());
        }
        uiQueryConfigure.setSessionId(BIConsts.OLAP_API_SESSION_ID_PREFIX+ CustomIdGenerator.generateRandomString(21));

        //String newConfigString = JSONObject.toJSONString(uiQueryConfigure);

        //return newConfigString;
        return uiQueryConfigure;
    }

    /**
     * 标准化分析配置：同环比配置
     * 将请求的分析内容覆盖现在模版已有
     * - 根据日期粒度不同匹配对应的同环比计算方式，避免日期粒度和计算方式不一致
     * - 若有同环比的二次计算字段，需要判断当前请求的同环比字段是否可满足计算字段的表达式
     * - 默认值处理：同环比的指标没传，默认=ALL，计算值没传，默认=差异率
     * @param uiQueryConfigure
     */
    public void normalizeAnalysisConfig(UIQueryConfigure uiQueryConfigure) {
        if (!queryOlapDataReq.hasAnalysis()) {
            return;
        }

        QueryOlapDataAnalysisCompareReq compareReq = this.queryOlapDataReq.getAnalysis().getCompare();

        // 按请求创建同环比配置
        AnalysisThbConfig newTbhConfig = new AnalysisThbConfig();
        newTbhConfig.setIsActive(Enabled.YES.getId());
        AnalysisThbItemConfig newThbItemConfig = new AnalysisThbItemConfig();
        // 指标列表
        List<UIQueryField> baseMeasuresForAnalysis = uiQueryConfigure.getResult().getMeasures().stream()
                .filter(m -> !m.isAnalysisCalc())
                .collect(Collectors.toList());
        if (BIUtil.isEmpty(compareReq.getMeasures()) || compareReq.getMeasures().contains(BIConsts.ALL_MEASURE)) {
            newThbItemConfig.getMeasureIdList().add(BIConsts.ALL_MEASURE);
        } else {
            Set<String> reqMeasureSet = compareReq.getMeasures().stream().filter(StrUtil::isNotBlank).map(String::trim).collect(Collectors.toSet());
            baseMeasuresForAnalysis = baseMeasuresForAnalysis.stream()
                    .filter(m -> CollectionUtils.containsAny(reqMeasureSet, m.getTitle(), m.getDisplayTitle()))
                    .collect(Collectors.toList());
            newThbItemConfig.getMeasureIdList().addAll(baseMeasuresForAnalysis.stream().map(UIQueryField::getId).collect(Collectors.toList()));
        }

        // 计算方式
        List<String> calcModeList = new ArrayList<>(compareReq.getCalcModes());
        // 校验日期粒度与同环比的匹配关系
        DateGranularity dateGranularity = DateGranularity.get(queryOlapDataReq.getQueryDate().getGranularity());
        List<String> enableCalcModeList = this.getThbCalcMode(dateGranularity);
        calcModeList.retainAll(enableCalcModeList);

        List<String> unsupportedCalcModeList = compareReq.getCalcModes().stream().filter(calcMode -> !enableCalcModeList.contains(calcMode)).collect(Collectors.toList());
        if (BIUtil.isNotEmpty(unsupportedCalcModeList)) {
            throw new BIException("你传入的时间粒度为: " + dateGranularity.getCode() + ", 当前时间粒度不支持同环比计算方式:" + StringUtils.join(unsupportedCalcModeList, ","));
        }

        newThbItemConfig.setCalcModes(calcModeList);

        // 计算内容
        List<String> calcContents = compareReq.getCalcContents();
        calcContents = BIUtil.isEmpty(calcContents) ? ListUtil.of(OlapApiCompareCalcContentType.DIFF_RATIO.toString()) : calcContents;
        Set<String> calcTypeCodes = new HashSet<>();
        for (String calcContent : calcContents) {
            OlapApiCompareCalcContentType compareCalcContentType = OlapApiCompareCalcContentType.get(calcContent);
            // 设置计算内容
            calcTypeCodes.add(compareCalcContentType.getCalcType().getCode());
        }

        newThbItemConfig.getCalcTypes().addAll(calcTypeCodes);
        newTbhConfig.getItems().add(newThbItemConfig);

        uiQueryConfigure.getAnalysis().setThb(newTbhConfig);
        // 若有同环比配置，需要判断同环比字段是否参与了四则运算：依赖的分析指标须在本次查询的同环比组合内，否则移除该四则指标
        List<UIQueryField> analysisCalcFields = uiQueryConfigure.getResult().getMeasures().stream().filter(UIQueryField::isAnalysisCalc).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(analysisCalcFields)) {
            QueryConfigure analysisCodeStub = new QueryConfigure();
            analysisCodeStub.setAnalysis(uiQueryConfigure.getAnalysis());
            AnalysisMeasureInitializer analysisMeasureInitializer = new AnalysisMeasureInitializer(analysisCodeStub);

            Set<String> queriedAnalysisCodes = new HashSet<>();
            for (UIQueryField bm : baseMeasuresForAnalysis) {
                String measureCode = bm.getCode();
                for (String calcMode : calcModeList) {
                    for (String calcTypeCode : calcTypeCodes) {
                        AnalysisItemConfig tmp = new AnalysisItemConfig();
                        tmp.setMeasureId(bm.getId());
                        if (StrUtil.isNotBlank(measureCode)) {
                            tmp.setMeasureCode(measureCode);
                        }
                        tmp.setCalcMode(calcMode);
                        tmp.setCalcType(calcTypeCode);
                        String analysisCode = analysisMeasureInitializer.buildAnalysisCode(tmp);
                        if (StrUtil.isNotEmpty(analysisCode)) {
                            queriedAnalysisCodes.add(analysisCode);
                        }
                    }
                }
            }

            Predicate<UIQueryField> isCalcFieldNotInThb = calcField -> {
                if (!calcField.isAnalysisCalc() || calcField.getCustomFieldConfigure() == null
                        || CollUtil.isEmpty(calcField.getCustomFieldConfigure().getExpressionIdMapping())) {
                    return false;
                }
                for (CustomFieldExpressionIdMapping mapping : calcField.getCustomFieldConfigure().getExpressionIdMapping()) {
                    String fieldId = mapping.getId();
                    AnalysisItemConfig itemCfg = mapping.getAnalysisItemConfig();
                    if (fieldId == null || !fieldId.contains(CustomFieldType.ANALYSIS.getIdentifier()) || itemCfg == null) {
                        continue;
                    }
                    String depCode = analysisMeasureInitializer.buildAnalysisCode(itemCfg);
                    if (StrUtil.isNotEmpty(depCode) && !queriedAnalysisCodes.contains(depCode)) {
                        return true;
                    }
                }
                return false;
            };
            uiQueryConfigure.getResult().getMeasures().removeIf(isCalcFieldNotInThb);
            uiQueryConfigure.getFilter().removeIf(isCalcFieldNotInThb);
        }
    }

    /**
     * queryDimensions 非空时：行/列维度在列表中的 isShow=1，否则置 null；未传或空列表保持视图默认。
     */
    private void applyQueryDimensionsVisibility(UIQueryConfigure uiQueryConfigure, QueryOlapDataReq req) {
        Set<String> names = getQueryDimensions(req);
        if (names.isEmpty()) {
            return;
        }
        for (UIQueryField f : uiQueryConfigure.getResult().getRowDimensions()) {
            applyOlapApiDimensionIsShow(uiQueryConfigure, f, names);
        }
        for (UIQueryField f : uiQueryConfigure.getResult().getColDimensions()) {
            applyOlapApiDimensionIsShow(uiQueryConfigure, f, names);
        }
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
     * queryMetrics 非空时：指标在列表中的 isShow=1，否则置 null；未传或空列表保持视图默认。
     */
    private void applyQueryMetricsVisibility(UIQueryConfigure uiQueryConfigure, QueryOlapDataReq req) {
        Set<String> names = getQueryMetrics(req);
        if (names.isEmpty()) {
            return;
        }
        for (UIQueryField f : uiQueryConfigure.getResult().getMeasures()) {
            applyOlapApiMeasureIsShow(f, names);
        }
    }

    private Set<String> getQueryMetrics(QueryOlapDataReq req) {
        if (CollUtil.isEmpty(req.getQueryMetrics())) {
            return Collections.emptySet();
        }
        Set<String> names = req.getQueryMetrics().stream()
                .map(QueryOlapDataQueryMetricReq::getFieldName)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
        return Collections.unmodifiableSet(names);
    }

    private void applyOlapApiMeasureIsShow(UIQueryField field, Set<String> requestedNames) {
        if (field == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(field.getCode())) {
            return;
        }
        boolean hit = olapApiDimensionNameHit(requestedNames, field.getTitle())
                || olapApiDimensionNameHit(requestedNames, field.getDisplayTitle());
        if (hit) {
            field.setIsShow(Enabled.YES.getId());
        } else {
            field.setIsShow(Enabled.NO.getId());
        }
    }

    /**
     * 从结果区克隆出的新筛选字段：templateViewDefault 保留克隆值，若为空则默认包含；include/exclude 显式设置。
     */
    private void applyOlapApiFilterValueTypeForNewFilter(UIQueryField uiQueryField, String filterTypeRaw) {
        FilterType mode = resolveOlapApiFilterMode(filterTypeRaw);
        if (mode == FilterType.INCLUDE) {
            uiQueryField.setFilterValueType(FieldValueFilterType.include.toString());
        } else if (mode == FilterType.EXCLUDE) {
            uiQueryField.setFilterValueType(FieldValueFilterType.exclude.toString());
        } else if (StrUtil.isBlank(uiQueryField.getFilterValueType())) {
            uiQueryField.setFilterValueType(FieldValueFilterType.include.toString());
        }
    }

    /**
     * 已存在于视图筛选区的字段：templateViewDefault 不改 filterValueType；include/exclude 覆盖。
     */
    private void applyOlapApiFilterValueType(UIQueryField uiQueryField, String filterTypeRaw) {
        FilterType mode = resolveOlapApiFilterMode(filterTypeRaw);
        if (mode == FilterType.INCLUDE) {
            uiQueryField.setFilterValueType(FieldValueFilterType.include.toString());
        } else if (mode == FilterType.EXCLUDE) {
            uiQueryField.setFilterValueType(FieldValueFilterType.exclude.toString());
        }
    }

    /**
     * @return INCLUDE | EXCLUDE | DEFAULT（含空、templateViewDefault、无法识别）
     */
    private FilterType resolveOlapApiFilterMode(String filterTypeRaw) {
        if (StrUtil.isBlank(filterTypeRaw)) {
            return FilterType.DEFAULT;
        }
        String t = filterTypeRaw.trim();
        return FilterType.get(t);
    }

    private void applyOlapApiDimensionIsShow(UIQueryConfigure uiQueryConfigure, UIQueryField field, Set<String> requestedNames) {
        if (field == null) {
            return;
        }
        boolean hit = olapApiDimensionNameHit(requestedNames, field.getTitle())
                || olapApiDimensionNameHit(requestedNames, field.getDisplayTitle());
        if (StringUtils.equals("日期", field.getTitle())) {
            setQueryDateAggregate(uiQueryConfigure, hit ? 0 : 1);
            return;
        }

        if (hit) {
            field.setIsShow(Enabled.YES.getId());
        } else {
            field.setIsShow(Enabled.NO.getId());
        }
    }

    private boolean olapApiDimensionNameHit(Set<String> requestedNames, String candidate) {
        if (StrUtil.isBlank(candidate)) {
            return false;
        }
        return requestedNames.contains(candidate.trim());
    }

    // 设置时间聚合
    private void setQueryDateAggregate(UIQueryConfigure uiQueryConfigure, int isAggQuery) {
        uiQueryConfigure.getResult().getAllFields().stream()
                .filter(UIQueryField::isCommonDate).forEach(v -> {
                    v.setIsAggQuery(isAggQuery);
                    v.setIsShow(1 - isAggQuery);
                });
        uiQueryConfigure.getFilter().stream()
                .filter(UIQueryField::isCommonDate).forEach(v -> {
                    v.setIsAggQuery(isAggQuery);
                    v.setIsShow(1 - isAggQuery);
                });
        uiQueryConfigure.getSetting().setIsAggQuery(isAggQuery);
    }

    /**
     * 按粒度获取对应的同环比计算方式
     * @param granularity
     * @return
     */
    public List<String> getThbCalcMode(DateGranularity granularity) {
        List<String> modeList = new ArrayList<>();
        if (granularity == DateGranularity.DAY) {
            modeList = ListUtil.of(
            AnalysisCalcMode.HB.getCode(),
            AnalysisCalcMode.TB_WEEK.getCode(),
            AnalysisCalcMode.TB_MONTH.getCode(),
            AnalysisCalcMode.TB_YEAR.getCode(),
            AnalysisCalcMode.TB_YEAR_2.getCode(),
            AnalysisCalcMode.TB_YEAR_3.getCode(),
            AnalysisCalcMode.TB_LN_YEAR.getCode(),
            AnalysisCalcMode.TB_LN_YEAR_2.getCode(),
            AnalysisCalcMode.TB_LN_YEAR_3.getCode(),
            AnalysisCalcMode.TB_YEAR_WEEK.getCode(),
            AnalysisCalcMode.TB_YEAR_WEEK_2.getCode(),
            AnalysisCalcMode.TB_YEAR_WEEK_3.getCode(),
            AnalysisCalcMode.TB_LN_YEAR_WEEK.getCode(),
            AnalysisCalcMode.TB_LN_YEAR_WEEK_2.getCode(),
            AnalysisCalcMode.TB_LN_YEAR_WEEK_3.getCode()
            );
        }else {
            modeList = ListUtil.of(AnalysisCalcMode.HB.getCode(), AnalysisCalcMode.TB_YEAR.getCode(), AnalysisCalcMode.TB_YEAR_2.getCode(), AnalysisCalcMode.TB_YEAR_3.getCode());
        }
        return modeList;
    }
}
