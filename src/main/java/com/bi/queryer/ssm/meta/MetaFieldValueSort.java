package com.bi.queryer.ssm.meta;

/**
 * @Author: contributor
 * @CreateTime: 2023-10-11  16:08
 * @Description: 字段值排序
 */
public class MetaFieldValueSort implements Comparable<MetaFieldValueSort>{

    private String fieldCode;

    private String fieldValue;

    private String fieldValueSortNum;

    public MetaFieldValueSort() {
    }

    public MetaFieldValueSort(String fieldCode, String fieldValue, String fieldValueSortNum) {
        this.fieldCode = fieldCode;
        this.fieldValue = fieldValue;
        this.fieldValueSortNum = fieldValueSortNum;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getFieldValueSortNum() {
        return fieldValueSortNum;
    }

    public void setFieldValueSortNum(String fieldValueSortNum) {
        this.fieldValueSortNum = fieldValueSortNum;
    }

    @Override
    public int compareTo(MetaFieldValueSort o) {
        if(fieldValueSortNum == null){
            return -1;
        }
        return fieldValueSortNum.compareTo(o.getFieldValueSortNum());
    }
}
