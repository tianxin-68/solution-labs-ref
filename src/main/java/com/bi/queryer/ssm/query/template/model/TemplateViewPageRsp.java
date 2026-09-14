package com.bi.queryer.ssm.query.template.model;

import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;

import java.util.ArrayList;
import java.util.List;

public class TemplateViewPageRsp extends TemplateRsp {

    /**
     * 默认选中的视图ID
     */
    private String defaultSelectViewId;

    /**
     * 视图集合
     */
    private List<TemplateViewEntity> viewList = new ArrayList<>();

    public String getDefaultSelectViewId() {
        return defaultSelectViewId;
    }

    public void setDefaultSelectViewId(String defaultSelectViewId) {
        this.defaultSelectViewId = defaultSelectViewId;
    }

    public List<TemplateViewEntity> getViewList() {
        return viewList;
    }

    public void setViewList(List<TemplateViewEntity> viewList) {
        this.viewList = viewList;
    }
}
