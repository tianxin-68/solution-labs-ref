package com.bi.queryer.ssm.enums;

public enum CtgDataType {

    OFFLINE("offline","离线"),
    RT("rt","实时"),
    PREDICT("predict","预测");

    CtgDataType(String code,String name){
        this.code = code;
        this.name = name;
    }

    private String code;

    private String name;

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

    public static CtgDataType get(String code) {
        for (CtgDataType c : CtgDataType.values()) {
            if (c.getCode().equalsIgnoreCase(code)) {
                return c;
            }
        }
        return OFFLINE;
    }

}
