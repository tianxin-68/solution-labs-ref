package com.bi.queryer.ssm.portal.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  14:29
 * @Description: 资源类型
 */
public enum ResType {

    UNKNOWN("", "未知"),
    PORTAL("portal", "门户"),
    PORTAL_MENU("portal_menu", "文件夹"),
    ANALYSIS_TEMPLATE("analysis_template", "看板"),
    TX_BI("txbi","txbi"),
    PORTAL_CTG("portal_ctg","门户目录"),
    /** 专题分析报告（静态 HTML/ZIP，内容 URL 存扩展表） */
    DYNAMIC_ANALYSIS_REPORT("dynamic_analysis_report", "专题分析报告"),
    DYNAMIC_ANALYSIS_REPORT_CTG("dynamic_analysis_report_ctg", "专题分析报告目录"),
    AI_SKILL_CTG("ai_skill_ctg", "技能集目录");

    private String code;

    private String name;

    ResType(String code,String name){
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static ResType get(String code) {
        for (ResType resType : values()) {
            if (resType.getCode().equalsIgnoreCase(code)) {
                return resType;
            }
        }
        return UNKNOWN;
    }
}
