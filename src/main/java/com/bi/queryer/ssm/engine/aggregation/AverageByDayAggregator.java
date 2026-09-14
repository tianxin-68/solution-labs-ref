package com.bi.queryer.ssm.engine.aggregation;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.promotion.PromotionConsts;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SqlAggregationParser;
import com.bi.queryer.util.period.QuarterDateUtil;
import com.bi.queryer.util.period.WeekDateUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @Author contributor
 * @Date 14:39 2023-09-22
 * @Description 日均值聚合器
 **/
public class AverageByDayAggregator extends DefaultAggregator{

    public AverageByDayAggregator(AggregatorContext cxt) {
        super(cxt);
    }

    @Override
    public String aggregate(QueryField field) {
        String finalExpression = super.aggregate(field);

        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        // 日粒度且不汇总，则不处理
        /**
         * 废弃
         * 原因：日粒度不汇总，但有列总计时也需要计算日均
        if(!commonDateField.isAggQuery() && DateGranularity.DAY == DateGranularity.get(commonDateField.getQueryDateGranularity())){
            return finalExpression;
        }
         */

        String aggExpression = field.getMeta().getAggExpression();

        // 有除法的聚合表达式，则不需要算日均
        if(BIUtil.isNotEmpty(aggExpression) && aggExpression.contains("/") && !BIUtil.isCountDistinctAggExpression(aggExpression)) {

            //2026-01-12 添加白名单，部分指标表达式，除常量可计算日均
            String averageMeasureCodes = SC.v("ssm.average.measure.code.white.list", "D_ORD_06756");
            if (StrUtil.isEmpty(averageMeasureCodes)) {
                return finalExpression;
            }

            //是否是单聚合表达式
            boolean isSingleAgg = SqlAggregationParser.isSingleAggregation(aggExpression);

            List<String> averageMeasureCodeList = Arrays.asList(averageMeasureCodes.split(","));
            String measureCode = field.getCode();
            //去掉日均后缀
            measureCode = measureCode.replace("_" + AggExpressionType.Avg_By_Day.getCode(), "");
            measureCode = measureCode.replace("_" + AggExpressionType.Avg_By_Day_Real.getCode(), "");
            if (!averageMeasureCodeList.contains(measureCode) && !isSingleAgg) {
                return finalExpression;
            }
        }

        //if(AggExpressionType.get(aggExpression) == AggExpressionType.Count_Distinct || aggExpression.toLowerCase().contains(BIConsts.COUNT_DISTINCT_FLAG)) {
        if(BIUtil.isCountDistinctAggExpression(aggExpression)){
            finalExpression = String.format("%s(%s)", AggExpressionType.Sum.getCode(), field.getCode());
        }

        DateGranularity dateGranularity = DateGranularity.get(commonDateField.getQueryDateGranularity());

        String daysExpression = "";
        if(commonDateField.isAggQuery()){
            // 日期汇总查询
            daysExpression = this.getFilterDaysExpression(commonDateField);
        }else {
            /**
            // 结束日期：用于剔除未到达的日期天数
            String endDate = DateUtil.yesterday().toDateStr();
            if(dateGranularity == DateGranularity.DAY){
                return finalExpression;
            }
             */
            switch (dateGranularity){
                case DAY:
                    daysExpression = this.getDayDaysExpression(field, cxt);
                    break;
                case WEEK:
                    //daysExpression = "7";
                    //daysExpression = this.getFilterDaysExpression(commonDateField);
                    daysExpression = this.getWeekDaysExpression(field, cxt);
                    break;
                case MONTH:
                    daysExpression = this.getMonthDaysExpression(field, cxt);
                    /*
                    daysExpression = String.format("bi_get_month_days(%s, '%s')", commonDateField.getCode(), endDate);

                    // 月和年计算总计时，因为dt为null会导致udf返回为null，从而列总计/整表总计为null
                    // 计算总计时的天数
                    String totalCalcDays =  this.getFilterDaysExpression(commonDateField);
                    daysExpression = String.format("if(%s is null, %s, %s)", commonDateField.getCode(), totalCalcDays, daysExpression);
                     */
                    break;
                case QUARTER:
                    daysExpression = this.getQuarterDaysExpression(field, cxt);
                    break;
                case YEAR:
                    daysExpression = this.getYearDaysExpression(field, cxt);
                    /*
                    daysExpression = String.format("bi_get_year_days(%s, '%s')", commonDateField.getCode(), endDate);
                    // 计算总计时的天数
                    totalCalcDays =  this.getFilterDaysExpression(commonDateField);
                    daysExpression = String.format("if(%s is null, %s, %s)", commonDateField.getCode(), totalCalcDays, daysExpression);
                     */
                    break;
            }
        }

        if(BIUtil.isEmpty(daysExpression)) {
            return finalExpression;
        }

        if(AggExpressionType.get(field.getDistinctByDayAggMode()) != AggExpressionType.Sum){
            finalExpression = String.format("(%s)* %s /%s", finalExpression, BIConsts.INT_TO_DOUBLE_PRECISION, daysExpression);
        }

        finalExpression = fx.tryCatch(finalExpression);

        return finalExpression;
    }

    /**
     * 获取日粒度天数
     * @param field
     * @param cxt
     * @return
     */
    protected String getDayDaysExpression(QueryField field, AggregatorContext cxt) {

        // 业务日历，日均计算处理
        if (cxt.config.getSettings().isBusinessCalendar()) {
            return this.getBusinessCalendarDayDaysExpression(field, cxt);
        }

        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        String daysExpression = "1";
        String totalCalcDays = this.getFilterDaysExpression(commonDateField);
        // 日总计时，因为dt为null会导致udf返回为null，从而列总计/整表总计为null
        // 计算总计时的天数
        daysExpression = String.format("if(%s is null, %s, %s)", commonDateField.getCode(), totalCalcDays, daysExpression);
        return daysExpression;
    }

    /**
     * 获取业务日历的日均天数
     * @return
     */
    protected String getBusinessCalendarDayDaysExpression(QueryField field, AggregatorContext cxt){
        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        String daysExpression = PromotionConsts.PROMOTION_DURATION_DAYS_CODE;
        Integer totalCalcDays = PromotionManager.getPromotionTotalDays(commonDateField,cxt.config);

        // 计算总计时的天数
        daysExpression = String.format("if(%s is null, %s, max(%s))", commonDateField.getCode(), totalCalcDays, daysExpression);
        return daysExpression;
    }

    /**
     * 获取周天数
     * @param field
     * @param cxt
     * @return
     */
    protected String getWeekDaysExpression(QueryField field, AggregatorContext cxt){
        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        String daysExpression = "7";
        String totalCalcDays = this.getFilterDaysExpression(commonDateField);
        // 周计算总计时，因为dt为null会导致udf返回为null，从而列总计/整表总计为null
        // 计算总计时的天数
        daysExpression = String.format("if(%s is null, %s, %s)", commonDateField.getCode(), totalCalcDays, daysExpression);
        return daysExpression;
    }

    /**
     * 获取月份天数
     * @param field
     * @param cxt
     * @return
     */
    protected String getMonthDaysExpression(QueryField field, AggregatorContext cxt){
        // 结束日期：用于剔除未到达的日期天数
        String endDate = BIUtil.listToStr(this.getEndDateList(field, cxt));
        if(BIUtil.isEmpty(endDate)){
            endDate = cxt.getConfig().getSettings().getLastAvailableDate().toDateStr();
        }
        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        String daysExpression = String.format("bi_get_month_days(%s, '%s')", commonDateField.getCode(), endDate);

        // 月和年计算总计时，因为dt为null会导致udf返回为null，从而列总计/整表总计为null
        // 计算总计时的天数
        String totalCalcDays =  this.getFilterDaysExpression(commonDateField);
        daysExpression = String.format("if(%s is null, %s, %s)", commonDateField.getCode(), totalCalcDays, daysExpression);

        return daysExpression;
    }

    protected String getQuarterDaysExpression(QueryField field, AggregatorContext cxt){
        // 结束日期：用于剔除未到达的日期天数
        String endDate = BIUtil.listToStr(this.getEndDateList(field, cxt));
        if(BIUtil.isEmpty(endDate)){
            endDate = DateUtil.yesterday().toDateStr();
        }
        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        String daysExpression = String.format("bi_get_quarter_days(%s, '%s')", commonDateField.getCode(), endDate);

        // 月和年计算总计时，因为dt为null会导致udf返回为null，从而列总计/整表总计为null
        // 计算总计时的天数
        String totalCalcDays =  this.getFilterDaysExpression(commonDateField);
        daysExpression = String.format("if(%s is null, %s, %s)", commonDateField.getCode(), totalCalcDays, daysExpression);

        return daysExpression;
    }

    /**
     * 获取年份天数
     * @param field
     * @param cxt
     * @return
     */
    protected String getYearDaysExpression(QueryField field, AggregatorContext cxt){
        // 结束日期：用于剔除未到达的日期天数
        String endDate = BIUtil.listToStr(this.getEndDateList(field, cxt));
        if(BIUtil.isEmpty(endDate)){
            endDate = DateUtil.yesterday().toDateStr();
        }
        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        String daysExpression = String.format("bi_get_year_days(%s, '%s')", commonDateField.getCode(), endDate);
        // 计算总计时的天数
        String totalCalcDays =  this.getFilterDaysExpression(commonDateField);
        daysExpression = String.format("if(%s is null, %s, %s)", commonDateField.getCode(), totalCalcDays, daysExpression);
        return daysExpression;
    }

    /**
     * 获取过滤的天数
     * @param dateField
     * @return
     */
    public String getFilterDaysExpression(QueryField dateField) {
        String daysExpression = "";
        Long days = 0L;
        if (dateField == null) {
            return daysExpression;
        }

        //业务日历，逻辑处理
        if(cxt.config.getSettings().isBusinessCalendar()) {
            Integer promotionTotalDays = PromotionManager.getPromotionTotalDays(dateField, cxt.config);
            return promotionTotalDays.toString();
        }

        List<FieldValue> values = FieldUtil.getFilterRealValues(dateField);
        if (BIUtil.isEmpty(values)) {
            return daysExpression;
        }
        FieldUtil.sortRangeValues(dateField, values);

        String startDate = values.get(0).getId();
        String endDate = startDate;
        if (values.size() > 1) {
            endDate = values.get(1).getId();
        }

        DateTime lastDay = cxt.getConfig().getSettings().getLastAvailableDate();

        DateGranularity dateGranularity = DateGranularity.get(dateField.getQueryDateGranularity());
        Date d1 = null;
        Date d2 = null;
        String formatStr = "";
        long removeDays = 0L;// 需要移除的天数
        switch (dateGranularity) {
            case DAY:
                d1 = DateUtil.parseDate(startDate);
                d2 = DateUtil.parseDate(endDate);
                days = DateUtil.between(d1, d2, DateUnit.DAY, true);
                break;
            case WEEK:
                d1 = WeekDateUtil.getWeekFirstDay(startDate);
                d2 = WeekDateUtil.getWeekLastDay(endDate);
                days = DateUtil.between(d1, d2, DateUnit.DAY, true);
                break;
            case MONTH:
                formatStr = "yyyyMM";
                d1 = DateUtil.parse(startDate, formatStr);
                d2 = DateUtil.endOfMonth(DateUtil.parse(endDate, formatStr));
                days = DateUtil.between(d1, d2, DateUnit.DAY, true);

                // 剔除今天到月底
                removeDays = DateUtil.between(lastDay, d2, DateUnit.DAY, false);
                if (removeDays > 0) {
                    days = days - removeDays;
                }
                break;
            case QUARTER:
                d1 = QuarterDateUtil.getQuarterFirstDay(startDate);
                d2 = QuarterDateUtil.getQuarterEndDay(endDate);
                days = DateUtil.between(d1, d2, DateUnit.DAY, true);

                // 剔除今天到季底
                removeDays = DateUtil.between(lastDay, d2, DateUnit.DAY, false);
                if (removeDays > 0) {
                    days = days - removeDays;
                }
                break;
            case YEAR:
                formatStr = "yyyy";
                d1 = DateUtil.parse(startDate, formatStr);
                d2 = DateUtil.endOfYear(DateUtil.parse(endDate, formatStr));
                days = DateUtil.between(d1, d2, DateUnit.DAY, true);

                // 剔除今天到年底
                removeDays = DateUtil.between(lastDay, d2, DateUnit.DAY, false);
                if (removeDays > 0) {
                    days = days - removeDays;
                }
                break;
        }

        days = days + 1;
        daysExpression = days + "";

        return daysExpression;
    }

    /**
     * 获取区间的结束日期列表：用于mtd/ytd计算日期天数
     * @param field
     * @param cxt
     * @return
     */
    protected List<String> getEndDateList(QueryField field, AggregatorContext cxt) {
        List<String> endDateList = new ArrayList<>();
        //  String endDate = DateUtil.yesterday().toDateStr();
        DateTime lastDay = cxt.getConfig().getSettings().getLastAvailableDate();
        endDateList.add(lastDay.toDateStr());
        return endDateList;
    }

}
