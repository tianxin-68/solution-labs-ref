package com.bi.queryer.ssm.api.vo.req;

import java.util.List;

public class QueryOlapDataQueryFilterReq {

    /**
     * 过滤方式：templateViewDefault（与视图保存一致，默认）、include（包含）、exclude（不包含）
     */
    private String filterType;

    private String fieldName;

    private List<Object> values;

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }
}
