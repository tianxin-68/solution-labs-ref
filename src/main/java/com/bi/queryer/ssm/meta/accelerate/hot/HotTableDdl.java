package com.bi.queryer.ssm.meta.accelerate.hot;

/**
 * @Author contributor
 * @Date 16:58 2024/8/19
 * @Description 热表ddl
 **/
public class HotTableDdl {
    private String hotTableName;
    private String dbEngine;

    private String sourceTableName;

    private String fieldList;

    private String partitionBy;

    private String ddlEtlContent;

    private String ddlEtlMode;

    private String logContent;

    private String createdBy;

    private String createdTime;

    public String getHotTableName() {
        return hotTableName;
    }

    public void setHotTableName(String hotTableName) {
        this.hotTableName = hotTableName;
    }

    public String getFieldList() {
        return fieldList;
    }

    public void setFieldList(String fieldList) {
        this.fieldList = fieldList;
    }

    public String getPartitionBy() {
        return partitionBy;
    }

    public void setPartitionBy(String partitionBy) {
        this.partitionBy = partitionBy;
    }

    public String getDdlEtlContent() {
        return ddlEtlContent;
    }

    public void setDdlEtlContent(String ddlEtlContent) {
        this.ddlEtlContent = ddlEtlContent;
    }

    public String getDbEngine() {
        return dbEngine;
    }

    public void setDbEngine(String dbEngine) {
        this.dbEngine = dbEngine;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getDdlEtlMode() {
        return ddlEtlMode;
    }

    public void setDdlEtlMode(String ddlEtlMode) {
        this.ddlEtlMode = ddlEtlMode;
    }
}
