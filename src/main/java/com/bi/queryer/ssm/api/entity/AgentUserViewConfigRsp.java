package com.bi.queryer.ssm.api.entity;

import java.util.List;

public class AgentUserViewConfigRsp {

    private List<AgentUserViewConfigItem> viewDimensions;
    private List<AgentUserViewConfigItem> viewMetrics;
    private List<AgentUserViewConfigItem> viewFilters;

    public List<AgentUserViewConfigItem> getViewDimensions() { return viewDimensions; }
    public void setViewDimensions(List<AgentUserViewConfigItem> viewDimensions) { this.viewDimensions = viewDimensions; }

    public List<AgentUserViewConfigItem> getViewMetrics() { return viewMetrics; }
    public void setViewMetrics(List<AgentUserViewConfigItem> viewMetrics) { this.viewMetrics = viewMetrics; }

    public List<AgentUserViewConfigItem> getViewFilters() { return viewFilters; }
    public void setViewFilters(List<AgentUserViewConfigItem> viewFilters) { this.viewFilters = viewFilters; }
}
