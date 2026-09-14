package com.bi.queryer.ssm.engine.grid.header.vo;

import com.bi.queryer.ssm.engine.config.field.FieldValue;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/8/15 14:05
 * @Description:
 */
public class GridHeaderFilterConfigVO {
    private String fieldId;

    private String code;

    private List<FieldValue> filterValues;

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public List<FieldValue> getFilterValues() {
        return filterValues;
    }

    public void setFilterValues(List<FieldValue> filterValues) {
        this.filterValues = filterValues;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
