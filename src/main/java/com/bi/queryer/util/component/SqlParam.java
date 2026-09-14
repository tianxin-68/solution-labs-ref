package com.bi.queryer.util.component;

public class SqlParam {

	private String name;//参数名称
	private String type;//参数类型
	private String value;//参数默认值
	
	public SqlParam clone(){
		SqlParam copy = new SqlParam();
		copy.name = this.name;
		copy.type = this.type;
		copy.value = this.value;
		return copy;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	

}
