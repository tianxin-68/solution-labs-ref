package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.ssm.mgr.exportImport.SSDExcel;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

/**
 * 表关联关系
 * @author contributor
 *
 */
public class MetaTableRelation implements Comparable<MetaTableRelation>, Cloneable,JSONSerializable {

	@SSDExcel(order = 1)
	private String id;

	@SSDExcel(order = 2)
	private String primaryTableId;

	/**
	 * 主表表名
	 */
	private String primaryTableName;

	@SSDExcel(order = 3)
	private String subTableId;

	/**
	 * 从表表名
	 */
	private String subTableName;

	@SSDExcel(order = 4)
	private String primaryFieldId;

	private String primaryFieldName;

	@SSDExcel(order = 5)
	private String subFieldId;

	private String subFieldName;

	@SSDExcel(order = 6)
	private String joinExpression ;

	@SSDExcel(order = 7)
	private Double weight;

	@SSDExcel(order = 8,trueOrFalse = true)
	private Integer isActive;
	private String createdTime;
	private String createdBy;
	private String updatedTime;
	private String updatedBy;
	private String joinOnType;
	private String subFieldId2;
	private String subFieldName2;

	private String dataEnv = DataEnv.OLD_SSM.getCode();

	public String getDataEnv() {
		return dataEnv;
	}

	public void setDataEnv(String dataEnv) {
		this.dataEnv = dataEnv;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPrimaryTableId() {
		return primaryTableId;
	}

	public void setPrimaryTableId(String primaryTableId) {
		this.primaryTableId = primaryTableId;
	}

	public String getSubTableId() {
		return subTableId;
	}

	public void setSubTableId(String subTableId) {
		this.subTableId = subTableId;
	}

	public String getPrimaryFieldName() {
		return primaryFieldName;
	}

	public void setPrimaryFieldName(String primaryFieldName) {
		this.primaryFieldName = primaryFieldName;
	}

	public String getSubFieldName() {
		return subFieldName;
	}

	public void setSubFieldName(String subFieldName) {
		this.subFieldName = subFieldName;
	}

	public String getJoinExpression() {
		return joinExpression;
	}

	public void setJoinExpression(String joinExpression) {
		this.joinExpression = joinExpression;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public String getPrimaryTableName() {
		return primaryTableName;
	}

	public void setPrimaryTableName(String primaryTableName) {
		this.primaryTableName = primaryTableName;
	}

	public String getSubTableName() {
		return subTableName;
	}

	public void setSubTableName(String subTableName) {
		this.subTableName = subTableName;
	}

	public String getPrimaryFieldId() {
		return primaryFieldId;
	}

	public void setPrimaryFieldId(String primaryFieldId) {
		this.primaryFieldId = primaryFieldId;
	}

	public String getSubFieldId() {
		return subFieldId;
	}

	public void setSubFieldId(String subFieldId) {
		this.subFieldId = subFieldId;
	}

	@Override
	public int compareTo(MetaTableRelation o) {
		Double num = this.weight - o.getWeight();
		return num > 0 ? 1 : -1;
	}

	public MetaTableRelation clone() {
		MetaTableRelation clone = new MetaTableRelation();
		try {
			clone = (MetaTableRelation) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return clone;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}


	public String getJoinOnType() {
		return joinOnType;
	}

	public void setJoinOnType(String joinOnType) {
		this.joinOnType = joinOnType;
	}

	public String getSubFieldId2() {
		return subFieldId2;
	}

	public void setSubFieldId2(String subFieldId2) {
		this.subFieldId2 = subFieldId2;
	}

	public String getSubFieldName2() {
		return subFieldName2;
	}

	public void setSubFieldName2(String subFieldName2) {
		this.subFieldName2 = subFieldName2;
	}
}
