package com.bi.queryer.sys.authapply.model.ssm;

import com.bi.queryer.sys.authapply.model.BaseTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 模块权限子表
 */
public class SsmModulePermissionChildTemplate {

    private String dataContentName;
    private String dataUsageScenario;
    private String sensitiveLevel;
    private String permissionDuration;

    public List<BaseTemplate> buildChildTemplate() {
        List<BaseTemplate> list = new ArrayList<>();
        list.add(new BaseTemplate("dataContentName", dataContentName));
        list.add(new BaseTemplate("dataUsageScenario", dataUsageScenario));
        list.add(new BaseTemplate("sensitiveLevel", sensitiveLevel));
        list.add(new BaseTemplate("permissionDuration", permissionDuration));
        return list;
    }

    public String getDataContentName() {
        return dataContentName;
    }

    public void setDataContentName(String dataContentName) {
        this.dataContentName = dataContentName;
    }

    public String getDataUsageScenario() {
        return dataUsageScenario;
    }

    public void setDataUsageScenario(String dataUsageScenario) {
        this.dataUsageScenario = dataUsageScenario;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getPermissionDuration() {
        return permissionDuration;
    }

    public void setPermissionDuration(String permissionDuration) {
        this.permissionDuration = permissionDuration;
    }
}
