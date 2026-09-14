package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

/**
 * @Auther: contributor
 * @Date: 2025/9/22 16:35
 * @Description:
 */
@Data
public class TemplateMoveCtgReq extends TemplateBatchOperateReq{
    /**
     * 移动的来源， 共享空间或我的空间
     */
    private String moveSource;
}
