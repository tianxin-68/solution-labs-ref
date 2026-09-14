package com.bi.queryer.ssm.query.template.model;

public class TemplateLinkFieldEnity {

    private String linkId;

    /**
     * 筛选行维度id
     */
    private String fieldId;

    /**
     * 筛选行维度编码
     */
    private String fieldCode;

    /**
     * 筛选行维度名称
     */
    private String fieldTitle;

    /**
     * 排序
     */
    private Double sortId;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
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

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

}
