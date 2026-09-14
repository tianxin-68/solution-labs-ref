package com.bi.queryer.ssm.api.entity;

public class AgentUserViewConfigItem {

    private String columnName;

    public AgentUserViewConfigItem() {}

    public AgentUserViewConfigItem(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
}
