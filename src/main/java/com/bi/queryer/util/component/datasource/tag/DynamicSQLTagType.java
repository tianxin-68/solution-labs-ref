package com.bi.queryer.util.component.datasource.tag;

public enum DynamicSQLTagType {
	isEmpty,
	isNotEmpty,
	isEqual,
	isNotEqual;
	
	public static DynamicSQLTagType getType(String typeStr){
		for(DynamicSQLTagType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return null;
	}
}
