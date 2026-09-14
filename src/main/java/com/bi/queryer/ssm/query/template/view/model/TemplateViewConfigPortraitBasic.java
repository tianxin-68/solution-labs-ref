package com.bi.queryer.ssm.query.template.view.model;

/**
 * 视图配置画像：基本信息（名称、视图 id 等）
 */
public class TemplateViewConfigPortraitBasic {

    private String viewId;

    private String viewName;

    private String tplId;

    private String tplName;

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
}
