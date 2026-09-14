package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.List;

@Data
public class TemplateBatchOperateReq {

    /**
     * 被操作模版集合
     */
    private List<String> tplIdList;

    /**
     * 移动到的目录ID
     */
    private String ctgId;
    /**
     * 模板类型:analysis_template 看板, query_template 查询模板
     */
    private String tplType;
}