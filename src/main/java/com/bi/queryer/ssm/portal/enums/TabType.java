package com.bi.queryer.ssm.portal.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-20  14:48
 * @Description: 页签类型
 */
public enum TabType {

    VISIT("visit","最近查看"),
    CREATE("create","我创建的"),
    FAV("fav","我收藏的");

    private String code;

    private String name;

    TabType(String code,String name){
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

    public static TabType get(String code) {
        for (TabType tabType : values()) {
            if (tabType.getCode().equalsIgnoreCase(code)) {
                return tabType;
            }
        }
        return VISIT;
    }
}
