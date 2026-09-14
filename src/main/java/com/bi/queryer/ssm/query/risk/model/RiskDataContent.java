package com.bi.queryer.ssm.query.risk.model;

import java.util.ArrayList;
import java.util.List;

public class RiskDataContent {

    private List<RiskDataField> dataFields = new ArrayList<>();

    public List<RiskDataField> getDataFields() {
        return dataFields;
    }

    public void setDataFields(List<RiskDataField> dataFields) {
        this.dataFields = dataFields;
    }
}
