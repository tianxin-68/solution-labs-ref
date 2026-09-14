package com.bi.queryer.sys.db;

import org.apache.commons.lang.StringUtils;

public class DataSourceContextHolder {   
	
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();   
  
    public static void setType(String dbType) {   
    	if(StringUtils.isEmpty(dbType)){
    		dbType = DataSourceType.Default.getId();
    	}
        contextHolder.set(dbType);   
    }   
  
    public static String getType() {   
        String type = (String) contextHolder.get();

        if(StringUtils.isEmpty(type)){
        	type = DataSourceType.Default.getId();
        }
        return type;
    }   
  
    public static void clearType() {   
        contextHolder.remove();   
    }   
}
