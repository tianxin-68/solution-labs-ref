package com.bi.queryer.ssm.llm.chatSession.entity;

import java.util.List;

public class ChatDataSnapshotRemark {

    /**
     * 查询模板 ID
     */
    private String tplId;

    /**
     * 查询模板名称
     */
    private String tplName;

    /**
     * 查询模板视图ID
     */
    private String viewId;

    // 视图名称
    private String viewName;

    /**
     * 组件id
     */
    private String widgetId;

    // 全局字段
    private List<String> dataRange;

    // 日期粒度
    private String dateGranularity;

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public List<String> getDataRange() {
        return dataRange;
    }

    public void setDataRange(List<String> dataRange) {
        this.dataRange = dataRange;
    }

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }
}
