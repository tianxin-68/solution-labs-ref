package com.bi.queryer.ssm.engine.analysis.dataset;

import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-24  19:45
 * @Description: 周分析数据集
 */
public class WeekAnalysisDataSet extends AnalysisDataSet{

    public WeekAnalysisDataSet(String dateField, AnalysisCalcMode calcMode) {
        super(dateField, calcMode);
    }

    @Override
    public String getDateFieldExpression(String defaultValue) {
        DateGranularity offsetGranularity = null;
        Integer interval = 0;
        switch (calcMode){
            case HB:
                offsetGranularity = DateGranularity.WEEK;
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
        String fieldExpression = fx.addWeek(this.getDateFieldFullName(), offsetGranularity,interval);
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
