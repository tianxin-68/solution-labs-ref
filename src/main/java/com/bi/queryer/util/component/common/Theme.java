package com.bi.queryer.util.component.common;

public enum Theme {
	Auto("自适应", "根据平台自动切换主题：移动为“黑色”、非移动为“常规”"),
	Black("黑色", "适用于移动端"),
	Default("常规", "适用于PC端");
	
	private String name ;
	private String desc;
	
	
	private Theme(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}
	
	public static Theme get(String str){
		for(Theme t : values()){
			if(t.toString().equalsIgnoreCase(str)){
				return t;
			}
		}
		return Black;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
}
