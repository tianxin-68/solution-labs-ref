package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-01-11  15:01
 */
@Data
public class TemplateLinkRsp {

    private String linkId;

    /**
     * 查询的模版ID (用于分享时，批量查询多个分享模版的跳转配置数据后，进行分组)
     */
    private String queryTplId;
    private String queryViewId;

    /**
     * 模板id
     */
    private String tplId;

    /**
     * 模板名称
     */
    private String tplName;

    /**
     * 视图id
     */
    private String viewId;

    /**
     * 视图名称
     */
    private String viewName;

    /**
     * 跳转字段列表
     */
    private List<TemplateLinkFieldRsp> templateLinkFieldList = new ArrayList<>();

    /**
     * 模板owner
     */
    private String tplOwner;

}
