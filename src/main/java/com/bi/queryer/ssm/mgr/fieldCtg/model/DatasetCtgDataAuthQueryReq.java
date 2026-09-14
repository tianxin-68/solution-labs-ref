package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-26  14:18
 * @Description: 查询权限配置实体
 */
@Data
public class DatasetCtgDataAuthQueryReq {

    /**
     * 全部 all 数据集 dataset 目录 ctg
     */
    private String itemType;

    /**
     * 全部=-9999 数据集=数据集id 目录=目录id
     */
    private String itemValue;

}
