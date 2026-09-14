package com.bi.queryer.ssm.api.vo.req;

/**
 * 通过 UI 查询配置（config）直接查询多维数据
 */
public class QueryOlapByConfigReq {

    /**
     * UI 查询配置 JSON（与模板 config 结构一致）
     */
    private String config;

    /**
     * 视图 ID
     */
    private String viewId;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }
}
