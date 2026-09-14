package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.Fraction;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.PercentFieldRatioUnitType;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:05 2023-08-01
 * @Description 占比：行总计算子
 **/
public class ZbRowTotalOperator extends BaseOperator {
    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        AnalysisItemConfig cfg = measureField.getAnalysisConfig();

        Map<String, QueryField> measureFieldMap = cxt.measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField->QueryField, (f1, f2)->f1));
        QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());

        String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

        // 百分比指标
        if (FieldUtil.isPercentField(rawMeasureField)) {
            ratioUnit = cfg.getPercentFieldRatioUnit();
        }

        String measureValue = String.format("%s.%s", cxt.currentDataSet.getName(), cfg.getMeasureCode());
        String calcExpression = getCalcExpression(cxt, measureValue, cxt.currentDataSet.getName(),BIConsts.GROUPING_VALUE,ratioUnit);
        return calcExpression;
    }

    public String lodCalc(OperatorContext cxt,String measureName,String ratioUnit) {
        return getCalcExpression(cxt, measureName, BIConsts.MAIN_TABLE_ALIAS,BIConsts.GROUPING_KEY,ratioUnit);
    }

    public String getCalcExpression(OperatorContext cxt,String measureValue,String tableAlias, String groupKey,String ratioUnit) {

        /**
         * 获取分子和分母
         */
        Fraction fraction = getCalcFraction(cxt, measureValue, tableAlias, groupKey);
        String formatString = "(%s * %s)/(%s)";

        //如果是百分比指标，占比 = 当期 - 汇总
        PercentFieldRatioUnitType ratioUnitType = PercentFieldRatioUnitType.get(ratioUnit);
        if(PercentFieldRatioUnitType.PT == ratioUnitType){
            formatString = "(%s * %s) - (%s)";
        }

        String calcExpression = String.format(
                formatString,
                fraction.getNumerator(),
                BIConsts.INT_TO_DOUBLE_PRECISION,
                fraction.getDenominator()
        );
        calcExpression = function.tryCatch(calcExpression);

        return calcExpression;
    }

    /**
     * 获取占比的分子与分母
     */
    public Fraction getCalcFraction(OperatorContext cxt, String measureValue, String tableAlias, String groupKey){

        Fraction fraction = new Fraction();

        List<QueryField> dimFields = cxt.dimFields;

        List<String> partitionByExpressionList = new ArrayList<>();
        for (QueryField field : dimFields) {
            partitionByExpressionList.add(String.format("%s.%s", tableAlias, field.getCode()));
        }

        String partitionByExpression = "";
        if (CollUtil.isNotEmpty(partitionByExpressionList)) {
            partitionByExpression = String.format(" PARTITION by %s ", BIUtil.listToStr(partitionByExpressionList, ","));
        }

        RowTotalOperator rowTotalOperator = new RowTotalOperator();
        List<Integer> groupingValues = rowTotalOperator.getGroupingValues(cxt.getConfig());

        String measureValueExpression = String.format("case when %s.%s in (%s) then %s else 0 end  ",
                tableAlias,
                groupKey,
                BIUtil.listToStr(groupingValues, ","),
                measureValue);

        String zbDenominator = String.format("sum(%s) over(%s) ",
                measureValueExpression,
                partitionByExpression);

        fraction.setNumerator(measureValue);
        fraction.setDenominator(zbDenominator);
        return fraction;
    }

    /**
     * 初始化配置：添加行总计列
     * @param config
     * @param cxt
     */
    @Override
    public void initQueryConfig(QueryConfigure config, QueryContext cxt) {


        //判断是否已经设置了行总计
        //设置了则不做初始化处理
        AnalysisTotalItemConfig analysisTotalItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.ROW_TOTAL);
        if(analysisTotalItemConfig != null){
            return;
        }

        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        totalConfig.setIsActive(Enabled.YES.getId());

        // 找出占比行总计的指标，并添加到汇总配置中，即设置只对指定指标进行汇总
        List<QueryField> measureFields = config.getResult().getMeasures().stream()
                .filter(f->{
                    return !f.isAppend() && Enabled.value(f.getIsAnalysis())
                            && AnalysisCalcMode.ZB_ROW_TOTAL == AnalysisCalcMode.get(f.getAnalysisConfig().getCalcMode());
                })
                .collect(Collectors.toList());
        if(BIUtil.isNotEmpty(measureFields)) {

            for(QueryField queryField : measureFields){

                // 添加行汇总
                AnalysisTotalItemConfig totalItemConfig = new AnalysisTotalItemConfig();
                totalItemConfig.setCalcMode(AnalysisCalcMode.ROW_TOTAL.getCode());
                totalItemConfig.setTotalType(AnalysisTotalType.ROW_TOTAL);

                // 编码规则和行总计保存一致
                String measureCode = queryField.getAnalysisConfig().getMeasureCode();
                measureCode = this.isCrossQuery(config) ? measureCode + "_" + AnalysisCalcMode.ROW_TOTAL.getCode() : BIConsts.ROW_TOTAL_COLUMN_CODE;
                totalItemConfig.setMeasureId(measureCode);
                totalItemConfig.setMeasureCode(measureCode);

                totalItemConfig.setZbTotal(true);
                totalConfig.add(totalItemConfig);
            }

        }

    }

}
