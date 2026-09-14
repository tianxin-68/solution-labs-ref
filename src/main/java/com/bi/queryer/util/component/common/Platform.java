package com.bi.queryer.util.component.common;


public enum Platform {
	PC,
	H5,
	IOS,
	Android,
	LED;
	
	public static Platform get(String typeStr){
		for(Platform type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return PC;
	}
}
