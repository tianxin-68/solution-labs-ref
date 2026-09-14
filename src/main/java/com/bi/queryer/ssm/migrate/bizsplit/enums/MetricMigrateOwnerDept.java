package com.bi.queryer.ssm.migrate.bizsplit.enums;

/**
 * 看板工作台目录迁移涉及的业务线（老业务线 + 拆分后的新业务线）。
 * 见 doc/02设计/看板工作台目录迁移-业务线拆分执行计划.md 第1节。
 */
public enum MetricMigrateOwnerDept {

    MAINTENANCE("maintenance", "保养"),
    MAINTENANCE_OIL("maintenance_oil", "保养油液"),
    MAINTENANCE_PARTS("maintenance_parts", "保养配件"),
    SUPERMARKET_REFIT("supermarket_refit", "超市改装"),
    ELECTRONIC_REFIT("electronic_refit", "电子改装"),
    BATTERY_CART("battery_cart", "电瓶车");

    private final String code;
    private final String bizLineName;

    MetricMigrateOwnerDept(String code, String bizLineName) {
        this.code = code;
        this.bizLineName = bizLineName;
    }

    public String getCode() {
        return code;
    }

    public String getBizLineName() {
        return bizLineName;
    }

    public static MetricMigrateOwnerDept get(String code) {
        for (MetricMigrateOwnerDept dept : values()) {
            if (dept.code.equalsIgnoreCase(code)) {
                return dept;
            }
        }
        return null;
    }
}
