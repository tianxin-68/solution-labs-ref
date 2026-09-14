package com.bi.queryer.ssm.mgr.fieldDef.model;
import com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper.DimBasicInfo;
import com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper.MetricBasicInfo;
import lombok.Data;

import java.io.Serializable;

/**
 * @description 白皮书
 */
@Data
public class ManualIndexWhitePaperEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 指标编号
     */
    private String indexNo;

    /**
     * 指标名
     */
    private String indexName;

    /**
     * 释义
     */
    private String interpretation;


    /**
     * 是否是指标 1 是 0 否
     */
    private Integer isMeasure;

    /**
     * 指标信息
     */
    private MetricBasicInfo metricBasicInfo;

    /**
     * 维度信息
     */
    private DimBasicInfo dimBasicInfo;

    private String fieldType;

    /**
     * 字段备注
     */
    private String fieldRemark;

    /**
     * 字段备注
     */
    private String relFieldRemark;
}
