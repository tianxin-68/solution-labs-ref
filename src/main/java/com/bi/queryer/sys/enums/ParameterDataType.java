package com.bi.queryer.sys.enums;

/**
 * 参数数据类型
 * @author contributor
 */
public enum ParameterDataType {


    String("字符串"),
    Number("数字"),
    MultipleString("多值字符串（逗号分隔）"),
    MultipleNumber("多值数字（逗号分隔）");

    private String desc;
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

    private ParameterDataType(String desc) {
        this.desc = desc;
    }

    public static ParameterDataType get(String code) {
        for(ParameterDataType e : ParameterDataType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return String;
    }

}
