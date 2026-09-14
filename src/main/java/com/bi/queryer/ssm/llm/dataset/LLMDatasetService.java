package com.bi.queryer.ssm.llm.dataset;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.llm.dataset.agent.operator.AgentOperatorFactory;
import com.bi.queryer.ssm.llm.dataset.agent.operator.impl.BaseAgentOperator;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryTop;
import com.bi.queryer.ssm.llm.function.FunctionFactory;
import com.bi.queryer.ssm.llm.function.FunctionType;
import com.bi.queryer.ssm.llm.function.IFunction;
import com.bi.queryer.ssm.llm.util.LLMUtil;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

import static cn.hutool.poi.excel.sax.ElementName.row;

public class LLMDatasetService {

    private LLMQueryConfig llmQueryConfig;
    private ResultDataSet dataSet;
    private Map<String, String> calcExpressionCodes;

    public LLMDatasetService(ResultDataSet dataSet,LLMQueryConfig llmQueryConfig,Map<String, String> calcExpressionCodes){
        this.dataSet = dataSet;
        this.llmQueryConfig = llmQueryConfig;
        this.calcExpressionCodes = calcExpressionCodes;
    }


    public JSONObject build() {

        if (dataSet == null) {
            return null;
        }

        IFunction function = FunctionFactory.get(llmQueryConfig);
        dataSet = function.appendDataSet(dataSet);

        BaseAgentOperator agentOperator = AgentOperatorFactory.get(llmQueryConfig);
        dataSet = agentOperator.appendDataSet(dataSet);

        dataSet = filterDataSet(dataSet, llmQueryConfig);
        // 格式化数据集
        JSONObject datasetJson = formatDataSet(dataSet);
        return datasetJson;
    }

    /**
     * 过滤数据集：
     * 1、指标值大小
     * 2、去掉总计、小计
     * 3、TopN
     * @param dataSet
     * @return
     */
    protected ResultDataSet filterDataSet(ResultDataSet dataSet,LLMQueryConfig llmQueryConfig) {
        ResultDataSet finalDataset = new ResultDataSet();
        if (dataSet == null) {
            return finalDataset;
        }

        // 先过滤掉所有维度都未null的记录
        dataSet = filterDimensionNullRows(dataSet, llmQueryConfig);

        List<Map<String, Object>> rows = dataSet.getRows();
        List<Map<String, Object>> finalRows = new ArrayList<>();

        List<LLMQueryField> llmFilterArray = llmQueryConfig.getFilter();
        Map<String, LLMQueryField> metricFilterMap = new HashMap<>();
        if (BIUtil.isNotEmpty(llmFilterArray)) {
            for (LLMQueryField filterItem : llmFilterArray) {

                List<Object> values = filterItem.getFilter_values();
                if (BIUtil.isEmpty(values)) {
                    continue;
                }
                String operator = filterItem.getOperator();
                if (BIUtil.isNotEmpty(operator) && (
                        operator.equals(">") || operator.equals(">=") ||
                                operator.equals("<") || operator.equals("<=")
                )) {
                    // fieldCode=[code1]/[code2] -> _cstm_01
                    // fieldCode=[code1]/[code2]_yw_ratio -> _cstm_01_yw_ratio
                    String fieldCode = LLMUtil.getFieldCode(filterItem);//filterItemJson.getString("field_code");
                    if (fieldCode != null && fieldCode.contains("[")) {
                        fieldCode = this.expressionCode2FieldCode(fieldCode);
                    }
                    metricFilterMap.put(fieldCode, filterItem);
                }
            }
        }

        for (Map<String, Object> row : rows) {
            // 去掉总计、小计
            Object groupKey = row.get(BIConsts.GROUPING_KEY);
            if (groupKey != null && Double.valueOf(groupKey + "") > 0) {
                continue;
            }

            // 过滤指标值
            boolean isAccept = true;
            for (String metricCode : metricFilterMap.keySet()) {
                Object value = row.get(metricCode);
                if (value == null) {
                    isAccept = false;
                    continue;
                }
                LLMQueryField filterItem = metricFilterMap.get(metricCode);
                List<Object> compareValues = filterItem.getFilter_values();
                String operator = filterItem.getOperator();
                Double compareValue = LLMUtil.toDouble(compareValues.get(0));
                if (compareValue == null) {
                    isAccept = false;
                    continue;
                }
                Double datasetValue = LLMUtil.toDouble(value);
                if (datasetValue == null) {
                    isAccept = false;
                    continue;
                }

                if (operator.equals(">") && datasetValue <= compareValue) {
                    isAccept = false;
                    continue;
                }
                if (operator.equals("<") && datasetValue >= compareValue) {
                    isAccept = false;
                    continue;
                }
                if (operator.equals(">=") && datasetValue < compareValue) {
                    isAccept = false;
                    continue;
                }
                if (operator.equals("<=") && datasetValue > compareValue) {
                    isAccept = false;
                    continue;
                }
            }
            if (isAccept) {
                finalRows.add(row);
            }
        }

        // 排序取TopN
        LLMQueryTop topConfig = llmQueryConfig.getTop();
        if (topConfig != null) {
            String sortFieldCode = this.expressionCode2FieldCode(topConfig.getSort_field());
            String sortType = topConfig.getSort_type();
            Integer limit = topConfig.getLimit();

            if(StrUtil.isNotEmpty(sortFieldCode) && StrUtil.isNotEmpty(sortType)) {

                finalRows = finalRows.stream().filter(row -> LLMUtil.toDouble(row.get(sortFieldCode)) != null).collect(Collectors.toList());

                // 排序
                finalRows.sort(new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        if (o1.get(sortFieldCode) == null || o2.get(sortFieldCode) == null) {
                            return -1;
                        }
                        Double v1 = LLMUtil.toDouble(o1.get(sortFieldCode)); //NumberUtil.toBigDecimal(o1.get(sortFieldCode) + "");
                        Double v2 = LLMUtil.toDouble(o2.get(sortFieldCode));//NumberUtil.toBigDecimal(o2.get(sortFieldCode) + "");

                        if ("true".equalsIgnoreCase(topConfig.getIsAbs())) {
                            v1 = Math.abs(v1);
                            v2 = Math.abs(v2);
                        }

                        int flag = "asc".equalsIgnoreCase(sortType) ? 1 : -1;
                        return v1.compareTo(v2) * flag;
                    }
                });

            }

            // 取topN
            if (limit != null && limit > 0 && finalRows.size() >= limit) {
                finalRows = finalRows.subList(0, limit);
            }
        }
        finalDataset.setRows(finalRows);
        finalDataset.setColumns(dataSet.getColumns());
        return finalDataset;
    }

    /**
     * 过滤掉维度都是null的记录
     * @param dataSet
     * @param llmQueryConfig
     * @return
     */
    protected ResultDataSet filterDimensionNullRows(ResultDataSet dataSet, LLMQueryConfig llmQueryConfig){
        List<Map<String, Object>> rows = dataSet.getRows();
        List<ResultDataSetColumn> columns = dataSet.getColumns();
        List<Map<String, Object>> finalRows = new ArrayList<>();

        List<ResultDataSetColumn> dimensionColumns = columns.stream().filter(c-> QueryArea.get(c.getRawQueryArea()) == QueryArea.RowDimension).collect(Collectors.toList());
        if(BIUtil.isEmpty(dimensionColumns)){
            return dataSet;
        }

        for(Map<String, Object> row : rows){
            boolean isAllDimensionNullValue = true;
            for(ResultDataSetColumn col : dimensionColumns){
                if(QueryArea.get(col.getRawQueryArea()) == QueryArea.RowDimension){
                    String cellValue = row.get(col.getRawCode()) + "";
                    boolean isNullValue = (BIUtil.isEmpty(cellValue) || BIConsts.NULL_VALUE.equalsIgnoreCase(cellValue));
                    isAllDimensionNullValue = isAllDimensionNullValue && isNullValue;
                }
            }
            if(!isAllDimensionNullValue){
                finalRows.add(row);
            }
        }
        dataSet.setRows(finalRows);
        return dataSet;
    }

    /**
     * 1、将数据集的表头打平
     * @param dataSet
     * @return
     */
    protected JSONObject formatDataSet(ResultDataSet dataSet){
        JSONObject resultDataset = new JSONObject();
        if(dataSet == null){
            return resultDataset;
        }
        // 将列去掉层级
        List<ResultDataSetColumn> leafColumns = new ArrayList<>();
        getLeafColumns(dataSet.getColumns(), "", leafColumns);

        // 去掉不需要的信息
        List<LLMQueryField> originalMetrics = this.llmQueryConfig.getResult().getOriginalMetrics();
        List<String> originalMetricsCodes = new ArrayList<>();
        for(LLMQueryField metric : originalMetrics) {
            String metricsCode = expressionCode2FieldCode(metric.getField_code());
            originalMetricsCodes.add(metricsCode);
        }

        JSONArray finalColumns = new JSONArray();
        List<String> finalCodeList = new ArrayList<>();
        for (ResultDataSetColumn leafColumn : leafColumns) {

            //预设指标不过滤
            FunctionType functionType = FunctionType.getByCode(leafColumn.getCode());
            if(FunctionType.UNKNOWN == functionType){
                //指标只返回实际请求的列
                if(QueryArea.Measure == QueryArea.get(leafColumn.getRawQueryArea())) {
                    String columnCode =leafColumn.getCode();
                    if (!originalMetricsCodes.contains(columnCode)) {
                        continue;
                    }
                }
            }

            JSONObject columnObject = new JSONObject();
            columnObject.put("code", leafColumn.getCode());
            columnObject.put("title", leafColumn.getTitle());
            columnObject.put("dataType", leafColumn.getDataType());
            columnObject.put("type", leafColumn.getType());
            finalCodeList.add(leafColumn.getCode());
            finalColumns.add(columnObject);
        }

        List<Map<String, Object>> fieldRows = new ArrayList<>();
        for(Map<String, Object> dataRow :dataSet.getRows()){
            Map<String, Object> row = new HashMap<>();
            for( String code : finalCodeList){
                row.put(code, dataRow.get(code));
            }
            fieldRows.add(row);
        }

        resultDataset.put("rows", fieldRows);
        resultDataset.put("columns", finalColumns);
        return resultDataset;
    }

    protected void getLeafColumns(List<ResultDataSetColumn> columns, String parentTitle, List<ResultDataSetColumn> leafColumns){
        if(BIUtil.isEmpty(columns)){
            return;
        }
        for(ResultDataSetColumn c : columns){
            List<ResultDataSetColumn> children = c.getChildren();
            String title = c.getTitle();
            if(BIUtil.isNotEmpty(parentTitle)){
                title = parentTitle + "_" + title;
            }
            if(BIUtil.isNotEmpty(children)) {
                this.getLeafColumns(children, title, leafColumns);
            }else{
                c.setTitle(title);
                leafColumns.add(c);
            }
        }
    }

    protected String expressionCode2FieldCode(String expressionCode){
        String fieldCode = "";
        if(expressionCode != null && expressionCode.contains("[")){
            // 计算字段需要替换code
            String rawCodeExpression = LLMUtil.getSourceFieldCodeFromAnalysisFieldCode(expressionCode);
            if(calcExpressionCodes.containsKey(rawCodeExpression)) {
                String customRealFieldCode = calcExpressionCodes.get(rawCodeExpression);
                fieldCode = expressionCode.replace(rawCodeExpression, customRealFieldCode);
            }
        }else{
            fieldCode = expressionCode;
        }
        return fieldCode;
    }

}
