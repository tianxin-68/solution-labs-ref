package com.bi.queryer.ssm.enums;

/**
 * @Auther: contributor
 * @Date: 2025/10/21 19:48
 * @Description:
 */
public enum QueryResponseFormat {
    MAP("map", "以map形式返回"),
    LIST("list", "以list形式返回"),
    ;

    private String code;

    private String desc;

    QueryResponseFormat(String code, String desc) {
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

    public static QueryResponseFormat get(String code) {
        for (QueryResponseFormat format : values()) {
            if (format.getCode().equalsIgnoreCase(code)) {
                return format;
            }
        }
        return MAP;
    }
}
