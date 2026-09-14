package com.bi.queryer.ssm.api.entity;

public class AgentUserViewEntity {

    private String viewId;
    private String viewName;
    private String viewDesc;
    private Integer sortId;
    private AgentUserViewConfigRsp config;

    public String getViewId() { return viewId; }
    public void setViewId(String viewId) { this.viewId = viewId; }

    public String getViewName() { return viewName; }
    public void setViewName(String viewName) { this.viewName = viewName; }

    public String getViewDesc() { return viewDesc; }
    public void setViewDesc(String viewDesc) { this.viewDesc = viewDesc; }

    public Integer getSortId() { return sortId; }
    public void setSortId(Integer sortId) { this.sortId = sortId; }

    public AgentUserViewConfigRsp getConfig() { return config; }
    public void setConfig(AgentUserViewConfigRsp config) { this.config = config; }
}
