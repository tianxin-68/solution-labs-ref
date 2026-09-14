package com.bi.queryer.util.component.datasource;

public enum DataSetProviderType {
	defaultDataSetProvider,
	xmlDataSetProvider;
	
	public static DataSetProviderType getType(String strType){
		for(DataSetProviderType type : values()){
			if(type.toString().equalsIgnoreCase(strType)){
				return type;
			}
		}
		return defaultDataSetProvider;
	}
}
