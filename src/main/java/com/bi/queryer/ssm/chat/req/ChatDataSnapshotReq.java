package com.bi.queryer.ssm.chat.req;

public class ChatDataSnapshotReq {

    /**
     * 查询配置，前端加密
     */
    private String queryConfig;

    /**
     * 查询模板图ID
     */
    private String viewId;

    /**
     * 组件id
     */
    private String widgetId;

    public String getQueryConfig() {
        return queryConfig;
    }

    public void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }
}
