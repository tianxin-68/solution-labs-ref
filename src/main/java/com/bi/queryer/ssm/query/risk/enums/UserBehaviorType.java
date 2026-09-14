package com.bi.queryer.ssm.query.risk.enums;

public enum UserBehaviorType {

    QUERY("query","访问"),
    DOWNLOAD("download","下载");


    UserBehaviorType(String code,String name){
        this.code = code;
        this.name = name;
    }

    private String name;

    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
