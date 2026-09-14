package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-01-12  16:18
 */
@Data
public class TemplateLinkAddReq {

    /**
     * 模板id
     */
    private String tplId;

    /**
     * 模板视图id
     */
    private String viewId;

    /**
     * 筛选行维度id
     */
    private String fieldId;

    /**
     * 筛选行维度编码
     */
    private String fieldCode;

    /**
     * 筛选行维度标题
     */
    private String fieldTitle;

    /**
     * 20241206 模版跳转支持多个维度
     */
    private List<TemplateLinkFieldAddReq> templateLinkFieldList = new ArrayList<>();

}
