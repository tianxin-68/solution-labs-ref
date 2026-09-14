package com.bi.queryer.ssm.api.enums;

public enum FilterType {
    INCLUDE("include","包含"),
    EXCLUDE("exclude","不包含"),
    DEFAULT("default","和当前视图保存的过滤方式一致\n");

    private String code;

    private String name;

    FilterType(String code,String name){
        this.code = code;
        this.name = name;
    }

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

    public static FilterType get(String code) {
        for (FilterType filterType : values()) {
            if (filterType.getCode().equalsIgnoreCase(code)) {
                return filterType;
            }
        }
        return DEFAULT;
    }
}
