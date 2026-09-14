package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.util.NumberUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:38 2023-08-01
 * @Description 行总计
 **/
public class RowTotalOperator extends TotalOperator {

    /**
     * 行总计计算
     * - 明细表：所有指标相加
     * - 交叉表：行汇总在上一步已计算，此处只需引用：因为交叉表涉及去重指标，所以必须在模型构建sql时处理行总计
     * @param cxt
     * @param measureField
     * @return
     */
    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        String expression = "";

        // 若指定了指标（如：占行总计），则指标直接相加
        if(cxt.analysisItemConfig instanceof AnalysisTotalItemConfig){
            if(((AnalysisTotalItemConfig)cxt.analysisItemConfig).isZbTotal()){
                // return this.total(cxt, measureField);
            }
        }
        expression = String.format("%s.%s", cxt.currentDataSet.getName(), measureField.getCode());
        /*
        if(cxt.isCrossDimensionQuery) {
            // 交叉表：行汇总在上一步已计算，此处只需引用：因为交叉表涉及去重指标，所以必须在模型构建sql时处理行总计
           expression = String.format("%s.%s", cxt.currentDataSet.getName(), measureField.getCode());
        }else{
            // 明细表：所有指标相加
            List<String>  totalItems = cxt.measureFields.stream().map(
                    f-> function.coalesce(
                            String.format("%s.%s", cxt.currentDataSet.getName(),f.getCode()),
                            "0")
                    ).collect(Collectors.toList());

            expression = BIUtil.listToStr(totalItems, "+");
        }
         */
        return expression;
    }

    /**
     * 预计算
     * @param config
     * @param measureField
     * @return
     */
    @Override
    public String preCalc(QueryConfigure config, QueryField measureField, Map<String, String> rawMeasureExpressions, List<StarModel> models) {
        if(rawMeasureExpressions ==  null || rawMeasureExpressions.isEmpty() || isCrossQuery(config)){
            // 交叉表：不处理，因为在交叉表转置时已处理
            return super.preCalc(config, measureField, rawMeasureExpressions, models);
        }

        List<String> fragments = new ArrayList<>();
        for(Map.Entry<String, String> rawMeasureExpression : rawMeasureExpressions.entrySet()){
            fragments.add(function.coalesce(rawMeasureExpression.getValue(), "0"));
        }

        String calcExpression = function.coalesce(BIUtil.listToStr(fragments, "+"));
        return calcExpression;
    }

    /**
     * 交叉维度：交叉维度项下的指定指标列相加
     * 明细：所有指标列相加
     * @param cxt
     * @param measureField
     * @return
     */
    @Override
    public String total(OperatorContext cxt, QueryField measureField) {
        /*
        Map<String, QueryField> measureFieldMap = cxt.measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField->QueryField, (f1, f2)->f1));
        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());

        List<String>  totalItems = cxt.measureFields.stream().filter(
                f->{
                    boolean isDenominator = !f.isAppend() && Enabled.isFalse(f.getIsAnalysis());
                    if(cxt.isCrossDimensionQuery){
                        isDenominator = isDenominator && rawMeasureField.getRawCode().equals(f.getRawCode());
                    }
                    return isDenominator;
                }).map(
                f->  function.coalesce(
                        String.format("%s.%s", cxt.currentDataSet.getName(),f.getCode()),
                        "0")
        ).collect(Collectors.toList());
        String calcExpression = BIUtil.listToStr(totalItems, "+");
         */
        String calcExpression = "";
        if(cxt.isCrossDimensionQuery){
            String analysisMeasureCode = measureField.getAnalysisConfig().getMeasureCode().split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
            calcExpression = String.format("%s.%s", cxt.currentDataSet.getName(), analysisMeasureCode + "_" + AnalysisCalcMode.ROW_TOTAL.getCode());
        }else {
            calcExpression = String.format("%s.%s", cxt.currentDataSet.getName(), BIConsts.ROW_TOTAL_COLUMN_CODE);
        }
        return calcExpression;
    }

    /**
     * 只有交叉表才支持
     * @param config
     * @param model
     * @return
     */
    @Override
    public List<String> groupingSets(QueryConfigure config, StarModel model) {
        // 若是占行总计，则指标不需要提前分组汇总，再最后一步直接相加
        AnalysisTotalItemConfig analysisItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.ROW_TOTAL);
        if(analysisItemConfig != null && analysisItemConfig.isZbTotal()){
            //return super.groupingSets(config, model);
        }

        List<QueryField> colFields = this.getColumnDimensions(config);
        List<String> groupingSets = super.groupingSets(config, model);

        // 无列维度时，行总计指标在会后一步相加，此处不做聚合处理
        if(BIUtil.isEmpty(colFields)) {
            return groupingSets;
        }

        List<QueryField> rowFields = this.getRowDimensions(config);
        if(BIUtil.isNotEmpty(rowFields)){
            rowFields = rowFields.stream().filter(f->!f.isAppend()).collect(Collectors.toList());
        }
        List<String> colFieldCodes = colFields.stream().map(f->f.getCode()).collect(Collectors.toList());
        List<String> groupFields = new ArrayList<>();
        String tableAlias = model.getFactTable().getAlias();
        for(QueryField rowField : rowFields){
            if(colFieldCodes.contains(rowField.getCode())){
                continue;
            }
            //tableAlias = rowField.getTable().getAlias();
            groupFields.add(String.format("%s.%s", tableAlias, rowField.getCode()));
        }
        String groupingSet = String.format("(%s)", BIUtil.listToStr(groupFields));
        groupingSets.add(groupingSet);

        //  若行总计+列总计时，需要出整表总计，用于计算行列总计右下角汇总值
        if(this.hasColumnTotalAndRowTotal(config)){
            groupingSets.add("()");
        }

        // 若行总计+列小计时，需按按小计维度退化一个维度再次聚合
        if(this.hasColumnSubtotalAndRowTotal(config)){
            List<List<String>> appendSubtotalGroupingFieldSets = this.getAppendColumnSubtotalGroupingFieldSet(config, model);
            for(List<String> subtotalGroupingFields : appendSubtotalGroupingFieldSets){
                if(BIUtil.isEmpty(subtotalGroupingFields)){
                    continue;
                }
                List<String> groupFieldNames = new ArrayList<>();
                for(String s : subtotalGroupingFields){
                    groupFieldNames.add(BIUtil.isEmpty(tableAlias) ? s : tableAlias + "." + s);
                }
//                groupingSet = String.format("(%s)", BIUtil.listToStr(subtotalGroupingFields));
                groupingSet = String.format("(%s)", BIUtil.listToStr(groupFieldNames));
                groupingSets.add(groupingSet);
            }
        }

        return groupingSets;
    }


    @Override
    public AnalysisTotalType getTotalType() {
        return AnalysisTotalType.ROW_TOTAL;
    }

    /**
     * 初始化配置：添加行总计列
     * - 明细表：只添加一个汇总列
     * - 交叉表：按指标添加汇总列，即每个指标都有一个汇总列
     * @param config
     * @param cxt
     */
    @Override
    public void initQueryConfig(QueryConfigure config, QueryContext cxt) {

    }


    @Override
    public boolean isSupportPivot(AnalysisItemConfig itemConfig) {
        return true;
    }

    /**
     * 交叉表时，将行总计转为
     * @return
     */
    @Override
    public String pivot(QueryConfigure config, QueryField colField) {

        String pivotExpression = "";
        List<String> conditions = new ArrayList<>();
        List<Integer> groupingValues = this.getGroupingValues(config);

        // 添加分组标识
        conditions.add(String.format("%s.%s in(%s)", BIConsts.PIVOT_TABLE_ALIAS, BIConsts.GROUPING_VALUE, BIUtil.listToStr(groupingValues)));
        String conditionExpression = BIUtil.listToStr(conditions, " and ");
        String expression = String.format("%s.%s", BIConsts.PIVOT_TABLE_ALIAS, colField.getCode());
        pivotExpression = function.ifExpression(conditionExpression, "'"+AnalysisTotalType.ROW_TOTAL.getCode()+"'", expression);
        return pivotExpression;
    }

    /**
     * 获取分组标识
     * @param config
     * @return
     */
    public List<Integer> getGroupingValues(QueryConfigure config) {
        List<Integer> groupingValues = new ArrayList<>();

        List<QueryField> colFields = this.getColumnDimensions(config);
        List<QueryField> rowFields = this.getRowDimensions(config);


        int groupingValue = AnalysisUtil.getGroupingValue(config, colFields);
        groupingValues.add(groupingValue);

        //  若有行总计+列总计时，需要出整表总计，用于计算行列总计交叉汇总值
        if (this.hasColumnTotalAndRowTotal(config) && BIUtil.isNotEmpty(rowFields)) {
            groupingValue = NumberUtil.binaryToInt(StringUtils.rightPad("", (rowFields.size() + colFields.size()), "1"));
            groupingValues.add(groupingValue);
        }

        //  若有行总计+列小计计时，需要出退化维度的列小计计，用于计算行列总计交叉汇总值
        if (this.hasColumnSubtotalAndRowTotal(config) && BIUtil.isNotEmpty(rowFields)) {
            groupingValues.addAll(this.getAppendColumnSubtotalGroupingValues(config));
        }

        return groupingValues;
    }

    /**
     * 获取转置后的字段名称
     * @param config
     * @param measure
     * @return
     */
    @Override
    public String getPivotFieldName(QueryConfigure config, QueryField measure) {
        return measure.getCode();
    }

    /**
     * 创建行总计结果列
     * - 明细表：1个行总计列
     * - 交叉表：每个结果指标一个列，且有一个父列
     * 列结构：
     * =====无同环比=====
     * --明细表---
     * 行总计
     * --交叉表---
     * 行总计
     * 指标1   指标2
     *
     * =====有同环比=====
     * --明细表---
     * 行总计
     * 本期值  环比  周同比
     * --交叉表---
     * 行总计
     * 指标1                  指标2
     * 本期值  环比  周同比     本期值  环比  周同比
     *
     * @param config
     * @return
     */
    @Override
    public List<ResultDataSetColumn> createTotalColumns(QueryConfigure config) {
        return super.createTotalColumns(config);
    }

    @Override
    public List<ResultDataSetColumn> createTotalLeafColumns(QueryConfigure config) {
        return super.createTotalLeafColumns(config);
    }

    /**
     * 是否同时有列总计和行总计
     * @param config
     * @return
     */
    protected boolean hasColumnTotalAndRowTotal(QueryConfigure config){
        if(config.getAnalysis().getTotal().getItem(AnalysisTotalType.ROW_TOTAL) != null
                && config.getAnalysis().getTotal().getItem(AnalysisTotalType.COL_TOTAL) != null){
            return true;
        }
        return false;
    }

    /**
     * 是否同时有列小计总计和行总计
     * @param config
     * @return
     */
    protected boolean hasColumnSubtotalAndRowTotal(QueryConfigure config){
        if(config.getAnalysis().getTotal().getItem(AnalysisTotalType.ROW_TOTAL) != null
                && config.getAnalysis().getTotal().getItem(AnalysisTotalType.COL_SUBTOTAL) != null){
            return true;
        }
        return false;
    }

    /**
     * 获取附加的列小计维度组合列表：用于列小计+行总计同时存在时，获取其交叉值
     * @param config
     * @param model
     * @return
     */
    protected List<List<String>> getAppendColumnSubtotalGroupingFieldSet(QueryConfigure config, StarModel model){
        List<List<String>> appendSubtotalGroupingFieldSets = new ArrayList<>();
        ColumnSubtotalOperator subtotalOperator = new ColumnSubtotalOperator();
        List<String> subtotalGroupingSets = subtotalOperator.groupingSets(config, model);
        for(String subtotalGroupingSet : subtotalGroupingSets){
            if(subtotalGroupingSet.contains(",")) {
                List<String> groupingFields = Arrays.asList(subtotalGroupingSet.replaceAll("[\\(\\)]", "").split(","));
                groupingFields = groupingFields.stream().map(f->{
                    if(f.contains(".")){
                        return f.trim().split("\\.")[1];
                    }else {
                        return f.trim();
                    }
                }).collect(Collectors.toList());
                if(groupingFields.size() >= 2){
                    groupingFields.remove(groupingFields.size() - 1);
                    appendSubtotalGroupingFieldSets.add(groupingFields);
                }
            }
        }

        return appendSubtotalGroupingFieldSets;
    }

    /**
     * 获取列小计+行总计时，列小计的附加组合值
     * @param config
     * @return
     */
    protected List<Integer> getAppendColumnSubtotalGroupingValues(QueryConfigure config){
        List<Integer> groupingValues = new ArrayList<>();
        List<List<String>> appendSubtotalGroupingFieldSets = this.getAppendColumnSubtotalGroupingFieldSet(config, null);
        if(BIUtil.isEmpty(appendSubtotalGroupingFieldSets)){
            return groupingValues;
        }

        Set<String> subtotalGroupingFieldCodes = new LinkedHashSet<>();

        for(List<String> fieldSet : appendSubtotalGroupingFieldSets){
            subtotalGroupingFieldCodes.addAll(fieldSet);
        }

        List<QueryField> rowFields = this.getRowDimensions(config);
        Long colDimCount = config.getResult().getFields().stream().filter(f->!f.isAppend()).filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).count();
        int index = 0;
        for (QueryField rowField : rowFields) {
            index++;
            if(!subtotalGroupingFieldCodes.contains(rowField.getCode())){
                continue;
            }
            String fieldGroupingBit = StringUtils.rightPad("", index, "0");
            String groupingBit = StringUtils.rightPad(fieldGroupingBit, rowFields.size(), "1") + StringUtils.rightPad("", colDimCount.intValue(), "1");
            int groupingValue = NumberUtil.binaryToInt(groupingBit);
            groupingValues.add(groupingValue);
        }

        return groupingValues;
    }
}
