package com.bi.queryer.ssm.query.template.enums;

public enum TemplateViewType {

    UNKNOW("", "未知"),
    PUBLIC("public", "公共视图"),
    PERSONAL("personal", "个人视图");

    private String code;

    private String desc;

    TemplateViewType(String code, String desc) {
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

    public static TemplateViewType get(String code) {
        for (TemplateViewType templateViewType : TemplateViewType.values()) {
            if (templateViewType.getCode().equalsIgnoreCase(code)) {
                return templateViewType;
            }
        }
        return UNKNOW;
    }

}
