package com.bi.queryer.ssm.enums;

public enum FieldType {

    UNKNOW("", "未知"),
    DIM("dim", "维度"),
    CUSTOM_DIM("custom_dim", "自定义维度"),
    ATOMIC("atomic","原子指标"),
    DERIVED("derived","派生指标"),
    COMPOUND("compound","复合指标"),
    CUSTOM_MEASURE("custom_measure","自定义指标"),
    CROSS_MODEL_MEASURE("cross_model_measure", "跨模型指标");

    private String code;

    private String desc;

    private FieldType(String code, String desc) {
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

    public static FieldType get(String type) {
        for (FieldType t : values()) {
            if (t.toString().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return UNKNOW;
    }


}
