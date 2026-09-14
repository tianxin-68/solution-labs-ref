package com.bi.queryer.ssm.llm.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.llm.dataset.agent.operator.AgentOperatorType;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;
import com.bi.queryer.ssm.llm.entity.agent.LLMAgentMetric;
import com.bi.queryer.ssm.llm.entity.agent.LLMAgentMetricConfig;
import com.bi.queryer.ssm.llm.function.FunctionFactory;
import com.bi.queryer.ssm.llm.function.IFunction;
import com.bi.queryer.ssm.llm.util.LLMUtil;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LLMConfigService {

    @Autowired
    protected QueryFieldService queryFieldService;

    @Autowired
    private BaseDao dao;

    //指标编码与id映射
    private Map<String,String> metricCodeIdMapping;

    public QueryConfigure createQueryConfigure(LLMQueryConfig llmQueryConfig) {

        metricCodeIdMapping = new HashMap<>();

        // 集团v2的数据集id
        String datasetId = SC.v("ssm.group.v2.datasetId", "68298a6d360c4c59ada307fd5fc0112c");
        Map<String, MetaField> fieldMap = buildDataSetFields(datasetId);
        QueryConfigure cfg = createQueryConfigure(llmQueryConfig, fieldMap);
        cfg.getSettings().setDatasetId(datasetId);

        return cfg;
    }

    protected Map<String, MetaField> buildDataSetFields(String datasetId){
        Map<String, MetaField> fieldMap = new HashMap<>();
        List treeNodes = queryFieldService.buildFieldTree(CategoryType.Front,datasetId);
        this.fetchFieldFromTreeNodes(treeNodes, fieldMap);
        return fieldMap;
    }

    protected void fetchFieldFromTreeNodes(List treeNodes, Map<String, MetaField> fieldMap){
        if(BIUtil.isEmpty(treeNodes)){
            return;
        }
        for(Object node : treeNodes){
            JSONObject nodeObject = (JSONObject) node;
            String type = nodeObject.getString("type");
            if("field".equalsIgnoreCase(type)){
                MetaField mf = JSON.parseObject(nodeObject.toJSONString(), MetaField.class);
                if(mf != null){
                    fieldMap.put(mf.getCode(), mf);
                }
            }
            JSONArray children = nodeObject.getJSONArray("children");
            fetchFieldFromTreeNodes(children, fieldMap);
        }
    }

    /**
     * @param llmQueryConfig
     * @return
     */
    protected QueryConfigure createQueryConfigure(LLMQueryConfig llmQueryConfig,  Map<String, MetaField> datasetFields) {

        //保存原始的指标配置
        llmQueryConfig.getResult().getOriginalMetrics().addAll(llmQueryConfig.getResult().getMetrics());

        normalizeLlmQueryConfig(llmQueryConfig);

        JSONObject queryConfig = new JSONObject();

        /** result **/
        JSONObject result = this.buildResultConfig(datasetFields, llmQueryConfig);
        queryConfig.put("result", result);

        /** filter **/
        JSONArray filter = this.buildFilterConfig(datasetFields, llmQueryConfig);
        queryConfig.put("filter", filter);

        /** analysis **/
        JSONObject analysis = this.buildAnalysisConfig(datasetFields, llmQueryConfig);
        queryConfig.put("analysis", analysis);

        /** setting **/
        JSONObject setting = this.buildSettingConfig(datasetFields, llmQueryConfig);
        queryConfig.put("setting", setting);

        SSDQueryTemplate templateEntity = new SSDQueryTemplate();
        templateEntity.setConfig(queryConfig.toJSONString());
        QueryConfigure cfg = new QueryConfigure(templateEntity);
        cfg.load();

        return cfg;
    }

    /**
     * 规范化llm查询配置
     * @return
     */
    protected LLMQueryConfig normalizeLlmQueryConfig(LLMQueryConfig llmQueryConfig) {
        IFunction function = FunctionFactory.get(llmQueryConfig);
        llmQueryConfig = function.normalizeLlmQueryConfig(llmQueryConfig);
        return llmQueryConfig;
    }

    protected JSONObject buildResultConfig(Map<String, MetaField> datasetFields,  LLMQueryConfig llmQueryConfig) {
        JSONObject result = new JSONObject();
        LLMQueryResult resultObject  = llmQueryConfig.getResult();
        if(resultObject == null) {
            return result;
        }
        // 避免重复添加
        Set<String> fieldCodesSet = new HashSet<>();
        // 行维度
        List<LLMQueryField> dimensions = resultObject.getDimensions();
        JSONArray rowDimensions = new JSONArray();
        if(BIUtil.isNotEmpty(dimensions)){
            int index = 0;
            for (LLMQueryField dimension : dimensions) {
                index++;
                String fieldCode = LLMUtil.getFieldCode(dimension); // dimensionJson.getString("field_code");
                if(fieldCodesSet.contains(fieldCode)){
                    continue;
                }

                //处理计算维度
                if (fieldCode.contains("[")) {

                    // 计算字段
                    String calcExpression = fieldCode;
                    Set<String> codeSet = getCalcFieldAtomicFieldCode(fieldCode);
                    for (String code : codeSet) {
                        MetaField metaField = getMetaFieldByCode(datasetFields, LLMUtil.getFieldCode(code));
                        // 转为id表达式
                        if (metaField != null) {
                            calcExpression = calcExpression.replaceAll("\\[" + code + "\\]", "[" + metaField.getId() + "]");
                        }
                    }
                    CustomFieldConfigure calcCfg = new CustomFieldConfigure();
                    calcCfg.setEnable(Enabled.YES.getId());
                    calcCfg.setExpression(calcExpression);
                    calcCfg.setRawExpression(fieldCode);
                    calcCfg.setIsMeasure(Enabled.NO.getId());

                    MetaField metaField = new MetaField();
                    metaField.setId("_x_rd_" + index);
                    metaField.setTitle(dimension.getField_title());
                    metaField.setCode(BIConsts.Custom_Field_Name_Suffix + QueryArea.RowDimension.getShortCode() + "_" + index);
                    JSONObject queryField = this.createQueryField(metaField);
                    queryField.put("customFieldConfigure", calcCfg);
                    rowDimensions.add(queryField);

                }else{

                    MetaField metaField = getMetaFieldByCode(datasetFields, fieldCode);
                    if(metaField == null){
                        continue;
                    }

                    JSONObject queryField = this.createQueryField(metaField);
                    if(Enabled.isTrue(metaField.getIsCommonDate())){
                        queryField.put("isAggQuery", 1);
                        queryField.put("isShow", 0);
                    }
                    rowDimensions.add(queryField);
                }

                fieldCodesSet.add(fieldCode);
            }
        }

        // 指标
        List<LLMQueryField> metrics = resultObject.getMetrics();
        JSONArray measures = new JSONArray();
        if(BIUtil.isNotEmpty(metrics)) {
            int index = 0;
            for (LLMQueryField metric : metrics) {
                index++;
                String fieldCode = LLMUtil.getFieldCode(metric); //metricJson.getString("field_code");
                if (isAnalysisField(fieldCode)) {
                    //continue;
                    // 若是分析字段：不能直接过滤，因为llm可能只传了分析字段
                    // 从分析字段提取原生字段code
                    fieldCode = LLMUtil.getSourceFieldCodeFromAnalysisFieldCode(fieldCode);
                }
                if (fieldCodesSet.contains(fieldCode)) {
                    continue;
                }
                if (fieldCode.contains("[")) {

                    AgentOperatorType agentOperatorType = AgentOperatorType.getByCode(fieldCode);
                    if (AgentOperatorType.UNKNOWN != agentOperatorType) {
                        continue;
                    }

                    // 计算字段
                    String calcExpression = fieldCode;
                    Set<String> codeSet = getCalcFieldAtomicFieldCode(fieldCode);
                    for (String code : codeSet) {
                        MetaField metaField = getMetaFieldByCode(datasetFields, LLMUtil.getFieldCode(code));

                        // 转为id表达式
                        if (metaField != null) {
                            //兼容日均
                            if (code.endsWith(AggExpressionType.Avg_By_Day.getCode())) {
                                calcExpression = calcExpression.replaceAll("\\[" + code + "\\]",
                                        "[" + metaField.getId() + "_" + AggExpressionType.Avg_By_Day.getCode() + "]");
                            } else {
                                calcExpression = calcExpression.replaceAll("\\[" + code + "\\]", "[" + metaField.getId() + "]");
                            }

                        }
                    }
                    CustomFieldConfigure calcCfg = new CustomFieldConfigure();
                    calcCfg.setEnable(Enabled.YES.getId());
                    calcCfg.setExpression(calcExpression);
                    calcCfg.setRawExpression(fieldCode);
                    calcCfg.setIsMeasure(Enabled.YES.getId());
                    calcCfg.setRatio(true);
                    calcCfg.setDecimal(2);

                    MetaField metaField = new MetaField();
                    metaField.setId("_x_" + index);
                    metaField.setTitle(metric.getField_title());
                    metaField.setCode(BIConsts.Custom_Field_Name_Suffix + QueryArea.Measure.getShortCode() + "_" + index);
                    JSONObject queryField = this.createQueryField(metaField);
                    queryField.put("customFieldConfigure", calcCfg);
                    measures.add(queryField);

                    metricCodeIdMapping.put(fieldCode, metaField.getId());

                } else {
                    MetaField metaField = getMetaFieldByCode(datasetFields, fieldCode);
                    if (metaField == null) {
                        continue;
                    }

                    JSONObject queryField = this.createQueryField(metaField);

                    //处理日均
                    if (fieldCode.endsWith(AggExpressionType.Avg_By_Day.getCode())) {
                        queryField.put("id", metaField.getId() + "_" + AggExpressionType.Avg_By_Day.getCode());
                        queryField.put("code", metaField.getCode() + "_" + AggExpressionType.Avg_By_Day.getCode());
                        queryField.put("aggExpressionType", AggExpressionType.Avg_By_Day.getCode());
                    }

                    measures.add(queryField);

                    metricCodeIdMapping.put(queryField.get("code").toString(), queryField.get("id").toString());
                }


                fieldCodesSet.add(fieldCode);
            }
        }
        result.put("rowDimensions", rowDimensions);
        result.put("measures", measures);
        return result;
    }

    protected boolean isAnalysisField(String fieldTitleOrCode){
        if(BIUtil.isEmpty(fieldTitleOrCode)){
            return false;
        }
        for (AnalysisCalcMode calcMode : AnalysisCalcMode.values()){
            if(fieldTitleOrCode.contains("_" + calcMode.getCode() + "_")){
                return true;
            }
        }
        return false;
    }

    protected Set<String> getCalcFieldAtomicFieldCode(String expression){
        Set<String> codeSet = new HashSet<String>();
        // 不处理分析字段
        if(isAnalysisField(expression)){
            return codeSet;
        }
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()){
            codeSet.add(matcher.group());
        }
        return codeSet;
    }

    protected JSONArray buildFilterConfig(Map<String, MetaField> datasetFields,  LLMQueryConfig llmQueryConfig) {
        JSONArray filter = new JSONArray();
        List<LLMQueryField> llmFilterArray = getLLMFilterFieldList(llmQueryConfig);
        if(llmFilterArray == null) {
            return filter;
        }
        JSONArray filterFields = new JSONArray();
        if(BIUtil.isNotEmpty(llmFilterArray)){
            for (LLMQueryField filterItem : llmFilterArray) {
                String fieldCode = LLMUtil.getFieldCode(filterItem); // filterItemJson.getString("field_code");
                MetaField metaField = getMetaFieldByCode(datasetFields, fieldCode);
                if(metaField == null){
                    continue;
                }
                String operator = filterItem.getOperator();
                if (BIUtil.isNotEmpty(operator) && (
                        operator.equals(">") || operator.equals(">=") ||
                                operator.equals("<") || operator.equals("<=")
                )) {
                    // 指标值过滤放到最后处理
                    continue;
                }
                JSONObject queryField = createQueryField(metaField);

                if (BIUtil.isNotEmpty(operator)){
                    operator = operator.toLowerCase();
                    if(operator.contains("not") || operator.contains("!=") || operator.contains("<>") || operator.contains("exclude")){
                        queryField.put("filterValueType", FieldValueFilterType.exclude.toString());
                    }
                }

                List<Object> values = filterItem.getFilter_values();
                if(values != null){
                    JSONArray filterItemValues = new JSONArray();

                    // 日期特殊处理
                    if(Enabled.isTrue(metaField.getIsCommonDate()) && values.size() == 1){
                        values.add(values.get(0));
                    }
                    for (Object value : values){
                        JSONObject valueItem = new JSONObject();
                        valueItem.put("id", value);
                        valueItem.put("title", value);
                        filterItemValues.add(valueItem);
                    }
                    queryField.put("values", filterItemValues);
                }
                filterFields.add(queryField);
            }
        }
        filter = filterFields;
        return filter;
    }

    /**
     * 封装llm过滤参数
     * 大模型传递的过滤参数+当前查询配置的过滤
     * @param llmQueryConfig
     * @return
     */
    protected List<LLMQueryField> getLLMFilterFieldList( LLMQueryConfig llmQueryConfig) {
        List<LLMQueryField> llmFilterArray = llmQueryConfig.getFilter();

        try {

            String wfSessionId = llmQueryConfig.getSettings().getWfSessionId();
            String wfAnalysisFilterStr = (String) dao.queryObject("llm.workflow.session.getWfAnalysisFilterById", wfSessionId, DataSourceType.AGENT);

            List<LLMQueryField> wfAnalysisFilter = JSONArray.parseArray(wfAnalysisFilterStr, LLMQueryField.class);
            if (CollUtil.isNotEmpty(wfAnalysisFilter)) {
                Map<String, LLMQueryField> llmFilterMap = new HashMap<>();
                for (LLMQueryField item : llmFilterArray) {
                    llmFilterMap.put(item.getField_code(), item);
                }

                for (LLMQueryField item : wfAnalysisFilter) {
                    if (llmFilterMap.containsKey(item.getField_code())) {
                        continue;
                    }

                    llmFilterArray.add(item);
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return llmFilterArray;
    }

    protected JSONObject buildAnalysisConfig(Map<String, MetaField> datasetFields, LLMQueryConfig llmQueryConfig) {

        IFunction function = FunctionFactory.get(llmQueryConfig);

        JSONObject analysisConfig =  new JSONObject();
        LLMQueryResult resultObject = llmQueryConfig.getResult();
        if(resultObject == null) {
            return analysisConfig;
        }
        List<LLMQueryField> metrics = resultObject.getMetrics();
        Set<String> calcModes = new HashSet<>();
        if(BIUtil.isNotEmpty(metrics)){
            for (LLMQueryField metric : metrics) {
                String titleOrCode = LLMUtil.getFieldCode(metric); //metricJson.getString("field_code");
                if(BIUtil.isEmpty(titleOrCode)){
                    continue;
                }
                for (AnalysisCalcMode calcMode : AnalysisCalcMode.values()){
                    if(titleOrCode.contains("_" + calcMode.getCode() + "_")){
                        calcModes.add(calcMode.getCode());
                    }
                }
            }

            calcModes = function.appendCalcModes(calcModes);
        }

        List<LLMAgentMetric> agentMetrics = (List<LLMAgentMetric>)dao.queryObjectList("llm.metric.queryAll", null,DataSourceType.AGENT);
        Map<String, LLMAgentMetricConfig> agentMetricConfigMap = new HashMap<>();
        if(CollUtil.isNotEmpty(agentMetrics)) {
            for (LLMAgentMetric metric : agentMetrics) {
                if (StrUtil.isEmpty(metric.getMetricConfig())) {
                    continue;
                }

                LLMAgentMetricConfig agentMetricConfig = JSON.parseObject(metric.getMetricConfig(), LLMAgentMetricConfig.class);
                if (agentMetricConfig == null) {
                    continue;
                }

                agentMetricConfigMap.put(metric.getMetricCode(), agentMetricConfig);
            }
        }

        if(BIUtil.isNotEmpty(calcModes)) {
            JSONObject thbCfg = new JSONObject();
            thbCfg.put("isActive", 1);

            List<String> measureIds = new ArrayList<>();
            List<String> percentMeasureIds = new ArrayList<>();

            for (String metricCode : metricCodeIdMapping.keySet()) {

                String metricId = metricCodeIdMapping.get(metricCode);
                LLMAgentMetricConfig agentMetricConfig = agentMetricConfigMap.get(metricCode);
                if (agentMetricConfig != null) {
                    if ("%".equalsIgnoreCase(agentMetricConfig.getPercentFieldRatioUnit())) {
                        percentMeasureIds.add(metricId);
                        continue;
                    }
                }

                measureIds.add(metricId);
            }

            JSONArray items = new JSONArray();

            if(CollUtil.isNotEmpty(measureIds)){
                JSONObject itemObject = buildItemObject(calcModes, measureIds, "pt");
                itemObject = function.appendThbItem(itemObject, datasetFields);
                items.add(itemObject);
            }

            if(CollUtil.isNotEmpty(percentMeasureIds)){
                JSONObject percentItemObject = buildItemObject(calcModes, percentMeasureIds, "%");
                percentItemObject = function.appendThbItem(percentItemObject, datasetFields);
                items.add(percentItemObject);
            }

            thbCfg.put("items", items);
            analysisConfig.put("thb", thbCfg);
        }

        analysisConfig = function.appendTotalZb(analysisConfig);
        return analysisConfig;
    }

    public JSONObject buildItemObject(Set<String> calcModes ,List<String> measureIdList,String percentFieldRatioUnit) {
        JSONObject itemObject = new JSONObject();
        itemObject.put("measureIdList", measureIdList);
        boolean hasCtr = calcModes.contains(AnalysisCalcMode.CONTRIBUTION_RATE.getCode()); // 是否有贡献率计算
        if (hasCtr) {
            itemObject.put("calcTypes", ListUtil.toList("r_value", "ratio", "value", "ctr"));
        } else {
            itemObject.put("calcTypes", ListUtil.toList("r_value", "ratio", "value"));
        }
        calcModes.remove("ctr");
        itemObject.put("calcModes", calcModes);
        itemObject.put("percentFieldRatioUnit", percentFieldRatioUnit);

        if (hasCtr) {
            JSONObject ctrObject = new JSONObject();
            ctrObject.put("items", ListUtil.toList("all"));
            itemObject.put("ctr", ctrObject);
        }

        return itemObject;
    }

    protected JSONObject buildSettingConfig(Map<String, MetaField> datasetFields,  LLMQueryConfig llmQueryConfig) {
        JSONObject setting = new JSONObject();
        setting.put("isAggQuery", 1);
        return setting;
    }

    protected MetaField getMetaFieldByCode(Map<String, MetaField> metaFields, String code) {

        String finalCode = code;

        //日均特殊处理
        if (code.endsWith(AggExpressionType.Avg_By_Day.getCode())) {
            finalCode = code.replaceAll("_"+ AggExpressionType.Avg_By_Day.getCode(), "");
        }

        MetaField mf = metaFields.get(finalCode);
        if (mf != null) {
            return mf;
        }

        // 日期字段特殊处理
        if ("dt".equalsIgnoreCase(finalCode)) {
            List<MetaField> dtFields = SSDMetaCacheManager.getFieldByCode("dt");
            if (BIUtil.isNotEmpty(dtFields)) {
                return dtFields.get(0);
            }
        }

        //通过名称找一次
        if (mf == null) {
            mf = SSDMetaCacheManager.getFieldByTitle(finalCode);
        }

        return mf;
    }

    protected JSONObject createQueryField(MetaField metaField) {
        JSONObject field = new JSONObject();
        field.put("id", metaField.getId());
        field.put("code", metaField.getCode());
        field.put("title", metaField.getTitle());
        field.put("moduleCtgId", metaField.getModuleCtgId());
        field.put("isAggQuery", 0);
        if ("dt".equalsIgnoreCase(metaField.getCode())) {
            field.put("isAggQuery", 1);
        }
        field.put("queryDateGranularity", "d");
        field.put("isShow", 1);
        field.put("values", new JSONArray());
        return field;
    }

}
