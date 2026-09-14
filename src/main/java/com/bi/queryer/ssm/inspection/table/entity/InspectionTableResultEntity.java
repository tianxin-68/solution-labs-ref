package com.bi.queryer.ssm.inspection.table.entity;

import java.util.ArrayList;
import java.util.List;

public class InspectionTableResultEntity {

    /**
     * 表名(带库名)
     */
    private String tableName;

    /**
     * 表负责人
     */
    private String tableOwner;

    /**
     * 缺失的分区
     */
    private List<String> emptyPartitionDateList = new ArrayList<>();

    /**
     * 最大分区
     */
    private String maxPartitionDate;

    /**
     * 最小分区
     */
    private String minPartitionDate;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 批次号
     */
    private Long batchNo;

    /**
     * 执行结果
     */
    private String execStatus;

    /**
     * 数据源 Key，如 Doris_Master
     */
    private String dsKey;

    /**
     * 备注
     */
    private String remark;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableOwner() {
        return tableOwner;
    }

    public void setTableOwner(String tableOwner) {
        this.tableOwner = tableOwner;
    }

    public List<String> getEmptyPartitionDateList() {
        return emptyPartitionDateList;
    }

    public void setEmptyPartitionDateList(List<String> emptyPartitionDateList) {
        this.emptyPartitionDateList = emptyPartitionDateList;
    }

    public String getMaxPartitionDate() {
        return maxPartitionDate;
    }

    public void setMaxPartitionDate(String maxPartitionDate) {
        this.maxPartitionDate = maxPartitionDate;
    }

    public String getMinPartitionDate() {
        return minPartitionDate;
    }

    public void setMinPartitionDate(String minPartitionDate) {
        this.minPartitionDate = minPartitionDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(Long batchNo) {
        this.batchNo = batchNo;
    }

    public String getExecStatus() {
        return execStatus;
    }

    public void setExecStatus(String execStatus) {
        this.execStatus = execStatus;
    }

    public String getDsKey() {
        return dsKey;
    }

    public void setDsKey(String dsKey) {
        this.dsKey = dsKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
