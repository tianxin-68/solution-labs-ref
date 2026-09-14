package com.bi.queryer.sys.enums;

/**
 * 角色类型枚举
 * @author contributor
 *
 */
public enum RoleType {	

	DEFAULT(0, "默认角色"),
	
	SYSTEM(2, "系统角色"),
	
	SSD(3,"自助取数"),

	APPLY(6,"数据工作台"),

	Sensitive(7, "敏感角色");
	
	private RoleType(int id, String desc) {
		this.id = id;
		this.desc = desc;
	}
	
	public static RoleType getType(int id){
		for(RoleType type : values()){
			if(id == type.getId()){
				return type;
			}
		}
		return SYSTEM;
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
