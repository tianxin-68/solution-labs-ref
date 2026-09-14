package com.bi.queryer.ssm.portal.enums;

/**
 * 授权来源（有权限的用户清单 authSource 字段）
 */
public enum AuthSourceType {

    /** 本层个人授权 */
    INDIVIDUAL("individual", "（本层）按个人授权"),
    /** 本层组织授权展开 */
    ORG("org", "（本层）按组织授权"),
    /** 从父层继承 */
    PARENT_DIR("parent_dir", "（从父层）继承父层权限");

    private final String code;

    private final String name;

    AuthSourceType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 按 code 查找枚举，找不到返回 null
     */
    public static AuthSourceType get(String code) {
        for (AuthSourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
