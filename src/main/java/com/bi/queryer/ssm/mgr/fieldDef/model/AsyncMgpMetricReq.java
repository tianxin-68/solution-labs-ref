package com.bi.queryer.ssm.mgr.fieldDef.model;

import lombok.Data;

import java.util.List;

/**
 * 指标管理平台同步指标替换数据req
 */
@Data
public class AsyncMgpMetricReq {

	/**
	 * 替换指标code
	 */
	private String metricCode;

	/**
	 * 被替换指标code
	 */
	private List<String> replaceMetricCodes;
}
