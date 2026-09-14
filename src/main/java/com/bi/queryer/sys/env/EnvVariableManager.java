package com.bi.queryer.sys.env;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.period.DateUtil;
import com.bi.queryer.util.period.Week;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 环境变量管理器<br/>
 * 环境变量表达式：${variable}或者${variable+num}，暂时只支持变量加减运算，变量名不区分大小写
 * @author contributor
 */
public abstract class EnvVariableManager {
    public static String contextPath = "";

    public static void initialize(ServletContextEvent event) {
        ServletContext application = event.getServletContext();
        contextPath = application.getContextPath() + "/";
    }

    /**
     * @param variableExpression 变量表达式
     * @param type:自定义变量和系统变量
     * @return
     */
    public static String value(String variableExpression, EnvType type) {
        String value = null;
        switch (type) {
            case All:
                value = value(variableExpression);
                if (StringUtil.isEmpty(value)) {
                    value = parseValue(variableExpression);
                }
                break;
            case Custom:
                value = parseValue(variableExpression);
                break;
            case System:
            default:
                value = value(variableExpression);
                break;
        }

        return value;
    }

    /**
     * 计算变量表达式值
     * @param variableExpression:<br/>环境变量表达式：${variable}或者${variable+num}，暂时只支持变量加减运算，变量名不区分大小写
     * @return
     */
    public static String value(String variableExpression) throws RuntimeException {
        if (StringUtil.isEmpty(variableExpression)) {
            return "";
        }
        variableExpression = variableExpression.trim();

        if (!validate(variableExpression)) {
            return variableExpression;
            //throw new RuntimeException("变量表达式不合法:" + variableExpression);
        }

        EnvVariable ev = null;
        String verExp = variableExpression.replaceAll("[\\$\\{\\}\\-\\+\\d]", "").trim();
        for (EnvVariable var : EnvVariable.values()) {
            if (var.toString().equalsIgnoreCase(verExp)) {
                ev = var;
                break;
            }
        }
        if (ev == null) {
            return "";
        }

        String newExpression = variableExpression.replaceAll("\\$", "").replaceAll("\\{", "").replaceAll("\\}", "");
        newExpression = newExpression.trim();
        String value = "";
        switch (ev) {
            case ContextPath:
                value = contextPath;
                break;
            case CurrentTime:
                value = parseCurrentTime(newExpression);
                break;
            case CurrentDay:
                value = parseCurrentDay(newExpression);
                break;
            case CurrentWeek:
                value = parseCurrentWeek(newExpression);
                break;
            case CurrentWeekName:
                value = parseCurrentWeekName(newExpression);
                break;
            case CurrentMonth:
                value = parseCurrentMonth(newExpression);
                break;
            case CurrentYear:
                value = parseCurrentYear(newExpression);
                break;
            case YesterdayMonthFirstDay:
                value = parseYesterdayMonthFirstDay(newExpression);
                break;
        }

        return value;
    }

    /**
     * 校验表达式的合法性
     * @param variableExpression
     * @return
     */
    public static boolean validate(String variableExpression) {
        if (BIUtil.isEmpty(variableExpression)) {
            return false;
        }
        variableExpression = variableExpression.trim();
        String exp = "\\$\\s*\\{[^\\}]+\\}";
        Pattern pattern = Pattern.compile(exp);
        Matcher matcher = pattern.matcher(variableExpression);
        return matcher.find();
    }

    /**
     * 解析表达式中的偏移量
     * @param variableExpression
     * @return
     */
    protected static int parseOffset(String variableExpression, EnvVariable ev) {
        String newExp = variableExpression.toUpperCase().replaceAll(ev.toString().toUpperCase(), "");
        int offset = 0;
        // 加号
        if (newExp.indexOf("+") != -1) {
            newExp = newExp.replaceAll("\\+", "").trim();
            offset = Integer.valueOf(newExp);
        }
        if (newExp.indexOf("-") != -1) {
            newExp = newExp.replaceAll("\\-", "").trim();
            offset = Integer.valueOf(newExp) * -1;
        }
        return offset;
    }

    /**
     * 解析当前日期
     * @param variableExpression
     * @return
     */
    public static String parseCurrentTime(String variableExpression) {
        // 判断是否有运算符
        int offset = parseOffset(variableExpression, EnvVariable.CurrentTime);
        Calendar c = Calendar.getInstance();
        long current = c.getTimeInMillis();
        current = offset * 60 * 1000 + current;
        c.setTimeInMillis(current);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = df.format(c.getTime());
        return value;
    }

    /**
     * 解析当前日期
     * @param variableExpression
     * @return
     */
    public static String parseCurrentDay(String variableExpression) {
        // 判断是否有运算符
        int offset = parseOffset(variableExpression, EnvVariable.CurrentDay);

        Date date = DateUtil.getDay(offset);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String value = df.format(date);
        return value;
    }

    /**
     * 解析当前周
     * @param variableExpression
     * @return
     */
    public static String parseCurrentWeek(String variableExpression) {
        // 判断是否有运算符
        int offset = parseOffset(variableExpression, EnvVariable.CurrentWeek);

        Date date = DateUtil.getDay(offset * 7);
        Week week = DateUtil.getWeek(date);
        String value = week.getWeekId() + "";
        return value;
    }

    /**
     * 解析当前周名称
     * @param variableExpression
     * @return
     */
    public static String parseCurrentWeekName(String variableExpression) {
        // 判断是否有运算符
        int offset = parseOffset(variableExpression, EnvVariable.CurrentWeekName);

        Date date = DateUtil.getDay(offset * 7);
        Week week = DateUtil.getWeek(date);
        String value = week.getFullName();
        return value;
    }

    /**
     * 解析当前月
     * @param variableExpression
     * @return
     */
    public static String parseCurrentMonth(String variableExpression) {
        // 判断是否有运算符
        int offset = parseOffset(variableExpression, EnvVariable.CurrentMonth);

        Date date = DateUtil.getDay(0);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + offset);
        int monthNum = c.get(Calendar.MONTH) + 1;
        String value = c.get(Calendar.YEAR) + "" + (monthNum > 9 ? monthNum : "0" + monthNum);
        return value;
    }


    /**
     * 解析当前年
     * @param variableExpression
     * @return
     */
    public static String parseCurrentYear(String variableExpression) {
        // 判断是否有运算符
        int offset = parseOffset(variableExpression, EnvVariable.CurrentYear);

        Date date = DateUtil.getDay(0);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) + offset);
        String value = c.get(Calendar.YEAR) + "";
        return value;
    }

    /**
     * 解析T+1日(昨天)的月首日
     * @param variableExpression
     * @return
     */
    public static String parseYesterdayMonthFirstDay(String variableExpression) {
        // 判断是否有运算符
        int offset = parseOffset(variableExpression, EnvVariable.YesterdayMonthFirstDay);
        //获取昨天
        Date yesterday = DateUtil.getYesterday();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        //获取昨天所在月第一天
        String monthFdayStr = DateUtil.getMontnFday(df.format(yesterday), "yyyy-MM-dd");
        Date monthFday = null;
        try {
            monthFday = df.parse(monthFdayStr);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Date date = DateUtil.getDay(monthFday, offset);
        String value = df.format(date);
        return value;
    }

    public static void main(String[] args) {
        String str = "sdfsdf1{ dayxx....  }sssdd";
        String exp = "\\$\\s*\\{[^\\}]+\\}";
        Pattern pattern = Pattern.compile(exp);
        Matcher matcher = pattern.matcher(str);
        System.out.println(matcher.find());

        str = "${CurrentMonthFday+1}";
        String value = value(str);
        System.out.println("CurrentMonthFirstDay:" + value);

        str = "${YesterdayMonthFirstDay-1}";
        String value1 = value(str);
        System.out.println("YesterdayMonthFirstDay:" + value1);
        /*
         str = "${CurrentDay-1}";
        String value = value(str);
        System.out.println("currentDay:" + value);
//        str = "${CurrentWeek-1}";
//        value = value(str);
//        System.out.println("CurrentWeek:" + value);

        str = "${CurrentMonth-1}";
        value = value(str);
        System.out.println("CurrentMonth:" + value);

//        str = "${CurrentYear-10}";
//        value = value(str);
//        System.out.println("CurrentYear:" + value);

        str = "第2屏可放9个入口";
        exp = "(\\d+)";
        pattern = Pattern.compile(exp);
        matcher = pattern.matcher(str);
        while(matcher.find()){
        	System.out.println("------------------");
        	System.out.println(matcher.group(1));
        }

        str = "select * from t -------sddfdfasdssss \n where 1=1";
        exp = "\\s*\\-\\-\\-*";
       // exp = "\\w";
        str = str.replaceAll(exp, "\r\n--");

        System.out.println(str);

        String exp2 = "${CurrentWeekName+1}";
        exp2 = exp2.replaceAll("[\\$\\{\\}\\-\\+\\d]", "");
        System.out.println(exp2);

        String exp3 = "${CurrentQuarter-4}";
        System.out.println(value(exp3));

        String exp4 = "${CurrentQuarterNum+1}";
        System.out.println(value(exp4));

        String exp5 = "${CurrentMonth}";
        System.out.println(value(exp5));
        */
    }

    public static String parseValue(String variableExpression) {
        if (StringUtil.isEmpty(variableExpression)) {
            return "";
        }
        variableExpression = variableExpression.trim();

        if (!validate(variableExpression)) {
            return variableExpression;
            //throw new RuntimeException("变量表达式不合法:" + variableExpression);
        }
        String verExp = variableExpression.replaceAll("[\\$\\{\\}\\-\\+]", "").trim();
//		VariableConfigService service =  (VariableConfigService) SpringContextUtil.getBean("variableConfigService");
//		return service.parseVariable(verExp);
        return verExp;
    }

    /**
     * 替换变量包装字符
     * @param str
     * @return
     */
    public static String replaceWrap(String str) {
        if (BIUtil.isEmpty(str)) {
            return str;
        }
        String newStr = str.replaceAll("[\\$\\{\\}]", "").trim();

        return newStr;
    }
}
