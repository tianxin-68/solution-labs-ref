package com.bi.queryer.sys.menu.vo;

import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.StringUtil;

public class MenuItemMeta {

	/**
	 * 报表Id
	 */
	private String rptId;

	/**
	 * 报表名称
	 */
	private String rptName;

	/**
	 * 数据日期类型
	 */
	private String rptDateType;

	/**
	 * 数据更新频率
	 */
	private String rptDateUptFreq;

	/**
	 * 需求提出owner
	 */
	private String reqOwner;

	/**
	 * 需求对接owner
	 */
	private String reqRecOwner;

	/**
	 * 数据开发owner
	 */
	private String dataDevOwner;

	/**
	 * 报表开发owner
	 */
	private String rptDevOwner;

	/**
	 * 报表描述
	 */
	private String rptDesc;

	/**
	 * 标签集合
	 */
    private String labels;

	/**
	 * tab页签集合
	 */
	private String metaItems;

	public String getRptId() {
		return rptId;
	}

	public void setRptId(String rptId) {
		this.rptId = rptId;
	}

	public String getRptName() {
		return rptName;
	}

	public void setRptName(String rptName) {
		this.rptName = rptName;
	}

	public String getRptDateType() {
		return rptDateType;
	}

	public void setRptDateType(String rptDateType) {
		this.rptDateType = rptDateType;
	}

	public String getRptDateUptFreq() {
		return rptDateUptFreq;
	}

	public void setRptDateUptFreq(String rptDateUptFreq) {
		this.rptDateUptFreq = rptDateUptFreq;
	}

	public String getReqOwner() {
		return reqOwner;
	}

	public void setReqOwner(String reqOwner) {
		this.reqOwner = reqOwner;
	}

	public String getReqRecOwner() {
		return reqRecOwner;
	}

	public void setReqRecOwner(String reqRecOwner) {
		this.reqRecOwner = reqRecOwner;
	}

	public String getDataDevOwner() {
		return dataDevOwner;
	}

	public void setDataDevOwner(String dataDevOwner) {
		this.dataDevOwner = dataDevOwner;
	}

	public String getRptDevOwner() {
		return rptDevOwner;
	}

	public void setRptDevOwner(String rptDevOwner) {
		this.rptDevOwner = rptDevOwner;
	}

	public String getRptDesc() {
		return rptDesc;
	}

	public void setRptDesc(String rptDesc) {
		this.rptDesc = rptDesc;
	}

	public String getLabels() {
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}

	public String getMetaItems() {
		return metaItems;
	}

	public void setMetaItems(String metaItems) {
		this.metaItems = metaItems;
	}
}
