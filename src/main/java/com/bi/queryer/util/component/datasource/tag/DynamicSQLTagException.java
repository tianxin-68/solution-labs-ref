package com.bi.queryer.util.component.datasource.tag;

public class DynamicSQLTagException extends Exception{
	private static final long serialVersionUID = 1L;
	
	public DynamicSQLTagException(String msg){
		super(msg);
	}
	
	public DynamicSQLTagException(Throwable e){
		super(e);
	}
}
