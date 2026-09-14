package com.bi.queryer.ssm.engine.aggregation;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.promotion.PromotionConsts;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateDiff;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:01 2023-09-22
 * @Description 数据对比日均值聚合器：针对分析场景添加特定逻辑
 **/
public class AnalysisAverageByDayAggregator extends AverageByDayAggregator{

    public AnalysisAverageByDayAggregator(AggregatorContext cxt) {
        super(cxt);
    }

    @Override
    public String getFilterDaysExpression(QueryField dateField) {
        if(cxt.compareIndex >= 0) {
            return this.getCustomCompareFilterDaysExpression(dateField);
        }

        // 月环比时，需要重新计算环比时长，因为聚合后的月环比日期天数不一致，如：当前2023-11，环比2023-10
        if(cxt.config.getFilterCommonDateField().isAggQuery()
                && cxt.analysisCalcMode == AnalysisCalcMode.HB
                && DateGranularity.MONTH == DateGranularity.get(dateField.getQueryDateGranularity())){
            return this.getMonthHbFilterDaysExpression(dateField);
        }

        return super.getFilterDaysExpression(dateField);
    }

    /**
     * 获取月环比过滤日期天数表达式（解决对比mtd/ytd,环比月份长度不一致的问题）
     * @param dateField
     * @return 真实天数值
     */
    public String getMonthHbFilterDaysExpression(QueryField dateField){
        String daysExpression = "";
        Long days = 0L;
        if(dateField == null){
            return daysExpression;
        }

        List<String> monthRange = AnalysisUtil.getCompareDateRange(cxt.config, cxt.analysisCalcMode , cxt.compareIndex);
        if(BIUtil.isEmpty(monthRange)) {
            return daysExpression;
        }
//        List<String> monthRange = months.get(0);

        Date monthStartDate = DateUtil.parse(monthRange.get(0), "yyyyMM");
        Date monthEndDate = DateUtil.endOfMonth(DateUtil.parse(monthRange.get(1), "yyyyMM"));

        List<String> filterDateList = DateUtil.rangeToList(monthStartDate, monthEndDate, DateField.DAY_OF_YEAR).stream().map(f -> f.toDateStr()).collect(Collectors.toList());

        // 剔除大于今天的日期
        String today = DateUtil.today();
        filterDateList = filterDateList.stream().filter(d -> d.compareTo(today) < 0).collect(Collectors.toList());

        // 剔除基准日期包含了当天时的自定义对比日期（如：当前=2023-09-23，基准日期(月)=202308~202309，对比月=202303~202304，则需要剔除掉20230423~20230430
        List<List<String>> excludeDateSegments = AnalysisUtil.getExcludeAnalysisDateFilterRange(cxt.config, AnalysisCalcMode.HB, cxt.compareIndex);
        List<String> excludeDates = new ArrayList<>();
        for(List<String> segment : excludeDateSegments){
            if(BIUtil.isEmpty(segment)){
                continue;
            }
            Date excludeStartDate = DateUtil.parseDate(segment.get(0));
            Date excludeEndDate = DateUtil.parseDate(segment.get(segment.size() - 1));
            excludeDates.addAll(DateUtil.rangeToList(excludeStartDate, excludeEndDate, DateField.DAY_OF_YEAR).stream().map(f -> f.toDateStr()).collect(Collectors.toList()));
        }
        filterDateList.removeAll(excludeDates);


        if(BIUtil.isEmpty(filterDateList)) {
            return daysExpression;
        }

        String startDate = filterDateList.get(0);
        String endDate = startDate;
        if(filterDateList.size() > 1){
            endDate = filterDateList.get(filterDateList.size() - 1);
        }

        Date d1 = DateUtil.parseDate(startDate);
        Date d2 = DateUtil.parseDate(endDate);
        days = DateUtil.between(d1, d2, DateUnit.DAY, true);
        days = days + 1;

//        if(days > 1){
            daysExpression = days + "";
//        }
        return daysExpression;
    }

    /**
     * 获取自定义对比的前端过滤日期天数表达式（解决对比mtd/ytd的问题）
     * @param dateField
     * @return 真实天数值
     */
    public String getCustomCompareFilterDaysExpression(QueryField dateField){
        String daysExpression = "";
        Long days = 0L;
        if(dateField == null){
            return daysExpression;
        }

        List<String> filterDateList = AnalysisUtil.getCompareDateList(cxt.config, cxt.compareIndex);

        // 剔除大于最大有效日期的数据
        String lastDay = cxt.config.getSettings().getLastAvailableDate().toDateStr();
        filterDateList = filterDateList.stream().filter(d -> d.compareTo(lastDay) <= 0).collect(Collectors.toList());

        // 剔除基准日期包含了当天时的自定义对比日期（如：当前=2023-09-23，基准日期(月)=202308~202309，对比月=202303~202304，则需要剔除掉20230423~20230430
        List<List<String>> excludeDateSegments = AnalysisUtil.getExcludeAnalysisDateFilterRange(cxt.config, AnalysisCalcMode.CUSTOM_COMPARE, cxt.compareIndex);
        List<String> excludeDates = new ArrayList<>();
        for(List<String> segment : excludeDateSegments){
            if(BIUtil.isEmpty(segment)){
                continue;
            }
            Date excludeStartDate = DateUtil.parseDate(segment.get(0));
            Date excludeEndDate = DateUtil.parseDate(segment.get(segment.size() - 1));
            excludeDates.addAll(DateUtil.rangeToList(excludeStartDate, excludeEndDate, DateField.DAY_OF_YEAR).stream().map(f -> f.toDateStr()).collect(Collectors.toList()));
        }
        filterDateList.removeAll(excludeDates);


        if(BIUtil.isEmpty(filterDateList)) {
            return daysExpression;
        }

        String startDate = filterDateList.get(0);
        String endDate = startDate;
        if(filterDateList.size() > 1){
            endDate = filterDateList.get(filterDateList.size() - 1);
        }

        Date d1 = DateUtil.parseDate(startDate);
        Date d2 = DateUtil.parseDate(endDate);
        days = DateUtil.between(d1, d2, DateUnit.DAY, true);
        days = days + 1;

//        if(days > 1){
            daysExpression = days + "";
//        }
        return daysExpression;
    }

    protected List<String> getEndDateList(QueryField field, AggregatorContext cxt){
        List<String> endDateList = new ArrayList<>();
        if(cxt.compareIndex < 0 && !cxt.isThb){
            return endDateList;
        }
        List<List<String>> excludeDateSegments = AnalysisUtil.getExcludeAnalysisDateFilterRange(cxt.config, cxt.analysisCalcMode, cxt.compareIndex);

        if(BIUtil.isEmpty(excludeDateSegments)){
            return endDateList;
        }

        for(List<String> segment : excludeDateSegments){
            String endDate = DateUtil.offset(DateUtil.parseDate(segment.get(0)),DateField.DAY_OF_YEAR, -1).toDateStr();
            endDateList.add(endDate);
        }

        return endDateList;
    }

    protected String getBusinessCalendarDayDaysExpression(QueryField field, AggregatorContext cxt) {

        //不是同阶段对比，则使用默认
        if (cxt.analysisCalcMode != null && !cxt.analysisCalcMode.isPromoTb()) {
            return super.getBusinessCalendarDayDaysExpression(field, cxt);
        }

        QueryField commonDateField = cxt.config.getFilterCommonDateField();
        String daysExpression = PromotionConsts.PROMOTION_DURATION_DAYS_CODE;

        List<String> filterDateList = PromotionManager.getPromoDateOffsetList(cxt.config, cxt.analysisCalcMode);
        Long totalCalcDays = DateDiff.calculateDays(filterDateList.get(0), filterDateList.get(filterDateList.size() - 1))
                +1;

        // 计算总计时的天数
        daysExpression = String.format("if(%s is null, %s, max(%s))", commonDateField.getCode(), totalCalcDays, daysExpression);
        return daysExpression;
    }

    public static void main(String[] args) {
        String str = "202312";
        DateTime dateTime = DateUtil.endOfMonth(DateUtil.parse(str, "yyyyMM"));
        dateTime = DateUtil.endOfMonth(dateTime);
        System.out.println(dateTime.toDateStr());
    }
}
