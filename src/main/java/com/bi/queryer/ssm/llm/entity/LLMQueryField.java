package com.bi.queryer.ssm.llm.entity;

import java.util.ArrayList;
import java.util.List;

public class LLMQueryField {

    private String field_code;

    private String field_title;

    private String operator;

    private List<Object> filter_values = new ArrayList<>();


    public LLMQueryField(){
    }

    public LLMQueryField(String field_code,String field_title){
        this.field_code = field_code;
        this.field_title = field_title;
    }


    public String getField_code() {
        return field_code;
    }

    public void setField_code(String field_code) {
        this.field_code = field_code;
    }

    public String getField_title() {
        return field_title;
    }

    public void setField_title(String field_title) {
        this.field_title = field_title;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Object> getFilter_values() {
        return filter_values;
    }

    public void setFilter_values(List<Object> filter_values) {
        this.filter_values = filter_values;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        return field_code.equals(((LLMQueryField)obj).getField_code());
    }
}
