package com.bi.queryer.ssm.meta;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-15  14:48
 * @Description: 数据集实体
 */
@Data
public class SSMDataset {

    /**
     * 数据集id
     */
    private String datasetId;

    /**
     * 数据集名称
     */
    private String datasetName;

    /**
     * 分析师
     */
    private String rptDevOwner;

    /**
     * 开发负责人
     */
    private String dataDevOwner;

    /**
     * 内容承载时间
     */
    private String dataDate;

    /**
     * 承载内容说明
     */
    private String dataDesc;

    /**
     * 排序id
     */
    private Double sortId;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 是否可用
     */
    private Integer isActive;

    /**
     * 是否标准数据集
     */
    private Integer isStandardDataset = Enabled.YES.getId();

    //数据集数据类型： 离线和实时
    private String datasetType;

}
