package com.bi.queryer.ssm.meta;


import com.bi.queryer.ssm.enums.DataSensitiveLevel;

import java.io.Serializable;
import java.util.List;

/**
 * @description 下载导出类
 * @author contributor
 * @date 2021-08-17
 */
public class SSDExportBaseRV implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * userName
     */
    private String userName;

    /**
     * businessId
     */
    private String businessId;

    /**
     * businessSource
     */
    private String businessSource = "SSM";

    /**
     * businessModule
     */
    private String businessModule = "查询数据导出";

    /**
     * callbackUrl
     */
    private String callbackUrl;

    /**
     * fileName
     */
    private String fileName;

    /**
     * remark
     */
    private String remark;

    /**
     * dsId
     */
    private String dsId;

    /**
     * querySql
     */
    private String querySql;

    /**
     * noticeChannel
     */
    private String noticeChannel = "all";

    /**
     * isNoticeLeader
     */
    private Integer isNoticeLeader = 0;

    /**
     * isNoticeLeader
     */
    private String noticeSign = "多维分析下载";

    /**
     * 字段信息
     */
    private List<SSDExportFieldInfo> fieldInfos;

    /**
     * rptRows
     */
    private Integer rptRows;

    /**
     * fileId
     */
    private String fileId;

    /**
     * fileSize
     */
    private Double fileSize;

    /**
     * fileUrl
     */
    private String fileUrl;

    /**
     * export_exec_info
     */
    private String message;

    /**
     * export_status
     */
    private String success;

    /**
     * auths
     */
    private List<String> auths;

    /**
     * 导出状态
     */
    private String status;

    /**
     * 是否要申请
     */
    private Boolean needApply;

    /**
     * 是否个人敏感
     */
    private Boolean personSensitive;

    /**
     * 是否个人敏感
     */
    private Boolean businessSensitive;

    /**
     * 数据敏感类别
     */
    private String sensitiveType;

    /**
     * 下载数据
     */
    private String downloadData;

    /**
     * 下载内容
     */
    private String downloadContent;

    /**
     * 申请人
     */
    private String applicant;

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
     * 申请原因
     */
    private String downloadRemark;

    /**
     * 个人敏感是否解密
     */
    private Integer decryptSensitiveField;

    /**
     * 是否需要工单审批
     */
    private Integer isApplyApprove;

    /**
     * 模块owner邮箱
     */
    private String moduleOwnerEmail;

    /**
     * 数据敏感级别
     */
    private String sensitiveLevel = DataSensitiveLevel.C1.getCode();

    public SSDExportBaseRV() {
    }

    public SSDExportBaseRV(String userName, String businessId, String callbackUrl, String fileName, String dsId, String querySql, Integer decryptSensitiveField) {
        this.userName = userName;
        this.businessId = businessId;
        this.callbackUrl = callbackUrl;
        this.fileName = fileName;
        this.dsId = dsId;
        this.querySql = querySql;
        this.decryptSensitiveField = decryptSensitiveField;
    }

    @Override
    public String toString() {
        return "SSDExportBaseRV{" +
                "businessId='" + businessId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", rptRows=" + rptRows +
                ", message='" + message + '\'' +
                ", success='" + success + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessSource() {
        return businessSource;
    }

    public void setBusinessSource(String businessSource) {
        this.businessSource = businessSource;
    }

    public String getBusinessModule() {
        return businessModule;
    }

    public void setBusinessModule(String businessModule) {
        this.businessModule = businessModule;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getDsId() {
        return dsId;
    }

    public void setDsId(String dsId) {
        this.dsId = dsId;
    }

    public String getQuerySql() {
        return querySql;
    }

    public void setQuerySql(String querySql) {
        this.querySql = querySql;
    }

    public String getNoticeChannel() {
        return noticeChannel;
    }

    public void setNoticeChannel(String noticeChannel) {
        this.noticeChannel = noticeChannel;
    }

    public Integer getIsNoticeLeader() {
        return isNoticeLeader;
    }

    public void setIsNoticeLeader(Integer isNoticeLeader) {
        this.isNoticeLeader = isNoticeLeader;
    }

    public String getNoticeSign() {
        return noticeSign;
    }

    public void setNoticeSign(String noticeSign) {
        this.noticeSign = noticeSign;
    }

    public List<SSDExportFieldInfo> getFieldInfos() {
        return fieldInfos;
    }

    public void setFieldInfos(List<SSDExportFieldInfo> fieldInfos) {
        this.fieldInfos = fieldInfos;
    }

    public Integer getRptRows() {
        return rptRows;
    }

    public void setRptRows(Integer rptRows) {
        this.rptRows = rptRows;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Double getFileSize() {
        return fileSize;
    }

    public void setFileSize(Double fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public List<String> getAuths() {
        return auths;
    }

    public void setAuths(List<String> auths) {
        this.auths = auths;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getNeedApply() {
        return needApply;
    }

    public void setNeedApply(Boolean needApply) {
        this.needApply = needApply;
    }

    public Boolean getPersonSensitive() {
        return personSensitive;
    }

    public void setPersonSensitive(Boolean personSensitive) {
        this.personSensitive = personSensitive;
    }

    public Boolean getBusinessSensitive() {
        return businessSensitive;
    }

    public void setBusinessSensitive(Boolean businessSensitive) {
        this.businessSensitive = businessSensitive;
    }

    public String getSensitiveType() {
        return sensitiveType;
    }

    public void setSensitiveType(String sensitiveType) {
        this.sensitiveType = sensitiveType;
    }

    public String getDownloadData() {
        return downloadData;
    }

    public void setDownloadData(String downloadData) {
        this.downloadData = downloadData;
    }

    public String getDownloadContent() {
        return downloadContent;
    }

    public void setDownloadContent(String downloadContent) {
        this.downloadContent = downloadContent;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
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

    public String getSecurityEMail() {
        return securityEMail;
    }

    public void setSecurityEMail(String securityEMail) {
        this.securityEMail = securityEMail;
    }

    public String getDownloadRemark() {
        return downloadRemark;
    }

    public void setDownloadRemark(String downloadRemark) {
        this.downloadRemark = downloadRemark;
    }

    public Integer getDecryptSensitiveField() {
        return decryptSensitiveField;
    }

    public void setDecryptSensitiveField(Integer decryptSensitiveField) {
        this.decryptSensitiveField = decryptSensitiveField;
    }

    public Integer getIsApplyApprove() {
        return isApplyApprove;
    }

    public void setIsApplyApprove(Integer isApplyApprove) {
        this.isApplyApprove = isApplyApprove;
    }

    public String getModuleOwnerEmail() {
        return moduleOwnerEmail;
    }

    public void setModuleOwnerEmail(String moduleOwnerEmail) {
        this.moduleOwnerEmail = moduleOwnerEmail;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }
}