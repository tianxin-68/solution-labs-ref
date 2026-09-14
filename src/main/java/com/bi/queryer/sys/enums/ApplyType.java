package com.bi.queryer.sys.enums;

/**
 * 申请类型
 * @author contributor
 */
public enum ApplyType {

    New("新增"),
    Update("更新"),
    Offline("下线"),
    Recovery("恢复");

    private String desc;
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    private ApplyType(String desc) {
        this.desc = desc;
    }

    public static ApplyType get(String code) {
        for(ApplyType e : ApplyType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return New;
    }
}
