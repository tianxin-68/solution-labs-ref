package com.bi.queryer.ssm.engine.analysis.dataset;

import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;

/**
 * @Author contributor
 * @Date 17:27 2023-06-15
 * @Description 年数据集
 **/
public class YearAnalysisDataSet extends AnalysisDataSet {

    public YearAnalysisDataSet(String dateField, AnalysisCalcMode calcMode) {
        super(dateField, calcMode);
    }

    @Override
    public String getDateFieldExpression(String defaultValue) {
        DateGranularity offsetGranularity = null;
        Integer interval = 0;
        switch (calcMode){
            case HB:
                offsetGranularity = DateGranularity.YEAR;
                interval = 1;
                break;
            case TB_YEAR:
                interval = 1;
                offsetGranularity = DateGranularity.YEAR;
                break;
        }

        String fieldExpression = fx.formatDate(
                fx.addDate(
                        fx.parseDate(this.getDateFieldFullName(), IFunction.Format_Year),
                        offsetGranularity,
                        interval
                )
                , IFunction.Format_Year);
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
