package com.bi.queryer.sys.env;

/**
 * 环境变量<br/>
 * 环境变量表达式：${variable}或者${variable+num}，暂时只支持变量加减运算，变量名不区分大小写
 * @author contributor
 *
 */
public enum EnvVariable {
	CurrentTime("当前时间，value格式yyyy-mm-dd hh:mi:ss","yyyy-MM-dd HH:mm:ss", true),
	CurrentDay("当前日期，value格式yyyy-mm-dd，如2015-11-01", "yyyy-MM-dd", true),
	CurrentWeek("当前周，value格式yyyyww，如201511","", true),
	CurrentWeekName("当前周名称，value格式yyyy/MM/dd~yyyy/MM/dd，如2016/01/01~2016/01/03","", true),
	CurrentMonth("当前月，value格式yyyy-mm，如2015-01", "yyyy-MM", true),
	CurrentYear("当前年，value格式yyyy，如2015","yyyy", true),

	CurrentWeekFirstDay("当前周一,value格式yyyy-mm-dd，如2015-01-01", "yyyy-MM-dd", true),
	CurrentWeekLastDay("当前周日,value格式yyyy-mm-dd，如2015-01-01", "yyyy-MM-dd", true),

	CurrentMonthFirstDay("当前月首日，value格式yyyy-mm-dd，如2015-01-01", "yyyy-MM-dd", true),
	CurrentMonthLastDay("当前月未日，value格式yyyy-mm-dd，如2015-01-31", "yyyy-MM-dd", true),

	CurrentQuarterFirstDay("当前季首日，value格式yyyy-mm-dd，如2015-01-01", "yyyy-MM-dd", true),
	CurrentQuarterLastDay("当前季末日，value格式yyyy-mm-dd，如2015-03-31", "yyyy-MM-dd", true),

	CurrentYearFirstDay("当前年首日，value格式yyyy-mm-dd，如2015-01-01", "yyyy-MM-dd", true),
	CurrentYearLastDay("当前年末日，value格式yyyy-mm-dd，如2015-12-31", "yyyy-MM-dd", true),

	YesterdayMonthFirstDay("昨天所属月首日，value格式yyyymmdd，如20150101","yyyy-MM-dd", true),
	YesterdayMonthLastDay("昨天所属月末，value格式yyyymmdd，如20150101","yyyy-MM-dd", true),

	ContextPath("上下文路径，如/bi/","", false);

	private String desc = "";

	// 是否没参与计算
	private boolean canCalc = false;

	private EnvVariable(String desc, String format, boolean canCalc){
		this.desc = desc;
		this.canCalc = canCalc;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public boolean isCanCalc() {
		return canCalc;
	}

	public void setCanCalc(boolean canCalc) {
		this.canCalc = canCalc;
	}
}
