package com.bi.queryer.sys.enums;

/**
 * 系统模块类型
 * @author contributor
 *
 */
public enum ModuleType {
	Entity("系统实体"),
	Unknow("未知");
	
	private String desc;
	
	private ModuleType(String desc) {
		this.desc = desc;
	}
	
	public static ModuleType get(String code) {
		for(ModuleType t : ModuleType.values()) {
			if(t.toString().equalsIgnoreCase(code)) {
				return t;
			}
		}
		return ModuleType.Unknow;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
