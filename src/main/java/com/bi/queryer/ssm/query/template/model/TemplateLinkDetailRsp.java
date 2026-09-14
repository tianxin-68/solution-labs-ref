package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-01-12  10:55
 */
@Data
public class TemplateLinkDetailRsp {

    /**
     * 跳转到其他模板的明细
     */
    private List<TemplateLinkRsp> linkToOtherTemplateList;

    /**
     * 被其他模板跳转至本模板的明细
     */
    private List<TemplateLinkRsp> linkFromOtherTemplateList;

}
