package com.bi.queryer.ssm.meta;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

/**
 * 敏感字段元数据
 * @author contributor
 *
 */
public class MetaSensitiveField implements Comparable<MetaSensitiveField>, JSONSerializable {

	// 标示
	private String id;

	// 编码
	private String code;

	// 名称
	private String name;

	// 标题
	private String title;

	// 敏感字段
	private Integer isSensitive = Enabled.NO.getId();

	// 所属类别id
	private String categoryId;
	
	// 所属类别名称
	private String categoryName;

	// 虚拟目录id
	private String virtualCategoryId;

	//虚拟目录名称
	private String virtualCategoryName;

	private Integer isActive;

	private String categoryPath = "";

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getIsSensitive() {
		return isSensitive;
	}

	public void setIsSensitive(Integer isSensitive) {
		this.isSensitive = isSensitive;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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

	public String getVirtualCategoryId() {
		return virtualCategoryId;
	}

	public void setVirtualCategoryId(String virtualCategoryId) {
		this.virtualCategoryId = virtualCategoryId;
	}

	public String getVirtualCategoryName() {
		return virtualCategoryName;
	}

	public void setVirtualCategoryName(String virtualCategoryName) {
		this.virtualCategoryName = virtualCategoryName;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public String getCategoryPath() {
		return categoryPath;
	}

	public void setCategoryPath(String categoryPath) {
		this.categoryPath = categoryPath;
	}

	@Override
	public int compareTo(MetaSensitiveField o) {
		int flag = this.categoryPath.compareTo(o.categoryPath);
		return flag;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}
}
