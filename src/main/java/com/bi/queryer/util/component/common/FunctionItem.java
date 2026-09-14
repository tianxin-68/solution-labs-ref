package com.bi.queryer.util.component.common;

/**
 * 功能项目
 * @author contributor
 *
 */
public class FunctionItem {
	private String name;
	
	private String code;
	
	private String jsFunc;
	
	private String desc;
	
	private String enableExpression = "";
	
	public FunctionItem() {
		
	}
	
	public FunctionItem(String code, String name, String jsFunc) {
		this();
		this.code = code;
		this.name = name;
		this.jsFunc = jsFunc;
	}
	
	public FunctionItem(String code, String name, String jsFunc, String desc) {
		this(code, name, jsFunc);
		this.desc = desc;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(code == null || obj == null) {
			return false;
		}
		return code.equals(((FunctionItem)obj).code);
	}
	
	@Override
	public int hashCode() {
		return code == null ? super.hashCode() : code.hashCode();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getJsFunc() {
		return jsFunc;
	}

	public void setJsFunc(String jsFunc) {
		this.jsFunc = jsFunc;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getEnableExpression() {
		return enableExpression;
	}

	public void setEnableExpression(String enableExpression) {
		this.enableExpression = enableExpression;
	}

}
