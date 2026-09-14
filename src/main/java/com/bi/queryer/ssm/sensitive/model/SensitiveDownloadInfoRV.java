package com.bi.queryer.ssm.sensitive.model;


import com.bi.queryer.ssm.enums.DataSensitiveLevel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @description 下载敏感数据
 * @author contributor
 * @date 2021-12-14
 */
public class SensitiveDownloadInfoRV implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * template_id
     */
    private String id;

    /**
     * ctgId
     */
    private String ctgId;

    /**
     * mode
     */
    private String mode;

    /**
     * name
     */
    private String name;

    /**
     * description
     */
    private String description;

    /**
     * createdBy
     */
    private String createdBy;

    /**
     * createdTime
     */
    private String createdTime;

    /**
     * updatedBy
     */
    private String updatedBy;

    /**
     * updatedTime
     */
    private String updatedTime;

    /**
     * templateOwner
     */
    private String templateOwner;

    /**
     * config
     */
    private String config;

    /**
     * 是否个人敏感
     */
    private Boolean highSensitive;

    /**
     * 是否个人敏感
     */
    private Boolean businessSensitive;

    /**
     * 数据商业敏感类别
     */
    private List<String> businessSensitiveType;

    /**
     * 高敏感目录
     */
    private List<String> highSensitiveCategoryId;

    /**
     * 数据商业敏感目录
     */
    private List<String> businessSensitiveCategoryId;

    /**
     * 所有目录Id
     */
    private List<String> allFieldCategoryId;

    /**
     * 下载条数
     */
    private Integer rowNum;

    /**
     * 用户
     */
    private String userName;

    /**
     * 当日已经下载条数
     */
    private Integer downloadedNum;

    /**
     * 是否解密导出个人数据
     */
    private Integer decryptSensitiveField;

    /**
     * 是否达到下载总量阈值
     */
    private Boolean downloadedThreshold;

    /**
     * 下载数据
     */
    private List<DownloadChildrenContentRV> downloadData = new ArrayList<>();

    /**
     * 下载内容
     */
    private List<DownloadChildrenContentRV> downloadContent = new ArrayList<>();

    private String sensitiveLevel = DataSensitiveLevel.C1.getCode();

    public SensitiveDownloadInfoRV() {
    }

    public SensitiveDownloadInfoRV(Boolean highSensitive, Boolean businessSensitive, List<String> businessSensitiveType, List<String> highSensitiveCategoryId, String userName) {
        this.highSensitive = highSensitive;
        this.businessSensitive = businessSensitive;
        this.businessSensitiveType = businessSensitiveType;
        this.highSensitiveCategoryId = highSensitiveCategoryId;
        this.userName = userName;
    }

    public SensitiveDownloadInfoRV(Boolean highSensitive, Boolean businessSensitive, Integer rowNum, String userName, Integer downloadedNum, Integer decryptSensitiveField, Boolean downloadedThreshold, List<DownloadChildrenContentRV> downloadData, List<DownloadChildrenContentRV> downloadContent) {
        this.highSensitive = highSensitive;
        this.businessSensitive = businessSensitive;
        this.rowNum = rowNum;
        this.userName = userName;
        this.downloadedNum = downloadedNum;
        this.decryptSensitiveField = decryptSensitiveField;
        this.downloadedThreshold = downloadedThreshold;
        this.downloadData = downloadData;
        this.downloadContent = downloadContent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getTemplateOwner() {
        return templateOwner;
    }

    public void setTemplateOwner(String templateOwner) {
        this.templateOwner = templateOwner;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Boolean getHighSensitive() {
        return highSensitive;
    }

    public void setHighSensitive(Boolean highSensitive) {
        this.highSensitive = highSensitive;
    }

    public Boolean getBusinessSensitive() {
        return businessSensitive;
    }

    public void setBusinessSensitive(Boolean businessSensitive) {
        this.businessSensitive = businessSensitive;
    }

    public List<String> getBusinessSensitiveType() {
        return businessSensitiveType;
    }

    public void setBusinessSensitiveType(List<String> businessSensitiveType) {
        this.businessSensitiveType = businessSensitiveType;
    }

    public List<String> getHighSensitiveCategoryId() {
        return highSensitiveCategoryId;
    }

    public void setHighSensitiveCategoryId(List<String> highSensitiveCategoryId) {
        this.highSensitiveCategoryId = highSensitiveCategoryId;
    }

    public List<String> getBusinessSensitiveCategoryId() {
        return businessSensitiveCategoryId;
    }

    public void setBusinessSensitiveCategoryId(List<String> businessSensitiveCategoryId) {
        this.businessSensitiveCategoryId = businessSensitiveCategoryId;
    }

    public List<String> getAllFieldCategoryId() {
        return allFieldCategoryId;
    }

    public void setAllFieldCategoryId(List<String> allFieldCategoryId) {
        this.allFieldCategoryId = allFieldCategoryId;
    }

    public Integer getRowNum() {
        return rowNum;
    }

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getDownloadedNum() {
        return downloadedNum;
    }

    public void setDownloadedNum(Integer downloadedNum) {
        this.downloadedNum = downloadedNum;
    }

    public Integer getDecryptSensitiveField() {
        return decryptSensitiveField;
    }

    public void setDecryptSensitiveField(Integer decryptSensitiveField) {
        this.decryptSensitiveField = decryptSensitiveField;
    }

    public Boolean getDownloadedThreshold() {
        return downloadedThreshold;
    }

    public void setDownloadedThreshold(Boolean downloadedThreshold) {
        this.downloadedThreshold = downloadedThreshold;
    }

    public List<DownloadChildrenContentRV> getDownloadData() {
        return downloadData;
    }

    public void setDownloadData(List<DownloadChildrenContentRV> downloadData) {
        this.downloadData = downloadData;
    }

    public List<DownloadChildrenContentRV> getDownloadContent() {
        return downloadContent;
    }

    public void setDownloadContent(List<DownloadChildrenContentRV> downloadContent) {
        this.downloadContent = downloadContent;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }
}
