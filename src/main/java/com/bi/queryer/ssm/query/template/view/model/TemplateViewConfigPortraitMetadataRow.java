package com.bi.queryer.ssm.query.template.view.model;

/**
 * 视图配置画像：数据结构一行（字段、类型、编码、释义）
 */
public class TemplateViewConfigPortraitMetadataRow {

    /** 字段展示名 */
    private String columnName;

    /**
     * 类型：指标还是维度
     */
    private String columnType;

    /**
     * 列描述
     */
    private String columnDesc;

    /**
     * 聚合方式
     */
    private String aggregationType;


    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getColumnDesc() {
        return columnDesc;
    }

    public void setColumnDesc(String columnDesc) {
        this.columnDesc = columnDesc;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }
}
