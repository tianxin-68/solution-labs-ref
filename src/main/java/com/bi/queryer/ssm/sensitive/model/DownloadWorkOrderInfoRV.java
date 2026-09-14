package com.bi.queryer.ssm.sensitive.model;


import com.bi.queryer.ssm.enums.DataSensitiveLevel;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description 下载工单信息表
 * @author contributor
 * @date 2021-12-14
 */
public class DownloadWorkOrderInfoRV implements Serializable {

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
     * 说明
     */
    private String workOrderRemark = "";

    /**
     * 申请人邮箱
     */
    private String applicantEMail;

    /**
     * 申请人
     */
    private String applicant;

    /**
     * 申请人
     */
    private String applicantRealName;

    /**
     * 申请人部门
     */
    private String applicantDept;

    /**
     * 部门领导邮箱
     */
    private String deptLeaderEMail;

    /**
     * BI/DW审批人
     */
    private String approverEMail;

    /**
     * 安全部门邮箱
     */
    private String securityEMail;

    /**
     * 是否敏感
     */
    private Boolean needApply = false;

    /**
     * 是否个人敏感
     */
    private Boolean highSensitive;

    /**
     * 是否个人敏感
     */
    private Boolean businessSensitive;

    /**
     * 商业数据敏感类别
     */
    private List<String> businessSensitiveType;

    /**
     * 数据个人敏感目录
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
     * 下载数据
     */
    private List<DownloadChildrenContentRV> downloadData;

    /**
     * 下载条数
     */
    private Integer rowNum;

    /**
     * 下载内容
     */
    private List<DownloadChildrenContentRV> downloadContent;

    /**
     * 当日已经下载条数
     */
    private Integer downloadedNum;

    /**
     * 是否达到下载总量阈值
     */
    private Boolean downloadedThreshold;

    /**
     * 个人敏感是否解密
     */
    private Integer decryptSensitiveField;

    /**
     * 申请原因
     */
    private String downloadRemark;

    /**
     * 导出或复制的数据集字符串
     */
    private String datasetString;

    /**
     * 模块owner邮箱
     */
    private String moduleOwnerEmail;

    /**
     * 数据敏感级别
     */
    private String sensitiveLevel = DataSensitiveLevel.C1.getCode();


    public String toSaveDBConfig(){
        Map<String, Object> downloadWorkOrderInfoRVMap = new HashMap<>();
        downloadWorkOrderInfoRVMap.put("businessSensitiveType", this.getBusinessSensitiveType());
        downloadWorkOrderInfoRVMap.put("updatedTime", this.getUpdatedTime());
        downloadWorkOrderInfoRVMap.put("updatedBy", this.getUpdatedBy());
        downloadWorkOrderInfoRVMap.put("applicantDept", this.getApplicantDept());
        downloadWorkOrderInfoRVMap.put("description", this.getDescription());
        downloadWorkOrderInfoRVMap.put("mode", this.getMode());
        downloadWorkOrderInfoRVMap.put("applicantEMail", this.getApplicantEMail());
        downloadWorkOrderInfoRVMap.put("applicantRealName", this.getApplicantRealName());
        downloadWorkOrderInfoRVMap.put("approverEMail", this.getApproverEMail());
        downloadWorkOrderInfoRVMap.put("createdBy", this.getCreatedBy());
        downloadWorkOrderInfoRVMap.put("deptLeaderEMail",this.getDeptLeaderEMail() );
        downloadWorkOrderInfoRVMap.put("rowNum", this.getRowNum());
        downloadWorkOrderInfoRVMap.put("ctgId", this.getCtgId());
        downloadWorkOrderInfoRVMap.put("name", this.getName());
        downloadWorkOrderInfoRVMap.put("createdTime", this.getCreatedTime());
        downloadWorkOrderInfoRVMap.put("securityEMail", this.getSecurityEMail());
        downloadWorkOrderInfoRVMap.put("id", this.getId());
        downloadWorkOrderInfoRVMap.put("templateOwner", this.getTemplateOwner());
        downloadWorkOrderInfoRVMap.put("config", this.getConfig());
        return JSONObject.toJSONString(downloadWorkOrderInfoRVMap);
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

    public String getWorkOrderRemark() {
        return workOrderRemark;
    }

    public void setWorkOrderRemark(String workOrderRemark) {
        this.workOrderRemark = workOrderRemark;
    }

    public String getApplicantEMail() {
        return applicantEMail;
    }

    public void setApplicantEMail(String applicantEMail) {
        this.applicantEMail = applicantEMail;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public String getApplicantRealName() {
        return applicantRealName;
    }

    public void setApplicantRealName(String applicantRealName) {
        this.applicantRealName = applicantRealName;
    }

    public String getApplicantDept() {
        return applicantDept;
    }

    public void setApplicantDept(String applicantDept) {
        this.applicantDept = applicantDept;
    }

    public String getDeptLeaderEMail() {
        return deptLeaderEMail;
    }

    public void setDeptLeaderEMail(String deptLeaderEMail) {
        this.deptLeaderEMail = deptLeaderEMail;
    }

    public String getApproverEMail() {
        return approverEMail;
    }

    public void setApproverEMail(String approverEMail) {
        this.approverEMail = approverEMail;
    }

    public String getModuleOwnerEmail() {
        return moduleOwnerEmail;
    }

    public void setModuleOwnerEmail(String moduleOwnerEmail) {
        this.moduleOwnerEmail = moduleOwnerEmail;
    }

    public String getSecurityEMail() {
        return securityEMail;
    }

    public void setSecurityEMail(String securityEMail) {
        this.securityEMail = securityEMail;
    }

    public Boolean getNeedApply() {
        return needApply;
    }

    public void setNeedApply(Boolean needApply) {
        this.needApply = needApply;
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

    public List<DownloadChildrenContentRV> getDownloadData() {
        return downloadData;
    }

    public void setDownloadData(List<DownloadChildrenContentRV> downloadData) {
        this.downloadData = downloadData;
    }

    public Integer getRowNum() {
        return rowNum;
    }

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    public List<DownloadChildrenContentRV> getDownloadContent() {
        return downloadContent;
    }

    public void setDownloadContent(List<DownloadChildrenContentRV> downloadContent) {
        this.downloadContent = downloadContent;
    }

    public Integer getDownloadedNum() {
        return downloadedNum;
    }

    public void setDownloadedNum(Integer downloadedNum) {
        this.downloadedNum = downloadedNum;
    }

    public Boolean getDownloadedThreshold() {
        return downloadedThreshold;
    }

    public void setDownloadedThreshold(Boolean downloadedThreshold) {
        this.downloadedThreshold = downloadedThreshold;
    }

    public Integer getDecryptSensitiveField() {
        return decryptSensitiveField;
    }

    public void setDecryptSensitiveField(Integer decryptSensitiveField) {
        this.decryptSensitiveField = decryptSensitiveField;
    }

    public String getDownloadRemark() {
        return downloadRemark;
    }

    public void setDownloadRemark(String downloadRemark) {
        this.downloadRemark = downloadRemark;
    }

    public String getDatasetString() {
        return datasetString;
    }

    public void setDatasetString(String datasetString) {
        this.datasetString = datasetString;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }
}
