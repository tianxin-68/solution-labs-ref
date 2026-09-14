package com.bi.queryer.ssm.engine.model.creator;

import com.bi.queryer.ssm.engine.model.StarModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author contributor
 * @Date 17:45 2024-04-15
 * @Description 模型校验结果
 **/
public class ModelValidateResult {
    private boolean success = true;

    // 无效表+字段
    private List<InvalidModel> invalidModels = new ArrayList<>();

    // 无效模型owner
    private Set<String> invalidModelOwners = new HashSet<>();

    private List<StarModel> validModels = new ArrayList<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<InvalidModel> getInvalidModels() {
        return invalidModels;
    }

    public void setInvalidModels(List<InvalidModel> invalidModels) {
        this.invalidModels = invalidModels;
    }

    public Set<String> getInvalidModelOwners() {
        return invalidModelOwners;
    }

    public void setInvalidModelOwners(Set<String> invalidModelOwners) {
        this.invalidModelOwners = invalidModelOwners;
    }

    public List<StarModel> getValidModels() {
        return validModels;
    }

    public void setValidModels(List<StarModel> validModels) {
        this.validModels = validModels;
    }
}
