package com.bi.queryer.ssm.enums;

/**
 * @Auther: contributor
 * @Date: 2025/9/11 14:49
 * @Description:
 */
public enum UserProfileTypeEnum {
    DEFAULT_PORTAL("defaultPortal", "默认门户"),
    ;

    UserProfileTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private final String code;

    private final String name;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static UserProfileTypeEnum get(String code) {
        for (UserProfileTypeEnum t : values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return DEFAULT_PORTAL;
    }

}
