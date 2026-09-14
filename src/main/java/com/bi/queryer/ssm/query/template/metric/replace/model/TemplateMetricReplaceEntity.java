package com.bi.queryer.ssm.query.template.metric.replace.model;

import java.util.Date;

/**
 * 模板字段替换映射（template_metric_replace）
 */
public class TemplateMetricReplaceEntity {

    private Long id;
    private String oldWpCode;
    private String newWpCode;
    private String newWpName;
    private String newFieldId;
    private Integer isActive;
    private String createdBy;
    private Date createdTime;
    private String updatedBy;
    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOldWpCode() {
        return oldWpCode;
    }

    public void setOldWpCode(String oldWpCode) {
        this.oldWpCode = oldWpCode;
    }

    public String getNewWpCode() {
        return newWpCode;
    }

    public void setNewWpCode(String newWpCode) {
        this.newWpCode = newWpCode;
    }

    public String getNewWpName() {
        return newWpName;
    }

    public void setNewWpName(String newWpName) {
        this.newWpName = newWpName;
    }

    public String getNewFieldId() {
        return newFieldId;
    }

    public void setNewFieldId(String newFieldId) {
        this.newFieldId = newFieldId;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }
}
