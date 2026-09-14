package com.bi.queryer.ssm.engine.analysis.operator.impl;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:12 2023-08-01
 * @Description 占比：整表总计
 **/
public class ZbWholeTableTotalOperator extends BaseOperator {
    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        WholeTableTotalOperator wholeTableOperator = new WholeTableTotalOperator();
        String zbDenominator = wholeTableOperator.total(cxt, measureField);

        Map<String, QueryField> measureFieldMap = cxt.measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField->QueryField, (f1, f2)->f1));
        QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());

        String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

        // 百分比指标
        if (FieldUtil.isPercentField(rawMeasureField)) {
            ratioUnit = cfg.getPercentFieldRatioUnit();
        }

        String formatString = "(%s.%s * %s)/%s";

        //如果是百分比指标，占比 = 当期 - 汇总
        PercentFieldRatioUnitType ratioUnitType = PercentFieldRatioUnitType.get(ratioUnit);
        if (PercentFieldRatioUnitType.PT == ratioUnitType) {
            formatString = "(%s.%s * %s) - %s";
        }

        String calcExpression = String.format(
                formatString,
                cxt.currentDataSet.getName(),
                cfg.getMeasureCode(),
                BIConsts.INT_TO_DOUBLE_PRECISION,
                function.tryCatch(zbDenominator)
        );
        calcExpression = function.tryCatch(calcExpression);
        return calcExpression;
    }

    /**
     * 初始化配置：添加整表总计列
     * @param config
     * @param cxt
     */
    @Override
    public void initQueryConfig(QueryConfigure config, QueryContext cxt) {

        //判断是否已经设置了整表总计
        //设置了则不做初始化处理
        AnalysisTotalItemConfig analysisTotalItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.WHOLE_TABLE);
        if(analysisTotalItemConfig != null){
            return;
        }

        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        totalConfig.setIsActive(Enabled.YES.getId());

        // 找出占比行总计的指标，并添加到汇总配置中，即设置只对指定指标进行汇总
        List<QueryField> measureFields = config.getResult().getMeasures().stream()
                .filter(f -> {
                    return !f.isAppend() && Enabled.value(f.getIsAnalysis())
                            && AnalysisCalcMode.ZB_WHOLE_TABLE_TOTAL == AnalysisCalcMode.get(f.getAnalysisConfig().getCalcMode());
                })
                .collect(Collectors.toList());
        if (BIUtil.isNotEmpty(measureFields)) {

            for (QueryField queryField : measureFields) {

                // 添加行汇总
                AnalysisTotalItemConfig totalItemConfig = new AnalysisTotalItemConfig();
                totalItemConfig.setCalcMode(AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode());
                totalItemConfig.setTotalType(AnalysisTotalType.WHOLE_TABLE);

                // 编码规则和行总计保存一致
                String measureCode = queryField.getAnalysisConfig().getMeasureCode();
                measureCode = measureCode + "_" + AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode();
                totalItemConfig.setMeasureId(measureCode);
                totalItemConfig.setMeasureCode(measureCode);

                totalItemConfig.setZbTotal(true);
                totalConfig.add(totalItemConfig);
            }

        }
    }
}
