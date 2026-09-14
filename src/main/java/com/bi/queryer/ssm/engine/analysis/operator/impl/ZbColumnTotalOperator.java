package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.util.NumberUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.Fraction;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.PercentFieldRatioUnitType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:13 2023-07-27
 * @Description 占比：列总计算子
 **/
public class ZbColumnTotalOperator extends ColumnTotalOperator{

    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        Map<String, QueryField> measureFieldMap = cxt.measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField->QueryField, (f1, f2)->f1));
        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());
        String calcExpression = "";

        String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

        // 百分比指标
        if(FieldUtil.isPercentField(rawMeasureField)) {
            ratioUnit = cfg.getPercentFieldRatioUnit();
        }

        String measureValue = String.format("%s.%s", cxt.currentDataSet.getName(), cfg.getMeasureCode());
        calcExpression =  getCalcExpression(cxt, measureValue, cxt.currentDataSet.getName(),BIConsts.GROUPING_VALUE,ratioUnit);
        return calcExpression;
    }

    @Override
    public String lodCalc(OperatorContext cxt,String measureName,String ratioUnit) {
        return getCalcExpression(cxt, measureName, BIConsts.MAIN_TABLE_ALIAS, BIConsts.GROUPING_KEY,ratioUnit);
    }

    public String getCalcExpression(OperatorContext cxt,String measureValue,String tableAlias,String groupKey,String ratioUnit) {

        String formatString = "";
        String calcExpression = "";

        /**
         * 获取分子和分母
         */
        Fraction fraction = getCalcFraction(cxt, measureValue, tableAlias, groupKey);
        formatString = "(%s * %s)/(%s)";

        //如果是百分比指标，占比 = 当期 - 汇总
        PercentFieldRatioUnitType ratioUnitType = PercentFieldRatioUnitType.get(ratioUnit);
        if(PercentFieldRatioUnitType.PT == ratioUnitType){
            formatString = "(%s * %s) - (%s)";
        }

        calcExpression = String.format(
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
    public Fraction getCalcFraction(OperatorContext cxt,String measureValue,String tableAlias,String groupKey) {

        Fraction fraction = new Fraction();

        //列总计的 grp_v
        List<QueryField> colDims = cxt.config.getResult().getFields().stream().filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension && !f.isAppend())
                .collect(Collectors.toList());
        int colDimCount = colDims.size();

        String fieldGroupingBit = StringUtils.rightPad("", -1, "0");
        String groupingBit = StringUtils.rightPad(fieldGroupingBit, cxt.getDimFields().size(), "1") + StringUtils.rightPad("", colDimCount, "0");
        int groupingValue = NumberUtil.binaryToInt(groupingBit);

        // 占比：列总计、列小计
        ColumnTotalOperator columnTotalOperator = new ColumnTotalOperator(groupingValue);
        String denominator = columnTotalOperator.total(cxt, measureValue, tableAlias, groupKey);

        List<String> whenExpressionList = new ArrayList<>();

        whenExpressionList.add(String.format(" when %s.%s = %s then %s ",
                tableAlias, groupKey, groupingValue, measureValue
        ));

        //处理行总计的占列总计
        AnalysisTotalConfig totalCfg = cxt.getConfig().getAnalysis().getTotal();
        AnalysisTotalItemConfig rowTotalConfig = totalCfg.getItem(AnalysisTotalType.ROW_TOTAL);
        if (colDimCount > 0 && rowTotalConfig != null) {
            whenExpressionList.add(String.format(" when %s.%s = %s then %s ",
                    tableAlias, groupKey, groupingValue + 1, measureValue
            ));

            RowTotalOperator RowTotalOperator = new RowTotalOperator();
            List<Integer> groupingValues = RowTotalOperator.getGroupingValues(cxt.config);
            groupingValues = groupingValues.stream().filter(f -> !f.equals(groupingValue + 1)).collect(Collectors.toList());

            String rowTotalDenominator = String.format(
                    "sum(if(%s.%s != %s,null,%s)) over()",
                    tableAlias,
                    groupKey,
                    groupingValue + 1,
                    measureValue

            );
            whenExpressionList.add(String.format(" when %s.%s in (%s) then %s ",
                    tableAlias, groupKey, BIUtil.listToStr(groupingValues, ","), rowTotalDenominator
            ));

        }

        denominator = String.format(" case %s else %s end ", BIUtil.listToStr(whenExpressionList, ""), denominator);

        fraction.setNumerator(measureValue);
        fraction.setDenominator(denominator);
        return fraction;
    }

    /**
     * 占列总计，需要计算分母，即把需要汇总的列配置到汇总配置中
     * @param config
     * @param cxt
     */
    @Override
    public void initQueryConfig(QueryConfigure config, QueryContext cxt) {

        //判断是否已经设置了列总计
        //设置了则不做初始化处理
        AnalysisTotalItemConfig totalItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.COL_TOTAL);
        if(totalItemConfig != null){
            return;
        }

        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        if(totalConfig == null) {
            totalConfig = new AnalysisTotalConfig();
        }

        totalConfig.setIsActive(Enabled.YES.getId());
        AnalysisTotalItemConfig itemConfig = new AnalysisTotalItemConfig();
        itemConfig.setTotalType(AnalysisTotalType.COL_TOTAL);
        itemConfig.setZbTotal(true);
        totalConfig.add(itemConfig);

        config.getAnalysis().setTotal(totalConfig);
    }

}
