package com.bi.queryer.ssm.meta;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

/**
 * ssd_query_field_stats
 * @author contributor
 */
public class SSDQueryFieldStatsEntity implements JSONSerializable {

	private static final long serialVersionUID = 1L;

	/**
	 * field_id
	 */
	private String fieldId;

	/**
	 * field_code
	 */
	private String fieldCode;

	/**
	 * field_name
	 */
	private String fieldName;

	/**
	 * field_title
	 */
	private String fieldTitle;

	/**
	 * ctg_id
	 */
	private String ctgId;

	/**
	 * ctg_name
	 */
	private String ctgName;

	/**
	 * query_count
	 */
	private Integer queryCount;

	/**
	 * query_user_count
	 */
	private Integer queryUserCount;

	/**
	 * min_query_time
	 */
	private String minQueryTime;

	/**
	 * max_query_time
	 */
	private String maxQueryTime;

	/**
	 * sort_id
	 */
	private Double sortId;

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

	public String getFieldTitle() {
		return fieldTitle;
	}

	public void setFieldTitle(String fieldTitle) {
		this.fieldTitle = fieldTitle;
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

	public Integer getQueryCount() {
		return queryCount;
	}

	public void setQueryCount(Integer queryCount) {
		this.queryCount = queryCount;
	}

	public Integer getQueryUserCount() {
		return queryUserCount;
	}

	public void setQueryUserCount(Integer queryUserCount) {
		this.queryUserCount = queryUserCount;
	}

	public String getMinQueryTime() {
		return minQueryTime;
	}

	public void setMinQueryTime(String minQueryTime) {
		this.minQueryTime = minQueryTime;
	}

	public String getMaxQueryTime() {
		return maxQueryTime;
	}

	public void setMaxQueryTime(String maxQueryTime) {
		this.maxQueryTime = maxQueryTime;
	}

	public Double getSortId() {
		return sortId;
	}

	public void setSortId(Double sortId) {
		this.sortId = sortId;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}
}
