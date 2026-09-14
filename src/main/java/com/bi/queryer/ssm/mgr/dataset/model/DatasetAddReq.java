package com.bi.queryer.ssm.mgr.dataset.model;

import com.bi.queryer.sys.enums.Enabled;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-15  13:54
 * @Description: 数据集新增
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetAddReq {


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
     * 是否可用
     */
    private Integer isActive;

    /**
     * 是否标准数据集
     */
    private Integer isStandardDataset = Enabled.YES.getId();
    // 数据集类型
    private String datasetType;

}
