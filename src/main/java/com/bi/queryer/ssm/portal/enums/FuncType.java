package com.bi.queryer.ssm.portal.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  17:13
 * @Description: 功能类型
 */
public enum FuncType {

    UNKNOWN("", "未知"),
    EDIT("edit","编辑"),
    VIEW("view","查看"),
    PUBLISH("publish","发布"),
    OFFLINE("offline","下线"),
    DELETE("delete","删除"),
    ADD_AUTH("add_auth","授权"),
    ADD_WORKER("add_worker","分配协作者");

    private String code;

    private String name;

    FuncType(String code, String name) {
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

    public static FuncType get(String code) {
        for (FuncType funcType : values()) {
            if (funcType.getCode().equalsIgnoreCase(code)) {
                return funcType;
            }
        }
        return UNKNOWN;
    }

}
