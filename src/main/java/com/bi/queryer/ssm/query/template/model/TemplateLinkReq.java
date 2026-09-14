package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-01-11  14:42
 * @Description: 模板跳转请求参数
 */
@Data
public class TemplateLinkReq {

    /**
     * 模板id
     */
    private String tplId;

    /**
     * 模板视图id
     */
    private String viewId;

    /**
     * 筛选行维度编码
     */
    private String fieldCode;

    /**
     * 筛选行维度编码列表
     */
    private List<String> fieldCodeList = new ArrayList<>();

}
