package com.bi.queryer.ssm.query.field.model;

public class QueryFieldReq {

    private String id;

    private String code;

    //模块目录id
    private String moduleCtgId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModuleCtgId() {
        return moduleCtgId;
    }

    public void setModuleCtgId(String moduleCtgId) {
        this.moduleCtgId = moduleCtgId;
    }
}
