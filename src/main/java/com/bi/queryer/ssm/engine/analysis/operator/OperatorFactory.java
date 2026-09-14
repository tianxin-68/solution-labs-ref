package com.bi.queryer.ssm.engine.analysis.operator;

import com.bi.queryer.ssm.engine.analysis.operator.impl.*;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;

/**
 * @Author contributor
 * @Date 11:17 2023-08-01
 * @Description 算子工场类
 **/
public abstract class OperatorFactory {
    public static BaseOperator getOperator(String calcMode){
        return getOperator(AnalysisCalcMode.get(calcMode));
    }
    public static BaseOperator getOperator(AnalysisCalcMode calcMode) {
        BaseOperator operator = null;
        switch (calcMode) {
            case HB:
            case TB_WEEK:
            case TB_MONTH:
            case TB_YEAR:
            case TB_YEAR_2:
            case TB_YEAR_3:
            case TB_LN_YEAR:
            case TB_LN_YEAR_2:
            case TB_LN_YEAR_3:
            case TB_YEAR_WEEK:
            case TB_YEAR_WEEK_2:
            case TB_YEAR_WEEK_3:
            case TB_PROMO_YEAR:
            case TB_PROMO_YEAR_2:
            case TB_PROMO_YEAR_3:
            case CUSTOM_COMPARE:
            case TB_LN_YEAR_WEEK:
            case TB_LN_YEAR_WEEK_2:
            case TB_LN_YEAR_WEEK_3:
                operator = new CompareOperator();
                break;
            case ZB_COL_SUBTOTAL:
                operator = new ZbColumnSubtotalOperator();
                break;
            case ZB_COL_TOTAL:
                operator = new ZbColumnTotalOperator();
                break;
            case ZB_ROW_TOTAL:
                operator = new ZbRowTotalOperator();
                break;
            case ZB_WHOLE_TABLE_TOTAL:
                operator = new ZbWholeTableTotalOperator();
                break;
            case COL_TOTAL:
                operator = new ColumnTotalOperator();
                break;
            case COL_SUBTOTAL:
                operator = new ColumnSubtotalOperator();
                break;
            case ROW_TOTAL:
                operator = new RowTotalOperator();
                break;
            case WHOLE_TABLE_TOTAL:
                operator = new WholeTableTotalOperator();
                break;
            case CONTRIBUTION_RATE:
                operator = new ContributionRateOperator();
                break;
            case ZB_THB:
                operator = new ZbThbOperator();
                break;
            case TARGET_VALUE:
            case BASELINE_VALUE:
            case CHALLENGE_VALUE:
            case TIME_PROGRESS:
            case PREDICT_VALUE:
                operator = new TargetValueOperator();
                break;
        }
        return operator;
    }

    public static TotalOperator getTotalOperator(AnalysisTotalType totalType){
        TotalOperator operator = null;
        switch (totalType){
            case COL_TOTAL:
                operator = new ColumnTotalOperator();
                break;
            case ROW_TOTAL:
                operator = new RowTotalOperator();
                break;
            case COL_SUBTOTAL:
                operator = new ColumnSubtotalOperator();
                break;
            case WHOLE_TABLE:
                operator = new WholeTableTotalOperator();
                break;
        }
        return operator;
    }


}
