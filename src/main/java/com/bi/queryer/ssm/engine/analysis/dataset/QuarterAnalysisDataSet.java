package com.bi.queryer.ssm.engine.analysis.dataset;

import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;

/**
 * 季度数据集
 */
public class QuarterAnalysisDataSet extends AnalysisDataSet{

    public QuarterAnalysisDataSet(String dateField, AnalysisCalcMode calcMode) {
        super(dateField, calcMode);
    }

    @Override
    public String getDateFieldExpression(String defaultValue) {
        DateGranularity offsetGranularity = null;
        Integer interval = 0;
        switch (calcMode){
            case HB:
                offsetGranularity = DateGranularity.QUARTER;
                interval = 1;
                break;
            case TB_YEAR:
                interval = 1;
                offsetGranularity = DateGranularity.YEAR;
                break;
            case TB_YEAR_2:
                interval = 2;
                offsetGranularity = DateGranularity.YEAR;
                break;
            case TB_YEAR_3:
                interval = 3;
                offsetGranularity = DateGranularity.YEAR;
                break;
        }

        String fieldExpression = fx.addQuarter(this.getDateFieldFullName(),offsetGranularity,interval);
        if(defaultValue != null) {
            fieldExpression = fx.coalesce(fieldExpression, "'" + defaultValue + "'");
        }
        return fieldExpression;
    }

    @Override
    public String getName() {
        return this.namePrefix + this.calcMode.getCode();
    }
}
