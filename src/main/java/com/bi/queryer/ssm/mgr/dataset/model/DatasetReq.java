package com.bi.queryer.ssm.mgr.dataset.model;

import lombok.Data;

import java.util.List;

@Data
public class DatasetReq {

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
     * 排序
     */
    private Double sortId;

    /**
     * 授权用户
     */
    private List<String> authUserNameList;

    /**
     * 是否标准数据集
     */
    private Integer isStandardDataset;

    //数据集类型
    private String datasetType;

}
