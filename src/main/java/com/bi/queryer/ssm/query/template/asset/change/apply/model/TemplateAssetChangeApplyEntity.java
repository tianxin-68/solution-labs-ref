package com.bi.queryer.ssm.query.template.asset.change.apply.model;

public class TemplateAssetChangeApplyEntity {

    /**
     * 申请ID
     */
    private Long applyId;

    /**
     * 模板ID
     */
    private String tplId;

    /**
     * 视图ID
     */
    private String viewId;

    /**
     * 申请说明
     */
    private String applyRemark;

    /**
     * 审批状态
     */
    private String approvalStatus;

    /**
     * 审批说明
     */
    private String approvalRemark;

    /**
     * 审批人
     */
    private String approvedBy;

    /**
     * 审批时间
     */
    private String approvedTime;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private String createdTime;

    /**
     * 修改人
     */
    private String updatedBy;

    /**
     * 申请等待时间
     */
    private String applyPendingTime;

    public Long getApplyId() {
        return applyId;
    }

    public void setApplyId(Long applyId) {
        this.applyId = applyId;
    }

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getApplyRemark() {
        return applyRemark;
    }

    public void setApplyRemark(String applyRemark) {
        this.applyRemark = applyRemark;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public void setApprovalRemark(String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
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

    public String getApplyPendingTime() {
        return applyPendingTime;
    }

    public void setApplyPendingTime(String applyPendingTime) {
        this.applyPendingTime = applyPendingTime;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getApprovedTime() {
        return approvedTime;
    }

    public void setApprovedTime(String approvedTime) {
        this.approvedTime = approvedTime;
    }
}
