package com.bi.queryer.ssm.query.template.asset.change.apply.model;

import com.bi.queryer.sys.enums.Enabled;

public class TemplateAssetChangeApplyCreateRsp {

    /**
     * 申请ID
     */
    private Long applyId;

    /**
     * 是否自动审批
     */
    private Integer isAutoApproval = Enabled.NO.getId();


    public Long getApplyId() {
        return applyId;
    }

    public void setApplyId(Long applyId) {
        this.applyId = applyId;
    }

    public Integer getIsAutoApproval() {
        return isAutoApproval;
    }

    public void setIsAutoApproval(Integer isAutoApproval) {
        this.isAutoApproval = isAutoApproval;
    }
}
