package com.bi.queryer.ssm.query.template.enums;

/**
 * @Auther: contributor
 * @Date: 2025/9/16 14:00
 * @Description:
 */
public enum FavTemplateType {
    QUERY_TEMPLATE("query_template", "查询模板"),
    CTG("ctg", "目录"),
    ANALYSIS_TEMPLATE("analysis_template", "看板"),
    TMP_ANALYSIS_TEMPLATE("tmp_analysis_template", "临时看板");

    private String code;

    private String desc;

    FavTemplateType(String code, String desc) {
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
}
