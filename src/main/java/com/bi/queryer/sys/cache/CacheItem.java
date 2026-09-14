package com.bi.queryer.sys.cache;

public class CacheItem {
	
	private String type;

	private String key;
	
	private String value;
	
	private String remark;
	
	public CacheItem() {
		
	}
	
	public CacheItem(String type, String key, String value, String remark) {
		this.type = type;
		this.key = key;
		this.value = value;
		this.remark = remark;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
