package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;

/**
 * 表关联的etl
 * @author contributor
 */
public class MetaTableEtlJob {

    private String tableId;

    private String etlJob;

    private String createdTime;

    private String createdBy;

    private String dataEnv = DataEnv.OLD_SSM.getCode();

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getEtlJob() {
        return etlJob;
    }

    public void setEtlJob(String etlJob) {
        this.etlJob = etlJob;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDataEnv() {
        return dataEnv;
    }

    public void setDataEnv(String dataEnv) {
        this.dataEnv = dataEnv;
    }
}
