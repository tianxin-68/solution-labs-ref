package com.bi.queryer.ssm.query.template.enums;

/**
 * 视图状态枚举
 */
public enum ViewStatusType {

    ACTIVE("active","生效"),
    TEMPORARY("temp","临时");

    private String code;
    private String desc;

    ViewStatusType(String code, String desc) {
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

    public static ViewStatusType getByCode(String code) {
        for (ViewStatusType value : ViewStatusType.values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return TEMPORARY;
    }

}