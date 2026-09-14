package com.bi.queryer.sys.enums;

public enum ResourceType {
	Menu("菜单"),
	Function("功能"),
	WorkOrderAutoSetRole("工单审批自动赋权"),
	Unknow("未知资源");
	
	private String name;
	
	private ResourceType(String name) {
		this.name = name;
	}
	
	public static ResourceType value(String code) {
		for(ResourceType t : ResourceType.values()) {
			if(t.toString().equalsIgnoreCase(code)) {
				return t;
			}
		}
		return Unknow;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
