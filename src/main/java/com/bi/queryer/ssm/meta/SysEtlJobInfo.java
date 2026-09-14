package com.bi.queryer.ssm.meta;

import java.util.Date;

/**
 * @author contributor
 */
public class SysEtlJobInfo {

    /**
     * etl_job
     */
    private String etlJob;

    /**
     * 调度日期
     */
    private String lastTxdate;

    /**
     * 最后一次运行结束时间
     */
    private String lastEndTime;

    /**
     * owner
     */
    private String owner;

    /**
     * etl_time
     */
    private Date etlTime;

    /**
     * 作业类型
     */
    private String jobType;

    /**
     * 当天是否运行完成
     */
    private Integer isTodayCompleted;

    public Integer getIsTodayCompleted() {
        return isTodayCompleted;
    }

    public void setIsTodayCompleted(Integer isTodayCompleted) {
        this.isTodayCompleted = isTodayCompleted;
    }

    public String getEtlJob() {
        return etlJob;
    }

    public void setEtlJob(String etlJob) {
        this.etlJob = etlJob;
    }

    public String getLastTxdate() {
        return lastTxdate;
    }

    public void setLastTxdate(String lastTxdate) {
        this.lastTxdate = lastTxdate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getEtlTime() {
        return etlTime;
    }

    public void setEtlTime(Date etlTime) {
        this.etlTime = etlTime;
    }

    public String getLastEndTime() {
        return lastEndTime;
    }

    public void setLastEndTime(String lastEndTime) {
        this.lastEndTime = lastEndTime;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
}
