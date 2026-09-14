package com.bi.queryer.ssm.sensitive.model;

import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

import java.util.Objects;

/**
 *  多维分析-敏感字段导出解密申请表
 * @author contributor
 */
public class SensitiveApply implements Cloneable, JSONSerializable {

	/**
	 * 申请id
	 */
	private String applyId;

	/**
	 * 申请用户
	 */
	private String userName;

	/**
	 * 字段id（逗号隔开）
	 */
	private String fieldId;

	/**
	 * 字段code（逗号隔开）
	 */
	private String fieldCode;

	/**
	 * 字段名称（逗号隔开）
	 */
	private String fieldName;

	/**
	 * 字段个数
	 */
	private Integer fieldNum;

	/**
	 * 是否已使用，1：是，0：否
	 */
	private Integer isUsed;

	/**
	 * 申请时间
	 */
	private String applyTime;

	/**
	 * 状态0：正常，1不正常
	 */
	private Integer status;

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

	public String getApplyId() {
		return applyId;
	}

	public void setApplyId(String applyId) {
		this.applyId = applyId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFieldId() {
		return fieldId;
	}

	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}

	public String getFieldCode() {
		return fieldCode;
	}

	public void setFieldCode(String fieldCode) {
		this.fieldCode = fieldCode;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public Integer getFieldNum() {
		return fieldNum;
	}

	public void setFieldNum(Integer fieldNum) {
		this.fieldNum = fieldNum;
	}

	public Integer getIsUsed() {
		return isUsed;
	}

	public void setIsUsed(Integer isUsed) {
		this.isUsed = isUsed;
	}

	public String getApplyTime() {
		return applyTime;
	}

	public void setApplyTime(String applyTime) {
		this.applyTime = applyTime;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
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

	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SensitiveApply sensitiveApply = (SensitiveApply) o;
		return applyId.equals(sensitiveApply.getApplyId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(applyId,userName,fieldId,fieldCode,fieldName,fieldNum,isUsed,applyTime,status,createdTime,updatedTime,createdBy,updatedBy);
	}

	public MetaField clone(){
		MetaField copy = new MetaField();
		try {
			copy = (MetaField) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return copy;
	}
}
