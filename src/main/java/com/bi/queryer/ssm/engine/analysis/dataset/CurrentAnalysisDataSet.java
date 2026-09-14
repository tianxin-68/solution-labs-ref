package com.bi.queryer.ssm.engine.analysis.dataset;

import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.util.BIConsts;

/**
 * @Author contributor
 * @Date 17:35 2023-06-15
 * @Description 当前数据集
 **/
public class CurrentAnalysisDataSet extends AnalysisDataSet{

    public CurrentAnalysisDataSet(String dateField, AnalysisCalcMode calcMode) {
        super(dateField, calcMode);
    }

    @Override
    public String getDateFieldExpression(String defaultValue) {
        if(defaultValue == null){
            return this.getDateFieldFullName();
        }else {
            String dateFieldExpression = fx.coalesce(this.getDateFieldFullName(), "'" + defaultValue + "'");
            return dateFieldExpression;
        }
    }

    public String getDateFieldExpression(AnalysisCalcMode calcMode) {
        // 年周同比时，基准日期偏移后于对比日期进行比较
        switch (calcMode){
            case TB_YEAR_WEEK:
            case TB_YEAR_WEEK_2:
            case TB_YEAR_WEEK_3:
                String fieldExpression = fx.getYearWeekDay(this.getDateFieldFullName(), calcMode.getOffset());
                fieldExpression = fx.coalesce(fieldExpression, "'" + BIConsts.SSM_ALL + "'");
                return fieldExpression;
        }
        return this.getDateFieldExpression();
    }

    @Override
    public String getName() {
        return this.namePrefix + "cur";
    }
}