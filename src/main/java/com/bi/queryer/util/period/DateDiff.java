package com.bi.queryer.util.period;


import com.bi.queryer.ssm.enums.DateGranularity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-24  10:14
 * @Description: 计算时间差
 */
public class DateDiff {

    public static long calculateDays(String date1, String date2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate d1 = LocalDate.parse(date1, formatter);
        LocalDate d2 = LocalDate.parse(date2, formatter);
        return ChronoUnit.DAYS.between(d1, d2);
    }

    public static long calculateWeeks(String date1, String date2) {

        int year1 = Integer.parseInt(date1.substring(0,4));
        int week1 = Integer.parseInt(date1.substring(4));

        Calendar instance1 = Calendar.getInstance();
        instance1.setFirstDayOfWeek(Calendar.MONDAY);
        instance1.setMinimalDaysInFirstWeek(4);
        instance1.set(Calendar.YEAR, year1);
        instance1.set(Calendar.WEEK_OF_YEAR,week1);

        int year2 = Integer.parseInt(date2.substring(0,4));
        int week2 = Integer.parseInt(date2.substring(4));

        Calendar instance2 = Calendar.getInstance();
        instance2.setFirstDayOfWeek(Calendar.MONDAY);
        instance2.setMinimalDaysInFirstWeek(4);
        instance2.set(Calendar.YEAR, year2);
        instance2.set(Calendar.WEEK_OF_YEAR,week2);

        long result = (instance2.getTime().getTime() - instance1.getTime().getTime()) / (1000 * 60 * 60 * 24 * 7);
        return result;
    }

    public static long calculateMonths(String date1, String date2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        YearMonth d1 = YearMonth.parse(date1 , formatter);
        YearMonth d2 = YearMonth.parse(date2, formatter);
        return ChronoUnit.MONTHS.between(d1, d2);
    }

    public static long calculateQuarters(String date1, String date2) {

        Date d1 = QuarterDateUtil.getQuarterFirstDay(date1);
        Date d2 = QuarterDateUtil.getQuarterFirstDay(date2);

        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);

        int year1 = cal1.get(Calendar.YEAR);
        int month1 = cal1.get(Calendar.MONTH) + 1;
        int quarter1 = (month1 - 1) / 3 + 1;

        int year2 = cal2.get(Calendar.YEAR);
        int month2 = cal2.get(Calendar.MONTH) + 1;
        int quarter2 = (month2 - 1) / 3 + 1;

        int totalQuarters = (year2 - year1) * 4 + (quarter2 - quarter1);
        return totalQuarters;
    }

    public static long calculateYears(String date1, String date2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        Year d1 = Year.parse(date1, formatter);
        Year d2 = Year.parse(date2, formatter);
        return ChronoUnit.YEARS.between(d1, d2);
    }

    public static long getDateDiff(String dateGranularity,String date1,String date2){

        long result = 0;
        switch (DateGranularity.get(dateGranularity)){

            case DAY:
                result = calculateDays(date1,date2);
                break;
            case WEEK:
                result = calculateWeeks(date1,date2);
                break;
            case MONTH:
                result = calculateMonths(date1,date2);
                break;
            case QUARTER:
                result = calculateQuarters(date1,date2);
                break;
            case YEAR:
                result = calculateYears(date1,date2);
                break;
        }

        return result;
    }

    public static void main(String[] args) throws ParseException {

//        System.out.println(calculateYears("2021","2023"));
//        System.out.println(calculateMonths("202205","202305"));
//        System.out.println(calculateDays("2023-08-22","2023-08-23"));
        //System.out.println(calculateWeeks("202301","202306"));

        System.out.println(calculateQuarters("2024-Q4","2023-Q4"));

    }

}
