package com.bi.queryer.ssm.query.template.enums;

/**
 * 审批状态
 */
public enum ApprovalStatusType {

    APPLY("apply", "待审批"),
    REVOKE("revoke","撤回"),
    APPROVED("approved", "审批通过"),
    REJECTED("rejected", "审批驳回");

    private String code;
    private String desc;

    ApprovalStatusType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static ApprovalStatusType getByCode(String code) {
        for (ApprovalStatusType value : ApprovalStatusType.values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return APPLY;
    }

}
