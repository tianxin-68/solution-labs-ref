package com.bi.queryer.ssm.query.template.model;

import java.util.ArrayList;
import java.util.List;

public class TemplateViewRsp {

    /**
     * 模板ID
     */
    private String tplId;

    private String tplName;

    /**
     * 视图ID
     */
    private String viewId;

    private String viewName;

    /**
     * 视图ID映射
     */
    private List<TemplateViewMapping> viewIdMappingList = new ArrayList<>();


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

    public List<TemplateViewMapping> getViewIdMappingList() {
        return viewIdMappingList;
    }

    public void setViewIdMappingList(List<TemplateViewMapping> viewIdMappingList) {
        this.viewIdMappingList = viewIdMappingList;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
}
