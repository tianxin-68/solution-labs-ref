package com.bi.queryer.ssm.api.vo.req;

/**
 * OLAP API 查询指定的行/列维度（按字段展示名 fieldName 匹配）
 */
public class QueryOlapDataQueryDimensionReq {

    private String fieldName;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
