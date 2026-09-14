package com.bi.queryer.util.component.message;

public enum ComponentMessageScope {
	Component("组件级别"),
	Page("非本页面级别"),
	Native("App Native"),
	Appliaction("整体应用级别");
	
	private String desc;
	
	private ComponentMessageScope(String desc){
		this.desc = desc;
	}
	
	public static ComponentMessageScope getScope(String str){
		for(ComponentMessageScope scope : values()){
			if(scope.toString().equalsIgnoreCase(str)){
				return scope;
			}
		}
		return Component;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
