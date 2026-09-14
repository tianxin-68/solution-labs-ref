package com.bi.queryer.ssm.query.template.enums;

public enum TemplateChangeContentType {

    DATASET("dataset", "数据集"),
    ASSET("asset","模版资产");


    private String code;

    private String name;

    TemplateChangeContentType(String code, String name) {
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

}
