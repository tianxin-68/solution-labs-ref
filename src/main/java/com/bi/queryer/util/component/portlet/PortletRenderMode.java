package com.bi.queryer.util.component.portlet;

public enum PortletRenderMode {
	Designer("设计模式"),
	Publisher("发布模式");
	
	private String desc = "";
	
	private PortletRenderMode(String desc){
		this.desc = desc;
	}
	
	public static PortletRenderMode getMode(String modeStr){
		for(PortletRenderMode mode : values()){
			if(mode.toString().equalsIgnoreCase(modeStr)){
				return mode;
			}
		}
		return Designer;
	}
	
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
