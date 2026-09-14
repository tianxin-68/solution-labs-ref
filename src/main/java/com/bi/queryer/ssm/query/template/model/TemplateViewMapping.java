package com.bi.queryer.ssm.query.template.model;

public class TemplateViewMapping {

    private String oldViewId;

    private String newViewId;

    public TemplateViewMapping() {
    }

    public TemplateViewMapping(String oldViewId, String newViewId) {
        this.oldViewId = oldViewId;
        this.newViewId = newViewId;
    }

    public String getOldViewId() {
        return oldViewId;
    }

    public void setOldViewId(String oldViewId) {
        this.oldViewId = oldViewId;
    }

    public String getNewViewId() {
        return newViewId;
    }

    public void setNewViewId(String newViewId) {
        this.newViewId = newViewId;
    }

}
