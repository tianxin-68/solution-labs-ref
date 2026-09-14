package com.bi.queryer.ssm.engine.model.creator;

import com.bi.queryer.ssm.engine.model.StarModel;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author contributor
 * @Date 18:12 2024-04-15
 * @Description 无效模型
 **/
public class InvalidModel {
    private StarModel model;

    private Set<String> fieldCodes = new HashSet<>();

    public InvalidModel(StarModel model) {
        this.model = model;
    }

    public Set<String> getFieldCodes() {
        return fieldCodes;
    }

    public void setFieldCodes(Set<String> fieldCodes) {
        this.fieldCodes = fieldCodes;
    }

    public StarModel getModel() {
        return model;
    }

    public void setModel(StarModel model) {
        this.model = model;
    }
}
