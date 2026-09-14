package com.bi.queryer.ssm.mgr.dataset.model;

import lombok.Data;

import java.util.Date;

/**
 * 数据集与行级权限维度关系
 */
@Data
public class SSMDatasetAuthDim {

    /**
     * 数据集id
     */
    private String datasetId;

    /**
     * 数据权限模块编码
     */
    private String moduleCode;

    /**
     * 数据权限维度编码
     */
    private String dimCode;

    /**
     * 权限模块名称（展示用冗余字段）
     */
    private String moduleName;

    /**
     * 权限维度名称（展示用冗余字段）
     */
    private String dimName;

    /**
     * 是否可申请
     */
    private Integer isApply;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private Date createdTime;
}
