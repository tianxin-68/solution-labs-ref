package com.bi.queryer.ssm.enums;

/**
 * 筛选对象类型
 */
public enum FieldFilterObjectType {

    ALL("all","汇总+明细"),
    DETAIL("detail","仅明细");

    private String code;
    private String desc;

    FieldFilterObjectType(String code, String desc) {
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

    public static FieldFilterObjectType get(String code) {
        for (FieldFilterObjectType t : values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return ALL;
    }

}
