package com.bi.queryer.ssm.engine.aggregation;

import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.util.BIConsts;

/**
 * @Author contributor
 * @Date 14:39 2023-09-22
 * @Description 日均值(有数据日期)聚合器
 **/
public class AverageByRealDayAggregator extends AverageByDayAggregator{

    public AverageByRealDayAggregator(AggregatorContext cxt) {
        super(cxt);
    }

    @Override
    public String getFilterDaysExpression(QueryField dateField) {
        String fieldCode = BIConsts.MIN_DATE_GRANULARITY_CODE;
        String daysExpression = String.format("count(distinct %s)", fieldCode);
        return daysExpression;
    }

    @Override
    protected String getDayDaysExpression(QueryField field, AggregatorContext cxt) {
        return this.getFilterDaysExpression(field);
    }

    @Override
    protected String getWeekDaysExpression(QueryField field, AggregatorContext cxt) {
        return this.getFilterDaysExpression(field);
    }

    @Override
    protected String getMonthDaysExpression(QueryField field, AggregatorContext cxt) {
        return this.getFilterDaysExpression(field);
    }

    @Override
    protected String getYearDaysExpression(QueryField field, AggregatorContext cxt) {
        return this.getFilterDaysExpression(field);
    }

    @Override
    protected String getQuarterDaysExpression(QueryField field, AggregatorContext cxt){
        return this.getFilterDaysExpression(field);
    }
}
