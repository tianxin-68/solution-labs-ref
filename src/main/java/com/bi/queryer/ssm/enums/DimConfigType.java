package com.bi.queryer.ssm.enums;

public enum DimConfigType {

    AUTO("auto","自动支持模版中可支持维度"),
    MANUAL("manual","手动指定维度");

    DimConfigType(String code,String desc){
        this.code = code;
        this.desc = desc;
    }

    private String code;

    private String desc;

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

    public static DimConfigType get(String code) {
        for (DimConfigType dimConfigType : values()) {
            if (dimConfigType.getCode().equalsIgnoreCase(code)) {
                return dimConfigType;
            }
        }
        return MANUAL;
    }

}
