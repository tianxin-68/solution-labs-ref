package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 15:51 2025/1/6
 * @Description 查询来源类型
 **/
public enum QuerySourceType {
    Analysis("analysis"),
    Query("query"),
    Export("export"),
    AGENT("agent"),
    INSPECT("inspect"),
    OLAP_API("olap_api"),
    /** AI 对话等场景：仅依赖传入的 config 执行查询 */
    CHAT("chat");

    private String code;

    QuerySourceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public static QuerySourceType get(String str) {
        for (QuerySourceType type : QuerySourceType.values()) {
            if (type.getCode().equalsIgnoreCase(str)) {
                return type;
            }
        }
        return Query;
    }
}
