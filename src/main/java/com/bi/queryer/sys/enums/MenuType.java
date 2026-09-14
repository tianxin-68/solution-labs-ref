package com.bi.queryer.sys.enums;

/**
 * 菜单链接类型
 * @author contributor
 */
public enum MenuType {
	COMMON(1, "普通菜单"),
	RPT_JAVA(2,"Java报表"),
	RPT_TABLEAU(3,"TABLEAU报表"),
	SYSTEM(5, "系统菜单"),
	External(7, "系统外部菜单"),
	Function(9, "菜单功能项(增、删、改、查等)");
	
	private MenuType(int id, String desc) {
		this.id = id;
		this.desc = desc;
	}
	
	public static MenuType getType(int id){
		for(MenuType type : values()){
			if(id == type.getId()){
				return type;
			}
		}
		return COMMON;
	}
	
	public static MenuType getType(String code){
		for(MenuType type : values()){
			if(type.toString().equalsIgnoreCase(code)){
				return type;
			}
		}
		return COMMON;
	}
	
	private int id;
	
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
	
}
