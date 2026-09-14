package com.bi.queryer.ssm.query.template.view.model;

/**
 * 按视图 id 串批量查询（id 之间以空格、英文逗号或英文分号分隔）。
 */
public class TemplateViewIdsQueryReq {

    /**
     * 视图 id，多个之间以空格、逗号或分号分隔
     */
    private String viewIds;

    public String getViewIds() {
        return viewIds;
    }

    public void setViewIds(String viewIds) {
        this.viewIds = viewIds;
    }
}
