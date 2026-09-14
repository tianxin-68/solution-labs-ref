package com.bi.queryer.ssm.query.template.asset.change.apply.model;

public class TemplateAssetChangeApplyReq {

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

    public Long getApplyId() {
        return applyId;
    }

    public void setApplyId(Long applyId) {
        this.applyId = applyId;
    }
}
