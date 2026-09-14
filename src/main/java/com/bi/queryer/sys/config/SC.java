package com.bi.queryer.sys.config;

public class SC extends SystemConfig{
	public static String v(String key){
		return value(key);
	}
	
	public static String v(String key, String defaultValue){
		return value(key, defaultValue);
	}

	public static String getString(String key, String defaultValue){
		return value(key, defaultValue);
	}

	public static Integer getInteger(String key, Integer defaultValue){
		try {
			return Integer.valueOf(SC.v(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Long getLong(String key, Long defaultValue){
		try {
			return Long.valueOf(SC.v(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Double getDouble(String key, Double defaultValue){
		try {
			return Double.valueOf(SC.v(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Boolean getBoolean(String key, Boolean defaultValue){
		try {
			return Boolean.valueOf(SC.v(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return defaultValue;
		}
	}
}
