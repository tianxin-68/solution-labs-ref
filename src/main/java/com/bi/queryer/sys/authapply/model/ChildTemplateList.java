package com.bi.queryer.sys.authapply.model;

import java.util.List;

public class ChildTemplateList {

    private String formName;

    private List<List<BaseTemplate>> templateBaseEntitiesList;

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public List<List<BaseTemplate>> getTemplateBaseEntitiesList() {
        return templateBaseEntitiesList;
    }

    public void setTemplateBaseEntitiesList(List<List<BaseTemplate>> templateBaseEntitiesList) {
        this.templateBaseEntitiesList = templateBaseEntitiesList;
    }
}
