package com.bi.queryer.ssm.query.template.asset.change.apply.model;

public class TemplateAssetChangeApplyRsp {

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
     * 视图名称
     */
    private String viewName;

    /**
     * 申请人
     */
    private String applyUser;

    /**
     * 申请人名称
     */
    private String applyUserName;

    /**
     * 申请说明
     */
    private String applyRemark;

    /**
     * 申请时间
     */
    private String applyTime;

    /**
     * 申请等待时间
     */
    private String applyPendingTime ;

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
     * 审批人名称
     */
    private String approvedUserName;


    /**
     * 审批时间
     */
    private String approvedTime;


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

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getApplyUser() {
        return applyUser;
    }

    public void setApplyUser(String applyUser) {
        this.applyUser = applyUser;
    }

    public String getApplyRemark() {
        return applyRemark;
    }

    public void setApplyRemark(String applyRemark) {
        this.applyRemark = applyRemark;
    }

    public String getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(String applyTime) {
        this.applyTime = applyTime;
    }

    public String getApplyPendingTime() {
        return applyPendingTime;
    }

    public void setApplyPendingTime(String applyPendingTime) {
        this.applyPendingTime = applyPendingTime;
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

    public String getApprovedTime() {
        return approvedTime;
    }

    public void setApprovedTime(String approvedTime) {
        this.approvedTime = approvedTime;
    }

    public String getApplyUserName() {
        return applyUserName;
    }

    public void setApplyUserName(String applyUserName) {
        this.applyUserName = applyUserName;
    }

    public String getApprovedUserName() {
        return approvedUserName;
    }

    public void setApprovedUserName(String approvedUserName) {
        this.approvedUserName = approvedUserName;
    }
}


