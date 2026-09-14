package com.bi.queryer.ssm.query.log;

/**
 * @Auther: contributor
 * @Date: 2025/12/3 14:26
 * @Description:
 */
public class SSMQueryTplFieldLogEntity {
    private String tplId;
    private String viewId;
    private String fieldId;
    private String fieldCode;
    private String fieldTitle;
    private String lastQueryLogId;
    private Integer fieldExprSize;
    private String createdBy;
    private String createdTime;

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

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldTitle() {
        return fieldTitle;
    }

    public void setFieldTitle(String fieldTitle) {
        this.fieldTitle = fieldTitle;
    }

    public String getLastQueryLogId() {
        return lastQueryLogId;
    }

    public void setLastQueryLogId(String lastQueryLogId) {
        this.lastQueryLogId = lastQueryLogId;
    }

    public Integer getFieldExprSize() {
        return fieldExprSize;
    }

    public void setFieldExprSize(Integer fieldExprSize) {
        this.fieldExprSize = fieldExprSize;
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
}
