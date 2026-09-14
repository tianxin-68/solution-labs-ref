package com.bi.queryer.ssm.portal.template.vo;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-20  14:40
 * @Description: 看板-列表查询参数
 */
@Data
public class AnalysisTemplateListQueryReq {

    /**
     * 页签类型 ： 最近查看 我创建的 我收藏的
     */
    private String tabType;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 页数
     */
    private Integer pageNum = 1;
}
