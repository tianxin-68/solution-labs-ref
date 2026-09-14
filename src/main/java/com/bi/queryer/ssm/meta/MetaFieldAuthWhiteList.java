package com.bi.queryer.ssm.meta;

import java.util.Date;

/**
 * @Author contributor
 * @Date 13:56 2025/11/26
 * @Description TODO
 **/
public class MetaFieldAuthWhiteList implements Cloneable{
    private String pkid; // 主键
    private String userName;
    private String fieldGroup;
    private String fieldCode;
    private String fieldTitle;
    private String createdBy; // 创建人
    private Date createdTime; // 创建时间

    public MetaFieldAuthWhiteList() {
    }

    public MetaFieldAuthWhiteList(String userName, String fieldGroup, String fieldCode, String fieldTitle) {
        this.userName = userName;
        this.fieldGroup = fieldGroup;
        this.fieldCode = fieldCode;
        this.fieldTitle = fieldTitle;
    }

    public String getPkid() {
        return pkid;
    }

    public void setPkid(String pkid) {
        this.pkid = pkid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFieldGroup() {
        return fieldGroup;
    }

    public void setFieldGroup(String fieldGroup) {
        this.fieldGroup = fieldGroup;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldTitle() {
        return fieldTitle;
    }

    public void setFieldTitle(String fieldTitle) {
        this.fieldTitle = fieldTitle;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public String toString() {
        return"pkid='" + pkid + '\'' +
                ", userName='" + userName + '\'' +
                ", fieldGroup='" + fieldGroup + '\'' +
                ", fieldCode='" + fieldCode + '\'' +
                ", fieldTitle='" + fieldTitle + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdTime=" + createdTime;
    }
}
