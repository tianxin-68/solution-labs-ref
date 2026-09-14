package com.bi.queryer.util.period;

import cn.hutool.core.date.DateTime;
import com.bi.queryer.sys.exception.BIException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-25  15:07
 * @Description: 周日期函数
 */
public class WeekDateUtil {

    private final static String fmt1 = "yyyy-MM-dd";

    private final static String fmt2 = "MM-dd";

    // -----------------------------------------周相关------------------------------------------------------//

    /**
     * 根据周id获取周的第一天
     * @param weekid
     * @return
     * @throws ParseException
     */
    public static Date getWeekFirstDay(String weekid) {
        Calendar calendar = getCalendar(weekid);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return calendar.getTime();
    }

    /**
     * 根据周id获取最后一天
     * @param weekid
     * @return
     */
    public static Date getWeekLastDay(String weekid){
        Calendar calendar = getCalendar(weekid);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return calendar.getTime();
    }

    /**
     * 根据周ID获取当前的周
     * @param weekid
     * @return
     */
    public static Calendar getCalendar(String weekid) {
        String weekYear = weekid.substring(0, 4);
        String weekOfYear = weekid.substring(4, 6);

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.set(Calendar.YEAR, Integer.valueOf(weekYear));
        calendar.set(Calendar.WEEK_OF_YEAR, Integer.valueOf(weekOfYear));
        return calendar;
    }

    public static Calendar getCalendarByFullName(String weekFullName) {
        String weekYear = weekFullName.substring(0, 4);
        String weekOfYear = weekFullName.substring(6, 8);

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.set(Calendar.YEAR, Integer.valueOf(weekYear));
        calendar.set(Calendar.WEEK_OF_YEAR, Integer.valueOf(weekOfYear));
        return calendar;
    }

    /**
     * 获取周id对应的周全名
     * @return
     */
    public static String getWeekFullnameById(String weekid) {

        Calendar calendar = getCalendar(weekid);
        String weekOfYearRe = getWeekOfYear(calendar);
        int weekYearRe = getWeekYear(calendar, weekOfYearRe, calendar.get(Calendar.YEAR));
        String weekFullname = getWeekFullname(calendar);

        weekFullname = String.format("%s-W%s(%s)",weekYearRe,weekOfYearRe,weekFullname);

        return weekFullname;
    }

    public static String getWeekByFullName(Calendar calendar) {
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        String weekOfYearRe = getWeekOfYear(calendar);
        int weekYearRe = getWeekYear(calendar, weekOfYearRe, calendar.get(Calendar.YEAR));
        String weekFullname = getWeekFullname(calendar);
        weekFullname = String.format("%s-W%s(%s)", weekYearRe, weekOfYearRe, weekFullname);
        return weekFullname;
    }

    /**
      * 获取当前的周开始时间～结束时间
     */
    public static String getWeekFullname(Calendar calendar){

        SimpleDateFormat df1 = new SimpleDateFormat(fmt2);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String startDateStr = df1.format(calendar.getTime());

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        String endDateStr = df1.format(calendar.getTime());

        return startDateStr + "～" + endDateStr;

    }

    /**
     * 获取周id
     * @param dateStr
     * @return
     */
    public static String getWeekId(String dateStr) {

        String weekId = "";
        try {

            SimpleDateFormat df = new SimpleDateFormat(fmt1);
            Date date = df.parse(dateStr);
            weekId = getWeekId(date);
        } catch (Exception e) {
            throw new BIException(String.format("%s日期格式转化异常",dateStr));
        }

        return weekId;
    }

    /**
     * 获取周id
     * @param date
     * @return
     */
    public static String getWeekId(Date date) {

        String weekId = "";
        Calendar calendar = getCalendar(date);
        String weekOfYear = getWeekOfYear(calendar);
        int weekYear = getWeekYear(calendar, weekOfYear, calendar.get(Calendar.YEAR));

        weekId = weekYear + weekOfYear;
        return weekId;
    }

    /**
     * 周加减
     * @param weekId
     * @param weekOffset
     * @return
     */
    public static String addWeek(String weekId,long weekOffset){
        String weekYear = weekId.substring(0, 4);
        String weekOfYear = weekId.substring(4, 6);

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.set(Calendar.YEAR, Integer.valueOf(weekYear));
        calendar.set(Calendar.WEEK_OF_YEAR, Integer.valueOf(weekOfYear));
        calendar.add(Calendar.WEEK_OF_YEAR, (int)weekOffset);
        String weekOfYearRe = getWeekOfYear(calendar);
        int weekYearRe = getWeekYear(calendar, weekOfYearRe, calendar.get(Calendar.YEAR));

        return  weekYearRe + weekOfYearRe;
    }

    /**
     * 获取年份对应日期所属偏移年份同周中对应的星期日期
     * 返回：偏移年份对应日期所属同周中对应的星期日期，如：dateStr=2023-01-01，yearOffset=-1，2023-01-01周id=202252，去年weekid=202152，2023-01-01是星期日，则返回：2022-01-02
     * @param dateStr
     * @param yearOffset
     * @return
     */
    public static DateTime getYearWeekDay(String dateStr, long yearOffset) {

        DateTime yearWeekDay = null;
        try {

            SimpleDateFormat df = new SimpleDateFormat(fmt1);
            Date date = df.parse(dateStr);
            Calendar calendar = getCalendar(date);

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String weekOfYear = getWeekOfYear(calendar);
            int weekYear = getWeekYear(calendar, weekOfYear, calendar.get(Calendar.YEAR)) + (int) yearOffset;
            String yearWeekDayStr = getYearWeekDay(calendar, weekYear, Integer.valueOf(weekOfYear), dayOfWeek);

            Date yearWeekDayDate = df.parse(yearWeekDayStr);
            yearWeekDay =new DateTime(yearWeekDayDate);

        } catch (Exception e) {
            throw new BIException(String.format("%s日期格式转化异常", dateStr));
        }

        return yearWeekDay;
    }

    public static Calendar shiftByYear(Calendar calendar, long yearOffset) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String weekOfYear = getWeekOfYear(calendar);
        int weekYear = getWeekYear(calendar, weekOfYear, calendar.get(Calendar.YEAR)) + (int) yearOffset;
        calendar.set(Calendar.YEAR, weekYear);
        calendar.set(Calendar.WEEK_OF_YEAR, Integer.valueOf(weekOfYear));
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        return calendar;
    }

    /**
     * 获取日期字符
     */
    public static String getYearWeekDay(Calendar calendar, int weekYear, int weekOfYear, int dayOfWeek){
        SimpleDateFormat df1 = new SimpleDateFormat(fmt1);
        calendar.set(Calendar.YEAR, weekYear);
        calendar.set(Calendar.WEEK_OF_YEAR, weekOfYear);
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        return df1.format(calendar.getTime());
    }


    public static Calendar getCalendar(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar;
    }

    /**
     * 获取一年中的第几周
     */
    public static String getWeekOfYear(Calendar calendar){
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        if (weekOfYear < 10) {
            return "0" + weekOfYear;
        }
        return "" + weekOfYear;
    }

    /**
     * 获取年份
     */
    public static int getWeekYear(Calendar calendar, String weekOfYear, int weekYear){
        int result = weekYear;
        if (calendar.get(Calendar.MONTH) == 0 && calendar.get(Calendar.DAY_OF_YEAR) < 4 && Integer.valueOf(weekOfYear) > 1){
            result = weekYear - 1;
        }
        if (calendar.get(Calendar.MONTH) == 11 && calendar.get(Calendar.DATE) >= 29 && Integer.valueOf(weekOfYear) == 1){
            result = weekYear + 1;
        }
        return result;
    }
}
