package com.bi.queryer.ssm.llm.resp;

import java.util.ArrayList;
import java.util.List;

public class AgentQueryField {

    /**
     * 编码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * 字段类型 dim 维度  measure 指标
     */
    private String fieldType;

    /**
     * 操作符
     */
    private String operator;

    /**
     * 过滤值类型
     */
    private String filterValueType;

    /**
     * 过滤值
     */
    private List<Object> filterValues = new ArrayList<>();

    /**
     * 筛选器类型
     */
    private String filterType;

    /**
     * 结果筛选、明细筛选
     */
    private String filterValueMode;

    /**
     * 模糊匹配，精准匹配
     */
    private String filterQueryRule;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getFilterValueType() {
        return filterValueType;
    }

    public void setFilterValueType(String filterValueType) {
        this.filterValueType = filterValueType;
    }

    public List<Object> getFilterValues() {
        return filterValues;
    }

    public void setFilterValues(List<Object> filterValues) {
        this.filterValues = filterValues;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getFilterValueMode() {
        return filterValueMode;
    }

    public void setFilterValueMode(String filterValueMode) {
        this.filterValueMode = filterValueMode;
    }

    public String getFilterQueryRule() {
        return filterQueryRule;
    }

    public void setFilterQueryRule(String filterQueryRule) {
        this.filterQueryRule = filterQueryRule;
    }
}
