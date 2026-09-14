package com.bi.queryer.ssm.schedule.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *  字段查询统计1个月总表
 * @author contributor
 */
@Data
public class SSDQueryFieldStatsTotal implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 字段id
	 */
	private String fieldId;

	/**
	 * 字段code
	 */
	private String fieldCode;

	/**
	 * 字段名称
	 */
	private String fieldName;

	/**
	 * 字段中文名称
	 */
	private String fieldTitle;

	/**
	 * 是否是度量
	 */
	private Integer isMeasure;

	/**
	 * 字段分类id
	 */
	private String ctgId;

	/**
	 * 字段分类名称
	 */
	private String ctgName;

	/**
	 * 总使用量
	 */
	private Integer queryCount;

	/**
	 * 查询使用量
	 */
	private Integer queryUserCount;

	/**
	 * 查询最早时间
	 */
	private Date minQueryTime;

	/**
	 * 查询最晚时间
	 */
	private Date maxQueryTime;

	/**
	 * 排序
	 */
	private Double sortId;

	/**
	 * 统计时间
	 */
	private Date dt;

}
