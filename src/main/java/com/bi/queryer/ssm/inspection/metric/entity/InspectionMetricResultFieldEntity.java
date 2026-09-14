package com.bi.queryer.ssm.inspection.metric.entity;

public class InspectionMetricResultFieldEntity {

    /**
     * 字段编码
     */
    private String fieldCode;

    /**
     * 字段标题
     */
    private String fieldTitle;

    /**
     * 字段owner
     */
    private String fieldOwner;

    /**
     * 指标为空的分区日期
     */
    private String metricEmptyPartitionDate;

    /**
     * 指标为空的原因
     */
    private String metricEmptyRemark;

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldTitle() {
        return fieldTitle;
    }

    public void setFieldTitle(String fieldTitle) {
        this.fieldTitle = fieldTitle;
    }

    public String getFieldOwner() {
        return fieldOwner;
    }

    public void setFieldOwner(String fieldOwner) {
        this.fieldOwner = fieldOwner;
    }

    public String getMetricEmptyPartitionDate() {
        return metricEmptyPartitionDate;
    }

    public void setMetricEmptyPartitionDate(String metricEmptyPartitionDate) {
        this.metricEmptyPartitionDate = metricEmptyPartitionDate;
    }

    public String getMetricEmptyRemark() {
        return metricEmptyRemark;
    }

    public void setMetricEmptyRemark(String metricEmptyRemark) {
        this.metricEmptyRemark = metricEmptyRemark;
    }
}
