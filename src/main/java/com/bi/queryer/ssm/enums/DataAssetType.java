package com.bi.queryer.ssm.enums;

/**
 * 数据资产类型
 */
public enum DataAssetType {

    UNKNOWN("", "未知"),
    QUERY_TEMPLATE("query_template", "查询模板"),
    ANALYSIS_TEMPLATE("analysis_template", "分析看板"),
    MULTI_QUERY_TEMPLATE("multi_query_template", "多模板看板");

    private String code;
    private String desc;

    DataAssetType(String code, String desc) {
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

    public static DataAssetType get(String code) {
        for (DataAssetType value : DataAssetType.values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
