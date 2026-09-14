package com.bi.queryer.util.component.enums;

public enum SortType {
	Default("默认排序"),
	Asc("升序"),
	Desc("降序");
	
	private String desc;
	
	private SortType(String desc) {
		this.desc = desc;
	}
	
	public static SortType value(String str) {
		for(SortType t : SortType.values()) {
			if(t.toString().equalsIgnoreCase(str)) {
				return t;
			}
		}
		return Default;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
