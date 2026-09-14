package com.bi.queryer.ssm.query.field.model;

import java.util.ArrayList;
import java.util.List;

public class FieldDataUpdateTimeReq {

    private String datasetId;

    private String fieldCode;

    private List<String> queryTableNames = new ArrayList<>();


    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public List<String> getQueryTableNames() {
        return queryTableNames;
    }

    public void setQueryTableNames(List<String> queryTableNames) {
        this.queryTableNames = queryTableNames;
    }
}
