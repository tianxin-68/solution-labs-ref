package com.bi.queryer.ssm.query.template.view.model;

public class TemplateViewReq {

    /**
     * 模板ID
     */
    private String tplId;

    /**
     * 视图id
     */
    private String viewId;

    /**
     * 视图名称
     */
    private String viewName;

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
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
}
