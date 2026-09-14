package com.bi.queryer.ssm.query.template.metric.replace.model;

/**
 * 模板字段替换日志（template_field_replace_log）
 */
public class TemplateFieldReplaceLogEntity {

    private Long id;
    /** 替换任务 ID（前端传入） */
    private String replaceTaskId;
    /** 看板 id */
    private String tplId;
    private String viewId;
    private String createdBy;
    private String createdTime;
    private String updatedBy;
    private String updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReplaceTaskId() {
        return replaceTaskId;
    }

    public void setReplaceTaskId(String replaceTaskId) {
        this.replaceTaskId = replaceTaskId;
    }

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }
}
