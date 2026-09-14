package com.bi.queryer.ssm.query.template.model;

public class TemplateLinkFieldAddReq {

    /**
     * 筛选行维度id
     */
    private String fieldId;

    /**
     * 筛选行维度编码
     */
    private String fieldCode;

    /**
     * 筛选行维度标题
     */
    private String fieldTitle;

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
}
