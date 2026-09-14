package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.sys.enums.Enabled;

public class MetaDataset {

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
     * 是否标准数据集
     */
    private Integer isStandardDataset = Enabled.YES.getId();

    //数据集数据类型： 离线和实时
    private String datasetType;

    private String dataEnv = DataEnv.OLD_SSM.getCode();

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getRptDevOwner() {
        return rptDevOwner;
    }

    public void setRptDevOwner(String rptDevOwner) {
        this.rptDevOwner = rptDevOwner;
    }

    public String getDataDevOwner() {
        return dataDevOwner;
    }

    public void setDataDevOwner(String dataDevOwner) {
        this.dataDevOwner = dataDevOwner;
    }

    public String getDataDate() {
        return dataDate;
    }

    public void setDataDate(String dataDate) {
        this.dataDate = dataDate;
    }

    public String getDataDesc() {
        return dataDesc;
    }

    public void setDataDesc(String dataDesc) {
        this.dataDesc = dataDesc;
    }

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Integer getIsStandardDataset() {
        return isStandardDataset;
    }

    public void setIsStandardDataset(Integer isStandardDataset) {
        this.isStandardDataset = isStandardDataset;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getDataEnv() {
        return dataEnv;
    }

    public void setDataEnv(String dataEnv) {
        this.dataEnv = dataEnv;
    }
}
