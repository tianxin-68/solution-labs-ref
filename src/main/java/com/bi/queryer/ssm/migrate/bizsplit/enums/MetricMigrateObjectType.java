package com.bi.queryer.ssm.migrate.bizsplit.enums;

/**
 * 迁移映射表（ssm_migrate_bizsplit_mapping）里 object_type 的取值（执行计划第5节）。
 */
public enum MetricMigrateObjectType {

    /** 门户本身（ssm_portal）：sourceRootName 等于门户名称时，整个门户连同其菜单树一起复制 */
    PORTAL("portal"),

    /** 门户目录节点（ssm_portal_menu），涵盖普通目录、专题报告、AI技能目录、共享空间菜单节点、看板菜单节点等 */
    PORTAL_MENU("portal_menu"),

    /** 看板主体（ssm_analysis_tpl_base） */
    ANALYSIS_TPL("analysis_tpl"),

    /** 查询模板（ssd_query_template） */
    QUERY_TPL("query_tpl"),

    /** 查询模板视图（ssd_query_template_view） */
    QUERY_TPL_VIEW("query_tpl_view"),

    /** 看板视图/页签（ssm_analysis_tpl_view），与查询模板视图是两个不同的东西，不要混用 QUERY_TPL_VIEW */
    ANALYSIS_TPL_VIEW("analysis_tpl_view"),

    /** 共享空间/查询模板目录节点（ssd_query_template_ctg） */
    SPACE_CTG("space_ctg");

    private final String code;

    MetricMigrateObjectType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MetricMigrateObjectType get(String code) {
        for (MetricMigrateObjectType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
