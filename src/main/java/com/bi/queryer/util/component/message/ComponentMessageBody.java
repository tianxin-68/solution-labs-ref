package com.bi.queryer.util.component.message;

import java.io.Serializable;

import com.bi.queryer.util.StringUtil;

public class ComponentMessageBody implements Serializable, Cloneable{
	
	private static final long serialVersionUID = 1L;

	protected String name = "";
	
	protected String title = "";
	
	protected String currentValue = "";
	
	protected String defaultValue = "";
	
	protected String valueType = ""; // 值类型 
	
	protected String type = "";// 消息体类型，用于区分不同消息体,由各个组件自己定义
	
	public ComponentMessageBody(String name, String defaultValue){
		this.name = name;
		this.defaultValue = defaultValue;
		this.currentValue = defaultValue;
	}
	
	public String getValue(){
		if(StringUtil.isEmpty(currentValue)){
			return defaultValue;
		}else{
			return currentValue;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || name == null) return false;
		ComponentMessageBody cmb = (ComponentMessageBody) obj;
		return name.equals(cmb.name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	public ComponentMessageBody clone(){
		ComponentMessageBody copy = new ComponentMessageBody(this.name, this.defaultValue);
		copy.name = this.name;
		copy.currentValue = this.currentValue;
		copy.defaultValue = this.defaultValue;
		copy.title = this.title;
		copy.type = this.type;
		copy.valueType = this.valueType;
		return copy;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(String currentValue) {
		this.currentValue = currentValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
