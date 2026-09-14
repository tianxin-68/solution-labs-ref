package com.bi.queryer.ssm.portal.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-07-03  14:16
 * @Description: 门户模块枚举
 */
public enum PortalModuleType {

    UNKNOWN("", "未知"),
    PORTAL("portal","业务门户"),
    SSM("ssm","多维分析"),
    STUDIO("studio","工作台");

    private String code;
    private String name;

    PortalModuleType(String code, String name){
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

    public static PortalModuleType get(String code) {
        for (PortalModuleType menuCodeType : values()) {
            if (menuCodeType.getCode().equalsIgnoreCase(code)) {
                return menuCodeType;
            }
        }
        return UNKNOWN;
    }
}
