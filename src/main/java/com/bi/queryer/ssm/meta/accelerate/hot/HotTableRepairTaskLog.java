package com.bi.queryer.ssm.meta.accelerate.hot;

/**
 * @Author contributor
 * @Date 19:43 2024/10/10
 * @Description TODO
 **/
public class HotTableRepairTaskLog {
    private String logId ;
    /** 补数id */
    private String taskId;
    /** 源表名 */
    private String sourceTableName ;
    /** 补数批次号 */
    private String taskBatchNo;
    /** 补数脚本类型:hive/doris */
    private String taskScriptType;
    /** 执行id(用于查询状态) */
    private String taskExecId;
    /** 执行状态:success/fail */
    private String taskExecStatus;
    /** 执行信息 */
    private String taskExecInfo;
    /** 执行开始时间 */
    private String taskExecStartTime;
    /** 执行结束结束 */
    private String taskExecEndTime;

    private String taskServerIp;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getTaskBatchNo() {
        return taskBatchNo;
    }

    public void setTaskBatchNo(String taskBatchNo) {
        this.taskBatchNo = taskBatchNo;
    }

    public String getTaskScriptType() {
        return taskScriptType;
    }

    public void setTaskScriptType(String taskScriptType) {
        this.taskScriptType = taskScriptType;
    }

    public String getTaskExecId() {
        return taskExecId;
    }

    public void setTaskExecId(String taskExecId) {
        this.taskExecId = taskExecId;
    }

    public String getTaskExecStatus() {
        return taskExecStatus;
    }

    public void setTaskExecStatus(String taskExecStatus) {
        this.taskExecStatus = taskExecStatus;
    }

    public String getTaskExecInfo() {
        return taskExecInfo;
    }

    public void setTaskExecInfo(String taskExecInfo) {
        this.taskExecInfo = taskExecInfo;
    }

    public String getTaskExecStartTime() {
        return taskExecStartTime;
    }

    public void setTaskExecStartTime(String taskExecStartTime) {
        this.taskExecStartTime = taskExecStartTime;
    }

    public String getTaskExecEndTime() {
        return taskExecEndTime;
    }

    public void setTaskExecEndTime(String taskExecEndTime) {
        this.taskExecEndTime = taskExecEndTime;
    }

    public String getTaskServerIp() {
        return taskServerIp;
    }

    public void setTaskServerIp(String taskServerIp) {
        this.taskServerIp = taskServerIp;
    }
}
