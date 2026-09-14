package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-25  14:46
 * @Description: 数据集和目录的权限保存实体
 */
@Data
public class DatasetCtgDataAuthSaveReq {

    /**
     * 权限配置
     */
    private List<DatasetCtgDataAuthDetailReq> dataAuthList = new ArrayList<>();

    /**
     * 全部 all 数据集 dataset 目录 ctg
     */
    private String itemType;

    /**
     * 全部=-9999 数据集=数据集id 目录=目录id
     */
    private String itemValue;

    /**
     * 是否继承
     */
    private Integer isInherited;

}
