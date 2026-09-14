package com.bi.queryer.ssm.mgr.dataset.model;

import com.bi.queryer.ssm.meta.SSMDataset;
import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-15  15:34
 * @Description: 数据集返回实体
 */
@Data
public class DatasetRsq {

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
     * 排序字段
     */
    private Double sortId;

    private Integer isActive;

    /**
     * 是否有权限
     */
    private Integer hasAuth = Enabled.NO.getId();

    /**
     * 是否标准数据集
     */
    private Integer isStandardDataset = Enabled.YES.getId();

    //数据集数据类型： 离线和实时
    private String datasetType;

    public DatasetRsq() {

    }

    public DatasetRsq(SSMDataset ssmDataset) {
        this.datasetId = ssmDataset.getDatasetId();
        this.datasetName = ssmDataset.getDatasetName();
        this.rptDevOwner = ssmDataset.getRptDevOwner();
        this.dataDevOwner = ssmDataset.getDataDevOwner();
        this.dataDate = ssmDataset.getDataDate();
        this.dataDesc = ssmDataset.getDataDesc();
        this.sortId = ssmDataset.getSortId();
        this.isActive = ssmDataset.getIsActive();
        this.isStandardDataset = ssmDataset.getIsStandardDataset();
        this.datasetType = ssmDataset.getDatasetType();
    }

}
