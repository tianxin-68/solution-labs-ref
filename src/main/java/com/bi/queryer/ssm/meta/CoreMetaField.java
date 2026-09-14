package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataSensitiveLevel;
import com.bi.queryer.util.BIUtil;

/**
 * @Author contributor
 * @Date 18:31 2025/12/23
 * @Description TODO
 **/
public class CoreMetaField {
    private String pkid;
    private String coreType;
    private String fieldTitle;
    private String fieldType;
    private String fieldCode;
    private String sensitiveLevel ;
    private String createdTime;
    private String updatedTime;
    private String createdBy;
    private String updatedBy;

    public String getPkid() {
        return pkid;
    }

    public void setPkid(String pkid) {
        this.pkid = pkid;
    }

    public String getCoreType() {
        return coreType;
    }

    public void setCoreType(String coreType) {
        this.coreType = coreType;
    }

    public String getFieldTitle() {
        return fieldTitle;
    }

    public void setFieldTitle(String fieldTitle) {
        this.fieldTitle = fieldTitle;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getSensitiveLevel() {
        if(BIUtil.isEmpty(sensitiveLevel)){
            sensitiveLevel = DataSensitiveLevel.C4.getCode();
        }
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
