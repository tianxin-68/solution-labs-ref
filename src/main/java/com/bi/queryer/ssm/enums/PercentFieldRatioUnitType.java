package com.bi.queryer.ssm.enums;

/**
 * @Author: contributor
 * @CreateTime: 2024-01-16  14:11
 * @Description: 百分比指标差异率单位
 */
public enum PercentFieldRatioUnitType {

    PT("pt"),
    PERCENT("%");

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    PercentFieldRatioUnitType(String code) {
        this.code = code;
    }

    public static PercentFieldRatioUnitType get(String code) {
        for (PercentFieldRatioUnitType t : values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return PT;
    }

}
