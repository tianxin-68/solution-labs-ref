package com.bi.queryer.ssm.engine.prepare;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortItem;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortSettings;
import com.bi.queryer.ssm.engine.config.ui.UIQueryAnalysis;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.engine.config.ui.UIQueryResult;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:32 2025/10/22
 * @Description 前置预处理查询引擎：在正式查询数据之前的数据准备，将此类数据存储到上下文中
 * 1、列维度枚举值：需要提前查询按第一个指标排序的Top N的枚举值
 **/
public class PrepareQueryEngine {
    private QueryConfigure config;
    private QueryContext cxt;

    public PrepareQueryEngine(QueryConfigure config, QueryContext cxt) {
        this.cxt = cxt.clone();
        this.config = config;
    }


    public PrepareQueryResult execute(){
        PrepareQueryResult result = new PrepareQueryResult();
        if(!"true".equalsIgnoreCase(SC.v("prepare.cross.item.enable", "true"))){
            return result;
        }
        try{
            ResultDataSet crossDimensionItems = this.prepareCrossDimensionItems();
            result.setCrossDimensionItemDataSet(crossDimensionItems);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 1、隐藏所有行维度
     * 2、将列维度调整到行维度
     * 3、将日期调整为汇总
     * 4、按第一个指标排降序，取top100
     * 5、top值存储到cxt中
     */
    protected ResultDataSet prepareCrossDimensionItems(){
        QueryConfigure itemConfig = this.createCrossDimensionItemConfig(this.config);
        if(itemConfig == null){
            return null;
        }

        // 现在最大列数
        int maxCount = BIConsts.COLUMN_DIM_ITEM_MAX_COUNT;
        itemConfig.getSettings().setQueryRowLimit(maxCount);

        // 是否是计算维度
        boolean isCalcDimension = this.config.getResult().getColDimensions().stream()
                                .filter(f->!BIConsts.ALL_MEASURE_CODE.equals(f.getCode()))
                                    .filter(f->f.isCustomDimension()).count() > 0;

        // 当前线程下主查询的数据源
        DataSourceType currentDataSource = DataSourceRouter.getCurrentDataSourceType();
        QueryEngine newEngine = QueryFactory.createEngine(itemConfig, cxt);
        // 按当前查询引擎重新设置查询引擎的数据源，解决场景：主查询使用doris，但此列维度枚举值查询可能是trino，导致查询语法不一致，最终报错
        DataSourceRouter.setQueryEngineDefaultDataSource(newEngine);
        ResultDataSet dataSet = new ResultDataSet();
        try {
            Object data = newEngine.execute().getData();
            dataSet = data == null ? null : (ResultDataSet) data;

            /**
             * 计算维度code替换：
             * 原因：
             * 计算维度生成数据集时，把计算维度放在行区域，导致计算维度code=Custom_Field_Name_Suffix + QueryArea.RowDimension.shortCode + id
             * 但在最终作为表头时，改计算字段的code=Custom_Field_Name_Suffix + QueryArea.ColumnDimension.shortCode + id
             */
            if(isCalcDimension && dataSet != null){
                String oldSuffix = BIConsts.Custom_Field_Name_Suffix + QueryArea.RowDimension.getShortCode();
                String newSuffix = BIConsts.Custom_Field_Name_Suffix + QueryArea.ColumnDimension.getShortCode();
                dataSet.getColumns().forEach(c -> {
                    c.setId(c.getId().replace(oldSuffix, newSuffix));
                    c.setCode(c.getCode().replace(oldSuffix, newSuffix));
                });

                List<Map<String, Object>> newRows = new ArrayList<>();
                dataSet.getRows().forEach(row ->{
                    Map<String, Object> newRow = new HashMap<>();
                    for(Map.Entry<String, Object> entry : row.entrySet()){
                        String key = entry.getKey();
                        if(key != null) {
                            String newKey = key.replace(oldSuffix, newSuffix);
                            newRow.put(newKey, entry.getValue());
                        }else{
                            newRow.put(key, entry.getValue());
                        }
                    }
                    newRows.add(newRow);
                });
                dataSet.setRows(newRows);
            }
        }catch (BIException e) {
            e.printStackTrace();
        }finally {
            // 还原数据源
            DataSourceRouter.setCurrentDataSourceType(currentDataSource);
        }
        return dataSet;
    }

    /**
     * 1、删除所有行维度
     * 2、将列维度调整到行维度
     * 3、将第一个指标排序设置为降序
     * 4、指标去掉所有同环比占比、汇总
     * 5、将日期调整为汇总
     *
     * 注意：
     * 1、必须通过修改配置文件的字符串json，模拟前端的配置查询。不能直接克隆QueryConfigure并修改相关属性，因为在后续的sql构建中会重新来config字符串反序列化
     */
    protected QueryConfigure createCrossDimensionItemConfig(QueryConfigure rawConfig){
        if(rawConfig == null || BIUtil.isEmpty(rawConfig.getConfig())){
            return null;
        }
        UIQueryConfigure uiQueryConfigure = JSONObject.parseObject(rawConfig.getConfig(), UIQueryConfigure.class);
        UIQueryResult queryResult = uiQueryConfigure.getResult();
        if(queryResult == null){
            return null;
        }
        //1. 去掉"所有指标"
        //2. 去掉隐藏的列维度
        List<UIQueryField> colFields = queryResult.getColDimensions().stream()
                                        .filter(c -> !BIConsts.ALL_MEASURE_CODE.equals(c.getCode()))
                                        .filter(f -> Enabled.value(f.getIsShow()))
                                        .collect(Collectors.toList());

        if(BIUtil.isEmpty(colFields)){
            return null;
        }

        UIQueryField colField = colFields.get(0);
        // 列维度若是公共日期或布尔类型，则不处理
        if(colFields.get(0).isCommonDate() || this.getFilterType(colField) == FieldFilterType.BooleanSelect){
            return null;
        }

        // 设置隐藏行维度
        List<UIQueryField> rowDimensions = uiQueryConfigure.getResult().getRowDimensions();
        rowDimensions.forEach(d -> d.setIsShow(Enabled.NO.getId()));

        // 将列维度转移到行维度中
        colFields.stream().filter(c -> !BIConsts.ALL_MEASURE_CODE.equals(c.getCode())).forEach(c -> rowDimensions.add(c));

        // 清空列维度
        uiQueryConfigure.getResult().getColDimensions().clear();

        // 设置日期汇总
        uiQueryConfigure.getFilter().forEach(f->{
            if(f.isCommonDate()){
                f.setIsAggQuery(Enabled.YES.getId());
                f.setQueryDateGranularity(this.config.getSettings().getDateGranularity());
            }
        });

        // 指标
        List<String> dependencyFields = new ArrayList<>();
        List<UIQueryField> measures = uiQueryConfigure.getResult().getMeasures();

        //lod字段不能在第一个，否则主视图选表不准确，导致列维度缺失
        measures = reorder(measures);

        for(int i = 0; i< measures.size(); i++){
            UIQueryField measure = measures.get(i);
            // 所有指标去掉所有同环比占比、汇总
            measure.setAnalysisConfig(new AnalysisItemConfig());
            if(i == 0){
                // 设置第一个指标的排序方式为降序
                QuerySortSettings sortSettings = uiQueryConfigure.getSetting().getTableStyle().getUpDownSortData();
                if(sortSettings != null){
                    String measureCode = measure.getCode();
                    // 若编码不存在且是自定义计算字段，则通过规则组织code
                    if(BIUtil.isEmpty(measure.getCode()) && measure.getCustomFieldConfigure() != null && !measure.getCustomFieldConfigure().isEmpty()){
                        measureCode = BIConsts.Custom_Field_Name_Suffix + QueryArea.Measure.getShortCode() + "_" + measure.getId().replaceAll("[\\.\\-\\ :]", "_");
                    }
                    sortSettings.setIsActive(Enabled.YES.getId());
                    sortSettings.getSortItems().clear();
                    QuerySortItem sortItem = new QuerySortItem();
                    sortItem.setColumnField(measureCode);
                    sortItem.setIsNullsLast(Enabled.YES.getId());
                    sortItem.setOrderType(FieldSortType.DESC.getCode());
                    sortItem.setOrderBy(measureCode);
                    sortSettings.getSortItems().add(sortItem);
                }
            }else {
                // 不是第一个指标，且是lod或lod二次计算字段，则隐藏
                if(measure.isLodField()){
                    measure.setIsShow(Enabled.NO.getId());
                }
                // lod四则运算的自定义类型=common，通过标识符判断：不能通过UIQueryField.isLodField判断
                Optional<List<CustomFieldExpressionIdMapping>> mappingsOpt = Optional.ofNullable(measure).map(UIQueryField::getCustomFieldConfigure).map(CustomFieldConfigure::getExpressionIdMapping);
                mappingsOpt.ifPresent(mappings -> {
                    for (CustomFieldExpressionIdMapping mapping : mappings) {
                        if (mapping.getId().contains(CustomFieldType.LOD.getIdentifier().toLowerCase())) {
                            measure.setIsShow(Enabled.NO.getId());
                        } else {
                            dependencyFields.add(mapping.getId());
                        }
                    }
                });
            }
        }

        List<UIQueryField> queryFields = measures.stream().filter(m -> Enabled.YES.getId().equals(m.getIsShow())).collect(Collectors.toList());
        if (BIUtil.isEmpty(queryFields) && BIUtil.isNotEmpty(dependencyFields)) {
            String fieldId = dependencyFields.get(0);
            measures.stream().filter(v -> v.getId().equals(fieldId))
                    .findFirst().ifPresent(v -> v.setIsShow(Enabled.YES.getId()));
        }

        // 去掉指标过滤：避免前端配置的多个行维度时，因前面只保留了列维度，导致相关指标值有误差，若有指标过滤，会导致维度枚举项错误过滤
        List<UIQueryField> filterFields = uiQueryConfigure.getFilter().stream().filter(f ->{
            // TODO 维度计算字段需要确认此逻辑是否可覆盖
            MetaField meta = SSDMetaCacheManager.getField(f.getId());
            return (meta != null && Enabled.isFalse(meta.getIsMeasure()));
        }).collect(Collectors.toList());
        uiQueryConfigure.setFilter(filterFields);


        // 去掉指标的总体分析配置
        uiQueryConfigure.setAnalysis(new UIQueryAnalysis());

        // 重置session id
        uiQueryConfigure.setSessionId(BIConsts.HEADER_PREPARE_SESSIONID_PREFIX + Guid.id());
        // 设置查询的响应数据集格式为list<map>
        uiQueryConfigure.getSetting().setResponseFormat(QueryResponseFormat.MAP.getCode());

        // 设置模板id
        SSDQueryTemplate tplEntity = new SSDQueryTemplate();
        if(this.config.getTemplateEntity() != null){
            tplEntity.setId(this.config.getTemplateEntity().getId() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
            tplEntity.setName(this.config.getTemplateEntity().getName() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
            tplEntity.setConfig(JSONObject.toJSONString(uiQueryConfigure));
        }
        // 按新配置重新加载
        QueryConfigure newConfig = new QueryConfigure(tplEntity);
        newConfig.setConfig(tplEntity.getConfig());
        newConfig.load();
        return newConfig;
    }

    /**
     * 重排序
     * @param measures
     * @return
     */
    protected List<UIQueryField> reorder(List<UIQueryField> measures) {

        if (CollUtil.isEmpty(measures)) {
            return measures;
        }

        // 第一个指标不是lod字段，不处理
        UIQueryField firstMeasure = measures.get(0);
        if (!firstMeasure.isLodField()) {
            return measures;
        }

        List<UIQueryField> lod = new ArrayList<>();
        List<UIQueryField> nonLod = new ArrayList<>();
        for (UIQueryField m : measures) {
            if (m.isLodField()) {
                lod.add(m);
            } else {
                nonLod.add(m);
            }
        }

        //非lod的指标提前
        List<UIQueryField> results = new ArrayList<>();
        results.addAll(nonLod);
        results.addAll(lod);

        return results;
    }


    /**
     * 废弃
     * 1、删除所有行维度
     * 2、将列维度调整到行维度
     * 3、将日期调整为汇总
     * 4、将第一个指标排序设置为降序
     */
    @Deprecated
    protected QueryConfigure createCrossDimensionItemConfig2(){
        // 优先从模型中获取列维度，因为模型中的列维度是经过优化后的，直接从config中获取的列维度可能会被优化掉导致查询报错
        List<QueryField> colFields = config.getResult().getColDimensions();

        if(BIUtil.isEmpty(colFields)) {
            return null;
        }

        // 列维度若是公共日期，则不处理
        if(colFields.get(0).isCommonDate()){
            return null;
        }

        // 重新构建配置
        QueryConfigure newCfg = new QueryConfigure();

        // 列维度 是否有排序类型
        boolean colFieldHasSortType = false;
        for(QueryField colField : colFields){
            // 若是日期则按日期升序
            FieldFilterType filterType = FieldUtil.getFilterType(colField);
            if(filterType.isDateRange()) {
                colField.setSortType(FieldSortType.ASC);
                colFieldHasSortType = true;
                break;
            }
            colFieldHasSortType = colFieldHasSortType || colField.getSortType() != FieldSortType.NONE;
        }

        // 若列维度未设置排序，则默认按第一个指标降序
        List<QueryField> measures = new ArrayList<>();

        // 添加指标：避免但模块下多维度时，通过指标来限定正确的查询表（无法找到正确的查询表）
        // 如：轮胎运营分析模块中：城市线级、轮胎规格、轮胎规格分类
        if(BIUtil.isNotEmpty(config.getResult().getMeasures())) {
            List<QueryField> normalMeasures =  config.getResult().getMeasures().stream().filter(f-> !Enabled.value(f.getIsAnalysis())).collect(Collectors.toList());
            for(int i = 0 ; i < normalMeasures.size(); i++){
                QueryField measureField = normalMeasures.get(i);
                // 默认按第一个指标
                if(i == 0 && !colFieldHasSortType){
                    measureField.setSortType(FieldSortType.DESC);
                }
                measures.add(measureField);
            }
        }

        // 将列维度转为行维度查询
        List<QueryField> newRowFields = new ArrayList<>();
        colFields.forEach(f-> {
            f.setQueryArea(QueryArea.RowDimension);
            if(!newRowFields.contains(f)) {
                newRowFields.add(f);
            }
        });

        newCfg.getResult().setMeasures(measures);
        newCfg.getResult().setRowDimensions(newRowFields);
        newCfg.getResult().addAdditionalField();

        List<QueryField> newFilterFields = new ArrayList<>();
        config.getFilter().getFields().forEach(
                f-> {
                    if (f.isCommonDate()) {
                        // 若是公共日期，则设置为汇总，避免日均指标查询时没有日期维度在行区域中
                        f.setIsAggQuery(Enabled.YES.getId());
                    }
                    newFilterFields.add(f);
                }
        );
        newCfg.getFilter().setFields(newFilterFields);

        newCfg.getResult().getFields().forEach(
                f-> {
                    if (f.isCommonDate() && f.isAppend()) {
                        //公共日期为计算字段引入，粒度不准问题修复。
                        f.setQueryDateGranularity(this.config.getSettings().getDateGranularity());
                    }
                }
        );

        // 设置模板id
        SSDQueryTemplate tplEntity = new SSDQueryTemplate();
        if(config.getTemplateEntity() != null){
            tplEntity.setId(config.getTemplateEntity().getId() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
            tplEntity.setName(config.getTemplateEntity().getName() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
            tplEntity.setConfig(config.getTemplateEntity().getConfig());
        }
        newCfg.setConfig(config.getConfig());
        newCfg.setTemplateEntity(tplEntity);
        newCfg.getSettings().setDateGranularity(config.getSettings().getDateGranularity());

        return newCfg;
    }

    public FieldFilterType getFilterType(UIQueryField field){
        List<FieldValue> values = field.getValues();
        FieldFilterType filterType = FieldFilterType.None;
        if(BIUtil.isNotEmpty(values)){
            filterType = FieldFilterType.get(values.get(0).getFilterShowType());
        }
        MetaField meta = SSDMetaCacheManager.getField(field.getId());
        if(filterType == FieldFilterType.None && meta != null){
            filterType = FieldFilterType.get(meta.getFilterShowType());
        }
        return filterType;
    }

    public QueryConfigure getConfig() {
        return config;
    }

    public void setConfig(QueryConfigure config) {
        this.config = config;
    }

    public QueryContext getCxt() {
        return cxt;
    }

    public void setCxt(QueryContext cxt) {
        this.cxt = cxt;
    }
}
