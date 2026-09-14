package com.bi.queryer.ssm.inspection.metric.entity;

import java.util.ArrayList;
import java.util.List;

public class InspectionMetricResultEntity {

    /**
     * 表ID
     */
    private String tableId;

    /**
     * 表名(带库名)
     */
    private String tableName;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 批次号
     */
    private Long batchNo;

    /**
     * 成功: success 失败：fail
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

    /**
     * 指标为空的集合
     */
    private List<InspectionMetricResultFieldEntity> inspectionMetricResultFieldEntityList = new ArrayList<>();

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
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

    public List<InspectionMetricResultFieldEntity> getInspectionMetricResultFieldEntityList() {
        return inspectionMetricResultFieldEntityList;
    }

    public void setInspectionMetricResultFieldEntityList(List<InspectionMetricResultFieldEntity> inspectionMetricResultFieldEntityList) {
        this.inspectionMetricResultFieldEntityList = inspectionMetricResultFieldEntityList;
    }
}
