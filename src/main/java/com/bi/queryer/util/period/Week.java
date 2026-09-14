package com.bi.queryer.util.period;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.StringUtil;

public class Week {
	private Integer weekId;
	
	private Integer yearId;
	
	private Integer weekNo; // 周编号
	
	private Date startDate;
	
	private String startDateStr;
	
	private Date endDate;
	
	private String endDateStr;
	
	private String startDateShortName;
	
	private String endDateShortName;
	
	private String fullName = ""; // 全名 2016/05/16~2016/05/22
	
	private boolean isCurrent = false; // 是否是当前周
	
	public JSONObject toJSON(){
		JSONObject obj = new JSONObject();
		obj.put("yearWeekId", weekId);
		obj.put("yearOfWeek", yearId);
		obj.put("weekNo", weekNo);
		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
		obj.put("startDate", startDate == null ? startDateStr :  sf.format(startDate));
		obj.put("endDate",  endDate == null ? endDateStr : sf.format(endDate));
		obj.put("startDateShortName", startDateShortName);
		obj.put("endDateShortName", endDateShortName);
		obj.put("isCurrent", isCurrent());
		obj.put("fullName", getFullName());
		return obj;
	}
	
	@Override
	public String toString() {
		String str = "id:" + weekId + "\t fullName:" + fullName;
		return str;
	}

	public Integer getWeekId() {
		return weekId;
	}

	public void setWeekId(Integer weekId) {
		this.weekId = weekId;
	}

	public Integer getYearId() {
		return yearId;
	}

	public void setYearId(Integer yearId) {
		this.yearId = yearId;
	}

	public Integer getWeekNo() {
		return weekNo;
	}

	public void setWeekNo(Integer weekNo) {
		this.weekNo = weekNo;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getStartDateShortName() {
		return startDateShortName;
	}

	public void setStartDateShortName(String startDateShortName) {
		this.startDateShortName = startDateShortName;
	}

	public String getEndDateShortName() {
		return endDateShortName;
	}

	public void setEndDateShortName(String endDateShortName) {
		this.endDateShortName = endDateShortName;
	}

	public boolean isCurrent() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, yearId);
		c.set(Calendar.MONTH, c.get(Calendar.MONTH));
		c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) - 7);
		
		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar end = Calendar.getInstance();
		if(endDate != null) {
			end.setTime(endDate);
		}else if(!StringUtil.isEmpty(endDateStr)){
			try {
				end.setTime(sf.parse(endDateStr));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		end.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 1);
		long startTime = 0;
		if(startDate == null && !StringUtil.isEmpty(startDateStr)) {
			try {
				startTime = sf.parse(startDateStr).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}else {
			startTime = startDate.getTime();
		}
		
		isCurrent = (c.getTime().getTime() >= startTime  && c.getTime().getTime() <= end.getTime().getTime()); 
		
		//SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		//System.out.println(df.format(c.getTime()) +  ":" + yearOfWeek + "\t" + weekNo + "\t" + startDate + "\t" + endDate + "\t" + isCurrent);
		
		return isCurrent;
	}

	public void setCurrent(boolean isCurrent) {
		this.isCurrent = isCurrent;
	}
	
	public static void main(String[] args) {
		Calendar c = Calendar.getInstance();
		//c.set(Calendar.YEAR, c.get(Calendar.YEAR));
		c.set(Calendar.YEAR, 2015);
		c.set(Calendar.MONTH, c.get(Calendar.MONTH));
		c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) - 7);
		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
		System.out.println(c.get(Calendar.DAY_OF_MONTH) + ":" + c.get(Calendar.YEAR) + ":"+sf.format(c.getTime()));
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getStartDateStr() {
		return startDateStr;
	}

	public void setStartDateStr(String startDateStr) {
		this.startDateStr = startDateStr;
	}

	public String getEndDateStr() {
		return endDateStr;
	}

	public void setEndDateStr(String endDateStr) {
		this.endDateStr = endDateStr;
	}
}
