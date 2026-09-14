package com.bi.queryer.ssm.migrate.bizsplit.enums;

/**
 * 目录/看板/查询模板名称的复制命名规则（执行计划第4节）。
 */
public enum MetricMigrateFilterExpandRule {

    /** 跨业务线派生：对子树内所有节点名称做 oldToken -> newToken 子串替换 */
    SUBSTRING_REPLACE("substring_replace", "整段子串替换：老业务线子串替换为新业务线子串"),

    /** 自我复制：oldToken 与 newToken 相同（不做替换），仅在根节点名称上追加 nameSuffix */
    SELF_COPY_SUFFIX_ONLY("self_copy_suffix_only", "自我复制：不替换业务线子串，仅根节点追加命名后缀");

    private final String code;
    private final String desc;

    MetricMigrateFilterExpandRule(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
