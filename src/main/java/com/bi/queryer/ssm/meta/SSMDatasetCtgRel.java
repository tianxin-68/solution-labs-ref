package com.bi.queryer.ssm.meta;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-15  16:33
 * @Description: 数据集与目录的关联
 */
@Data
public class SSMDatasetCtgRel {

    private String relId;

    private String ctgId;

    private String datasetId;

}
