package com.bi.queryer.ssm.governance.enums;

/**
 * 治理对象类型
 */
public enum GovObjectType {

    VIEW("view", "视图"),
    FIELD("field", "字段（指标/维度）"),
    METRIC("metric", "指标"),
    DIM("dim", "维度");

    private final String code;

    private final String desc;

    GovObjectType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static GovObjectType get(String code) {
        for (GovObjectType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
