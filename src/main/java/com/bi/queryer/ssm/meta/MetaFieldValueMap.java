package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

/**
 * @Auther: contributor
 * @Date: 2025/3/28 14:47
 * @Description:
 */

public class MetaFieldValueMap implements JSONSerializable, Cloneable{
    private String fieldCode;

    private String fieldKey;

    private String fieldValue;

    private String createdBy;

    private String createdTime;

    private String dataEnv = DataEnv.OLD_SSM.getCode();

    public static MetaFieldValueMap of(String fieldCode, String itemKey, String itemValue, String createdBy) {
        MetaFieldValueMap metaFieldValueMap = new MetaFieldValueMap();
        metaFieldValueMap.setFieldCode(fieldCode);
        metaFieldValueMap.setFieldKey(itemKey);
        metaFieldValueMap.setFieldValue(itemValue);
        metaFieldValueMap.setCreatedBy(createdBy);
        return metaFieldValueMap;
    }


    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getDataEnv() {
        return dataEnv;
    }

    public void setDataEnv(String dataEnv) {
        this.dataEnv = dataEnv;
    }

    @Override
    public JSONObject toJSON() {
        return null;

    }
}
