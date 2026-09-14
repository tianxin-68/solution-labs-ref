package com.bi.queryer.ssm.query.field.model;


import java.util.ArrayList;
import java.util.List;

public class FieldMetaDataReq {

    private List<QueryFieldReq> fieldList = new ArrayList<>();

    private String moduleCtgId;

    public List<QueryFieldReq> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<QueryFieldReq> fieldList) {
        this.fieldList = fieldList;
    }

    public String getModuleCtgId() {
        return moduleCtgId;
    }

    public void setModuleCtgId(String moduleCtgId) {
        this.moduleCtgId = moduleCtgId;
    }
}
