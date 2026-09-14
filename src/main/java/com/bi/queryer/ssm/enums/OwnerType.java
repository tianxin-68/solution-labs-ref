package com.bi.queryer.ssm.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-29  16:38
 * @Description: 权限主体类型枚举
 */
public enum OwnerType {

    USER("user", "用户"),
    DEPT("dept", "组织"),
    ROLE("role", "角色");

    OwnerType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private String code;

    private String name;

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

    public static OwnerType get(String code) {
        for (OwnerType t : values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return USER;
    }

}
