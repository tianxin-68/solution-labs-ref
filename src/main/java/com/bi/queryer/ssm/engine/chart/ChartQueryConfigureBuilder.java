package com.bi.queryer.ssm.engine.chart;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.chart.rt.UIRtChartDatasetNormalizer;
import com.bi.queryer.ssm.engine.chart.vo.ChartAnalysisConfigureVO;
import com.bi.queryer.ssm.engine.chart.vo.ChartQueryConfigVO;
import com.bi.queryer.ssm.engine.config.QueryAnalysis;
import com.bi.queryer.ssm.engine.config.QueryFilter;
import com.bi.queryer.ssm.engine.config.QueryResult;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortItem;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortSettings;
import com.bi.queryer.ssm.engine.config.settings.style.QueryStyleSettings;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaDataset;
import com.bi.queryer.ssm.meta.RtTableInfo;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isEmpty;
import static com.bi.queryer.util.BIUtil.isNotEmpty;

/**
 * @Auther: contributor
 * @Date: 2024/7/10 09:33
 * @Description:
 */
public class ChartQueryConfigureBuilder {

    public static void buildQueryConfigure(String config, ChartQueryConfigure queryConfigure) {
        //json to ChartQueryConfigVO
        ChartQueryConfigVO vo = JSONObject.parseObject(config, ChartQueryConfigVO.class);
        if (vo == null || queryConfigure == null) {
            return;
        }

        QuerySettings settings = vo.getSetting() == null ? new QuerySettings() : vo.getSetting();
        buildDatasetType(settings);

        //实时趋势图格式化
        UIRtChartDatasetNormalizer uiRtChartDatasetNormalizer = new UIRtChartDatasetNormalizer(vo);
        uiRtChartDatasetNormalizer.normalize(vo);

        //自定义指标类型的过滤字段id集合
        List<String> customMeasureFilterIds = new ArrayList<>();

        //ChartQueryConfigVO to QueryConfigure
        queryConfigure.setSessionId(vo.getSessionId());

        QueryFilter queryFilter = buildQueryFilter(vo.getGlobalFilters(), vo.getFilters());
        buildCustomMeasureFilter(queryFilter,customMeasureFilterIds);
        queryConfigure.setFilter(queryFilter);

        QueryFilter globalFilter = buildGlobalQueryFilter(vo.getGlobalFilters());

        //将全局筛选器的时间筛选范围替换为明细筛选的时间范围
        replaceCommonDateFilterValue(queryFilter, globalFilter);
        buildCustomMeasureFilter(globalFilter,customMeasureFilterIds);
        queryConfigure.setGlobalFilter(globalFilter);

        vo.setCustomMeasureFilterIds(customMeasureFilterIds);
        queryConfigure.setResult(buildQueryResult(vo));

        queryConfigure.setAnalysis(buildQueryAnalysis(vo, queryFilter));

        settings.setQueryTimeoutSec(QuerySessionSettingManager.getQueryTimeoutSec());
        settings.setIsAggQuery(Enabled.NO.getId());
        QueryField commonDateField = queryFilter.getFields().stream()
                .filter(QueryField::isCommonDate).findFirst().orElse(null);
        if (commonDateField != null) {
            QueryStyleSettings styleSettings = new QueryStyleSettings();
            QuerySortSettings sortSettings = new QuerySortSettings();
            QuerySortItem sortItem = new QuerySortItem();
            sortItem.setOrderBy(commonDateField.getCode());
            sortItem.setOrderType("asc");
            sortItem.setColumnField(commonDateField.getCode());
            sortSettings.setIsActive(Enabled.YES.getId());
            sortSettings.setSortItems(Collections.singletonList(sortItem));
            styleSettings.setUpDownSortData(sortSettings);
            settings.setTableStyle(styleSettings);
        }


        queryConfigure.setSettings(settings);
    }

    /**
     * 构建数据集类型,标记是实时还是离线
     */
    public static void buildDatasetType(QuerySettings settings) {
        String datasetId = settings.getDatasetId();
        MetaDataset dataset = SSDMetaCacheManager.getDataset(datasetId);
        if (dataset != null) {
            DataTypeEnum datasetType = DataTypeEnum.codeOf(dataset.getDatasetType());
            settings.setDatasetType(datasetType);
        }
    }


    /**
     * 构建自定义指标过滤字段
     * @param queryFilter
     */
    public static void buildCustomMeasureFilter(QueryFilter queryFilter,List<String> customMeasureFilterIds){

       for(QueryField field: queryFilter.getFields()){
           //计算指标过滤，设置查询区域为指标
           //计算指标的code生成规则依赖queryArea
           if (FieldType.CUSTOM_MEASURE == FieldType.get(field.getFieldType())) {
               field.setQueryArea(QueryArea.Measure);
               customMeasureFilterIds.add(field.getId());
           }
       }

    }

    private static void replaceCommonDateFilterValue(QueryFilter queryFilter, QueryFilter globalFilter) {
        if (queryFilter == null || globalFilter == null) {
            return;
        }

        //明细筛选的时间字段
        QueryField commonDateField = queryFilter.getFields().stream()
                .filter(QueryField::isCommonDate).findFirst().orElse(null);

        List<QueryField> fields = globalFilter.getFields();
        if (isNotEmpty(fields) && commonDateField != null) {
            fields.stream().filter(QueryField::isCommonDate).forEach(field -> {
                field.setValues(commonDateField.getValues());
                field.setIsAggQuery(Enabled.NO.getId());
            });
        }
    }

    private static QueryAnalysis buildQueryAnalysis(ChartQueryConfigVO vo, QueryFilter queryFilter) {
        QueryAnalysis queryAnalysis = new QueryAnalysis();
        if (isEmpty(vo.getAnalyses())) {
            return queryAnalysis;
        }

        QueryField commonDate = queryFilter.getFields().stream().filter(QueryField::isCommonDate)
                .findFirst().orElse(new QueryField());
        List<String> baseDates = commonDate.getValues().stream().map(FieldValue::getTitle).collect(Collectors.toList());

        AnalysisThbConfig thbConfig = new AnalysisThbConfig();
        AnalysisCompareConfig compareConfig = new AnalysisCompareConfig();
        int compareIndex = 0;
        for (ChartAnalysisConfigureVO config : vo.getAnalyses()) {
            AnalysisCalcMode calcMode = AnalysisCalcMode.get(config.getCalcMode());
            if (calcMode.isTb() || calcMode.isHb()) {
                thbConfig.setIsActive(Enabled.YES.getId());
                thbConfig.getItems().add(buildThbItemConfig(config, commonDate));
            } else if (AnalysisCalcMode.CUSTOM_COMPARE == calcMode) {
                compareConfig.setIsActive(Enabled.YES.getId());
                compareConfig.getItems().add(buildCompareItemConfig(config, commonDate, baseDates, compareIndex++));
            }
        }

        queryAnalysis.setCompare(compareConfig);
        queryAnalysis.setThb(thbConfig);

        return queryAnalysis;
    }

    private static AnalysisCompareItemConfig buildCompareItemConfig(ChartAnalysisConfigureVO config, QueryField commonDate,
                                                                    List<String> baseDates, int compareIndex) {
        AnalysisCompareItemConfig compareItemConfig = new AnalysisCompareItemConfig();
        compareItemConfig.setCompareIndex(compareIndex);
        compareItemConfig.setBaseDates(baseDates);
        compareItemConfig.setCalcMode(config.getCalcMode());
        compareItemConfig.setCompareDates(config.getCompareDates());
        compareItemConfig.setTitle(config.getTitle());
        compareItemConfig.setCalcTypes(config.getCalcTypes());
        compareItemConfig.setPercentFieldRatioUnit(config.getPercentFieldRatioUnit());
        compareItemConfig.setMeasureIdList(Collections.singletonList(BIConsts.ALL_MEASURE));
        compareItemConfig.setDateGranularity(commonDate.getQueryDateGranularity());
        return compareItemConfig;
    }

    private static AnalysisThbItemConfig buildThbItemConfig(ChartAnalysisConfigureVO config, QueryField commonDate) {
        AnalysisThbItemConfig thbItemConfig = new AnalysisThbItemConfig();
        thbItemConfig.setCalcTypes(config.getCalcTypes());
        thbItemConfig.setMeasureIdList(Collections.singletonList(BIConsts.ALL_MEASURE));
        thbItemConfig.setPercentFieldRatioUnit(config.getPercentFieldRatioUnit());
        thbItemConfig.setCalcModes(Collections.singletonList(config.getCalcMode()));
        return thbItemConfig;
    }

    private static QueryResult buildQueryResult(ChartQueryConfigVO vo) {
        QueryResult result = new QueryResult();
        if (vo == null) {
            return result;
        }

        List<QueryField> rowDimensions = loadFields(vo.getDimensions(), false, 0,vo);
        result.setRowDimensions(rowDimensions);

        // 指标
        result.setMeasures(loadFields(vo.getMeasures(), true, rowDimensions.size(),vo));

        addAdditionalField(result.getFields());

        return result;
    }

    /**
     * 添加附加字段：用于添加计算字段的原子字段
     */
    public static void addAdditionalField(List<QueryField> fields) {
        List<QueryField> allAppendFields = new ArrayList<>();
        if (isEmpty(fields)) {
            return;
        }
        // 对计算字段分解（后台计算字段+前台用户自定义计算字段）
        for (QueryField qf : fields) {
            if (FieldUtil.isCalcField(qf)) {
                qf.setCalc(true);
                FieldUtil.setCalcFieldAtomFields(qf, fields);

                List<QueryField> appendFields = qf.getCalcAtomFields().stream().filter(QueryField::isAppend).collect(Collectors.toList());

                //跨模型计算字段和前端构建的计算字段
                //如果引用字段不在指标区域，则新创建并添加到指标区，并设置为隐藏
                if (
                        !qf.isLodField() &&
                        (
                                FieldType.CROSS_MODEL_MEASURE == FieldType.get(qf.getFieldType())
                                ||
                                qf.getCode().contains(BIConsts.Custom_Field_Name_Suffix)
                        )
                ) {
                    for (QueryField appendField : appendFields) {
                        if (appendField.isMeasure()) {
                            //计算字段规范化处理：如果引用字段不在指标区域，则新创建并添加到指标区，并设置为隐藏
                            appendField.setAppend(false);
                            appendField.setIsShow(Enabled.NO.getId());
                        }
                        allAppendFields.add(appendField);
                    }
                } else {
                    allAppendFields.addAll(appendFields);
                }
            }
        }
        // 添加附加字段到结果字段列表中
        for (QueryField appendField : allAppendFields) {
            if (!fields.contains(appendField)) {
                fields.add(appendField);
            }
        }
    }

    //过滤条件附加字段，排除lod
    public static void addFilterAdditionalField(List<QueryField> fields) {
        List<QueryField> allAppendFields = new ArrayList<>();
        if (isEmpty(fields)) {
            return;
        }
        // 对计算字段分解（后台计算字段+前台用户自定义计算字段）
        for (QueryField qf : fields) {
            if (FieldUtil.isCalcField(qf) && !qf.isLodField()) {
                qf.setCalc(true);
                FieldUtil.setCalcFieldAtomFields(qf, fields);

                List<QueryField> appendFields = qf.getCalcAtomFields().stream().filter(QueryField::isAppend).collect(Collectors.toList());

                //跨模型计算字段和前端构建的计算字段
                //如果引用字段不在指标区域，则新创建并添加到指标区，并设置为隐藏
                if (
                        !qf.isLodField() &&
                                (
                                        FieldType.CROSS_MODEL_MEASURE == FieldType.get(qf.getFieldType())
                                                ||
                                                qf.getCode().contains(BIConsts.Custom_Field_Name_Suffix)
                                )
                ) {
                    for (QueryField appendField : appendFields) {
                        if (appendField.isMeasure()) {
                            //计算字段规范化处理：如果引用字段不在指标区域，则新创建并添加到指标区，并设置为隐藏
                            appendField.setAppend(false);
                            appendField.setIsShow(Enabled.NO.getId());
                        }
                        allAppendFields.add(appendField);
                    }
                } else {
                    allAppendFields.addAll(appendFields);
                }
            }
        }
        // 添加附加字段到结果字段列表中
        for (QueryField appendField : allAppendFields) {
            if (!fields.contains(appendField)) {
                fields.add(appendField);
            }
        }
    }

    private static List<QueryField> loadFields(List<QueryField> fields, boolean isMeasure, double order,ChartQueryConfigVO vo) {
        List<String> customMeasureFilterIds = vo.getCustomMeasureFilterIds();
        List<QueryField> queryFields = new ArrayList<>();
        if (BIUtil.isEmpty(fields)) {
            return queryFields;
        }

        Set<String> calcFieldsRefIdList = new HashSet<>();
        boolean allLodMeasure = true;
        for (QueryField field : fields) {
            //前端传过来的分析字段，不处理。V1.7.1 统一由后台处理
            if (Enabled.value(field.getIsAnalysis())) {
                continue;
            }

            field.setIsResult(true);
            QueryArea queryArea = isMeasure ? QueryArea.Measure : QueryArea.RowDimension;
            field.setQueryArea(queryArea);
            field.setRawQueryArea(queryArea);
            field.setShowOrder(order);
            field.init();
            order += 1;

            if (isMeasure && Enabled.isTrue(field.getIsShow())) {
                allLodMeasure = allLodMeasure && field.isLodField();
            }

            //汇总查询时，行维度去掉公共日期
            if (QueryArea.RowDimension == queryArea && field.isAggQuery()) {
                continue;
            }

            queryFields.add(field);

            if (
                    ( Enabled.isTrue(field.getIsShow()) || customMeasureFilterIds.contains(field.getId()))
                            && field.getCustomFieldConfigure() != null &&
                    field.getCustomFieldConfigure().getExpression() != null) {
                if (field.isAnalysisCalc()) {
                    List<CustomFieldExpressionIdMapping> idMappings = field.getCustomFieldConfigure().getExpressionIdMapping();
                    if (idMappings != null) {
                        for (CustomFieldExpressionIdMapping idMapping : idMappings) {
                            if (idMapping == null) {
                                continue;
                            }
                            if (idMapping.getAnalysisItemConfig() != null) {
                                calcFieldsRefIdList.add(idMapping.getAnalysisItemConfig().getMeasureId());
                            } else {
                                // 依赖的是本期值
                                calcFieldsRefIdList.add(idMapping.getId());
                            }

                        }
                    }
                } else {
                    String expression = field.getCustomFieldConfigure().getExpression();
                    Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
                    Matcher matcher = pattern.matcher(expression);
                    while (matcher.find()) {
                        calcFieldsRefIdList.add(matcher.group());
                    }
                }
            }
        }

        // 去掉不显示的字段，但若原字段不显示，但被其他计算字段引用，且计算字段显示状态，则也需要加载，只是在最终查询结果中不显示
        Function<QueryField, Boolean> filter;
        if (isMeasure) {
            final QueryField aCommonField;
            if (allLodMeasure) {
                //实时数据集，通过粒度获取一个非lod的指标
                if(vo.getSetting().isRtDataset()){
                    aCommonField = getSingleNonLodMeasure(queryFields,vo.getSetting());
                }else{
                    aCommonField = queryFields.stream().filter(v -> !v.isLodField()).findFirst().orElse(null);
                }
            } else {
                aCommonField = null;
            }
            filter = f -> Objects.equals(aCommonField, f)
                    || Enabled.isTrue(f.getIsShow())
                    || calcFieldsRefIdList.contains(f.getId())
                    || customMeasureFilterIds.contains(f.getId())  ;
        } else {
            filter = f -> Enabled.isTrue(f.getIsShow());
        }
        return queryFields.stream().filter(filter::apply).collect(Collectors.toList());
    }

    /**
     * 在当前切片粒度下，获取一个非lod的指标
     * @return
     */
    public static QueryField getSingleNonLodMeasure(List<QueryField> queryFields,QuerySettings settings) {

        String dataSliceGranularity = settings.getDataSliceGranularity();
        String dataSetId = settings.getDatasetId();
        for (QueryField field : queryFields) {

            if (field.isLodField()) {
                continue;
            }

            List<RtTableInfo> rtTableInfos = SSDMetaCacheManager.getFieldRtTableByCode(dataSetId, field.getCode());
            if (CollUtil.isEmpty(rtTableInfos)) {
                continue;
            }

            //找到一个粒度匹配的指标
            for (RtTableInfo rtTableInfo : rtTableInfos) {
                if (dataSliceGranularity.equalsIgnoreCase(rtTableInfo.getDataSliceGranularity())) {
                    return field;
                }
            }
        }

        throw new RuntimeException(String.format("当前实时趋势图粒度%s下,没有可查询的普通指标，无法查询！请切换查询粒度后重新选择！",dataSliceGranularity));
    }

    public static QueryFilter buildQueryFilter(List<QueryField> globalFilters, List<QueryField> filters) {
        QueryFilter res = new QueryFilter();
        double index = 0;
        List<QueryField> fields = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        //优先明细筛选
        if (isNotEmpty(filters)) {
            for (QueryField field : filters) {
                List<FieldValue> values = field.getValues();
                if (isEmpty(values)) {
                    continue;
                }
                values = values.stream().filter(v -> isNotEmpty(v.getId())).collect(Collectors.toList());
                if (isEmpty(values)) {
                    continue;
                }
                ids.add(field.getId());
                if (isNotEmpty(field.getCode())) {
                    ids.add(field.getCode());
                }
                field.setIsFilter(true);
                field.setShowOrder(index);

                field.init();
                index += 1;

                field.setValues(transferFilterValues(field, values));

                fields.add(field);
            }
        }

        if (isNotEmpty(globalFilters)) {
            for (QueryField field : globalFilters) {
                if (ids.contains(field.getId()) || (isNotEmpty(field.getCode()) && ids.contains(field.getCode()))) {
                    continue;
                }

                List<FieldValue> values = field.getValues();
                if (isEmpty(values)) {
                    continue;
                }
                values = values.stream().filter(v -> isNotEmpty(v.getId())).collect(Collectors.toList());
                if (isEmpty(values)) {
                    continue;
                }
                field.setIsFilter(true);
                field.setShowOrder(index);
                field.init();
                index += 1;

                fields.add(field);
            }
        }

        addFilterAdditionalField(fields);
        res.setFields(fields);
        return res;
    }

    private static QueryFilter buildGlobalQueryFilter(List<QueryField> filters) {
        QueryFilter res = new QueryFilter();
        if (isEmpty(filters)) {
            return res;
        }

        double index = 0;
        List<QueryField> fields = new ArrayList<>();
        for (QueryField field : filters) {
            List<FieldValue> values = field.getValues();
            if (isEmpty(values)) {
                continue;
            }
            values = values.stream().filter(v -> isNotEmpty(v.getId())).collect(Collectors.toList());
            if (isEmpty(values)) {
                continue;
            }
            field.setIsFilter(true);
            field.setShowOrder(index);
            field.init();
            index += 1;

            fields.add(field);
        }

        addFilterAdditionalField(fields);
        res.setFields(fields);
        return res;
    }

    //对特殊过滤值的处理
    public static List<FieldValue> transferFilterValues(QueryField field, List<FieldValue> values) {
        FieldFilterType filterType = FieldUtil.getFilterType(field);
        DataType dataType = DataType.getType(field.getMeta().getDataType());
        if (filterType == FieldFilterType.BooleanSelect) {
            for (FieldValue value : values) {
                if (dataType.isDecimal()) {
                    if ("是".equals(value.getId())) {
                        value.setId("1");
                    } else if ("否".equals(value.getId())) {
                        value.setId("0");
                    } else if ("其他".equals(value.getId())) {
                        value.setId(BIConsts.NULL_VALUE);
                    }
                }
            }
        } else if (filterType == FieldFilterType.MultiSelect) {
            /*处理趋势图枚举值映射
            String fieldCode = field.getCode();
            Map<String, String> fieldValueMap = SSDMetaCacheManager.getFieldValueMapByFieldCode(fieldCode);
            if (CollectionUtils.isEmpty(values) || fieldValueMap.isEmpty()) {
                return values;
            }
            Map<String, List<String>> map = fieldValueMap.entrySet().stream()
                    .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
            List<FieldValue> res = new ArrayList<>(values.size());
            for (FieldValue v : values) {
                List<String> orgValues = map.get(v.getId());
                if (CollectionUtils.isEmpty(orgValues)) {
                    res.add(v);
                } else {
                    orgValues.forEach(o -> res.add(new FieldValue(o, o, filterType.getCode())));
                }
            }
            return res; */
        } else if (filterType == FieldFilterType.Textarea) {
            String value = values.stream().map(FieldValue::getId)
                    .collect(Collectors.joining("\n"));
            field.setFilterQueryRule(FilterQueryRuleType.Exact.getCode());
            return Arrays.asList(new FieldValue(value, value, filterType.getCode()));
        } else if (filterType != null && filterType.isRange()) {
            if (CollectionUtils.isEmpty(values)) {
                return values;
            }
            List<String> ids = values.stream().map(FieldValue::getId)
                    .map(v -> !field.isCommonDate() && filterType == FieldFilterType.MonthRange ? v.replace("-", "") : v)
                    .collect(Collectors.toList());
            List<FieldValue> res = new ArrayList<>(4);
            if (CollectionUtils.size(ids) == 1) {
                String id = ids.get(0);
                res.add(new FieldValue(id, id, filterType.getCode()));
                res.add(new FieldValue(id, id, filterType.getCode()));
            } else {
                ids.sort(Comparator.comparing(v -> v));
                String start = ids.get(0);
                String end = ids.get(ids.size() - 1);
                res.add(new FieldValue(start, start, filterType.getCode()));
                res.add(new FieldValue(end, end, filterType.getCode()));
            }
            return res;
        }
        return values;
    }
}
