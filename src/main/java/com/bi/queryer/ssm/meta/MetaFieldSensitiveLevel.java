package com.bi.queryer.ssm.meta;

/**
 * @Author contributor
 * @Date 14:28 2025/9/4
 * @Description 字段安全等级
 **/
public class MetaFieldSensitiveLevel {
    private String dbName;
    private String tableName;
    private String fieldName;
    private String sensitiveLevel;
    private String sensitiveCtg1Name;
    private String sensitiveCtg2Name;
    private String sensitiveCtg3Name;

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getSensitiveCtg1Name() {
        return sensitiveCtg1Name;
    }

    public void setSensitiveCtg1Name(String sensitiveCtg1Name) {
        this.sensitiveCtg1Name = sensitiveCtg1Name;
    }

    public String getSensitiveCtg2Name() {
        return sensitiveCtg2Name;
    }

    public void setSensitiveCtg2Name(String sensitiveCtg2Name) {
        this.sensitiveCtg2Name = sensitiveCtg2Name;
    }

    public String getSensitiveCtg3Name() {
        return sensitiveCtg3Name;
    }

    public void setSensitiveCtg3Name(String sensitiveCtg3Name) {
        this.sensitiveCtg3Name = sensitiveCtg3Name;
    }
}
