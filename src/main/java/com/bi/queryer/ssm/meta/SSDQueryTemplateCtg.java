package com.bi.queryer.ssm.meta;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 废弃
 */
@Deprecated
public class SSDQueryTemplateCtg implements JSONSerializable {

	/**
	 * ctg_id
	 */
	private String ctgId;

	/**
	 * ctg_name
	 */
	private String ctgName;

	/**
	 * 排序
	 */
	private Integer sortId;

	/**
	 * ctg_parent_id
	 */
	private String ctgParentId;

	/**
	 * 是否可用
	 */
	private Integer isActive;

	/**
	 * 创建时间
	 */
	private String createdTime;

	/**
	 * 修改时间
	 */
	private String updatedTime;

	/**
	 * 创建人
	 */
	private String createdBy;

	/**
	 * 修改人
	 */
	private String updatedBy;

	/**
	 * 是否为模板所有者
	 */
	private boolean isTemplateOwner = false;

	public boolean isTemplateOwner() {
		return isTemplateOwner;
	}

	public void setTemplateOwner(boolean templateOwner) {
		isTemplateOwner = templateOwner;
	}

	private List<SSDQueryTemplateCtg> children = new ArrayList<SSDQueryTemplateCtg>();

	public List<SSDQueryTemplateCtg> getChildren() {
		return children;
	}

	public void setChildren(List<SSDQueryTemplateCtg> children) {
		this.children = children;
	}

	public String getCtgId() {
		return ctgId;
	}

	public void setCtgId(String ctgId) {
		this.ctgId = ctgId;
	}

	public String getCtgName() {
		return ctgName;
	}

	public void setCtgName(String ctgName) {
		this.ctgName = ctgName;
	}

	public Integer getSortId() {
		return sortId;
	}

	public void setSortId(Integer sortId) {
		this.sortId = sortId;
	}

	public String getCtgParentId() {
		return ctgParentId;
	}

	public void setCtgParentId(String ctgParentId) {
		this.ctgParentId = ctgParentId;
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

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
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


	public SSDQueryTemplateCtg(String ctgId, String ctgName, Integer sortId, String ctgParentId, Integer isActive) {
		this.ctgId = ctgId;
		this.ctgName = ctgName;
		this.sortId = sortId;
		this.ctgParentId = ctgParentId;
		this.isActive = isActive;
	}

	public SSDQueryTemplateCtg() {
	}
}
