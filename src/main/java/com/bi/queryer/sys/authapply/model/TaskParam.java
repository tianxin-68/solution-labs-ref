package com.bi.queryer.sys.authapply.model;

import java.util.List;

public class TaskParam extends TaskBaseParam {

    private List<BaseTemplate> templateList;

    private List<ChildTemplateList> childrenTemplateList;

    public List<BaseTemplate> getTemplateList() {
        return templateList;
    }

    public void setTemplateList(List<BaseTemplate> templateList) {
        this.templateList = templateList;
    }

    public List<ChildTemplateList> getChildrenTemplateList() {
        return childrenTemplateList;
    }

    public void setChildrenTemplateList(List<ChildTemplateList> childrenTemplateList) {
        this.childrenTemplateList = childrenTemplateList;
    }
}
