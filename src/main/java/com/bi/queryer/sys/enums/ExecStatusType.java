package com.bi.queryer.sys.enums;

/**
 * 执行状态枚举
 * @author contributor
 */
public enum ExecStatusType {

    Save("已保存，未申请"),
    Apply("已申请，待审批"),
    Approved("审批通过，已上线"),
    ApprovedOffline("审批通过，已下线"),
    ApprovalFailed("审批未通过，已驳回"),
    Revoke("已撤销"),
    Delete("已删除");


    private String desc;
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    private ExecStatusType(String desc) {
        this.desc = desc;
    }

    public static ExecStatusType get(String code) {
        for(ExecStatusType e : ExecStatusType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return Save;
    }
}
