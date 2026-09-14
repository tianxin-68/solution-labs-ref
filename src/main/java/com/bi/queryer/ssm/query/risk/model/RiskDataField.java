package com.bi.queryer.ssm.query.risk.model;

import com.bi.queryer.ssm.enums.DataSensitiveLevel;

public class RiskDataField {

    private String fieldName;

    /**
     * 敏感等级
     */
    private String sensitiveLevel = DataSensitiveLevel.C1.getCode();

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
