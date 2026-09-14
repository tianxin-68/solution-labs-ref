package com.bi.queryer.ssm.engine.analysis.operator.impl;

import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.bi.queryer.ssm.enums.PercentFieldRatioUnitType;
import com.bi.queryer.ssm.util.FieldUtil;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 19:50 2023-07-31
 * @Description 对比比算子
 **/
public class CompareOperator extends BaseOperator {

    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        Map<String, QueryField> measureFieldMap = cxt.measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField -> QueryField, (f1, f2) -> f1));
        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());
        String calcExpression = "";
        String formatString = "";
        if (FieldUtil.isPercentField(rawMeasureField)) {

            if (AnalysisCalcType.REAL_VALUE == AnalysisCalcType.get(cfg.getCalcType())) {
                formatString = "%s.%s ";
                calcExpression = String.format(formatString,
                        cxt.analysisDataSet.getName(),
                        cfg.getDependFieldCode());
            } else if(AnalysisCalcType.VALUE == AnalysisCalcType.get(cfg.getCalcType())){
                formatString = "COALESCE(%s.%s,0) - COALESCE(%s.%s,0)";
                String analysisExpression = String.format(
                        formatString,
                        cxt.currentDataSet.getName(),
                        cfg.getDependFieldCode(),
                        cxt.analysisDataSet.getName(),
                        cfg.getDependFieldCode()
                );
                calcExpression = function.tryCatch(analysisExpression);
            }else{
                PercentFieldRatioUnitType percentFieldRatioUnitType = PercentFieldRatioUnitType.get(cfg.getPercentFieldRatioUnit());

                //按 % 计算
                if(PercentFieldRatioUnitType.PERCENT == percentFieldRatioUnitType){
                    calcExpression = buildRatioCalcExpression(cxt,cfg);
                }else{
                    formatString = "COALESCE(%s.%s,0) - COALESCE(%s.%s,0)";
                    String analysisExpression = String.format(
                            formatString,
                            cxt.currentDataSet.getName(),
                            cfg.getDependFieldCode(),
                            cxt.analysisDataSet.getName(),
                            cfg.getDependFieldCode()
                    );
                    calcExpression = function.tryCatch(analysisExpression);
                }
            }

        }

        // 非百分比指标
        if (!FieldUtil.isPercentField(rawMeasureField)) {
            // 同环比
            switch (AnalysisCalcType.get(cfg.getCalcType())) {
                case VALUE:
                    formatString = "COALESCE(%s.%s,0) - COALESCE(%s.%s,0)";
                    //formatString = "%s.%s - %s.%s";
                    break;
                case REAL_VALUE:
                    formatString = "%s.%s ";
                    calcExpression = String.format(formatString,
                            cxt.analysisDataSet.getName(),
                            cfg.getDependFieldCode());
                    return calcExpression;
                case RATIO:
                default:
                    calcExpression = buildRatioCalcExpression(cxt,cfg);
                    return calcExpression;
            }

            String analysisExpression = String.format(
                    formatString,
                    cxt.currentDataSet.getName(),
                    cfg.getDependFieldCode(),
                    cxt.analysisDataSet.getName(),
                    cfg.getDependFieldCode()
            );
            calcExpression = function.tryCatch(analysisExpression);
        }

        return calcExpression;
    }

    /**
     * 构建差异率的计算表达式
     * @return
     */
    public String buildRatioCalcExpression(OperatorContext cxt,AnalysisItemConfig cfg){

//        String numeratorSql = String.format("COALESCE(%s.%s, 0) -  COALESCE(%s.%s, 0)",
//                cxt.currentDataSet.getName(),
//                cfg.getMeasureCode(),
//                cxt.analysisDataSet.getName(),
//                cfg.getMeasureCode()
//        );
//        String denominatorSql = String.format(" COALESCE(%s.%s, 0)",
//                cxt.analysisDataSet.getName(),
//                cfg.getMeasureCode()
//        );

        String numeratorSql = String.format(" %s.%s -  %s.%s ",
                cxt.currentDataSet.getName(),
                cfg.getDependFieldCode(),
                cxt.analysisDataSet.getName(),
                cfg.getDependFieldCode()
        );
        String denominatorSql = String.format(" %s.%s",
                cxt.analysisDataSet.getName(),
                cfg.getDependFieldCode()
        );
        // 比率：分母改为绝对值，处理分母为负数的场景
        denominatorSql = function.abs(denominatorSql);
        String formatString =  function.division(numeratorSql, denominatorSql, false);
        /*
        String.format("bi_devision(%s,%s)",
                numeratorSql,
                denominatorSql);
         */

       return formatString;

    }

    @Override
    public boolean isSupportPivot(AnalysisItemConfig itemConfig) {
        // 先判断原始计算方式是否支持
        AnalysisCalcMode calcMode = AnalysisCalcMode.get(itemConfig.getRawCalcMode());
        if(!calcMode.isCompare()) {
            BaseOperator operator = OperatorFactory.getOperator(calcMode);
            return operator.isSupportPivot(itemConfig);
        }
        return super.isSupportPivot(itemConfig);
    }

    /**
     * 获取对比偏移日期表达式
     * @return
     */
    public String getCompareOffsetDateExpression(String field, AnalysisCalcMode calcMode, Integer customCompareIndex){

        return field;
    }
}
