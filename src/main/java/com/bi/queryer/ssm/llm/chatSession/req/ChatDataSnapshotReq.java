package com.bi.queryer.ssm.llm.chatSession.req;

import com.bi.queryer.ssm.engine.result.ResultDataSet;

import java.util.List;

public class ChatDataSnapshotReq {

    private ResultDataSet dataset;

    /**
     * 查询配置,前端加密
     */
    private String queryConfig;

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

    public ResultDataSet getDataset() {
        return dataset;
    }

    public void setDataset(ResultDataSet dataset) {
        this.dataset = dataset;
    }

    public String getQueryConfig() {
        return queryConfig;
    }

    public void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }
}
