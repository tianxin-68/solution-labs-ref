package com.bi.queryer.ssm.api.ticket;

/**
 * 高敏字段条目
 */
public class SensitiveFieldItem {

    private String fieldId;

    private String fieldCode;

    private String fieldTitle;

    /**
     * 敏感等级 c1/c2/c3/c4
     */
    private String sensitiveLevel;

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

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }
}