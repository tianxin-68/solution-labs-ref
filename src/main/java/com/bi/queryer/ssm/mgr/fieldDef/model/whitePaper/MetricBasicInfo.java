package com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper;

import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class MetricBasicInfo {

    /**
     * 指标编码
     */
    private String metricCode;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 指标规范命名
     */
    private String metricStandardName;

    /**
     * 指标类型
     */
    private String metricType;

    /**
     * 指标类型描述
     */
    private String metricTypeDesc;


    /**
     * 指标释义
     */
    private String metricDesc;

    /**
     * 指标备注
     */
    private String metricRemark;

    private String relMetricRemark;


    /**
     * 原子指标名称（派生指标使用）
     */
    private String relMetricName;

    /**
     * 原子指标编码（派生指标使用）
     */
    private String relMetricCode;

    /**
     * 维限定（派生指标使用）
     */
    private List<DeriveMetricDimItem> dimItemList;


    //指标业务过程名称 -- 数据域-模块-业务过程
    private String metricProcessPathName;

    //词根
    private String metricProcessRootWordName;

    //指标实体名称
    private String metricEntityName;

    //指标度量名称
    private String metricMeasureName;

    //指标原子修饰词名称
    private String metricCaliberName;

    /**
     * 指标表达式
     */
    private String metricExp;

    /**
     * 表达式中的指标详情
     */
    private Map<String, MetricDetail> expRelMetrics;

}
