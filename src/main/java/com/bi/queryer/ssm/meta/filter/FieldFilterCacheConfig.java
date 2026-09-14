package com.bi.queryer.ssm.meta.filter;

/**
 * @Author contributor
 * @Date 14:39 2024/12/9
 * @Description 字段过滤器缓存配置
 **/
public class FieldFilterCacheConfig {
    private String fieldId;

    private String fieldCode;

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
