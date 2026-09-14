package com.bi.queryer.util.period;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.lunaryearweek.LunarYearWeekManager;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.promotion.model.PromotionCfg;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.DateType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class DateUtil {

	//protected final static Log log = LogFactory.getLog(DateUtil.class.getClass());

    private static final Map<String, String> DT_WEEK_MAP = new Hashtable<>();
	
	public final static String fmt1="yyyyMMdd";
    public final static String fmt2="yyyy-MM-dd";

	 /**
     * 把字符串日期转换为Calendar 
     * 
     * @return
     */
    public static Calendar strToCalendar(String datestr,String formatstr) {
	 Calendar time=Calendar.getInstance(); 
     if(StringUtil.isEmpty(datestr))
         return time;
     if(StringUtil.isEmpty(formatstr))
         formatstr="yyyy-MM-dd";
     if(checkDatestrFormatstr(datestr,formatstr)){
         return null;
     }
     DateFormat sdf=new SimpleDateFormat(formatstr);
     Date date=null;
     try {
         date = sdf.parse(datestr);
     } catch (Exception e) {
//         log.debug("公共方法调用getWeekInYear，日期转换失败");
         return null;
     }
     time.setTime(date);
     return time;
}
    /**
     * 把日期转换为年月日字符串 
     * 
     * @param date
     * @return
     */
    public static String toDateStr(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * 把日期转换为年月日字符串 
     * 
     * @param date
     * @return
     */
    public static String toDateStr(Date date,String fmt) {
    	if(StringUtil.isEmpty(fmt)){
    		return new SimpleDateFormat("yyyy-MM-dd").format(date);
    	}else{
    		return new SimpleDateFormat(fmt).format(date);
        }
    }
    
    /**
     * 把日期转换为年月日 时分秒字符串 
     * 
     * @param date
     * @return
     */
    public static String toLongDateStr(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
    
    /**
	 * 方法描述：string  转date
	 * 参数：String
	 * 创建人：contributor
	 * 创建时间：2014-11-25 下午4:58:30
	 * 邮件地址：contributor@example.com
	 * 返回值:Date
	 */ 
    public static Date stirngToDate(String dateTime) throws ParseException{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");  
		Date date = sdf.parse(dateTime);
		return date;
	}

    /**
     * 把当前日期转换为年月日字符串 
     * 
     * @return
     */
    public static String toDateStr() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    public static String toDateMonthStr() {
        return new SimpleDateFormat("yyyy-MM").format(new Date());
    }

    /**
     * 把当前日期转换为年月日 时分秒字符串 
     * 
     * @return
     */
    public static String ToLongDateStr() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
    
    /**
     * 根据当前的月 得到你要的月份 输出格式是 200608
     * 
     * @param number
     *            int 负数为已经过去的月份
     * @return String add by chenxin
     */
    public static String getSithMoth(int number) {
        Calendar cd = Calendar.getInstance();
        cd.add(cd.MONTH, number);
        String moth = getCalendar(cd, null);
        moth = moth.substring(0, 6);
        return moth;

    }

    /**
     * 方法描述：得到当前日期型的字符串 输入参数：aa 输出参数：当前日期型的字符串 
     */

    public static String getCalendar(Calendar aa, String split) {
        try {
            long longCalendar = 0;
            // 获得当前日期
            // Calendar cldCurrent = Calendar.getInstance();
            Calendar cldCurrent = aa;
            // 获得年月日
            String strYear = String.valueOf(cldCurrent.get(Calendar.YEAR));
            String strMonth = String
                    .valueOf(cldCurrent.get(Calendar.MONTH) + 1);
            String strDate = String.valueOf(cldCurrent.get(Calendar.DATE));
            // 整理格式
            if (strMonth.length() < 2) {
                strMonth = "0" + strMonth;
            }
            if (strDate.length() < 2) {
                strDate = "0" + strDate;
            }
            if (split != null && !split.equals("")) {
                return strYear + split + strMonth + split + strDate;
            } else {
                return strYear + strMonth + strDate;
            }
        } catch (Exception Exp) {
            return "";
        }
    }
    /**
     * 判断两个值是否匹配，即输入的日期不带-格式，格式化字符串又带-这一类的情况
     * @param datestr 日期
     * @param formatstr 格式化参数，yyyy-mm-dd,yyyymmdd
     * */
    private static boolean checkDatestrFormatstr(String datestr,String formatstr){
        if(formatstr.indexOf("-")>-1&&datestr.indexOf("-")==-1){
//        	log.debug("公共方法调用checkDatestrFormatstr，检查到日期与格式字符串不匹配");
            return true;
        }
        if(formatstr.indexOf("-")==-1&&datestr.indexOf("-")>-1){
//            log.debug("公共方法调用checkDatestrFormatstr，检查到日期与格式字符串不匹配");
            return true;
        }
        return false;
    }
    
    
    /**
     * 得到指定日期的年周数，如果没有指定日期则计算今天的年周数
     * @param datestr 日期
     * @param formatstr 格式化参数，yyyy-mm-dd,yyyymmdd
     * */
    public static String getWeekInYear(String datestr,String formatstr){
        Calendar time=Calendar.getInstance(); 
        if(StringUtil.isEmpty(datestr))
            return StringUtil.ifNull(time.get(Calendar.WEEK_OF_YEAR));
        if(StringUtil.isEmpty(formatstr))
            formatstr="yyyy-MM-dd";
        if(checkDatestrFormatstr(datestr,formatstr)){
            return "";
        }
        DateFormat sdf=new SimpleDateFormat(formatstr);
        Date date=null;
        try {
            date = sdf.parse(datestr);
        } catch (Exception e) {
//            log.debug("公共方法调用getWeekInYear，日期转换失败");
            return "";
        }
        time.setTime(date);
        return StringUtil.ifNull(time.get(Calendar.WEEK_OF_YEAR));
    }
    
    /**
	  * 根据日期字符串判断当月第几日
	  * @param str
	  * @return
	  * @throws Exception
	  */
	 public static int getDayWeek(String str){
		 SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd");
		 Date date;
		 int day=0;
		try {
			date = sdf.parse(str);
			 Calendar calendar = Calendar.getInstance();
			 calendar.setTime(date);
		     //第几天，从周日开始
		     day = calendar.get(Calendar.DAY_OF_WEEK);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     return day;
	 }
    /**
     * 得到上月的第一天
     * */
    public static String getBeforeMontnFday(){
        Calendar time=Calendar.getInstance(); 
        time.add(Calendar.MONTH, -1);
        time.set(Calendar.DATE, 1);
        return new SimpleDateFormat("yyyy-MM-dd").format(time.getTime());
    }
    /**
     * 得到上月的最后一天
     * */
    public static String getBeforeMontnLday(){
        Calendar time=Calendar.getInstance(); 
        time.set(Calendar.DATE, 1);
        time.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd").format(time.getTime());
    }
    
    /**
     * 得到指定月最后一天
     * */
    public static String getNextMontnLday(int index){
        Calendar time=Calendar.getInstance(); 
        time.set(Calendar.MONTH, index);
        time.set(Calendar.DATE, 1);
        time.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd").format(time.getTime());
    }
    
    /**
     * 得到指定月第一天
     * */
    public static String getBeforeMontnFday(int index){
        Calendar time=Calendar.getInstance(); 
        time.set(Calendar.MONTH, index-1);
        time.set(Calendar.DATE, 1);
        return new SimpleDateFormat("yyyy-MM-dd").format(time.getTime());
    }
    /**
     * 得到本月的第一天
     * @param datestr 日期
     * @param formatstr 格式化参数，yyyy-mm-dd,yyyymmdd
     * */
    public static String getMontnFday(String datestr,String formatstr){
        Calendar time=Calendar.getInstance(); 
        if(!StringUtil.isEmpty(datestr)){//没传入参数算今天,否则算指定日期
            if(StringUtil.isEmpty(formatstr))
                formatstr="yyyy-MM-dd";
            if(checkDatestrFormatstr(datestr,formatstr)){
                return "";
            }
            DateFormat sdf=new SimpleDateFormat(formatstr);
            Date date=null;
            try {
                date = sdf.parse(datestr);
            } catch (Exception e) {
//                log.debug("公共方法调用getWeekInYear，日期转换失败");
                return "";
            }
            time.setTime(date);
        }
        time.set(Calendar.DATE, 1);
        return new SimpleDateFormat(formatstr).format(time.getTime());
    }
    /**
     * 得到本月的最后一天
     * @param datestr 日期
     * @param formatstr 格式化参数，yyyy-mm-dd,yyyymmdd
     * */
    public static String getMontnLday(String datestr,String formatstr){
        Calendar time=Calendar.getInstance(); 
        if(!StringUtil.isEmpty(datestr)){//没传入参数算今天,否则算指定日期
            if(StringUtil.isEmpty(formatstr))
//                formatstr="yyyy-MM-dd";
            if(checkDatestrFormatstr(datestr,formatstr)){
                return "";
            }
            DateFormat sdf=new SimpleDateFormat(formatstr);
            Date date=null;
            try {
                date = sdf.parse(datestr);
            } catch (Exception e) {
//                log.debug("公共方法调用getWeekInYear，日期转换失败");
                return "";
            }
            time.setTime(date);
        }
        time.add(Calendar.MONTH, 1);
        time.set(Calendar.DATE, 1);
        time.add(Calendar.DATE, -1);
        return new SimpleDateFormat(formatstr).format(time.getTime());
    }
    /**
     * 计算指定日期所属季度的第一天在一年中周序号
     * */
    public static int getWeekNoInYearByFisDate(String datestr){
        if(StringUtil.isEmpty(datestr)){
            datestr=toDateStr();
        }
        int month=0;
        String tmp_date=datestr.substring(0, 4);
        if(datestr.indexOf("-")==-1){
            month=Integer.parseInt(datestr.substring(4, 6));
        }else{
            month=Integer.parseInt(datestr.substring(5, 7));
        }
        if(month<4){
            tmp_date=tmp_date+"-01-01";
        }else if(month<7){
            tmp_date=tmp_date+"-04-01";
        }else if(month<10){
            tmp_date=tmp_date+"-07-01";
        }else{
            tmp_date=tmp_date+"-10-01";
        }
        Calendar time=Calendar.getInstance(); 
        DateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        Date date=null;
        try {
            date = sdf.parse(tmp_date);
        } catch (Exception e) {
//            log.debug("公共方法调用getWeekNoInYearByDate，日期转换失败");
            return 0;
        }
        time.setTime(date);
        return time.get(Calendar.WEEK_OF_YEAR);
    } 
    
    /**
     * 计算指定日期所属季度的最后一天在一年中周序号
     * */
    public static int getWeekNoInYearByLastDate(String datestr){
        if(StringUtil.isEmpty(datestr)){
            datestr=toDateStr();
        }
        int month=0;
        String tmp_date=datestr.substring(0, 4);
        if(datestr.indexOf("-")==-1){
            month=Integer.parseInt(datestr.substring(4, 6));
        }else{
            month=Integer.parseInt(datestr.substring(5, 7));
        }
        if(month<4){
            tmp_date=tmp_date+"-03-31";
        }else if(month<7){
            tmp_date=tmp_date+"-06-30";
        }else if(month<10){
            tmp_date=tmp_date+"-09-30";
        }else{
            tmp_date=tmp_date+"-12-31";
        }
        Calendar time=Calendar.getInstance(); 
        DateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        Date date=null;
        try {
            date = sdf.parse(tmp_date);
        } catch (Exception e) {
//            log.debug("公共方法调用getWeekNoInYearByDate，日期转换失败");
            return 0;
        }
        time.setTime(date);
        return time.get(Calendar.WEEK_OF_YEAR);
    } 
    
    
    /**
     * 以今天为基础计算，y,m,d对应计算的年月日的跨度
     * */
    public static String computeByToday(int y,int m,int d,String type){
        String t=type;
        if(StringUtil.isEmpty(t)){
            t="yyyy-MM-dd";
        }
        Calendar time=Calendar.getInstance(); 
        if(y!=0){
            time.add(Calendar.YEAR, y);
        }
        if(m!=0){
            time.add(Calendar.MONTH, m);
        }
        if(d!=0){
            time.add(Calendar.DATE, d);
        }
        return new SimpleDateFormat(t).format(time.getTime());
    }
    
    /**
     * 以date_id为基础计算，y,m,d对应计算的年月日的跨度
     * */
    public static String computeByDay(int y,int m,int d,String type,String date_id){
        String t=type;
        if(StringUtil.isEmpty(t)){
            t="yyyy-MM-dd";
        }
        Calendar time=strToCalendar(date_id,t);
        if(y!=0){
            time.add(Calendar.YEAR, y);
        }
        if(m!=0){
            time.add(Calendar.MONTH, m);
        }
        if(d!=0){
            time.add(Calendar.DATE, d);
        }
        return new SimpleDateFormat(t).format(time.getTime());
    }
    
    /**
     * 返回日期全串 2014-07-09/星期三/第28周 格式
     * @param day
     * @return
     */
    public static String formatFullStr(Day day){
    	String fullStr = "";
    	if(day == null){
    		return fullStr;
    	}
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    	String dateStr = df.format(day.getDateId());
    	String weekStr = (day.getWeekId() + "").substring(4, 6);
    	fullStr = dateStr + "/" + day.getDayOfWeek() + "/" + "第" + weekStr + "周";
    	return fullStr;
    }
    
    /**
     * 以date_id为基础计算，y,m,d对应计算的年月日的跨度，返回日期全串 2014-07-09/星期三/第28周 格式
     * */
    public static String computeByDayToFullStr(int y,int m,int d,String type,String date_id){
        String t=type;
        if(StringUtil.isEmpty(t)){
            t="yyyy-MM-dd";
        }
        Calendar time=strToCalendar(date_id,t);
        if(y!=0){
            time.add(Calendar.YEAR, y);
        }
        if(m!=0){
            time.add(Calendar.MONTH, m);
        }
        if(d!=0){
            time.add(Calendar.DATE, d);
        }
        String datestr=new SimpleDateFormat(t).format(time.getTime());
        String weekstr=getWeekDay(time);
        String weeknum="第"+time.get(Calendar.WEEK_OF_YEAR)+"周";
        return datestr+"/"+weekstr+"/"+weeknum;
    }
    
    /**
     * 返回星期
     * */
    public static String getWeekDay(Calendar c){
    	   if(c == null){
    	    return "星期一";
    	   }
    	  
    	   if(Calendar.MONDAY == c.get(Calendar.DAY_OF_WEEK)){
    	    return "星期一";
    	   }
    	   if(Calendar.TUESDAY == c.get(Calendar.DAY_OF_WEEK)){
    	    return "星期二";
    	   }
    	   if(Calendar.WEDNESDAY == c.get(Calendar.DAY_OF_WEEK)){
    	    return "星期三";
    	   }
    	   if(Calendar.THURSDAY == c.get(Calendar.DAY_OF_WEEK)){
    	    return "星期四";
    	   }
    	   if(Calendar.FRIDAY == c.get(Calendar.DAY_OF_WEEK)){
    	    return "星期五";
    	   }
    	   if(Calendar.SATURDAY == c.get(Calendar.DAY_OF_WEEK)){
    	    return "星期六";
    	   }
    	   if(Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK)){
    	    return "星期日";
    	   }
    	  
    	   return "星期一";
    	}
    
    /**
     * 得到当前天往前30天的数据
     * @return
     */
    public static String getDayScope(String nowDay){
    	try {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    		Date date = sdf.parse(nowDay);
    		
    		Calendar time=Calendar.getInstance(); 
    		time.setTime(date);
    		time.add(Calendar.DAY_OF_MONTH, -29);
    		return sdf.format(time.getTime());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    
    /**
     * 得到当前月往前12个月的数据
     * @return
     */
    public static String getMonthScope(String nowMonth){
    	try {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
    		Date date = sdf.parse(nowMonth);
    		
    		Calendar time=Calendar.getInstance(); 
    		time.setTime(date);
    		time.add(Calendar.MONTH, -11);
    		return sdf.format(time.getTime());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * 得到当前周向前12个周的数据
     * @param nowWeek
     * @return
     */
    public static String getWeekScope(String nowWeek){
    	try {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    		Date date = sdf.parse(nowWeek);
    		
    		Calendar time=Calendar.getInstance(); 
    		time.setTime(date);
    		time.add(Calendar.WEEK_OF_YEAR, -11);
    		return StringUtil.ifNull(time.get(Calendar.WEEK_OF_YEAR));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    /**
     * 字符串转换日期
     * */
    public static Date castStrToDate(String str){
    	DateFormat dd=new SimpleDateFormat("yyyy-MM-dd");
		Date date=null;
		try {
			if(str==null){
				str=toDateStr();
			}
			date = dd.parse(str);
		} catch (Exception e) {
//			log.debug("公共方法调用castStrToDate，日期转换失败:"+e.getMessage());
		}
		return date;
    }
    
    /**
	 * 取得当前日期所在周的第一天
	 * 
	 * @param date
	 * @return
	 */
	public static Date getFirstDayOfWeek(Date date) {
		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTime(date);
		c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek()); // Monday
		return c.getTime();
	}

	/**
	 * 取得当前日期所在周的最后一天
	 * 
	 * @param date
	 * @return
	 */
	public static Date getLastDayOfWeek(Date date) {
		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTime(date);
		c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek() + 6); // Sunday
		return c.getTime();
	}
	
	  /**
		 * 取得上周的第一天
		 * 
		 * @param date
		 * @return
		 */
		public static Date getFirstDayOfLastWeek(Date date,int day) {
			Calendar c = new GregorianCalendar();
			c.setFirstDayOfWeek(Calendar.MONDAY);
			c.setTime(date);
			c.add(Calendar.WEEK_OF_MONTH, day);
			c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek()); // Monday
			return c.getTime();
		}

		/**
		 * 取得上周的最后一天
		 * 
		 * @param date
		 * @return
		 */
		public static Date getLastDayOfLastWeek(Date date,int day) {
			Calendar c = new GregorianCalendar();
			c.setFirstDayOfWeek(Calendar.MONDAY);
			c.setTime(date);
			c.add(Calendar.WEEK_OF_MONTH, day);
			c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek() + 6); // Sunday
			return c.getTime();
		}
	
    /**
     * 获取到指定天所属的周的第一天和最后一天
     * */
    public static Date[] getWeekDayLimit(String datestr,String fmt){
    	if(StringUtil.isEmpty(fmt))
    		fmt="yyyy-MM-dd";
        if(checkDatestrFormatstr(datestr,fmt)){
            return null;
        }
        DateFormat sdf=new SimpleDateFormat(fmt);
        Date date=null;
        try {
            date = sdf.parse(datestr);
        } catch (Exception e) {
//            log.debug("公共方法调用getWeekInYear，日期转换失败");
            return null;
        }
        Date[] end=new Date[2];
        end[0]=getFirstDayOfWeek(date);
        end[1]=getLastDayOfWeek(date);
        return end;
    }

    /**
     * 获取到指定天所属的周的第一天和最后一天
     * */
    public static String[] getWeekDayLimitStr(String datestr,String fmt){
    	Date[] end=getWeekDayLimit(datestr,fmt);
    	if(end==null) return null;
    	String[] end_=new String[2];
    	end_[0]=toDateStr(end[0],fmt);
    	end_[1]=toDateStr(end[1],fmt);
//    	System.out.println(end_[0]+"===="+end_[1]);
    	return end_;
    }
    
    /**
     * 获取今日剩余的秒数
     * */
    public static int getNowDayResidueSecond(){
    	Date d1=new Date();
    	Date d2=castStrToDate(computeByToday(0,0,1,null));
    	//System.out.println(d1.getTime());
    	//System.out.println(d2.getTime());
    	int end=Integer.parseInt(StringUtil.ifNull((d2.getTime()-d1.getTime())/1000));
    	//System.out.println(end);
    	return end;
    } 
    /**
     * 两个时间间隔 天数
     * */
    public static int getDaySub(String beginDateStr,String endDateStr){
        int day=0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate;
        Date endDate;
        try
        {
            beginDate = format.parse(beginDateStr);
            endDate= format.parse(endDateStr);    
            day=(int) ((endDate.getTime()-beginDate.getTime())/(24*60*60*1000));  
        } catch (ParseException e)
        {
            // TODO 自动生成 catch 块
            e.printStackTrace();
        }   
        return day;
    }
    
    /**
     * 日期 加上天数  获取新的时间
     * @throws ParseException 
     * */
    public static String addDaySub(String beginDateStr,int day) throws ParseException{
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    	long dif = df.parse(beginDateStr).getTime()+86400*1000*day;
    	Date date=new Date(); 
        date.setTime(dif);
        //System.out.println("减少一天之后：" + df.format(date));
        return df.format(date).toString();
    }

    public static String getBeforYear(String dayTime) throws ParseException{
    	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");  
		Date todayDate = sdf.parse(dayTime);
		long afterTime=(todayDate.getTime()/1000)-60*60*24*365; 
		todayDate.setTime(afterTime*1000);   
		String beforYear=sdf.format(todayDate); 
    	return beforYear;
    }
    
    /**
     * 获取昨天日期
     * @return
     */
    public static Date getYesterday(){
    	return getDay(-1);
    }
    
    /**
     * 获取今天
     * @return
     */
    public static Date getToday(){
    	return getDay(0);
    }
    
    /**
     * 根据偏移量获取日期
     * @param offset 偏移天数
     * @return
     */
    public static Date getDay(int offset){
    	Calendar c = Calendar.getInstance();
    	c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + offset);
    	return c.getTime();
    }
    
    public static Date getDay(Date startDay, int offset){
    	Calendar c = Calendar.getInstance();
    	c.setTime(startDay);
    	c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + offset);
    	return c.getTime();
    }
    
    public static String getWorkDay(int offset){
    	Calendar c = Calendar.getInstance();
    	c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + offset);
    	return getWorkDay(c.getTime(), offset);
    }
    
    public static String getWorkDay(Date date, int offset){
    	BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
    	String sqlId = "util.queryWorkDay";
    	Map<String, String> params = new HashMap<String, String>();
    	Calendar c = Calendar.getInstance();
    	c.setTime(date);
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
    	params.put("queryDate", sdf.format(date));
    	if(SC.isLocal){
    		return "";
    	}
    	Map map = (Map) dao.queryObject(sqlId, params, DataSourceType.Default);
    	String workDateStr = (String) map.get("WORKDATESTR");
    	return workDateStr;
    }
    
    /**
     * 获取当前周的星期一
     * @return
     */
    public static Date getCurWeekMonday(){
    	Calendar c = Calendar.getInstance();
    	int dayOfWeek = getDayOfWeek(c);
    	c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) - (dayOfWeek - 1));
    	return c.getTime();
    }
    
    /**
     * 获取当前周
     * @return
     */
    public static Week getCurrentWeek(){
    	//WeekService weekService = (WeekService) SpringServiceLocator.getInstance().getService("weekService");
    	Date day = getDay(-7);
    	//Week week = weekService.getWeekByDay(ToDateStr(day));
    	Week week = getWeek(day);
    	return week;
    }
    
    /**
     * 获取当前周Id
     * @return
     */
    public static Integer getCurrentWeekId(){
    	Week week = getCurrentWeek();
    	Integer weekId = -99;
    	if(week != null){
    		weekId = week.getWeekId();
    	}
    	return weekId;
    }
    
    /**
     * 通过日期获取所属周
     * @param date:格式yyyy-mm-dd
     * @return
     */
    public static Week getWeek(String date){
    	BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
    	String sqlId = "common.queryWeekByDay";
    	Map<String, String> params = new HashMap<String, String>();
    	params.put("dateStr", date);
    	Week week = (Week) dao.queryObject(sqlId, params, DataSourceType.Default);
    	return week;
    }
    
    /**
     * 通过日期获取所属周（查询日期维度表)
     * @param date
     * @return
     */
    public static Week getWeek(Date date){
    	Week week = new Week();
    	if(date == null) return week;
		String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);
		return getWeek(dateStr);
    }
    
    /**
     * 获取给定年份的周列表
     * @return
     */
    public static List<Week> getWeeks(String year){
    	BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
    	String sqlId = "common.queryWeekByYear";
    	Map params = new HashMap();
    	params.put("year", year + "");
    	List<Week> weeks = (List<Week>) dao.queryObjectList(sqlId, params, DataSourceType.Default);
    	return weeks;
    }
    
    /**
     * 通过日期获取所属周<br/>
     * 计算规则：周不跨年，第一天属于当前年的第一周，每年最后一天属于当前年的最后一周的最后一天
     * @param date
     * @return 周
     */
    public static Week getWeek3(Date date){
    	Week week = new Week();
    	Calendar c = Calendar.getInstance();
    	c.setTime(date);
    	int year = c.get(Calendar.YEAR);// 所属年份
    	
    	Calendar yearStartDay = Calendar.getInstance();
    	yearStartDay.set(year, 0, 1);
    	
    	Calendar yearEndDay = Calendar.getInstance();
    	yearEndDay.set(year, 11, 31);
    	
    	//int month = c.get(Calendar.MONTH) + 1;// 所属月份
    	int dayOfWeek = getDayOfWeek(c); // 星期几
    	
    	Calendar weekStartDay = Calendar.getInstance();
    	weekStartDay.setTime(date);
    	weekStartDay.set(Calendar.DAY_OF_MONTH, weekStartDay.get(Calendar.DAY_OF_MONTH)  + (dayOfWeek - 1) * -1);
    	if(weekStartDay.getTimeInMillis() < yearStartDay.getTimeInMillis()){ // 起始日小于1月1号，则为元旦
    		weekStartDay = yearStartDay;
    	}
    	week.setStartDate(new java.sql.Date(weekStartDay.getTimeInMillis()));
    	
    	Calendar weekEndDay = Calendar.getInstance();
    	weekEndDay.setTime(date);
    	weekEndDay.set(Calendar.DAY_OF_MONTH, weekEndDay.get(Calendar.DAY_OF_MONTH)  +  (7 - dayOfWeek));
    	
    	if(weekEndDay.getTimeInMillis() > yearEndDay.getTimeInMillis()){ // 结束日大于12/31，则为12/31
    		weekEndDay = yearEndDay;
    	}
    	week.setEndDate(new java.sql.Date(weekEndDay.getTimeInMillis()));
    	
    	String yearWeekId = "199901";// 年周id
    	
    	// 计算周id yyyyww
    	int dayOfYear = c.get(Calendar.DAY_OF_YEAR);
    	int weekNo = new Double(Math.ceil(dayOfYear / 7.0)).intValue();
    	yearWeekId = year + "" + (weekNo < 10 ? "0" + weekNo : weekNo);
    	
    	week.setWeekId(Integer.valueOf(yearWeekId));
    	week.setYearId(Integer.valueOf(yearWeekId.substring(0, 4)));
    	week.setWeekNo(Integer.valueOf(yearWeekId.substring(4, 6)));
    	
    	SimpleDateFormat df = new SimpleDateFormat("MM/dd");
    	week.setStartDateShortName(df.format(week.getStartDate()));
    	week.setEndDateShortName(df.format(week.getEndDate()));
    	
    	df = new SimpleDateFormat("yyyy/MM/dd");
    	week.setFullName(df.format(week.getStartDate()) + "~" + df.format(week.getEndDate()));
    	
    	return week;
    }
    
    /**
     * 通过日期获取所属周<br/>
     * 计算规则：每年的第一个星期开始计算为当前的第一周
     * @param date
     * @return 周
     */
    public static Week getWeek2(Date date){
    	Week week = new Week();
    	Calendar c = Calendar.getInstance();
    	c.setTime(date);
    	int year = c.get(Calendar.YEAR);// 所属年份
    	//int month = c.get(Calendar.MONTH) + 1;// 所属月份
    	int dayOfWeek = getDayOfWeek(c); // 星期几
    	
    	Calendar weekStartDay = Calendar.getInstance();
    	weekStartDay.setTime(date);
    	weekStartDay.set(Calendar.DAY_OF_MONTH, weekStartDay.get(Calendar.DAY_OF_MONTH)  + (dayOfWeek - 1) * -1);
    	week.setStartDate(new java.sql.Date(weekStartDay.getTimeInMillis()));
    	
    	Calendar weekEndDay = Calendar.getInstance();
    	weekEndDay.setTime(date);
    	weekEndDay.set(Calendar.DAY_OF_MONTH, weekEndDay.get(Calendar.DAY_OF_MONTH)  +  (7 - dayOfWeek));
    	week.setEndDate(new java.sql.Date(weekEndDay.getTimeInMillis()));
    	
    	String yearWeekId = "199901";// 年周id
    	
    	// 当前日期偏移到周日，如果周日日期跨年，则为下一年的第一周
    	int weekEndYear = weekEndDay.get(Calendar.YEAR);
    	if(year != weekEndYear){
    		yearWeekId = weekEndYear + "01";
    	}else{ // 不跨年，则从年初开始计算
    		// 所属周在本年第N天
    		int dayOfYear = c.get(Calendar.DAY_OF_YEAR);
    		// 年初
    		c.set(Calendar.DAY_OF_YEAR, 1);
    		int firstDayOfWeek = getDayOfWeek(c);
    		// 指定日期所属周日的偏移量
    		int offsetDays = (dayOfYear + (7 - dayOfWeek)) - (7 - firstDayOfWeek + 1); // 相对第一周周末的偏移天数
    		int weekNo = (offsetDays / 7) + 1;
    		if(weekNo < 10){
    			yearWeekId = year + "0" + weekNo;
    		}else{
    			yearWeekId = year + "" + weekNo;
    		}
    	}
    	//System.out.println(df.format(c.getTime()));
    	week.setWeekId(Integer.valueOf(yearWeekId));
    	week.setYearId(Integer.valueOf(yearWeekId.substring(0, 4)));
    	week.setWeekNo(Integer.valueOf(yearWeekId.substring(4, 6)));
    	
    	SimpleDateFormat df = new SimpleDateFormat("MM/dd");
    	week.setStartDateShortName(df.format(week.getStartDate()));
    	week.setEndDateShortName(df.format(week.getEndDate()));
    	
//    	System.out.println(week);
    	
    	return week;
    }
    
    /**
     * 获取星期N
     * @param date
     * @return
     */
    public static int getDayOfWeek(Date date){
    	Calendar c = Calendar.getInstance();
    	c.setTime(date);
    	return getDayOfWeek(c);
    }
    
    /**
     * 从1~7：星期一~星期天
     * @param c
     * @return
     */
    public static int getDayOfWeek(Calendar c){
    	int dayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
    	if(dayOfWeek == 0){// 周日
    		dayOfWeek = 7;
    	}
    	return dayOfWeek;
    }
    
    /**
     * 
     * @param weekId
     * @param offset
     * @return
     */
    public static int addWeek(int weekId, int offset){
    	int weekNo = weekId % 100;
    	int year = weekId / 100;
    	Calendar c = Calendar.getInstance();
    	c.set(Calendar.YEAR, year);
    	
    	// 计算当前年的第一天是星期N
    	Calendar firstDay = Calendar.getInstance();
    	firstDay.set(Calendar.YEAR, year);
    	firstDay.set(Calendar.DAY_OF_YEAR, 1);
    	int dayOfWeek = getDayOfWeek(firstDay);
    	
    	// 偏移天数
    	int dayOfYear = (weekNo - 1) * 7 + dayOfWeek + offset * 7;
    	c.set(Calendar.DAY_OF_YEAR, dayOfYear);
    	
    	Week week = getWeek(c.getTime());
    	
//    	System.out.println(week);
    	
    	return week.getWeekId();
    }
    
    /**
     * 获取季度
     * @param date
     * @return
     */
    public static int getSeason(Date date) {  
    	  
        int season = 0;  
  
        Calendar c = Calendar.getInstance();  
        c.setTime(date);  
        int month = c.get(Calendar.MONTH);  
        switch (month) {  
        case Calendar.JANUARY:  
        case Calendar.FEBRUARY:  
        case Calendar.MARCH:  
            season = 1;  
            break;  
        case Calendar.APRIL:  
        case Calendar.MAY:  
        case Calendar.JUNE:  
            season = 2;  
            break;  
        case Calendar.JULY:  
        case Calendar.AUGUST:  
        case Calendar.SEPTEMBER:  
            season = 3;  
            break;  
        case Calendar.OCTOBER:  
        case Calendar.NOVEMBER:  
        case Calendar.DECEMBER:  
            season = 4;  
            break;  
        default:  
            break;  
        }  
        return season;  
    }  
    
    /**
     * 获取年份
     * @param offset
     * @return
     */
    public static Integer getYear(int offset){
    	Calendar c = Calendar.getInstance();
    	int year = c.get(Calendar.YEAR);
    	year = year + offset;
    	return year;
    }
    
    public static Integer getYear(Date date){
    	Calendar c = Calendar.getInstance();
    	c.setTime(date);
    	int year = c.get(Calendar.YEAR);
    	return year;
    }
    
    /**
     * 开始周 加上周  获取新的周
     * */
   /* public static int addWeeK(int beginWeek,int weekDay) {
    	int weekNum= beginWeek%100;
    	if(weekDay>=0){
    		if(weekNum+weekDay>52){
    			beginWeek=beginWeek+100-52+weekDay;
    		}else{
    			beginWeek=beginWeek+weekDay;
    		}
    	}else{
    		if(weekNum<weekDay*-1){
    			beginWeek=beginWeek+52-100+weekDay;
			}else{
    			beginWeek=beginWeek+weekDay;
    		}
    	}
        return beginWeek;
    }*/

    /**
     * 获取范围列表
     * @param start  yyyy-MM-dd、yyyy-MM、yyyy
     * @param end
     * @param dateType
     * @return
     */
    public static List<String> getRangeList(String start, String end, DateType dateType){
        List<String> rangeList = new ArrayList<>();
        String swap = "";
        if(start.compareTo(end) > 0){
            swap = start;
            start = end;
            end = swap;
        }

        if(dateType == DateType.Week){
            Date startDate = WeekDateUtil.getWeekFirstDay(start);
            Date endDate = WeekDateUtil.getWeekLastDay(end);
            List<String> weekIdList = new ArrayList<>();
            while(startDate.getTime() <= endDate.getTime()){
                String weekId = WeekDateUtil.getWeekId(startDate);
                if(!weekIdList.contains(weekId)){
                    weekIdList.add(weekId);
                }
                startDate = cn.hutool.core.date.DateUtil.offsetDay(startDate, 1);
            }

            for(String weekId : weekIdList){
               // String weekName = WeekDateUtil.getWeekFullnameById(weekId);
                // rangeList.add(weekName);
                rangeList.add(weekId);
            }

            return rangeList;
        }

        if(dateType == DateType.Quarter) {
            rangeList = QuarterDateUtil.getQuarterList(start,end);
            return rangeList;
        }

        String dateFormat = "";
        DateField dateField = null;
        switch (dateType) {
            case Day:
                dateFormat = "yyyy-MM-dd";
                dateField = DateField.DAY_OF_YEAR;
                break;
            case Month:
                dateFormat = "yyyyMM";
                dateField = DateField.MONTH;
                break;
            case Year:
                dateFormat = "yyyy";
                dateField = DateField.YEAR;
                break;
        }

        DateTime startTime = cn.hutool.core.date.DateUtil.parse(start, dateFormat);
        DateTime endTime = cn.hutool.core.date.DateUtil.parse(end, dateFormat);

        List<DateTime> dateTimes = cn.hutool.core.date.DateUtil.rangeToList(startTime, endTime, dateField);

        for(DateTime dateTime : dateTimes) {
            rangeList.add(cn.hutool.core.date.DateUtil.format(dateTime, dateFormat));
        }

        return rangeList;
    }

    /**
     * 月份数据集合是否含有当月
     * @param values
     * @return
     */
    public static Boolean isContainCurrentMonth(List<String> values) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        String currentMonth = sdf.format(new Date());

        long currentMonthCount = values.stream().filter(v -> currentMonth.equalsIgnoreCase(v)).count();
        if (currentMonthCount > 0) {
            return true;
        }

        return false;
    }

    /**
     * 获取最大日期
     * @param date1
     * @param date2
     * @return
     */
    public static String getMaxDate(String date1,String  date2) {
        if (StrUtil.isEmpty(date1)) {
            return date2;
        }

        if (StrUtil.isEmpty(date2)) {
            return date1;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dt1 = LocalDateTime.parse(date1, formatter);
            LocalDateTime dt2 = LocalDateTime.parse(date2, formatter);

            int result = dt1.compareTo(dt2);
            // 负数表示dt1小，0表示相等，正数表示dt1大
            if (result < 0) {
                return date2;
            }

        } catch (DateTimeParseException e) {
            System.out.println("时间格式解析错误: " + e.getMessage());
        }

        return date1;
    }


    /**
     * 获取需要排序的日期
     * @param excludeDate
     * @param day
     * @return
     */
    public static List<String> getExcludeMonthDateRange(Date excludeDate,int day) {

        List<String> excludeMonthDateRange = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(excludeDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<LocalDate> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (date.getDayOfMonth() > day) {
                result.add(date);
            }
        }

        if (CollUtil.isNotEmpty(result)) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(fmt2);
            excludeMonthDateRange.add(dateFormat.format(result.get(0)));
            excludeMonthDateRange.add(dateFormat.format(result.get(result.size() - 1)));
        }

        return excludeMonthDateRange;
    }

    /**
     * 转换为农历日期
     * @param dateStr，格式yyyy-MM-dd
     * @return
     */
    public static String toLunar(String dateStr) {
        Lunar lunar = new Lunar(cn.hutool.core.date.DateUtil.parseDate(dateStr));
        String lunarStr = lunar.getLunarYear() + "年" + lunar.getLunarMonthString() + "月" + lunar.getLunarDayString();
        return lunarStr;
    }

    private static final char[] TRADITIONAL_NUMBERS = {'零', '壹', '貳', '參', '肆', '伍', '陸', '柒', '捌', '玖'};

    //农历格式：繁体年份+数字月份+农历日，如贰肆11廿四；
    public static String toLunarDate(String dateStr) {

        if(StrUtil.isEmpty(dateStr)) {
            return dateStr;
        }

        Lunar lunar = new Lunar(cn.hutool.core.date.DateUtil.parseDate(dateStr));
        String lunarYear = (lunar.getLunarYear()+"").substring(2,4);
        String traditionalName = "";
        for (int i = 0; i < lunarYear.length(); i++){
            int num = Character.getNumericValue(lunarYear.charAt(i));
            traditionalName += TRADITIONAL_NUMBERS[num];
        }

        String lunarStr = traditionalName + (lunar.isLeap() ? "闰" : "") + lunar.getLunarMonth() + lunar.getLunarDayString();
        return lunarStr;
    }

    /**
     * 将日期格式为农历
     * @param value
     * @param dataType
     * @param formatStr
     * @return
     */
    public static String formatDateToLunar(Object value, String dataType, String formatStr, Map<String, CompareDateMapping> compareDateMappings){
        if(value == null){
            return "";
        }

        // 基准日期
        List<String> descList = new ArrayList<>();
        descList.add(String.format("%s(农历%s)",value,DateUtil.toLunar(value.toString())));

        // 年同比日期
        List<AnalysisCalcMode> calcModes = new ArrayList<>();
        calcModes.add(AnalysisCalcMode.TB_LN_YEAR);
        calcModes.add(AnalysisCalcMode.TB_LN_YEAR_2);
        calcModes.add(AnalysisCalcMode.TB_LN_YEAR_3);

        CompareDateMapping mapping = compareDateMappings.get(value.toString());
        if(mapping != null){
            for(AnalysisCalcMode calcMode : calcModes){
                String compareDate = mapping.getMappingDates().get(calcMode.getCode());
                if(BIUtil.isEmpty(compareDate)){
                    continue;
                }
                String calcModeDesc = calcMode.getDesc();
                String compareDateDesc = String.format("%s%s", calcModeDesc, String.format("%s(农历%s)",compareDate,DateUtil.toLunar(compareDate.toString())));
                descList.add(compareDateDesc);
            }
        }

        String lunarString = BIUtil.listToStr(descList, " - ");
        return lunarString;
    }

    /**
     * 构建对比日期列表
     */
    public static Map<String, LinkedHashMap<String,String>> buildCompareDateMapping(QueryConfigure config,Map<String,String> compareTitleMapping){
        Map<String, LinkedHashMap<String,String>> mappings = new HashMap<>();
        // 若显示农历时处理
        if (!Enabled.value(config.getSettings().getShowDateRemark())) {
            return mappings;
        }

        //自然日历校验日期粒度和是否汇总
        if(!config.getSettings().isBusinessCalendar()) {
            //日粒度才处理
            if (DateGranularity.DAY != DateGranularity.get(config.getSettings().getDateGranularity())) {
                return mappings;
            }

            //汇总不处理
            if (config.isAggQuery()) {
                return mappings;
            }
        }

        //获取去重后的分析项
        Set<String> keySet = new HashSet<>();
        List<AnalysisItemConfig> analysisItemConfigs = new ArrayList<>();
        compareTitleMapping.clear();
        for(QueryField f : config.getResult().getMeasures()) {
            if (!Enabled.value(f.getIsAnalysis())) {
                continue;
            }

            AnalysisItemConfig analysisItemConfig = f.getAnalysisConfig();
            String calcCode = analysisItemConfig.getCalcMode();
            AnalysisCalcMode calcMode = AnalysisCalcMode.get(calcCode);

            //非对比的分析类型不处理
            if (!calcMode.isCompare()) {
                continue;
            }

            String key = analysisItemConfig.getCalcMode();
            //特殊处理自定义对比，自定义对比需要添加序号
            if(calcMode == AnalysisCalcMode.CUSTOM_COMPARE){
                key = String.format("%s_%s", analysisItemConfig.getCalcMode(), analysisItemConfig.getCompareIndex());
                compareTitleMapping.put(key,analysisItemConfig.getCompareTitle());
            }
            if (keySet.contains(key)) {
                continue;
            }

            keySet.add(key);
            analysisItemConfigs.add(analysisItemConfig);
        }

        if(BIUtil.isEmpty(analysisItemConfigs)){
            return mappings;
        }

        //业务日历单独处理
        if(config.getSettings().isBusinessCalendar()){
            mappings =  buildBusinessCompareDateMapping(config,analysisItemConfigs);
            return mappings;
        }

        List<String> baseDateList = AnalysisUtil.getFilterDateList(config);
        if(BIUtil.isEmpty(baseDateList)){
            return mappings;
        }
        Collections.sort(baseDateList);

        Map<String, List<String>> calcCompareDateLists = new HashMap<>();
        for(AnalysisItemConfig analysisItemConfig : analysisItemConfigs) {
            AnalysisCalcMode calcMode = AnalysisCalcMode.get(analysisItemConfig.getCalcMode());
            List<String> compareRange = AnalysisUtil.getCompareDateRange(config, calcMode, analysisItemConfig.getCompareIndex());
            if(BIUtil.isEmpty(compareRange)){
                continue;
            }
            String startDate = compareRange.get(0);
            String endDate = compareRange.get(compareRange.size() - 1);
            List<String> compareDateList = DateUtil.getRangeList(startDate, endDate, DateType.Day);
            if(BIUtil.isEmpty(compareDateList)){
                continue;
            }
            Collections.sort(compareDateList);

            String key = analysisItemConfig.getCalcMode();
            if(calcMode == AnalysisCalcMode.CUSTOM_COMPARE){
                key = String.format("%s_%s", analysisItemConfig.getCalcMode(), analysisItemConfig.getCompareIndex());
            }
            calcCompareDateLists.put(key, compareDateList);
        }

        for(Integer i = 0; i < baseDateList.size(); i++){
            String baseDate = baseDateList.get(i);
            LinkedHashMap<String,String> compareDates = new LinkedHashMap<>();

            List<String> calcModeList = new ArrayList<>(calcCompareDateLists.keySet());
            //排序，为先农历再公历
            calcModeList.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    AnalysisCalcMode calcMode1 = AnalysisCalcMode.get(o1);
                    AnalysisCalcMode calcMode2 = AnalysisCalcMode.get(o2);
                    return calcMode1.getSortId().compareTo(calcMode2.getSortId());
                }
            });

            /**
             * 对比日期列表Map，key=dt value = 对比日期
             * 2026-01-03  值= key = calcMode value = 对比日期
             */
            for(String calcMode : calcModeList) {

                AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);
                //农历年周，月同比、年同比应该日期不对齐，按天计算
                if (analysisCalcMode.isTblnyw() ||
                        AnalysisCalcMode.TB_YEAR == analysisCalcMode ||
                        AnalysisCalcMode.TB_YEAR_2 == analysisCalcMode ||
                        AnalysisCalcMode.TB_YEAR_3 == analysisCalcMode ||
                        AnalysisCalcMode.TB_MONTH == analysisCalcMode) {
                    String compareDate = AnalysisUtil.getCompareDateByCalcMode(analysisCalcMode, baseDate);
                    compareDates.put(calcMode, compareDate);
                    continue;
                }

                List<String> compareDateList = calcCompareDateLists.get(calcMode);
                if (compareDateList.size() - 1 >= i) {
                    String compareDate = compareDateList.get(i);
                    if (BIConsts.MAX_END_DATE.equalsIgnoreCase(compareDate)) {
                        compareDate = "";
                    }
                    compareDates.put(calcMode, compareDate);
                } else {
                    compareDates.put(calcMode, "");
                }
            }

            mappings.put(baseDate, compareDates);
        }
        return mappings;
    }

    /**
     * 获取业务对比的日期映射
     * @return
     */
    public static Map<String, LinkedHashMap<String,String>> buildBusinessCompareDateMapping(QueryConfigure config,List<AnalysisItemConfig> analysisItemConfigs) {
        Map<String, LinkedHashMap<String, String>> mappings = new HashMap<>();
        List<String> promoIdentifierList = PromotionManager.getPromoIdentifierList(config);
        if (CollUtil.isEmpty(promoIdentifierList)) {
            return mappings;
        }

        List<String> calcModeList = analysisItemConfigs.stream().map(AnalysisItemConfig::getCalcMode).collect(Collectors.toList());
        //排序，为先农历再公历
        calcModeList.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                AnalysisCalcMode calcMode1 = AnalysisCalcMode.get(o1);
                AnalysisCalcMode calcMode2 = AnalysisCalcMode.get(o2);
                return calcMode1.getSortId().compareTo(calcMode2.getSortId());
            }
        });

        for (String promoIdentifier : promoIdentifierList) {
            PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(promoIdentifier);
            if (promotionCfg == null) {
                continue;
            }

            String startDate = promotionCfg.getStartDate();
            String endDate = promotionCfg.getEndDate();

            LinkedHashMap<String, String> startCompareDates = new LinkedHashMap<>();
            LinkedHashMap<String, String> EndCompareDates = new LinkedHashMap<>();

            for (String calcMode : calcModeList) {
                AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);

                if (analysisCalcMode.isPromoTb()) {
                    Integer promoYearOffset = promotionCfg.getPromoYear() + analysisCalcMode.getOffset();
                    String offsetPromoIdentifier = promoIdentifier.replace(promotionCfg.getPromoYear().toString(), promoYearOffset.toString());
                    PromotionCfg offsetPromotionCfg = PromotionManager.getPromotionCfg(offsetPromoIdentifier);
                    if (offsetPromotionCfg == null) {
                        startCompareDates.put(calcMode, "");
                        EndCompareDates.put(calcMode, "");
                    } else {
                        startCompareDates.put(calcMode, offsetPromotionCfg.getStartDate());
                        EndCompareDates.put(calcMode, offsetPromotionCfg.getEndDate());
                    }

                    continue;
                }

                String startCompareDate = AnalysisUtil.getCompareDateByCalcMode(analysisCalcMode, startDate);
                startCompareDates.put(calcMode, startCompareDate);

                String endCompareDate = AnalysisUtil.getCompareDateByCalcMode(analysisCalcMode, endDate);
                EndCompareDates.put(calcMode, endCompareDate);
            }

            mappings.put(startDate, startCompareDates);
            mappings.put(endDate, EndCompareDates);
        }

        return mappings;
    }

    public static String getWeekName(String date) {
        if (date == null || date.isEmpty()) {
            return "";
        }
        String res = DT_WEEK_MAP.get(date);
        if (res == null) {
            res = getWeekDay(date);
            if (!res.isEmpty()) {
                DT_WEEK_MAP.put(date, res);
            }
        }
        return res;
    }

    /**
     * 获取周名称
     * @param date
     */
    public static String getWeekNameWithSpace(String date) {
        String weekName = getWeekName(date);

        //如果周名称不为空，前面添加一个空格
        if (StrUtil.isNotEmpty(weekName)) {
            weekName = " " + weekName;
        }

        return weekName;
    }


    public static void main(String [] avgs){
        String date = "2024-01-02";
        toLunarDate("2024-01-02");
    }

    /**
     * 将 yyyy-MM-dd 字符串解析并返回指定的周几
     */
    public static String getWeekDay(String dateStr) {
        try {
            // LocalDate 默认支持 yyyy-MM-dd 格式
            LocalDate date = LocalDate.parse(dateStr);
            return date.getDayOfWeek().getDisplayName(TextStyle.NARROW_STANDALONE, Locale.CHINESE);
        } catch (Exception e) {
            return "";
        }
    }
}
