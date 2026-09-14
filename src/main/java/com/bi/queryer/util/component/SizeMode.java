package com.bi.queryer.util.component;

public enum SizeMode {
	Auto("自适应"),
	Percent("百分比"),
	Fixed("固定值");
	
	private String desc = "";
	
	private SizeMode(String desc){
		this.desc = desc;
	}
	
	public static SizeMode getMode(String typeStr){
		for(SizeMode type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return Auto;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
