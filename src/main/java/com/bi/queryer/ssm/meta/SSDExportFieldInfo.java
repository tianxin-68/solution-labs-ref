package com.bi.queryer.ssm.meta;


import java.io.Serializable;

/**
 * @description 下载导出类字段信息
 * @author contributor
 * @date 2021-08-17
 */

public class SSDExportFieldInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * fieldName
     */
    private String fieldName;

    /**
     * fieldTitle
     */
    private String fieldTitle;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldTitle() {
        return fieldTitle;
    }

    public void setFieldTitle(String fieldTitle) {
        this.fieldTitle = fieldTitle;
    }
}