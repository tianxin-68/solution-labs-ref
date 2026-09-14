package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.Date;

@Data
public class TemplateShareRelEntity {

    private String sourceTplId;

    private String targetTplId;

    private String rootTplId;

    private String sourceViewId;

    private String targetViewId;

    private String rootViewId;

    private String createdBy;

    private Date createdTime;

    public TemplateShareRelEntity() {

    }

    public TemplateShareRelEntity(String sourceTplId, String targetTplId, String createdBy) {
        this.sourceTplId = sourceTplId;
        this.targetTplId = targetTplId;
        this.createdBy = createdBy;
    }

    public String getSourceViewId() {
        return sourceViewId;
    }

    public void setSourceViewId(String sourceViewId) {
        this.sourceViewId = sourceViewId;
    }

    public String getTargetViewId() {
        return targetViewId;
    }

    public void setTargetViewId(String targetViewId) {
        this.targetViewId = targetViewId;
    }

    public String getRootViewId() {
        return rootViewId;
    }

    public void setRootViewId(String rootViewId) {
        this.rootViewId = rootViewId;
    }
}