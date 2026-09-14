package com.bi.queryer.ssm.engine.config.field;

import java.util.ArrayList;
import java.util.List;

public class FieldCategoryAggregationItem {
    private List<FieldValue> values =  new ArrayList<>();
    private String name = "";

    public List<FieldValue> getValues() {
        return values;
    }

    public void setValues(List<FieldValue> values) {
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
