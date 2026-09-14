package com.bi.queryer.ssm.portal.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  16:22
 * @Description: 角色类型
 */
public enum RoleType {

    UNKNOWN("", "未知"),
    PORTAL_ADMIN("portal_admin", "门户管理员"),
    PORTAL_WORKER("portal_worker", "门户协作者"),
    PORTAL_VIEWER("portal_viewer", "门户查看者");

    private String code;

    private String name;

    RoleType(String code, String name) {
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

    public static RoleType get(String code) {
        for (RoleType roleType : values()) {
            if (roleType.getCode().equalsIgnoreCase(code)) {
                return roleType;
            }
        }
        return UNKNOWN;
    }

}
