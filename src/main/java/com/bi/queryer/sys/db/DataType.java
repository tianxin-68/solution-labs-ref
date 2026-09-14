package com.bi.queryer.sys.db;

public enum DataType {
	String,
	Double,
	Integer,
	BigInt,
	Date,
	Datetime;
	
	public static DataType getType(String strType){
		for(DataType type : values()){
			if(type.toString().equalsIgnoreCase(strType)){
				return type;
			}
		}
		return String;
	}

	public boolean isDate(){
		return this == Date || this == Datetime;
	}

	public boolean isDecimal(){
		return this == Double || this == Integer || this == BigInt;
	}

	public boolean isInteger(){
		return this == Integer || this == BigInt;
	}
}
