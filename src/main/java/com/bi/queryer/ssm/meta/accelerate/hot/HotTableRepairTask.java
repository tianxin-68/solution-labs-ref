package com.bi.queryer.ssm.meta.accelerate.hot;

import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.util.BIUtil;

/**
 * @Author contributor
 * @Date 19:32 2024/10/10
 * @Description 热表补数基础表
 **/
public class HotTableRepairTask {
    /** 补数id */
    private String taskId;
    /** 源表名 */
    private String sourceTableName ;
    /** 补数开始日期 */
    private String taskStartDate;
    /** 补数结束日期 */
    private String taskEndDate;
    /** 补数批次号 */
    private String taskBatchNo;
    /** 补数脚本类型:hive/doris */
    private String taskScriptType;
    /** 补数脚本内容 */
    private String taskScriptContent;
    /** 执行状态:ready/running/success/fail */
    private String taskExecStatus;
    /** 执行信息 */
    private String taskExecInfo;
    /** 执行开始时间 */
    private String taskExecStartTime;
    /** 执行结束结束 */
    private String taskExecEndTime;

    private String taskDesc;

    /** 创建人 */
    private String createdBy ;
    /** 创建时间 */
    private String createdTime ;
    /** 更新人 */
    private String updatedBy ;
    /** 更新时间 */
    private String updatedTime ;

    private Integer slaPriority;

    private String taskServerIp;

    private String taskExecId;

    private String dataCheckResult;

    private String dataCheckInfo;

    private String dataCheckTime;

    private Integer isHotMinDateUpdated;

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

    public String getTaskStartDate() {
        return taskStartDate;
    }

    public void setTaskStartDate(String taskStartDate) {
        this.taskStartDate = taskStartDate;
    }

    public String getTaskEndDate() {
        return taskEndDate;
    }

    public void setTaskEndDate(String taskEndDate) {
        this.taskEndDate = taskEndDate;
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

    public String getTaskScriptContent() {
        return taskScriptContent;
    }

    public void setTaskScriptContent(String taskScriptContent) {
        this.taskScriptContent = taskScriptContent;
    }

    public String getTaskExecStatus() {
        if(BIUtil.isEmpty(taskExecStatus)){
            taskExecStatus = TaskExecStatus.READY.getCode();
        }
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getTaskDesc() {
        return taskDesc;
    }

    public void setTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
    }

    public Integer getSlaPriority() {
        return slaPriority;
    }

    public void setSlaPriority(Integer slaPriority) {
        this.slaPriority = slaPriority;
    }

    public String getTaskServerIp() {
        return taskServerIp;
    }

    public void setTaskServerIp(String taskServerIp) {
        this.taskServerIp = taskServerIp;
    }

    public boolean isHiveTask(){
        return DBType.Hive == DBType.getType(this.taskScriptType);
    }

    public boolean isDorisTask(){
        return DBType.Doris == DBType.getType(this.taskScriptType);
    }

    public String getTaskBatchName(){
        return String.format("%s_%s_%s",this.sourceTableName, this.taskStartDate, this.taskEndDate);
    }

    public String getTaskExecId() {
        return taskExecId;
    }

    public void setTaskExecId(String taskExecId) {
        this.taskExecId = taskExecId;
    }

    public String getDataCheckResult() {
        return dataCheckResult;
    }

    public void setDataCheckResult(String dataCheckResult) {
        this.dataCheckResult = dataCheckResult;
    }

    public String getDataCheckInfo() {
        return dataCheckInfo;
    }

    public void setDataCheckInfo(String dataCheckInfo) {
        this.dataCheckInfo = dataCheckInfo;
    }

    public String getDataCheckTime() {
        return dataCheckTime;
    }

    public void setDataCheckTime(String dataCheckTime) {
        this.dataCheckTime = dataCheckTime;
    }

    public Integer getIsHotMinDateUpdated() {
        return isHotMinDateUpdated;
    }

    public void setIsHotMinDateUpdated(Integer isHotMinDateUpdated) {
        this.isHotMinDateUpdated = isHotMinDateUpdated;
    }
}
