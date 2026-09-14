package com.bi.queryer.sys.enums;

public enum DateType {
	RTM("实时"),
	Day("日"),
	Week("周"),
	Month("月"),
	Quarter("季"),
	Year("年");
	private String desc;
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	private DateType(String desc) {
		this.desc = desc;
	}
	
	public static DateType get(String code) {
		for(DateType d : DateType.values()) {
			if(d.toString().equalsIgnoreCase(code)) {
				return d;
			}
		}
		return Day;
	}
}
