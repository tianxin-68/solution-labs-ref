package com.bi.queryer.ssm.api.vo.req;

/**
 * OLAP API 查询指定的指标（按字段展示名 fieldName 匹配）
 */
public class QueryOlapDataQueryMetricReq {

    private String fieldName;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
