package com.bi.queryer.ssm.portal.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  16:55
 * @Description: 菜单类型
 */
public enum PortalMenuType {

    UNKNOWN("", "未知"),
    COMMON("common","普通目录"),
    ANALYSIS_TEMPLATE("analysis_template","看板"),
    TX_BI("txbi","txbi"),
    SPACE_CTG("space_ctg","共享空间目录"),
    CONFIG_USED_SPACE_CTG("config_used_space_ctg","看板配置使用的共享空间目录"),
    QUERY_TEMPLATE_CTG("query_template_ctg","查询模板目录"),
    PORTAL_CTG("portal_ctg","门户目录"),
    /** 专题分析报告（静态 HTML/ZIP，内容 URL 存扩展表） */
    DYNAMIC_ANALYSIS_REPORT("dynamic_analysis_report", "专题分析报告"),
    DYNAMIC_ANALYSIS_REPORT_CTG("dynamic_analysis_report_ctg", "专题分析报告目录"),
    AI_SKILL_CTG("ai_skill_ctg", "技能集目录");

    private String code;
    private String name;

    PortalMenuType(String code, String name){
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

    public static PortalMenuType get(String code) {
        for (PortalMenuType menuType : values()) {
            if (menuType.getCode().equalsIgnoreCase(code)) {
                return menuType;
            }
        }
        return UNKNOWN;
    }

}
