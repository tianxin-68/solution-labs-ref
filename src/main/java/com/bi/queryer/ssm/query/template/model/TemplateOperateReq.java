package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

/**
 * 模版收藏置顶详情req
 */
@Data
public class TemplateOperateReq {

    /**
     * 模版ID
     */
    private String tplId;

    /**
     * 置顶顺序
     */
    private Double topSortId;

    /**
     * 模版视图ID
     */
    private String viewId;

    /**
     * 模版类型
     */
    private String tplType;
}
