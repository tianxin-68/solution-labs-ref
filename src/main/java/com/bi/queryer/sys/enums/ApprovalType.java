package com.bi.queryer.sys.enums;

/**
 * 审批状态
 * @author contributor
 */
public enum ApprovalType {

    Agree("同意"),
    Reject("驳回");

    private String desc;
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    private ApprovalType(String desc) {
        this.desc = desc;
    }

    public static ApprovalType get(String code) {
        for(ApprovalType e : ApprovalType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return Agree;
    }
}
