package com.bi.queryer.util.period;

import java.util.Date;

public class Day {
	
	private Date dateId;
	
	private String dayOfWeek;// 星期
	
	private Integer weekId;// 所属周id
	
	public static void main(String[] args) {
		Day day = new Day();
		String str = DateUtil.computeByDayToFullStr(0,0,0, "", "2014-11-16");
		System.out.println(str);
	}


	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public Integer getWeekId() {
		return weekId;
	}

	public void setWeekId(Integer weekId) {
		this.weekId = weekId;
	}


	public Date getDateId() {
		return dateId;
	}


	public void setDateId(Date dateId) {
		this.dateId = dateId;
	}
}
