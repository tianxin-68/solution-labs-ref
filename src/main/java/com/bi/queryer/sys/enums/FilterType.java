package com.bi.queryer.sys.enums;

/**
 * 控件类型
 * @author contributor
 */
public enum FilterType {
	INPUT(1,"Text", "文本输入"),
	SELECT(2, "Select","下拉框"),
	DATE(3,"Date","时间控件");
	
	private FilterType(int id, String name ,String desc) {
		this.id = id;
		this.name = name;
		this.desc = desc;
	}
	
	public static FilterType getType(int id){
		for(FilterType type : values()){
			if(id == type.getId()){
				return type;
			}
		}
		return INPUT;
	}
	
	public static FilterType getType(String code){
		for(FilterType type : values()){
			if(type.toString().equalsIgnoreCase(code)){
				return type;
			}
			if(type.getName().equalsIgnoreCase(code)){
				return type;
			}
		}
		return null;
	}
	
	private int id;
	
	private String name;
	
	private String desc;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
