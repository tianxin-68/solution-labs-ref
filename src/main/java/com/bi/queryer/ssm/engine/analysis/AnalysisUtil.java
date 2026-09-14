package com.bi.queryer.ssm.engine.analysis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.lunaryearweek.LunarYearWeekManager;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.promotion.model.PromotionCfg;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateDiff;
import com.bi.queryer.util.period.Lunar;
import com.bi.queryer.util.period.QuarterDateUtil;
import com.bi.queryer.util.period.WeekDateUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:40 2023-07-04
 * @Description 分析工具类
 **/
public abstract class AnalysisUtil {

    /**
     * ----废弃：begin----
     * 扩大日期字段的过滤范围，便于让同环比指标计算有值
     * 使用场景：有同环比分析
     * 示例：如：查询 20230602 ~ 20230608 GMV日环比，需要将日期过滤日期扩大到 20230601 ~ 20230608
     * 实现步骤：
     * 1、获取所有分析字段的分析计算方式（同比、环比）
     * 2、已原始日期过滤值为基准，按1中的同环比日期进行扩大，并添加到此日期字段中
     * ----废弃：end----
     * 获取需要对比日期范围：只返回首位2个日期
     */
    public static List<String> getCompareDateRange(QueryConfigure config, AnalysisCalcMode analysisCalcMode, Integer customCompareIndex) {
        List<String> rangeList = new ArrayList<>();
        List<String> baseDateList = getFilterDateList(config);
        if (BIUtil.isEmpty(baseDateList)) {
            return rangeList;
        }

        // 排序
        Collections.sort(baseDateList);

        List<String> baseDateRange = new ArrayList<>();
        String baseBeginDate = baseDateList.get(0);
        String baseEndDate = baseDateList.get(baseDateList.size() - 1);
        baseDateRange.add(baseBeginDate);
        baseDateRange.add(baseEndDate);

        // 同环比分析类型
        List<QueryField> measureFields = config.getResult().getMeasures();
        if (BIUtil.isEmpty(measureFields)) {
            return rangeList;
        }

        DateGranularity dateGranularity = DateGranularity.get(config.getSettings().getDateGranularity());
        if (dateGranularity == null) {
            return rangeList;
        }

        Set<AnalysisCalcMode> calcModes = getAnalysisCalcModes(config, analysisCalcMode);
        
        // 获取每个分析计算类型的偏移日期
        List<String> offsetDateList = new ArrayList<>();
        for (AnalysisCalcMode calcMode : calcModes) {

            /**
             * 获取同阶段同环比的偏移日期
             */
            if(calcMode.isPromoTb()){
                List<String> promoDateList = PromotionManager.getPromoDateOffsetList(config,calcMode);
                if (BIUtil.isNotEmpty(promoDateList)) {
                    offsetDateList.add(promoDateList.get(0));
                    offsetDateList.add(promoDateList.get(promoDateList.size() - 1));
                }
                continue;
            }

            if (calcMode == AnalysisCalcMode.CUSTOM_COMPARE) {

                if(config.getSettings().isBusinessCalendar()){
                    continue;
                }

                List<String> compareDateList = getCompareDateList(config, customCompareIndex);
                if (BIUtil.isNotEmpty(compareDateList)) {
                    offsetDateList.add(compareDateList.get(0));
                    offsetDateList.add(compareDateList.get(compareDateList.size() - 1));
                }
                continue;
            }

            // 获取农历年周的偏移日期
            if(calcMode.isTblnyw()){

                //因为农历年周的偏移日期非递增，需要使用整个范围的所有时间去偏移
                if(config.getSettings().isBusinessCalendar()){
                    baseDateList = DateUtil.rangeToList(DateUtil.parseDate(baseDateList.get(0)),
                            DateUtil.parseDate(baseDateList.get(baseDateList.size() - 1)), DateField.DAY_OF_YEAR)
                            .stream().map(f -> f.toDateStr()).collect(Collectors.toList());
                }

                offsetDateList = LunarYearWeekManager.getLunarYearWeekDateRange(baseDateList, calcMode);
                continue;
            }

            Date offsetDate = null;
            for (String dateStr : baseDateRange) {
                if (calcMode == AnalysisCalcMode.HB) {
                    if (dateGranularity == DateGranularity.DAY) {
                        offsetDate = DateUtil.offsetDay(DateUtil.parseDate(dateStr), -1);
                    }

                    if (dateGranularity == DateGranularity.WEEK) {
                        offsetDate = DateUtil.offsetDay(DateUtil.parseDate(dateStr), -7);
                    }

                    if (dateGranularity == DateGranularity.MONTH) {
                        offsetDate = DateUtil.offset(DateUtil.parseDate(dateStr), DateField.MONTH, -1);
                    }

                    if (dateGranularity == DateGranularity.QUARTER) {
                        offsetDate = DateUtil.offset(DateUtil.parseDate(dateStr), DateField.MONTH, -3);
                    }
                }
                if (calcMode == AnalysisCalcMode.TB_WEEK) {
                    offsetDate = DateUtil.offsetDay(DateUtil.parseDate(dateStr), -7);
                }
                if (calcMode == AnalysisCalcMode.TB_MONTH) {
                    offsetDate = DateUtil.offset(DateUtil.parseDate(dateStr), DateField.MONTH, -1);
                }
                if (calcMode == AnalysisCalcMode.TB_YEAR || calcMode == AnalysisCalcMode.TB_YEAR_2 || calcMode == AnalysisCalcMode.TB_YEAR_3) {
                    if (dateGranularity == DateGranularity.WEEK) {
                        offsetDate = WeekDateUtil.getYearWeekDay(dateStr, calcMode.getOffset());
                    } else {
                        offsetDate = DateUtil.offset(DateUtil.parseDate(dateStr), DateField.YEAR, calcMode.getOffset());
                    }
                }
                if (calcMode == AnalysisCalcMode.TB_YEAR_WEEK || calcMode == AnalysisCalcMode.TB_YEAR_WEEK_2 || calcMode == AnalysisCalcMode.TB_YEAR_WEEK_3) {
                    offsetDate = WeekDateUtil.getYearWeekDay(dateStr, calcMode.getOffset());
                }
                if (offsetDate != null) {
                    String offsetDateStr = new DateTime(offsetDate).toDateStr();
                    if (!offsetDateList.contains(offsetDateStr)) {
                        offsetDateList.add(offsetDateStr);
                    }
                }
            }

            // 农历的年同比日期范围：已结束日期为基准
            if (isLunarYearCompare(calcMode)) {
                // 农历日期
                offsetDateList = getLunarDateRange(baseDateList, calcMode);
            }


        }

        // 去重
        //offsetDateList = offsetDateList.stream().distinct().collect(Collectors.toList());

        // 排查
        Collections.sort(offsetDateList);

        if(BIUtil.isEmpty(offsetDateList)) {
            return rangeList;
        }

        // 差集作为扩展过滤条件，其中把连续日期转换为日期分段（between），即增加where子句的可读性
        //rangeList = splitContinuousRange(offsetDateList, dateGranularity);
        //rangeList.add(offsetDateList);
        List<String> offsetDateIdList = new ArrayList<>();
        for(String offsetDate : offsetDateList){
            String dateId = "";
            switch (dateGranularity){
                case YEAR:
                    dateId = offsetDate.substring(0, 4).replaceAll("\\-", "");
                    break;
                case MONTH:
                    dateId = offsetDate.substring(0, 7).replaceAll("\\-", "");
                    break;
                case QUARTER:
                    dateId = QuarterDateUtil.getQuarterId(offsetDate);
                    break;
                case WEEK:
                    dateId = WeekDateUtil.getWeekId(offsetDate);
                    break;
                case DAY:
                default:
                    dateId = offsetDate;
                    break;
            }
            offsetDateIdList.add(dateId);
        }

        // 确保有2个时间点
        if(offsetDateIdList.size() == 1){
            offsetDateIdList.add(offsetDateIdList.get(0));
        }

        rangeList = offsetDateIdList;

        return rangeList;
    }

    /**
     * 获取对比日期
     * @param calcMode
     * @param dateStr
     * @return
     */
    public static String getCompareDateByCalcMode(AnalysisCalcMode calcMode,String dateStr) {

        String compareDateStr = "";

        Date compareDate = null;

        if (calcMode == AnalysisCalcMode.HB) {
            compareDate = DateUtil.offsetDay(DateUtil.parseDate(dateStr), -1);
        }
        if (calcMode == AnalysisCalcMode.TB_WEEK) {
            compareDate = DateUtil.offsetDay(DateUtil.parseDate(dateStr), -7);
        }

        if (calcMode == AnalysisCalcMode.TB_MONTH) {
            compareDate = DateUtil.offset(DateUtil.parseDate(dateStr), DateField.MONTH, -1);
        }
        if (calcMode == AnalysisCalcMode.TB_YEAR || calcMode == AnalysisCalcMode.TB_YEAR_2 || calcMode == AnalysisCalcMode.TB_YEAR_3) {
            compareDate = DateUtil.offset(DateUtil.parseDate(dateStr), DateField.YEAR, calcMode.getOffset());
        }
        if (calcMode == AnalysisCalcMode.TB_YEAR_WEEK || calcMode == AnalysisCalcMode.TB_YEAR_WEEK_2 || calcMode == AnalysisCalcMode.TB_YEAR_WEEK_3) {
            compareDate = WeekDateUtil.getYearWeekDay(dateStr, calcMode.getOffset());
        }

        // 农历年同比
        if (calcMode.isTblny()) {
            List<String> lunarDateRange = new ArrayList<>();
            lunarDateRange.add(dateStr);
            List<String> offsetDateList = getLunarDateRange(lunarDateRange, calcMode);
            if (CollUtil.isNotEmpty(offsetDateList)) {
                return offsetDateList.get(0);
            }
        }

        //农历年同
        if (calcMode.isTblnyw()) {
            compareDateStr = LunarYearWeekManager.getLunarYearWeekDate(dateStr, calcMode);
            return compareDateStr;
        }

        if (compareDate != null) {
            compareDateStr = DateUtil.format(compareDate, "yyyy-MM-dd");
        }

        return compareDateStr;
    }

    /**
     * 是否是农历对比
     * @param calcMode
     * @return
     */
    public static boolean isLunarYearCompare(AnalysisCalcMode calcMode) {

        //农历年同比处理
        if (AnalysisCalcMode.TB_LN_YEAR == calcMode || AnalysisCalcMode.TB_LN_YEAR_2 == calcMode || AnalysisCalcMode.TB_LN_YEAR_3 == calcMode) {
            return true;
        }

        return false;
    }

    /**
     * 获取公历日期对应农历日期范围
     * @param baseDateList
     * @param calcMode
     * @return
     */
    public static List<String> getLunarDateRange(List<String> baseDateList, AnalysisCalcMode calcMode){
        // 农历日期
        List<String> offsetDateRange = new ArrayList<>();

        String baseBeginDate = baseDateList.get(0);
        String baseEndDate = baseDateList.get(baseDateList.size() - 1);

        Date endOffsetDate = null;
        int skipSize = 0;
        for(int i = baseDateList.size() - 1; i >=0; i--) {
            endOffsetDate = Lunar.offsetLunarYear(cn.hutool.core.date.DateUtil.parseDate(baseEndDate), calcMode.getOffset());
            if(endOffsetDate != null){
                break;
            }
            skipSize++;
        }
        if(endOffsetDate != null) {
            Long diffDays = cn.hutool.core.date.DateUtil.between(cn.hutool.core.date.DateUtil.parseDate(baseBeginDate), cn.hutool.core.date.DateUtil.parseDate(baseEndDate), DateUnit.DAY, true);
            Date beginOffsetDate = cn.hutool.core.date.DateUtil.offset(endOffsetDate, DateField.DAY_OF_YEAR, new Long((diffDays - skipSize) * -1).intValue());
            offsetDateRange.add(new DateTime(beginOffsetDate).toDateStr());
            offsetDateRange.add(new DateTime(endOffsetDate).toDateStr());
        }

        return offsetDateRange;
    }

    public static Set<AnalysisCalcMode> getAnalysisCalcModes(QueryConfigure config,AnalysisCalcMode analysisCalcMode) {

        boolean isCustomCompare = AnalysisCalcMode.CUSTOM_COMPARE == analysisCalcMode ? true : false;
        Set<AnalysisCalcMode> calcModes = new LinkedHashSet<>();

        for (QueryField measureField : config.getResult().getMeasures()) {

            if (Enabled.value(measureField.getIsAnalysis()) &&
                    (AnalysisCalcMode.get(measureField.getAnalysisConfig().getCalcMode()).isCompare()
                            || AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(measureField.getAnalysisConfig().getCalcMode())
                            || AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(measureField.getAnalysisConfig().getCalcMode())
                    )
            ) {
                AnalysisCalcMode calcMode =measureField.getAnalysisConfig().getRawThbCalcMode();

                //取当前数据集对应的计算方式
                if (calcMode != analysisCalcMode) {
                    continue;
                }

                if (calcMode.isCompare()) {

                    //如果是自定义对比的数据源，筛选计算方式为自定义对比
                    if (isCustomCompare && AnalysisCalcMode.CUSTOM_COMPARE == calcMode) {
                        calcModes.add(calcMode);
                    }

                    if (!isCustomCompare && AnalysisCalcMode.CUSTOM_COMPARE != calcMode) {
                        calcModes.add(calcMode);
                    }
                }
            }
        }

        return calcModes;
    }

    /**
     * 构造需要排除的日期的sql
     * 场景：当月的环比，年同比，需要对齐日期
     * @return
     */
    public static List<List<String>> getExcludeAnalysisDateFilterRange(QueryConfigure config, AnalysisCalcMode analysisCalcMode,Integer customCompareIndex) {
        List<List<String>> result = new ArrayList<>();

        //只处理月粒度
        DateGranularity dateGranularity = DateGranularity.get(config.getSettings().getDateGranularity());
        if (DateGranularity.MONTH != dateGranularity && DateGranularity.QUARTER != dateGranularity) {
            return result;
        }

        QueryField dateField = config.getFilterCommonDateField();
        if (dateField == null) {
            return result;
        }

        List<FieldValue> baseDates = FieldUtil.getFilterRealValues(dateField);
        if (BIUtil.isEmpty(baseDates)) {
            return result;
        }

        Date d1 = null;
        Date d2 = null;

        //季度处理
        if (DateGranularity.QUARTER == dateGranularity) {
            d1 = QuarterDateUtil.getQuarterFirstDay(baseDates.get(0).getId());
            d2 = QuarterDateUtil.getQuarterEndDay(baseDates.get(baseDates.size() - 1).getId());
        } else if (DateGranularity.MONTH == dateGranularity) {
            d1 = DateUtil.parse(baseDates.get(0).getId(), "yyyyMM");
            d2 = DateUtil.endOfMonth(DateUtil.parse(baseDates.get(baseDates.size() - 1).getId(), "yyyyMM"));
        }

        String today = DateUtil.today();
        boolean isContainToday = DateUtil.rangeToList(d1, d2, DateField.DAY_OF_YEAR).stream().map(f -> f.toDateStr()).filter(d -> d.compareTo(today) >= 0).count() > 0;
        if (!isContainToday) {
            return result;
        }

        /*
        //判断基准日期是否包含当月
        List<String> values = fieldValues.stream().map(FieldValue::getId).collect(Collectors.toList());
        boolean isContainCurrentMonth = com.bi.queryer.util.period.DateUtil.isContainCurrentMonth(values);
        if (!isContainCurrentMonth) {
            return result;
        }
         */


        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        int dayCurrent = calendar.get(Calendar.DAY_OF_MONTH);

        //最大可用日期小于昨天，则取最大可用日期
        DateTime lastDay =config.getSettings().getLastAvailableDate();
        calendar.setTime(lastDay);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        //日期粒度为季度，当前日期是1号，则需要排除整月，day置为0
        if(DateGranularity.QUARTER == dateGranularity) {
            if (dayCurrent == 1 && dayCurrent != day) {
                day = 0;
            }
        }

        Set<AnalysisCalcMode> calcModes = getAnalysisCalcModes(config, analysisCalcMode);
        for (AnalysisCalcMode calcMode : calcModes) {
            List<String> filterRange = new ArrayList<>();
            Date excludeDate = null;
            switch (calcMode) {
                case HB:
                    if (DateGranularity.MONTH == dateGranularity) {
                        excludeDate = DateUtil.offset(currentDate, DateField.MONTH, -1);
                    } else if (DateGranularity.QUARTER == dateGranularity) {
                        excludeDate = DateUtil.offset(currentDate, DateField.MONTH, -3);
                    }

                    break;
                case TB_YEAR:
                    excludeDate = DateUtil.offset(currentDate, DateField.YEAR, -1);
                    break;
                case TB_YEAR_2:
                    excludeDate = DateUtil.offset(currentDate, DateField.YEAR, -2);
                    break;
                case TB_YEAR_3:
                    excludeDate = DateUtil.offset(currentDate, DateField.YEAR, -3);
                    break;
                case CUSTOM_COMPARE:
                    List<String> compareDateList = getCompareDateList(config, customCompareIndex);
                    if (CollUtil.isNotEmpty(compareDateList)) {
                        //此处取最后一个
                        excludeDate = DateUtil.parseDate(compareDateList.get(compareDateList.size() - 1));
                    }
                    break;
            }
            filterRange = com.bi.queryer.util.period.DateUtil.getExcludeMonthDateRange(excludeDate, day);
            if (CollUtil.isNotEmpty(filterRange)) {
                result.add(filterRange);
            }
        }

        return result;
    }


    /**
     * 获取过滤日期后的日期范围
     * @return
     */
    public static List<String> getFilterDateList(QueryConfigure config){
        List<String> finalFilterValues = null;
        List<QueryField> filterFields = config.getFilter().getFields();
        if(BIUtil.isEmpty(filterFields)){
            return finalFilterValues;
        }

        // 日期过滤字段之间为and关系，则取各日期日期过滤范围的交集
        for(QueryField field : filterFields){
            FieldFilterType filterType = FieldUtil.getFilterType(field); //FieldFilterType.get(field.getMeta().getFilterShowType());
            if(!field.isCommonDate() || !FieldFilterType.isDateRange(filterType) || filterType == FieldFilterType.DatetimeRange) {
                continue;
            }
            DateGranularity dateGranularity = getQueryDateGranularity(field);
            List<FieldValue> values = field.getValues();
            if(BIUtil.isEmpty(values)) {
                continue;
            }

            // 支持多段区间查询：各区间段为or关系，则去并集
            List<String> currentFilterValues = new ArrayList<>();

            // 支持业务日历
            if(field.isCommonDate() && config.getSettings().isBusinessCalendar()){
                currentFilterValues.addAll(PromotionManager.getFilterDateList(field,config));
            }else{
                FieldUtil.sortRangeValues(field, values);
                int valueCount = (values.size() / 2) * 2; // 只取2的倍数值
                for (int v = 0; v < valueCount; v = v + 2) {
                    FieldValue v1 = values.get(v);
                    FieldValue v2 = values.get(v + 1);

                    Date startDate = null;
                    Date endDate = null;
                    // 日过滤
                    if(dateGranularity == DateGranularity.DAY) {
                        startDate = DateUtil.parseDate(v1.getId());
                        endDate = DateUtil.parseDate(v2.getId());
                    }

                    if(dateGranularity == DateGranularity.WEEK){
                        startDate = WeekDateUtil.getWeekFirstDay(v1.getId());
                        endDate = WeekDateUtil.getWeekLastDay(v2.getId());
                    }

                    // 月过滤
                    if(dateGranularity == DateGranularity.MONTH) {
                        startDate = DateUtil.parse(v1.getId() + "01", "yyyyMMdd");
                        endDate = DateUtil.offset(
                                DateUtil.offset(
                                        DateUtil.parse(v2.getId() + "01", "yyyyMMdd")
                                        , DateField.MONTH, 1)
                                ,DateField.DAY_OF_YEAR, -1);
                    }

                    // 季度过滤
                    if(dateGranularity == DateGranularity.QUARTER) {
                        startDate = QuarterDateUtil.getQuarterFirstDay(v1.getId());
                        endDate = QuarterDateUtil.getQuarterEndDay(v2.getId());
                    }

                    // 年过滤
                    if(dateGranularity == DateGranularity.YEAR) {
                        startDate = DateUtil.parseDate(v1.getId() + "-01-01");
                        endDate = DateUtil.parseDate(v2.getId() + "-12-31");
                    }

                    if(startDate == null || endDate == null) {
                        continue;
                    }

                    List filterValuesSegment = DateUtil.rangeToList(startDate, endDate, DateField.DAY_OF_YEAR).stream().map(f->f.toDateStr()).collect(Collectors.toList());

                    // 同一个字段取并集
                    currentFilterValues.addAll(filterValuesSegment);
                }
            }

            if(finalFilterValues == null && BIUtil.isNotEmpty(currentFilterValues)) {
                finalFilterValues = currentFilterValues;
            }
            // 不同字段取交集
            finalFilterValues = (List<String>) CollectionUtil.intersection(finalFilterValues, currentFilterValues);
        }

        return finalFilterValues;
    }

    /**
     * 获取自定义对比日期
     * @return
     */
    public static List<String> getCompareDateList(QueryConfigure config,Integer customCompareIndex) {

        List<String> finalFilterValues = new ArrayList<>();
        DateGranularity dateGranularity = DateGranularity.get(config.getSettings().getDateGranularity());

        for (AnalysisCompareItemConfig analysisCompareItemConfig : config.getAnalysis().getCompare().getItems()) {

            //判断自定义对比的索引，每一段对比取自己的时间
            if (!customCompareIndex.equals(analysisCompareItemConfig.getCompareIndex())) {
                continue;
            }

            String v1 = analysisCompareItemConfig.getCompareDates().get(0);
            String v2 = analysisCompareItemConfig.getCompareDates().get(1);

            Date startDate = null;
            Date endDate = null;
            // 日过滤
            if (DateGranularity.DAY == dateGranularity) {
                startDate = DateUtil.parseDate(v1);
                endDate = DateUtil.parseDate(v2);
            }

            if (DateGranularity.WEEK == dateGranularity) {
                startDate = WeekDateUtil.getWeekFirstDay(v1);
                endDate = WeekDateUtil.getWeekLastDay(v2);
            }

            // 月过滤
            if (DateGranularity.MONTH == dateGranularity) {
                startDate = DateUtil.parse(v1 + "01", "yyyyMMdd");
                endDate = DateUtil.offset(
                        DateUtil.offset(
                                DateUtil.parse(v2 + "01", "yyyyMMdd")
                                , DateField.MONTH, 1)
                        , DateField.DAY_OF_YEAR, -1);
            }

            // 季度过滤
            if (DateGranularity.QUARTER == dateGranularity) {
                startDate = QuarterDateUtil.getQuarterFirstDay(v1);
                endDate = QuarterDateUtil.getQuarterEndDay(v2);
            }

            // 年过滤
            if (DateGranularity.YEAR == dateGranularity) {
                startDate = DateUtil.parseDate(v1 + "-01-01");
                endDate = DateUtil.parseDate(v2 + "-12-31");
            }

            List filterValuesSegment = DateUtil.rangeToList(startDate, endDate, DateField.DAY_OF_YEAR).stream().map(f -> f.toDateStr()).collect(Collectors.toList());
            finalFilterValues.addAll(filterValuesSegment);
        }

        return finalFilterValues;

    }

    /**
     * 将日期列表拆分为连续范围
     * @param dateList
     * @return
     */
    public static List<List<String>> splitContinuousRange(List<String> dateList, DateGranularity dateGranularity){
        List<List<String>> rangeList = new ArrayList<>();
        if(dateGranularity == DateGranularity.DAY){
            rangeList = splitContinuousDayRange(dateList);
        }
        if(dateGranularity == DateGranularity.WEEK){
            rangeList = splitContinuousWeekRange(dateList);
        }
        if(dateGranularity == DateGranularity.MONTH){
            rangeList = splitContinuousMonthRange(dateList);
        }
        if(dateGranularity == DateGranularity.YEAR){
            rangeList = splitContinuousYearRange(dateList);
        }

        return rangeList;
    }

    public static List<List<String>> splitContinuousDayRange(List<String> dateList){
        List<List<String>> rangeList = new ArrayList<>();
        for (int i = 0; i < dateList.size(); i++) {
            String start = dateList.get(i);
            String end = start;
            for (int j = i; j < dateList.size(); j++) {
                String offset = DateUtil.offset(DateUtil.parse(end), DateField.DAY_OF_YEAR, 1).toDateStr();
                if (dateList.contains(offset)) {
                    end = offset;
                    i++;
                } else {
                    List<String> item = new ArrayList<>();
                    item.add(start);
                    item.add(end);
                    rangeList.add(item);
                    break;
                }
            }
        }
        return rangeList;
    }

    public static List<List<String>> splitContinuousWeekRange(List<String> dateList){
        List<String> weekList = dateList.stream().map(d -> WeekDateUtil.getWeekId(d)).distinct().collect(Collectors.toList());
        List<List<String>> rangeList = new ArrayList<>();
        for (int i = 0; i < weekList.size(); i++) {
            String start = weekList.get(i);
            String end = start;
            for (int j = i; j < weekList.size(); j++) {
                String offset = WeekDateUtil.addWeek(end,1);
                if (weekList.contains(offset)) {
                    end = offset;
                    i++;
                } else {
                    List<String> item = new ArrayList<>();
                    item.add(start);
                    item.add(end);
                    rangeList.add(item);
                    break;
                }
            }
        }
        return rangeList;
    }

    public static List<List<String>> splitContinuousMonthRange(List<String> dateList){
        List<String> monthList = dateList.stream().map(d -> d.substring(0, 7).replaceAll("\\-", "")).distinct().collect(Collectors.toList());
        List<List<String>> rangeList = new ArrayList<>();
        for (int i = 0; i < monthList.size(); i++) {
            String start = monthList.get(i);
            String end = start;
            for (int j = i; j < monthList.size(); j++) {
                String offset = DateUtil.offset(DateUtil.parse(end, "yyyyMM"), DateField.MONTH, 1).toString("yyyyMM");
                if (monthList.contains(offset)) {
                    end = offset;
                    i++;
                } else {
                    List<String> item = new ArrayList<>();
                    item.add(start);
                    item.add(end);
                    rangeList.add(item);
                    break;
                }
            }
        }
        return rangeList;
    }

    public static List<List<String>> splitContinuousYearRange(List<String> dateList){
        List<String> yearList = dateList.stream().map(d -> d.substring(0, 4).replaceAll("\\-", "")).distinct().collect(Collectors.toList());
        List<List<String>> rangeList = new ArrayList<>();
        for (int i = 0; i < yearList.size(); i++) {
            String start = yearList.get(i);
            String end = start;
            for (int j = i; j < yearList.size(); j++) {
                String offset = DateUtil.offset(DateUtil.parse(end, "yyyy"), DateField.YEAR, 1).toString("yyyy");
                if (yearList.contains(offset)) {
                    end = offset;
                    i++;
                } else {
                    List<String> item = new ArrayList<>();
                    item.add(start);
                    item.add(end);
                    rangeList.add(item);
                    break;
                }
            }
        }
        return rangeList;
    }

    /**
     * 是否是分析的日期字段：和分析日期自动同源的字段都作为分析日期字段，即字段编码和分析日期自动编码一致，处理扩展的月、年字段
     * @param field
     * @param analysisDateFieldCode
     * @return
     */
    public static boolean isAnalysisDateField(QueryField field, String analysisDateFieldCode){
        boolean isAnalysisFilterField = false;
        if(analysisDateFieldCode.equalsIgnoreCase(field.getCode())) {
            isAnalysisFilterField = true;
        }
        if(BIUtil.isNotEmpty(field.getMeta().getExtendSrcId())) {
            MetaField extendSrcField = SSDMetaCacheManager.getField(field.getMeta().getExtendSrcId());
            if(extendSrcField != null && analysisDateFieldCode.equalsIgnoreCase(extendSrcField.getCode())) {
                isAnalysisFilterField = true;
            }
        }
        return isAnalysisFilterField;
    }

    /**
     * 获取给定维度列表的grouping值
     * @param config
     * @param groupingFields
     * @return
     */
    public static int getGroupingValue(QueryConfigure config, List<QueryField> groupingFields){
        int value = 0 ;
        if(BIUtil.isEmpty(groupingFields)){
            return value;
        }
        List<QueryField> allDimFields = new ArrayList<>();
        allDimFields.addAll(config.getResult().getRowDimensions());
        List<QueryField> columnDimensions = config.getResult().getColDimensions();
        columnDimensions = columnDimensions.stream().filter(f->!f.isAppend()).collect(Collectors.toList());
        allDimFields.addAll(columnDimensions);

        allDimFields = allDimFields.stream().filter(f->!f.isAppend()).collect(Collectors.toList());

        return getGroupingValue(allDimFields, groupingFields);
    }

    public static int getGroupingValue(List<QueryField> allDimFields, List<QueryField> groupingFields){
        int value = 0 ;
        if(BIUtil.isEmpty(groupingFields)){
            return value;
        }

        Map<String, Integer> matchBits = new LinkedHashMap<>();
        for(QueryField dimField : allDimFields){
            matchBits.put(dimField.getCode(), 0);
            for(QueryField groupingField : groupingFields){
                if(dimField.getCode().equalsIgnoreCase(groupingField.getCode())){
                    matchBits.put(dimField.getCode(), 1);
                    break;
                }
            }
        }
        String bitString = BIUtil.listToStr(matchBits.values(), "");
        value = NumberUtil.binaryToInt(bitString);
        return value;
    }

    public static int getGroupingValue(List<QueryField> allDimFields, QueryField groupingField){
        if(groupingField == null) {
            return 0;
        }
        List<QueryField> groupingFields = new ArrayList<>();
        groupingFields.add(groupingField);
        return getGroupingValue(allDimFields, groupingFields);
    }


    /**
     * 获取查询的日期粒度
     * @param field
     * @return
     */
    public static DateGranularity getQueryDateGranularity(QueryField field){
        if(field.isCommonDate()) {
            return DateGranularity.get(field.getQueryDateGranularity());
        }else {
            FieldFilterType filterType = FieldUtil.getFilterType(field); //FieldFilterType.get(field.getMeta().getFilterShowType());
            if(filterType == FieldFilterType.DateRange) {
                return DateGranularity.DAY;
            }

            // 月过滤
            if(filterType == FieldFilterType.MonthRange) {
                return DateGranularity.MONTH;
            }

            // 年过滤
            if(filterType == FieldFilterType.YearRange) {
                return DateGranularity.YEAR;
            }

        }
        return null;
    }

    /**
     * 对grouping set拆分后去重
     * @param groupingSets
     * @return
     */
    public static Set<String> distinctGroupingSets(Set<String> groupingSets){
        if(BIUtil.isEmpty(groupingSets)) {
            return groupingSets;
        }

        Map<String, String> distinctGroupingSets = new LinkedHashMap<>();
        for(String groupingSet : groupingSets){
            if(BIUtil.isEmpty(groupingSet)){
                distinctGroupingSets.put(groupingSet, groupingSet);
                continue;
            }

            List<String> groupingSetItems = Arrays.asList(groupingSet.toLowerCase().replaceAll("[\\(\\)]","").split(","));
            Collections.sort(groupingSetItems);
            String sortedGroupingSet = String.format("(%s)", BIUtil.listToStr(groupingSetItems));

            if(!distinctGroupingSets.containsKey(sortedGroupingSet)){
                distinctGroupingSets.put(sortedGroupingSet, groupingSet);
            }
        }
        Set<String> result = new LinkedHashSet<>();
        for(String grpSet : distinctGroupingSets.values()){
            result.add(grpSet);
        }
        return result;
    }

    /**
     * 日粒度下的月同比和年同比，某些场景下时间长度比基准日期长，需要过滤掉多余的日期
     * 例：基准时间端 2025/2/27 ~ 2025/3/1
     * 月同比 2025/1/27 ~ 2025/2/1
     * 需要排除 2025/1/29 ~ 2025/1/31
     * 实现步骤：
     * 对比日期按同比方式偏移，存在重复的日期只保留一个，其余的为需要排除的日期
     */
    public static List<String> getExcludeTbDateList(QueryConfigure config,AnalysisCalcMode analysisCalcMode,List<String> compareDateList) {

        List<String> result = new ArrayList<>();

        if (CollUtil.isEmpty(compareDateList)) {
            return result;
        }

        QueryField commonDateField = config.getFilterCommonDateField();
        if (commonDateField == null) {
            return result;
        }

        List<String> baseDateList = commonDateField.getValues().stream().map(f -> f.getId()).collect(Collectors.toList());
        if (CollUtil.isEmpty(baseDateList)) {
            return result;
        }

        //时间端长度一致不处理
        long baseOffset = DateDiff.calculateDays(baseDateList.get(0), baseDateList.get(1));
        long compareOffset = DateDiff.calculateDays(compareDateList.get(0), compareDateList.get(1));
        if (baseOffset == compareOffset) {
            return result;
        }

        Map<String, String> dateMap = new HashMap<>();
        List<DateTime> dateTimeList = DateUtil.rangeToList(DateUtil.parseDate(compareDateList.get(0)), DateUtil.parseDate(compareDateList.get(1)), DateField.DAY_OF_YEAR);
        for (DateTime date : dateTimeList) {

            String offsetDateStr = null;
            switch (analysisCalcMode) {
                case TB_MONTH:
                    offsetDateStr = DateUtil.offset(date, DateField.MONTH, 1).toDateStr();
                    break;
                case TB_YEAR:
                case TB_YEAR_2:
                case TB_YEAR_3:
                    offsetDateStr = DateUtil.offset(date, DateField.YEAR, analysisCalcMode.getOffset() * -1).toDateStr();
                    break;
            }

            if (dateMap.containsKey(offsetDateStr)) {
                result.add(String.format("'%s'",date.toDateStr()));
            } else {
                dateMap.put(offsetDateStr, "");
            }
        }

        return result;
    }


    /**
     * 获取排序的总计类型：多个总计取个最优排序总计类型
     * @return
     */
    public static AnalysisTotalType getOrderByTotalType(QueryConfigure config){
        AnalysisTotalType totalType = AnalysisTotalType.NONE;
        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        if(!totalConfig.isActive()) {
            return totalType;
        }

        Set<AnalysisTotalType> totalTypeSet = totalConfig.getItems().stream().map(item -> item.getTotalType()).collect(Collectors.toSet());
        if(totalTypeSet.contains(AnalysisTotalType.COL_SUBTOTAL)){
            return AnalysisTotalType.COL_SUBTOTAL;
        }
        if(totalTypeSet.contains(AnalysisTotalType.COL_TOTAL)){
            return AnalysisTotalType.COL_TOTAL;
        }
        return totalType;
    }


    public static void main(String[] args) {
        System.out.println(DateUtil.parse("202306","yyyyMM").toDateStr());

        List<String> dataList = new ArrayList<>();
        dataList.add("2023-01-05");
        dataList.add("2023-01-06");
        dataList.add("2023-01-07");
        dataList.add("2023-01-11");
        dataList.add("2023-01-12");
        dataList.add("2023-01-13");
        dataList.add("2023-01-25");
        dataList.add("2023-01-27");
        dataList.add("2023-01-28");

        dataList.add("2023-02-01");
        dataList.add("2023-02-02");
        dataList.add("2023-02-28");

        dataList.add("2023-02-28");
        dataList.add("2023-03-01");
        dataList.add("2023-03-02");

        List<List<String>> ranges = splitContinuousRange(dataList, DateGranularity.DAY);
        for(List<String> item : ranges){
            System.out.println(item);
        }

        ranges = splitContinuousRange(dataList, DateGranularity.MONTH);
        for(List<String> item : ranges){
            System.out.println(item);
        }

        System.out.println(NumberUtil.binaryToInt("001"));

        String pad = StringUtils.rightPad("", 4, "1");
        System.out.println(pad);

        pad = StringUtils.rightPad("0", 4, "1");
        System.out.println(pad);

        pad = StringUtils.rightPad("", 0, "1");
        System.out.println(pad);
    }
}
