package com.bi.queryer.ssm.meta.accelerate.hot;

import com.bi.queryer.util.BIUtil;

/**
 * @Author contributor
 * @Date 16:57 2024/8/19
 * @Description 热表数据源
 **/
public class HotTableSource {
    private String pkid;

    private String sourceTableName;

    private String sourceTableDesc;

    private String sourceDbEngine;

    private String supportHotDbEngine;

    /**
     * 需要热化的初始化数据天数,-1 不过滤时间
     */
    private Integer hotDataInitDays;

    /**
     * 热化数据的最大天数,-1 不过滤时间
     */
    private Integer hotDataMaxDays;

    private Integer isActive ;

    private String createdBy;

    private String updatedBy;

    private String createdTime;

    private String updatedTime;

    /**
     * 全量 full 增量 incremental
     */
    private String dataUpdateMode;

    /**
     * 数据更新天数
     */
    private Integer dataIncrementalDays;

    /**
     * 是否需要分区
     */
    private Integer isNeedPartition;

    /**
     * 时间粒度 日d 周w 月m 年 y
     */
    private String dateGranularity;

    /**
     * 时间格式
     */
    private String dateFormatType;

    /**
     *  热数据要求天数
     */
    private Integer hotDataRequireDays;

    /**
     * 热数据补数批次天数
     */
    private Integer repairDataBatchDays;

    private Integer isAutoRepairData;

    /**
     * 分区字段名
     */
    private String partitionBy;


    public String getPkid() {
        return pkid;
    }

    public void setPkid(String pkid) {
        this.pkid = pkid;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getSourceTableDesc() {
        return sourceTableDesc;
    }

    public void setSourceTableDesc(String sourceTableDesc) {
        this.sourceTableDesc = sourceTableDesc;
    }

    public String getSourceDbEngine() {
        return sourceDbEngine;
    }

    public void setSourceDbEngine(String sourceDbEngine) {
        this.sourceDbEngine = sourceDbEngine;
    }

    public String getSupportHotDbEngine() {
        return supportHotDbEngine;
    }

    public void setSupportHotDbEngine(String supportHotDbEngine) {
        this.supportHotDbEngine = supportHotDbEngine;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getHotDataInitDays() {
        return hotDataInitDays;
    }

    public void setHotDataInitDays(Integer hotDataInitDays) {
        this.hotDataInitDays = hotDataInitDays;
    }

    public Integer getHotDataMaxDays() {
        return hotDataMaxDays;
    }

    public void setHotDataMaxDays(Integer hotDataMaxDays) {
        this.hotDataMaxDays = hotDataMaxDays;
    }

    public String getDataUpdateMode() {
        return dataUpdateMode;
    }

    public void setDataUpdateMode(String dataUpdateMode) {
        this.dataUpdateMode = dataUpdateMode;
    }

    public Integer getDataIncrementalDays() {
        return dataIncrementalDays;
    }

    public void setDataIncrementalDays(Integer dataIncrementalDays) {
        this.dataIncrementalDays = dataIncrementalDays;
    }

    public Integer getIsNeedPartition() {
        return isNeedPartition;
    }

    public void setIsNeedPartition(Integer isNeedPartition) {
        this.isNeedPartition = isNeedPartition;
    }

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public String getDateFormatType() {
        return dateFormatType;
    }

    public void setDateFormatType(String dateFormatType) {
        this.dateFormatType = dateFormatType;
    }

    public Integer getHotDataRequireDays() {
        return hotDataRequireDays;
    }

    public void setHotDataRequireDays(Integer hotDataRequireDays) {
        this.hotDataRequireDays = hotDataRequireDays;
    }

    public Integer getRepairDataBatchDays() {
        return repairDataBatchDays;
    }

    public void setRepairDataBatchDays(Integer repairDataBatchDays) {
        this.repairDataBatchDays = repairDataBatchDays;
    }

    public Integer getIsAutoRepairData() {
        return isAutoRepairData;
    }

    public void setIsAutoRepairData(Integer isAutoRepairData) {
        this.isAutoRepairData = isAutoRepairData;
    }

    public String getPartitionBy() {
        if(BIUtil.isEmpty(partitionBy)){
            partitionBy = "dt";
        }
        return partitionBy;
    }

    public void setPartitionBy(String partitionBy) {
        this.partitionBy = partitionBy;
    }
}
