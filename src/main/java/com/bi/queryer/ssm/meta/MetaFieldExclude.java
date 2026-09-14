package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.mgr.exportImport.SSDExcel;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 字段互斥元信息:按照code互斥
 * @author contributor
 *
 */
public class MetaFieldExclude implements JSONSerializable {

	@SSDExcel(order = 1)
	private String code;

	@SSDExcel(order = 2)
	private String name;

	@SSDExcel(order = 3)
	private String excludeCode;


	@SSDExcel(order = 4)
	private String excludeName;

	@SSDExcel(order = 5)
	private String categoryId;

	@SSDExcel(order = 6)
	private String excludeCategoryId;

	@SSDExcel(order = 7)
	private String excludeDesc;

	private String categoryName;

	private String excludeCategoryName;



	//互斥类型
	private String type;

	private List<MetaFieldExclude> fieldExcludeList;

	@SSDExcel(order = 8,trueOrFalse = true)
	private Integer isActive;
	private String createdTime;
	private String createdBy;
	private String updatedTime;
	private String updatedBy;
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(code == null || excludeCode == null) return false;
		MetaFieldExclude m = (MetaFieldExclude) obj;
		return code.equals(m.code) && excludeCode.equals(m.excludeCode);
	}
	
	@Override
	public int hashCode() {
		return (code + excludeCode).hashCode();
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getExcludeCode() {
		return excludeCode;
	}

	public void setExcludeCode(String excludeCode) {
		this.excludeCode = excludeCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExcludeName() {
		return excludeName;
	}

	public void setExcludeName(String excludeName) {
		this.excludeName = excludeName;
	}

	public String getExcludeCategoryId() {
		return excludeCategoryId;
	}

	public void setExcludeCategoryId(String excludeCategoryId) {
		this.excludeCategoryId = excludeCategoryId;
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

	public String getExcludeCategoryName() {
		return excludeCategoryName;
	}

	public void setExcludeCategoryName(String excludeCategoryName) {
		this.excludeCategoryName = excludeCategoryName;
	}

	public String getExcludeDesc() {
		return excludeDesc;
	}

	public void setExcludeDesc(String excludeDesc) {
		this.excludeDesc = excludeDesc;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<MetaFieldExclude> getFieldExcludeList() {
		return fieldExcludeList;
	}

	public void setFieldExcludeList(List<MetaFieldExclude> fieldExcludeList) {
		this.fieldExcludeList = fieldExcludeList;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}
}
