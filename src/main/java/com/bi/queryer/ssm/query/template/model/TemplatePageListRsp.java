package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.List;

@Data
public class TemplatePageListRsp {
    /**
     * 列表总数
     */
    private Integer total;

    /**
     * 分页数据
     */
    private List<TemplateViewPageRsp> rows;
}
