package com.bi.queryer.ssm.engine.analysis.operator.impl;

import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.bi.queryer.ssm.enums.PercentFieldRatioUnitType;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.util.BIConsts;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 占比同环比操作符
 */
public class ZbThbOperator extends BaseOperator {

    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {

        AnalysisZbThbItemConfig cfg = (AnalysisZbThbItemConfig) measureField.getAnalysisConfig();
        AnalysisCalcMode zbCalcMode = AnalysisCalcMode.get(cfg.getZbCalcMode());
        BaseOperator operator = OperatorFactory.getOperator(zbCalcMode);

        AnalysisCalcType calcType = AnalysisCalcType.get(cfg.getCalcType());
        String calcExpression = "";

        Map<String, QueryField> measureFieldMap = cxt.measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField->QueryField, (f1, f2)->f1));
        QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());

        String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

        // 百分比指标
        if (FieldUtil.isPercentField(rawMeasureField)) {
            ratioUnit = cfg.getPercentFieldRatioUnit();
        }

        String analysisMeasureValue = String.format("%s.%s", cxt.analysisDataSet.getName(), cfg.getMeasureCode());
        String analysisExpression =  operator.getCalcExpression(cxt, analysisMeasureValue, cxt.analysisDataSet.getName(), BIConsts.GROUPING_VALUE,ratioUnit);

        switch (calcType){
            case REAL_VALUE:
                calcExpression = analysisExpression;
                break;
            case VALUE:
            case RATIO:
                String currentMeasureValue = String.format("%s.%s", cxt.currentDataSet.getName(), cfg.getMeasureCode());
                String currentExpression = operator.getCalcExpression(cxt, currentMeasureValue, cxt.currentDataSet.getName(), BIConsts.GROUPING_VALUE,ratioUnit);
                calcExpression = String.format("(%s -%s)",currentExpression,analysisExpression);
                break;
        }

        return calcExpression;
    }

    public String getLodCalcExpression(OperatorContext cxt, QueryField measureField,String currentMeasureValue,String analysisMeasureValue, String ratioUnit) {
        AnalysisZbThbItemConfig cfg = (AnalysisZbThbItemConfig) measureField.getAnalysisConfig();

        AnalysisCalcMode zbCalcMode = AnalysisCalcMode.get(cfg.getZbCalcMode());
        BaseOperator operator = OperatorFactory.getOperator(zbCalcMode);

        AnalysisCalcType calcType = AnalysisCalcType.get(cfg.getCalcType());

        String calcExpression = "";
        String analysisExpression =  operator.lodCalc(cxt, analysisMeasureValue,ratioUnit);

        switch (calcType){
            case REAL_VALUE:
                calcExpression = analysisExpression;
                break;
            case VALUE:
            case RATIO:
                String currentExpression = operator.lodCalc(cxt, currentMeasureValue,ratioUnit);
                calcExpression = String.format("(%s -%s)",currentExpression,analysisExpression);
                break;
        }

        return calcExpression;
    }

}
