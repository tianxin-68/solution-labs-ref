package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-07-02  17:45
 * @Description: 模板更新时间返回实体
 */
@Data
public class TemplateDataUpdateTimeRsp {

    /**
     * 模板id
     */
    private String tplId;

    /**
     * 数据集名称
     */
    private String datasetName;

    /**
     * 更新时间
     */
    private String dataUpdateTime;

}
