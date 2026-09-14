package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

@Data
public class DatasetCtgDataAuthDetailEntity {

    private String ownerId;

    private String ownerType;

    /**
     * 全部 all 数据集 dataset 目录 ctg
     */
    private String itemType;

    /**
     * 全部=-9999 数据集=数据集id 目录=目录id
     */
    private String itemValue;

    /**
     * 创建人
     */
    private String createdBy;
}
