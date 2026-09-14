package com.bi.queryer.ssm.llm;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.chart.ChartQueryConfigureBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryFilter;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.enums.FieldValueFilterType;
import com.bi.queryer.ssm.llm.config.LLMConfigService;
import com.bi.queryer.ssm.llm.dataset.LLMDatasetService;
import com.bi.queryer.ssm.llm.entity.LLMAnalysisDate;
import com.bi.queryer.ssm.llm.entity.LLMAnalysisMetric;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.req.BuildAgentFilterReq;
import com.bi.queryer.ssm.llm.resp.AgentQueryField;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LLMAgentApiService {

    @Autowired
    private LLMConfigService llmConfigService;

    // key=[code1]/[code2] , value=fieldCode
    protected Map<String, String> calcExpressionCodes = new HashMap<>();

    public JSONObject query(String llmQueryConfigStr,QueryContext cxt) {

        JSONObject datasetJson = new JSONObject();

        try {

            LLMQueryConfig llmQueryConfig = JSON.parseObject(llmQueryConfigStr, LLMQueryConfig.class);
            QueryConfigure cfg = llmConfigService.createQueryConfigure(llmQueryConfig);
            // 计算字段的表达式找到对应的code
            buildCalcExpressionCodeMapping(cfg);

            QueryEngine engine = QueryFactory.createEngine(cfg, cxt);
            DataSourceRouter.setQueryEngineDefaultDataSource(engine);
            ResultDataSet dataSet = (ResultDataSet) engine.execute().getData();

            LLMDatasetService llmDatasetService = new LLMDatasetService(dataSet, llmQueryConfig, calcExpressionCodes);
            datasetJson = llmDatasetService.build();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DataSourceRouter.removeCurrentDataSourceType();
        }

        return datasetJson;
    }

    /**
     * key=expression([code1]/[code2]),value=fieldCode
     * @return
     */
    protected void buildCalcExpressionCodeMapping(QueryConfigure cfg){
        List<QueryField> metricFields = cfg.getResult().getMeasures();
        for(QueryField f : metricFields){
            if(!f.isCalc()){
                continue;
            }
            String rawExpr = f.getCustomFieldConfigure().getRawExpression();
            if(BIUtil.isEmpty(rawExpr)){
                continue;
            }
            calcExpressionCodes.put(rawExpr, f.getCode());
        }
    }

    public JSONObject getAnalysisObject(QueryConfigure queryConfigure, LLMAnalysisMetric metrics, LLMAnalysisDate analysisDate) {
        List<String> analysisObject = new ArrayList<>();

        // 分析的指标
        if (metrics != null) {
            String metricTitle = metrics.getTitle();
            String metricCode = metrics.getCode();
            if ("D_ORD_02745".equalsIgnoreCase(metricCode)) {
                metricTitle = "APP_轮胎商品关支率";
            }

            if ("轮胎-销售商品件数B0".equalsIgnoreCase(metricTitle)) {
                metricTitle = "APP_轮胎商品关支率";
            }

            analysisObject.add(String.format("指标：%s", metricTitle));
        }

        List<String> paramDateValues = new ArrayList<>();
        if (analysisDate != null) {
            List<String> values = analysisDate.getValues();
            if (values != null) {
                paramDateValues.addAll(values);
            }
        }

        // 查询配置的过滤条件
        QueryFilter filter = queryConfigure.getFilter();
        //传给大模型的过滤类型
        List<LLMQueryField> llmFilterFields = new ArrayList<>();

        List<String> dimensionFilterInfos = new ArrayList<>();
        List<String> metricFilterInfos = new ArrayList<>();
        for (QueryField field : filter.getFields()) {
            List<FieldValue> values = field.getValues();
            if (BIUtil.isEmpty(values)) {
                continue;
            }
            MetaField mf = field.getMeta();

            LLMQueryField llmQueryField = new LLMQueryField(field.getCode(), field.getTitle());
            llmQueryField.setFilter_values(values.stream().map(FieldValue::getId).collect(Collectors.toList()));
            llmQueryField.setOperator(field.getFilterValueType());
            llmFilterFields.add(llmQueryField);

            if(Enabled.value(mf.getIsMeasure())){
                metricFilterInfos.add(String.format("%s:%s",field.getTitle(),field.getValuesTitle()));
            }else{
                // 公共日期
                if (Enabled.isTrue(mf.getIsCommonDate())) {
                    String value1 = values.get(0).getTitle();
                    String value2 = null;
                    if (values.size() == 1) {
                        value2 = value1;
                    } else {
                        value2 = values.get(1).getTitle();
                    }
                    if (BIUtil.isNotEmpty(paramDateValues)) {
                        value1 = paramDateValues.get(0);
                        value2 = paramDateValues.get(paramDateValues.size() - 1);
                    } else {
                        // 传递给前端
                        paramDateValues.add(value1);
                        paramDateValues.add(value2);
                    }
                    // 覆盖查询配置的日期过滤值
                    llmQueryField.setFilter_values(Arrays.asList(new String[]{value1, value2}));
                    dimensionFilterInfos.add(String.format("日期：%s 到 %s", value1, value2));
                } else {
                    FieldValueFilterType operator = FieldValueFilterType.include;
                    FieldValueFilterType valueFilterType = FieldValueFilterType.get(field.getFilterValueType());
                    if (valueFilterType == FieldValueFilterType.exclude) {
                        operator = FieldValueFilterType.exclude;
                    }
                    List<String> valueTitles = values.stream().map(FieldValue::getTitle).collect(Collectors.toList());
                    dimensionFilterInfos.add(String.format("%s：%s %s", mf.getTitle(), operator == FieldValueFilterType.include ? "等于" : "不等于", BIUtil.listToStr(valueTitles, ",", "\"")));
                }
            }

        }

        if (BIUtil.isNotEmpty(dimensionFilterInfos)) {
            analysisObject.add("维度过滤：");
            analysisObject.addAll(dimensionFilterInfos);
        }

        if (CollUtil.isNotEmpty(metricFilterInfos)) {
            analysisObject.add("指标过滤：");
            analysisObject.addAll(metricFilterInfos);
        }

        JSONObject analysisInfo = new JSONObject();
        analysisInfo.put("analysisObject", BIUtil.listToStr(analysisObject, "\n"));

        LLMAnalysisDate resultAnalysisDate = new LLMAnalysisDate();
        resultAnalysisDate.setValues(paramDateValues);
        analysisInfo.put("analysisDate", resultAnalysisDate);
        analysisInfo.put("analysisFilter", JSON.toJSONString(llmFilterFields));

        return analysisInfo;
    }

    /**
     * 构建大模型过滤条件
     * @return
     */
    public List<AgentQueryField> buildAgentFilter(BuildAgentFilterReq req) {

        List<AgentQueryField> result = new ArrayList<>();

        //过滤去掉计算指标
        //过滤掉计算维度
        List<QueryField> globalFilters = req.getGlobalFilters().stream()
                .filter(
                        f ->
                        FieldType.CUSTOM_MEASURE != FieldType.get(f.getFieldType()) &&
                        FieldType.CUSTOM_DIM != FieldType.get(f.getFieldType()) &&
                        !(StringUtils.endsWith(f.getCode(), AggExpressionType.Avg_By_Day.getCode()) || StringUtils.endsWith(f.getCode(), AggExpressionType.Avg_By_Day_Real.getCode()))
                )
                .collect(Collectors.toList());

        List<QueryField> filters = req.getFilters().stream()
                .filter(
                        f ->
                        FieldType.CUSTOM_MEASURE != FieldType.get(f.getFieldType()) &&
                        FieldType.CUSTOM_DIM != FieldType.get(f.getFieldType()) &&
                        !(StringUtils.endsWith(f.getCode(), AggExpressionType.Avg_By_Day.getCode()) || StringUtils.endsWith(f.getCode(), AggExpressionType.Avg_By_Day_Real.getCode()))
                )
                .collect(Collectors.toList());

        QueryFilter queryFilter = ChartQueryConfigureBuilder.buildQueryFilter(globalFilters, filters);

        for (QueryField queryField : queryFilter.getFields()) {

            if (CollUtil.isEmpty(queryField.getValues())) {
                continue;
            }

            AgentQueryField agentQueryField = new AgentQueryField();
            agentQueryField.setCode(queryField.getCode());
            agentQueryField.setName(queryField.getTitle());

            agentQueryField.setFieldType("dim");
            if (Enabled.value(queryField.getMeta().getIsMeasure())) {
                agentQueryField.setFieldType("measure");
            }

            agentQueryField.setFilterValueType(queryField.getFilterValueType());
            agentQueryField.setFilterValues(queryField.getValues().stream().map(FieldValue::getId).collect(Collectors.toList()));

            agentQueryField.setFilterType(FieldUtil.getFilterType(queryField).getCode());
            agentQueryField.setFilterValueMode(queryField.getFilterValueMode());
            agentQueryField.setFilterQueryRule(queryField.getFilterQueryRule());

            result.add(agentQueryField);
        }

        return result;
    }

}
