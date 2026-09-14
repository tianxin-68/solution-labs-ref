package com.bi.queryer.ssm.engine.config;

import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.alibaba.fastjson.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 10:09 2023-10-13
 * @Description 中观
 **/
public class QueryMesoscopic {
    private List<QueryField> fields = new ArrayList<QueryField>();

    public List<QueryField> getFields() {
        return fields;
    }

    public void setFields(List<QueryField> fields) {
        this.fields = fields;
    }

    public void load(JSONArray elements) {
        if(elements == null || elements.isEmpty()){
            return;
        }
        for(int i = 0; i < elements.size(); i++){
            QueryField field = elements.getObject(i, QueryField.class);
            fields.add(field);
        }
    }
}
