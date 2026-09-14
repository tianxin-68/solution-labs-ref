package com.bi.queryer.ssm.enums;

/**
 * @Author contributor
 * @Date 13:53 2022-12-08
 * @Description 值过滤类型
 **/
public enum FieldValueFilterType {
    exclude("不包含"),
    include("包含");

    private String desc;

    private FieldValueFilterType(String desc) {
        this.desc = desc;
    }

    public static FieldValueFilterType get(String type) {
        for(FieldValueFilterType t : values()){
            if(t.toString().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return include;
    }

    public static boolean isExclude(String type) {
        return exclude == get(type);
    }

    public static boolean isInclude(String type) {
        return include == get(type);
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
