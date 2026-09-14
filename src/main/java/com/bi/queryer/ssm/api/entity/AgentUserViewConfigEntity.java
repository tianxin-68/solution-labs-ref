package com.bi.queryer.ssm.api.entity;

public class AgentUserViewConfigEntity {

    private String pkid;
    private String viewId;
    private String columnName;
    private String columnArea;

    public String getPkid() { return pkid; }
    public void setPkid(String pkid) { this.pkid = pkid; }

    public String getViewId() { return viewId; }
    public void setViewId(String viewId) { this.viewId = viewId; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getColumnArea() { return columnArea; }
    public void setColumnArea(String columnArea) { this.columnArea = columnArea; }
}
