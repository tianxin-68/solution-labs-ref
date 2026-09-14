package com.bi.queryer.ssm.mgr.fieldCtg.model;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-26  11:28
 * @Description: 数据集目录继承实体
 */
@Data
public class DatasetCtgInheritEntity {

    /**
     * 数据集 dataset 目录 ctg
     */
    private String itemType;

    /**
     * 数据集=数据集id 目录=目录id
     */
    private String itemValue;

    /**
     * 是否继承
     */
    private Integer isInherited = Enabled.YES.getId();

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 修改人
     */
    private String updatedBy;

}
