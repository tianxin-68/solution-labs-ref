package com.bi.queryer.util.component;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.UpperCaseMap;

/**
 * 组件实体类
 * @author contributor
 *
 */
public abstract class ComponentEntity {
	private String id;
	
	private String name;
	
	private String code;
	
	private String description;
	
	private String categoryId;
	
	private String categoryName;
	
	private String config;
	
	private String createdBy;
	
	private String createdTime;
	
	private String updatedBy;
	
	private String updatedTime;
	
	abstract public ComponentType getType();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}
	
	public UpperCaseMap toUpperCaseMap(){
		UpperCaseMap map = new UpperCaseMap();
		JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(this));
		map.putAll(json);
		return map;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
