package com.bi.queryer.sys.cache;

public enum CacheType {
	User,
	Menu,
	Authority,
	Common,
	API;

	public static CacheType get(String typeStr){
		for(CacheType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return User;
	}
}
