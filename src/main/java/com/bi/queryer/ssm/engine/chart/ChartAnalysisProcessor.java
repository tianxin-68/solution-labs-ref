package com.bi.queryer.ssm.engine.chart;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.chart.rt.SSMRTChartAnalysisProcessor;
import com.bi.queryer.ssm.engine.config.QueryAnalysis;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateDiff;
import com.bi.queryer.util.period.QuarterDateUtil;
import com.bi.queryer.util.period.WeekDateUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Auther: contributor
 * @Date: 2024/7/22 15:25
 * @Description:
 */
public class ChartAnalysisProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(ChartAnalysisProcessor.class);

    public  void doPostProcessor(QueryEngine engine, ResponseMessage responseMessage) {

        try {

            QueryConfigure queryConfigure  = engine.getConfig();

            removeColTotal(responseMessage);
            //补齐日期不连续的值
            fillDiscontinuousDateValues(queryConfigure, responseMessage);
            //添加分析对比日期，用于趋势图的图例
            addAnalysisCompareDate(engine, queryConfigure, responseMessage);

        } catch (Exception e) {
            LOG.error("chart analysis post process", e);
        }
    }

    private  void removeColTotal(ResponseMessage responseMessage) {
        ResultDataSet resultDataSet = (ResultDataSet) responseMessage.getData();
        if (resultDataSet == null || BIUtil.isEmpty(resultDataSet.getRows())) {
            return;
        }
        List<Map<String, Object>> rows = resultDataSet.getRows();
        rows.removeIf(row -> BIUtil.isNotEmpty("" + row.get(BIConsts.GROUPING_VALUE)));
    }

    /**
     * 根据查询配置，填充不连续日期值。
     * 该方法用于处理数据集中日期字段不连续的情况，通过在日期之间插入空行来实现日期的连续显示。
     */
    protected void fillDiscontinuousDateValues(QueryConfigure queryConfigure, ResponseMessage responseMessage) {
        if (responseMessage == null || !responseMessage.getSuccess()) {
            return;
        }

        ResultDataSet resultDataSet = (ResultDataSet) responseMessage.getData();
        if (resultDataSet == null || BIUtil.isEmpty(resultDataSet.getRows())) {
            return;
        }

        String dateGranularityStr = queryConfigure.getSettings().getDateGranularity();
        DateGranularity dateGranularity = DateGranularity.get(dateGranularityStr);

        QueryField commonDate = queryConfigure.getResultCommonDateField();
        boolean showLunarDate = queryConfigure.isShowLunarDate();
        String dateKey = commonDate.getCode();
        String realDateKey = showLunarDate ? BIConsts.DATE_RAW_KEY : dateKey;
        List<Map<String, Object>> rows = resultDataSet.getRows();
        Iterator<Map<String, Object>> iterator = rows.iterator();
        Map<String, Object> firstRow = iterator.next();

        // 初始化模板行数据
        Map<String, Object> templateRow = initTemplateRow(resultDataSet.getColumns(), firstRow);

        Object startDateObj = firstRow.get(realDateKey);
        String startDateStr = startDateObj instanceof String ? startDateObj.toString() : null;
        Calendar startDate = getCurDateCalendarByFullName(dateGranularity, startDateStr);
        if (startDate == null) {
            return;
        }

        int index = 1;
        List<Pair<Integer, Map<String, Object>>> newRows = new ArrayList<>();
        // 根据日期粒度，获取日期增加的步长，用于计算后续日期。
        Pair<Integer, Integer> calendarAddStep = getCalendarAddStep(dateGranularity);
        String preDate = startDateStr;
        while (iterator.hasNext()) {
            Map<String, Object> row = iterator.next();
            Object date = row.get(realDateKey);
            if (!(date instanceof String)) {
                index++;
                continue;
            }

            String curDateStr = date.toString();
            if (curDateStr.equals(preDate)) {
                index++;
                continue;
            }

            preDate = curDateStr;
            Calendar curDate = getCurDateCalendarByFullName(dateGranularity, curDateStr);
            if (curDate == null) {
                index++;
                continue;
            }

            // 检查边界条件
            if (curDate.before(startDate)) {
                // 如果当前日期早于开始日期，则调整开始日期为当前日期
                startDate = (Calendar) curDate.clone();
                index++;
                continue;
            }

            startDate.add(calendarAddStep.getKey(), calendarAddStep.getValue());
            // 当当前日期不等于起始日期时，插入空行来填充日期间隔。
            while (!isCalendarEquals(curDate, startDate, dateGranularity)) {
                //日期不连续，补齐, 可能间隔了多个日期
                Map<String, Object> newRow = new HashMap<>(templateRow);
                newRow.put(realDateKey, formatCurDateCalendar(dateGranularity, startDate));
                newRows.add(Pair.of(index, newRow));
                startDate.add(calendarAddStep.getKey(), calendarAddStep.getValue());
                index++;
            }
            index++;
        }

        for (Pair<Integer, Map<String, Object>> item : newRows) {
            rows.add(item.getLeft(), item.getRight());
        }
    }

    /**
     * 初始化模板行数据。
     * 根据给定的列定义和第一行数据，创建一个模板行，该模板行只包含维度代码对应的值。
     * 如果第一行数据为空，则返回空映射。
     */
    protected Map<String, Object> initTemplateRow(List<ResultDataSetColumn> columns, Map<String, Object> firstRow) {
        if (BIUtil.isEmpty(firstRow)) {
            return Collections.emptyMap();
        }

        Map<String, Object> templateRow = new HashMap<>(firstRow.size());
        for (ResultDataSetColumn column : columns) {
            QueryArea queryArea = QueryArea.get(column.getRawQueryArea());
            if (QueryArea.RowDimension == queryArea) {
                String dimCode = column.getRawCode();
                if (IFunction.Format_Lunar_Date.equalsIgnoreCase(column.getDataFormat())) {
                    dimCode = BIConsts.DATE_RAW_KEY;
                }
                templateRow.put(dimCode, firstRow.get(dimCode));
            }
        }
        return templateRow;
    }

    /**
     * 根据日期粒度获取日期增加的步长。
     *
     * @param dateGranularity 日期粒度，支持日、周、月、季度。
     * @return 一个包含两个元素的Pair对象，第一个元素表示增加的单位（如日、周、月等），第二个元素表示增加的步长。
     */
    private Pair<Integer, Integer> getCalendarAddStep(DateGranularity dateGranularity) {
        switch (dateGranularity) {
            case DAY:
                return Pair.of(Calendar.DAY_OF_YEAR, 1);
            case WEEK:
                return Pair.of(Calendar.WEEK_OF_YEAR, 1);
            case MONTH:
                return Pair.of(Calendar.MONTH, 1);
            case QUARTER:
                return Pair.of(Calendar.MONTH, 3);
        }
        return Pair.of(Calendar.DAY_OF_YEAR, 1);
    }

    /**
     * 判断两个日期在指定粒度下是否相等。
     */
    private boolean isCalendarEquals(Calendar curDate, Calendar compareDate, DateGranularity dateGranularity) {
        if (compareDate.equals(curDate)) {
            return true;
        }

        switch (dateGranularity) {
            case DAY:
                return curDate.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR) &&
                        curDate.get(Calendar.MONTH) == compareDate.get(Calendar.MONTH) &&
                        curDate.get(Calendar.DAY_OF_MONTH) == compareDate.get(Calendar.DAY_OF_MONTH);
            case WEEK:
                return curDate.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR) &&
                        curDate.get(Calendar.WEEK_OF_YEAR) == compareDate.get(Calendar.WEEK_OF_YEAR);
            case MONTH:
                return curDate.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR) &&
                        curDate.get(Calendar.MONTH) == compareDate.get(Calendar.MONTH);
            case QUARTER:
                return curDate.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR) &&
                        curDate.get(Calendar.MONTH) / 3 == compareDate.get(Calendar.MONTH) / 3;
        }
        return false;
    }

    protected void addAnalysisCompareDate(QueryEngine engine, QueryConfigure queryConfigure,
                                               ResponseMessage responseMessage) {
        if (responseMessage == null || !responseMessage.getSuccess()) {
            return;
        }

        ResultDataSet resultDataSet = (ResultDataSet) responseMessage.getData();
        if (resultDataSet == null) {
            return;
        }

        QueryAnalysis queryAnalysis = queryConfigure.getAnalysis();
//        if (!queryAnalysis.isActive()) {
//            return;
//        }

        Map<String, LinkedHashMap<String, String>> compareDateMappings = engine.getCompareDateMappings();
        QueryField commonDate = queryConfigure.getResultCommonDateField();
        String dateGranularityStr = queryConfigure.getSettings().getDateGranularity();
        DateGranularity dateGranularity = DateGranularity.get(dateGranularityStr);
        String realDateKey = commonDate.getCode();

        for (Map<String, Object> row : resultDataSet.getRows()) {
            Object date = row.get(realDateKey);
            String dateStr = date instanceof String ? date.toString() : null;

            Calendar curDate = getCurDateCalendarByFullName(dateGranularity, dateStr);
            if (curDate == null) {
                continue;
            }

            //是否含有农历
            boolean isContainLunarDate = false;

            //同环比
            if (queryAnalysis.isActive() && queryAnalysis.getThb().isActive()) {
                for (AnalysisThbItemConfig itemConfig : queryAnalysis.getThb().getItems()) {
                    String calcMode = itemConfig.getCalcModes().get(0);
                    String dateKey = String.format("%s_%s", calcMode, commonDate.getCode());

                    AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);

                    if (AnalysisCalcMode.TB_LN_YEAR == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_2 == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_3 == analysisCalcMode || analysisCalcMode.isTblnyw()) {
                        isContainLunarDate = true;
                    }

                    if (DateGranularity.DAY.equals(dateGranularity)) {
                        String compareDate = compareDateMappings.getOrDefault(dateStr, new LinkedHashMap<>()).get(calcMode);
                        //农历年同比图例显示
                        if (AnalysisCalcMode.TB_LN_YEAR == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_2 == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_3 == analysisCalcMode) {
                            row.put(dateKey, formatLunarDate(compareDate));
                        } else if (analysisCalcMode.isTblnyw()) {
                            row.put(dateKey, formatLunarDate(compareDate));
                        } else {
                            row.put(dateKey, formatDate(compareDate));
                        }
                    } else {
                        Calendar compareDate = getThbRelativeDate(dateGranularity, curDate, calcMode);

                        //农历年同比图例显示
                        if (AnalysisCalcMode.TB_LN_YEAR == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_2 == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_3 == analysisCalcMode) {
                            row.put(dateKey, formatLunarDate(DateUtil.format(compareDate.getTime(), DatePattern.NORM_DATE_FORMAT)));
                        } else {
                            row.put(dateKey, formatCurDateCalendar(dateGranularity, compareDate));
                        }
                    }
                }
            }

            //农历对比时，本期值也需要标识农历
            if (isContainLunarDate) {
                row.put(String.format("%s_%s", "", commonDate.getCode()), formatLunarDate(DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT)));
            } else {
                row.put(String.format("%s_%s", "", commonDate.getCode()), formatDate(DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT)));
            }

            if (queryAnalysis.isActive() && queryAnalysis.getCompare().isActive()) {
                for (AnalysisCompareItemConfig itemConfig : queryAnalysis.getCompare().getItems()) {
                    String calcMode = itemConfig.getCalcMode();
                    Calendar compareDate = getCompareRelativeDate(dateGranularity, curDate, itemConfig);
                    String dateKey = String.format("%s_%d_%s", calcMode, itemConfig.getCompareIndex(), commonDate.getCode());
                    row.put(dateKey, formatCurDateCalendar(dateGranularity, compareDate));
                }
            }
        }
    }

    protected static String formatLunarDate(String date) {
        if (BIUtil.isEmpty(date)) {
            return "无";
        }
        return String.format("%s %s (%s)"
                , date
                , com.bi.queryer.util.period.DateUtil.getWeekName(date)
                , com.bi.queryer.util.period.DateUtil.toLunarDate(date)
        );
    }

    protected static String formatDate(String date) {
        if (BIUtil.isEmpty(date)) {
            return "无";
        }
        return String.format("%s %s"
                , date
                , com.bi.queryer.util.period.DateUtil.getWeekName(date)
        );
    }


    //通过前端展示的日期名称，转成日历类型
    protected static Calendar getCurDateCalendarByFullName(DateGranularity dateGranularity, String dateStr) {
        if (dateStr == null) {
            return null;
        }
        try {
            Calendar curDate = null;

            switch (dateGranularity) {
                case DAY:
                    curDate = Calendar.getInstance();
                    curDate.setTime(DateUtil.parseDate(dateStr));
                    break;
                case WEEK:
                    curDate = WeekDateUtil.getCalendarByFullName(dateStr);
                    break;
                case MONTH:
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
                    curDate = Calendar.getInstance();
                    curDate.setTime(DateUtil.parse(dateStr, format));
                    break;
                case QUARTER:
                    curDate = Calendar.getInstance();
                    curDate.setTime(QuarterDateUtil.getQuarterFirstDay(dateStr));
                    break;
                case YEAR:
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, Integer.valueOf(dateStr));
                    break;
            }
            return curDate;
        } catch (Exception e) {
            LOG.error("parse date by full name error, dateStr: {}, dateGranularity: {}", dateStr, dateGranularity, e);
            return null;
        }
    }

    protected static Calendar getCurDateCalendar(DateGranularity dateGranularity, String dateStr) {
        Calendar curDate = null;

        switch (dateGranularity) {
            case DAY:
                curDate = Calendar.getInstance();
                curDate.setTime(DateUtil.parseDate(dateStr));
                break;
            case WEEK:
                curDate = WeekDateUtil.getCalendar(dateStr);
                break;
            case MONTH:
                SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
                curDate = Calendar.getInstance();
                curDate.setTime(DateUtil.parse(dateStr, format));
                break;
            case QUARTER:
                curDate = Calendar.getInstance();
                curDate.setTime(QuarterDateUtil.getQuarterFirstDay(dateStr));
                break;
            case YEAR:
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, Integer.valueOf(dateStr));
                break;
        }
        return curDate;
    }

    protected static String formatCurDateCalendar(DateGranularity dateGranularity, Calendar curDate) {

        switch (dateGranularity) {
            case DAY:
                return DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT);
            case WEEK:
                return WeekDateUtil.getWeekByFullName(curDate);
            case MONTH:
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
                return DateUtil.format(curDate.getTime(), format);
            case QUARTER:
                String dateStr = DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT);
                return QuarterDateUtil.getQuarterId(dateStr);
            case YEAR:
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy");
                return DateUtil.format(curDate.getTime(), format1);
        }
        return null;
    }

    protected static Calendar getCompareRelativeDate(DateGranularity dateGranularity, Calendar curDate, AnalysisCompareItemConfig itemConfig) {
        Calendar baseStartDate = getCurDateCalendar(dateGranularity, itemConfig.getBaseDates().get(0));
        Calendar compareStartDate = getCurDateCalendar(dateGranularity, itemConfig.getCompareDates().get(0));

        //季度处理
        if(DateGranularity.QUARTER == dateGranularity){
            String baseQuarterId =  QuarterDateUtil.getQuarterId(DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT));
            String compareQuarterId =  QuarterDateUtil.getQuarterId(DateUtil.format(baseStartDate.getTime(), DatePattern.NORM_DATE_FORMAT));
            long offset = DateDiff.calculateQuarters(compareQuarterId,baseQuarterId);

            Calendar result = Calendar.getInstance();
            result.setTime(compareStartDate.getTime());
            result.add(Calendar.MONTH, (int) (3*offset));
            return result;
        }

        int calendarField = 0;

        Long offset = null;

        // 日过滤
        if (DateGranularity.DAY == dateGranularity) {
            offset = DateDiff.getDateDiff(dateGranularity.getCode(), DateUtil.format(baseStartDate.getTime(), "yyyy-MM-dd"), DateUtil.format(curDate.getTime(), "yyyy-MM-dd"));
            calendarField = Calendar.DAY_OF_YEAR;
        } else if (DateGranularity.WEEK == dateGranularity) {
            offset = DateDiff.getDateDiff(dateGranularity.getCode(), WeekDateUtil.getWeekId(DateUtil.format(baseStartDate.getTime(), "yyyy-MM-dd")), WeekDateUtil.getWeekId(DateUtil.format(curDate.getTime(), "yyyy-MM-dd")));
            calendarField = Calendar.WEEK_OF_YEAR;
        } else if (DateGranularity.MONTH == dateGranularity) {
            offset = DateDiff.getDateDiff(dateGranularity.getCode(), DateUtil.format(baseStartDate.getTime(), "yyyyMM"), DateUtil.format(curDate.getTime(), "yyyyMM"));
            calendarField = Calendar.MONTH;
        } else if (DateGranularity.YEAR == dateGranularity) {
            offset = DateDiff.getDateDiff(dateGranularity.getCode(), DateUtil.format(baseStartDate.getTime(), "yyyy"), DateUtil.format(curDate.getTime(), "yyyy"));
            calendarField = Calendar.YEAR;
        } else {
            return null;
        }

        Calendar result = Calendar.getInstance();
        result.setTime(compareStartDate.getTime());
        result.add(calendarField, offset.intValue());
        return result;
    }

    protected static Calendar getThbRelativeDate(DateGranularity dateGranularity, Calendar curDate, String calcModeStr) {
        Calendar result = Calendar.getInstance();
        result.setTime(curDate.getTime());
        AnalysisCalcMode calcMode = AnalysisCalcMode.get(calcModeStr);
        if (calcMode == AnalysisCalcMode.HB) {
            if (dateGranularity == DateGranularity.DAY) {
                result.add(Calendar.DAY_OF_YEAR, -1);
            } else if (dateGranularity == DateGranularity.WEEK) {
                result.add(Calendar.WEEK_OF_YEAR, -1);
            } else if (dateGranularity == DateGranularity.MONTH) {
                result.add(Calendar.MONTH, -1);
            } else if (dateGranularity == DateGranularity.QUARTER) {
                result.add(Calendar.MONTH, -3);
            } else if (dateGranularity == DateGranularity.YEAR) {
                result.add(Calendar.YEAR, -1);
            }
        } else if (calcMode == AnalysisCalcMode.TB_WEEK) {
            result.add(Calendar.DAY_OF_YEAR, -7);
        } else if (calcMode == AnalysisCalcMode.TB_MONTH) {
            result.add(Calendar.MONTH, -1);
        } else if (calcMode == AnalysisCalcMode.TB_YEAR || calcMode == AnalysisCalcMode.TB_YEAR_2 || calcMode == AnalysisCalcMode.TB_YEAR_3) {
            if (dateGranularity == DateGranularity.WEEK) {
                WeekDateUtil.shiftByYear(result, calcMode.getOffset());
            } else {
                result.add(Calendar.YEAR, calcMode.getOffset());
            }
        } else if (calcMode == AnalysisCalcMode.TB_YEAR_WEEK || calcMode == AnalysisCalcMode.TB_YEAR_WEEK_2 || calcMode == AnalysisCalcMode.TB_YEAR_WEEK_3) {
            WeekDateUtil.shiftByYear(result, calcMode.getOffset());
        }

        //农历年同比处理
        if (AnalysisCalcMode.TB_LN_YEAR == calcMode || AnalysisCalcMode.TB_LN_YEAR_2 == calcMode || AnalysisCalcMode.TB_LN_YEAR_3 == calcMode) {

            List<String> baseDateList = new ArrayList<>();
            baseDateList.add(DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT));
            List<String> dateList = AnalysisUtil.getLunarDateRange(baseDateList, calcMode);
            result.setTime(DateUtil.parseDate(dateList.get(0)));
        }

        return result;
    }

}
