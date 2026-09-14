package com.bi.queryer.ssm.governance.enums;

/**
 * 治理策略类型
 */
public enum GovPolicyType {

    /**
     * 视图：N 天内无访问下线
     */
    NO_VISIT("no_visit", "超过%d天无查询下线", "N 天内无任何用户查询"),

    /**
     * 视图：已离职 + 90天未访问 + 个人视图，快速清理（直接下线，跳过通知）
     */
    OWNER_LEFT_NO_VISIT("owner_left_no_visit", "owner离职+超过%d天未查询快速清理", "责任人全部离职 且 180天无查询 且 个人视图"),

    /**
     * 指标/维度：N 天内无引用
     */
    FIELD_NO_VISIT("field_no_visit", "超过%d天无查询下线", "近 N 天在多维查询日志中无引用");

    private final String code;

    private final String name;

    private final String desc;

    GovPolicyType(String code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getName(Integer day) {
        return String.format(name, day);
    }

    public String getDesc() {
        return desc;
    }

    public static GovPolicyType get(String code) {
        for (GovPolicyType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
