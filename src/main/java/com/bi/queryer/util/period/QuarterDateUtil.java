package com.bi.queryer.util.period;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.sys.exception.BIException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 季度日期函数
 */
public class QuarterDateUtil {

    private final static String fmt1 = "yyyy-MM-dd";

    /**
     * 获取季度第一天
     * @param quarterId
     * @return
     */
    public static Date getQuarterFirstDay(String quarterId) {

        try {
            // 解析年份和季度
            int year = Integer.parseInt(quarterId.substring(0, 4));
            int quarter = Integer.parseInt(quarterId.substring(6, 7));

            int month = getFirstMonthOfQuarter(quarter);

            // 计算季度的第一天
            String dateStr = String.format("%s-0%s-01", year, month);

            SimpleDateFormat df = new SimpleDateFormat(fmt1);
            Date date = df.parse(dateStr);

            return date;

        } catch (Exception e) {
            throw new BIException(String.format("%s日期格式转化异常", quarterId));
        }

    }

    /**
     * 获取季度最后一天
     * @param quarterId
     * @return
     */
    public static Date getQuarterEndDay(String quarterId) {

        try {

            // 解析年份和季度
            int year = Integer.parseInt(quarterId.substring(0, 4));
            int quarter = Integer.parseInt(quarterId.substring(6, 7));
            int month = getFirstMonthOfQuarter(quarter) + 2;

            // 计算季度的最后一天
            LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

            // 使用TemporalAdjusters.lastDayOfMonth()来获取月份的最后一天
            LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

            String dateStr = String.format("%s-0%s-%s", year, month, lastDayOfMonth.getDayOfMonth());
            SimpleDateFormat df = new SimpleDateFormat(fmt1);
            Date date = df.parse(dateStr);

            return date;

        } catch (Exception e) {
            throw new BIException(String.format("%s日期格式转化异常", quarterId));
        }
    }

    /**
     * 获取日期所在季度
     * @param dateStr
     * @return
     */
    public static String getQuarterId(String dateStr){

        try {

            SimpleDateFormat df = new SimpleDateFormat(fmt1);
            Date date = df.parse(dateStr);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // 获取季度
            int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH返回的月份是从0开始的，所以需要+1
            int quarter = month / 3;
            if (month % 3 != 0) {
                quarter++;
            }

            return String.format("%s-Q%s", calendar.get(Calendar.YEAR), quarter);


        } catch (Exception e) {
            throw new BIException(String.format("%s日期格式转化异常", dateStr));
        }
    }

    private static int getFirstMonthOfQuarter(int quarter) {
        switch (quarter) {
            case 1:
                return 1; // January
            case 2:
                return 4; // April
            case 3:
                return 7; // July
            case 4:
                return 10; // October
            default:
                throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }
    }

    /**
     * 获取2个季度之间的所有季度
     * @param start
     * @param end
     * @return
     */
    public static List<String> getQuarterList(String start, String end) {

        List<String> quarterList = new ArrayList<>();

        try {

            String[] startQuarterArray = start.split("-Q");
            String[] endQuarterArray = end.split("-Q");

            int startYear = Integer.parseInt(startQuarterArray[0]);
            int startQ = Integer.parseInt(startQuarterArray[1]);

            int endYear = Integer.parseInt(endQuarterArray[0]);
            int endQ = Integer.parseInt(endQuarterArray[1]);

            for (int year = startYear; year <= endYear; year++) {

                int idx = year == startYear ? startQ : 1;
                int size = year == endYear ? endQ : 4;

                for (int q = idx; q <= size; q++) {
                    quarterList.add(year + "-Q" + q);
                }
            }

        } catch (Exception e) {
            throw new BIException(String.format("%s季度日期格式转化异常%s-%s", start, end));
        }

        return quarterList;
    }

    public static void main(String[] args) {
//        String quarterId = "2023-Q1";
//        Date firstDay = getQuarterFirstDay(quarterId);
//        Date endDay = getQuarterEndDay(quarterId);
//        System.out.println(firstDay);
//        System.out.println(endDay);
//
//        String dateStr = "2023-04-23";
//        String quarterId2 = getQuarterId(dateStr);
//        System.out.println(quarterId2);

        List<String> quarterList = getQuarterList("2023-Q2","2024-Q3");
        System.out.println(quarterList);
    }
}
