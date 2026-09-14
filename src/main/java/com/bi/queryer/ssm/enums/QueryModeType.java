package com.bi.queryer.ssm.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-22  16:54
 * @Description: 查询模式
 */
public enum QueryModeType {

    ALL("all", "查询所有"),
    COLUMNS("columns", "仅查表头"),
    MOCK_DATA("mock_data", "模拟数据"),
    VAILD_COLUMNS("vaild_columns", "验证字段");

    private String code;

    private String desc;

    QueryModeType(String code, String desc) {
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

    public static QueryModeType get(String code) {
        for (QueryModeType queryModeType : values()) {
            if (queryModeType.getCode().equalsIgnoreCase(code)) {
                return queryModeType;
            }
        }
        return ALL;
    }

}
