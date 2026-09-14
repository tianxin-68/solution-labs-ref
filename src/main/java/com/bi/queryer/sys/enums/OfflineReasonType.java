package com.bi.queryer.sys.enums;

/**
 * 下线原因类型
 * @author contributor
 */
public enum OfflineReasonType {

    NEW_RPT_REPLACE("new_rpt_replace","新报表替换"),
    RPT_NO_LONGER_IN_USER("rpt_no_longer_in_user","报表不再使用"),
    OTHER("other","其他");

    private OfflineReasonType(String code,String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OfflineReasonType get(String code) {
        for (OfflineReasonType type : OfflineReasonType.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return OTHER;
    }

    private String code;

    private String desc;

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
}
