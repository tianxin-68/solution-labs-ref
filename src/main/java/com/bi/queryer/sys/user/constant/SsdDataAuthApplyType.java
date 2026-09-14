package com.bi.queryer.sys.user.constant;

/**
 * dataAuthList 单项操作类型：add=新增权限，delete=删除权限
 */
public enum SsdDataAuthApplyType {

    ADD("add"),
    DELETE("delete");

    private final String type;

    SsdDataAuthApplyType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static boolean isDelete(String applyType) {
        return DELETE.type.equalsIgnoreCase(applyType);
    }

    public static boolean isAdd(String applyType) {
        return applyType == null || applyType.isEmpty() || ADD.type.equalsIgnoreCase(applyType);
    }
}
