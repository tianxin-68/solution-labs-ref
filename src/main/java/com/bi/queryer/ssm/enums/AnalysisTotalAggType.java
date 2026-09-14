package com.bi.queryer.ssm.enums;

/**
 * @Auther: contributor
 * @Date: 2025/10/15 15:27
 * @Description:
 */
public enum AnalysisTotalAggType {
    DEFAULT("default", "默认", ""),
    SUM("sum", "求和", "sum(%s)"),
    AVG("avg", "平均值", "avg(%s)"),
    //平均个数（sum/默认）
    AVG_COUNT("avgCount", "平均个数", "（sum/默认）"),
    ;

    private String desc;
    private String code;

    private AnalysisTotalAggType(String code, String desc, String title) {
        this.code = code;
        this.desc = desc;
    }

    public static AnalysisTotalAggType get(String str) {
        for (AnalysisTotalAggType m : values()) {
            if (m.toString().equalsIgnoreCase(str) || m.code.equalsIgnoreCase(str)) {
                return m;
            }
        }
        return DEFAULT;
    }

    public static boolean isDefault(String str) {
        return AnalysisTotalAggType.DEFAULT == get(str);
    }

    public boolean isDefault() {
        return AnalysisTotalAggType.DEFAULT == this;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
