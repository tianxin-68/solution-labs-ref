package com.bi.queryer.ssm.enums;

public enum ChatBusinessType {

    QUERY_TEMPLATE("query_template", "查询模板"),
    ANALYSIS_TEMPLATE("analysis_template", "分析模板"),
    ANALYSIS_TMP_TEMPLATE("analysis_tmp_template", "个人多模板");

    private String code;

    private String desc;

    private ChatBusinessType(String code, String desc) {
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

    public static ChatBusinessType get(String code) {
        for (ChatBusinessType value : ChatBusinessType.values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return QUERY_TEMPLATE;
    }
}
