package com.bi.queryer.ssm.portal.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-22  16:33
 * @Description: 拖拽落点的类型
 */
public enum DragType {

    BEFORE("before","拖拽到上面"),
    AFTER("after","拖拽到下面");

    private String code;

    private String name;

    DragType(String code,String name){
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

    public static DragType get(String code) {
        for (DragType dragType : values()) {
            if (dragType.getCode().equalsIgnoreCase(code)) {
                return dragType;
            }
        }
        return BEFORE;
    }
}
