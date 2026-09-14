package com.bi.queryer.ssm.query.template.model;

public class TemplateLinkFieldRsp implements Comparable<TemplateLinkFieldRsp>{

    private String linkId;

    /**
     * 维度字段id
     */
    private String fieldId;

    /**
     * 维度字段编码
     */
    private String fieldCode;

    /**
     * 维度字段标题
     */
    private String fieldTitle;

    private Double sortId;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
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

    @Override
    public int compareTo(TemplateLinkFieldRsp o) {
        if (o == null) {
            return -1;
        }
        Double num = (this.sortId - o.getSortId());
        return num > 0 ? 1 : -1;
    }
}
