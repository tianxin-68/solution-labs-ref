package com.bi.queryer.ssm.engine.accelerate.hot.repair;

import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:43 2024/10/11
 * @Description 补数批次
 **/
public class HotTableRepairTable implements Comparable{
    private String sourceTableName;

    private Integer slaPriority = 15;

    private List<HotTableRepairTask> taskList = new ArrayList<>();

    public HotTableRepairTable(){}

    public HotTableRepairTable(String sourceTableName, Integer slaPriority) {
        this.sourceTableName = sourceTableName;
        this.slaPriority = slaPriority;
    }

    public List<HotTableRepairTask> getRunningTasks(){
        List<HotTableRepairTask> unfinishedBatches = this.taskList.stream()
                .filter(batch -> TaskExecStatus.RUNNING == TaskExecStatus.get(batch.getTaskExecStatus()))
                .collect(Collectors.toList());
        return unfinishedBatches;
    }

    public List<HotTableRepairTask> getReadyTasks(){
        List<HotTableRepairTask> unfinishedBatches = this.taskList.stream()
                .filter(batch -> TaskExecStatus.READY == TaskExecStatus.get(batch.getTaskExecStatus()))
                .collect(Collectors.toList());
        return unfinishedBatches;
    }

    public List<HotTableRepairTask> getSuccessTask(){
        return taskList.stream().filter(batch -> TaskExecStatus.SUCCESS == TaskExecStatus.get(batch.getTaskExecStatus())).collect(Collectors.toList());
    }

    public List<HotTableRepairTask> getSuccessTasks(String batchName){
        return taskList.stream()
                .filter(t -> TaskExecStatus.SUCCESS == TaskExecStatus.get(t.getTaskExecStatus()))
                .filter(t -> t.getTaskBatchName().equals(batchName))
                .collect(Collectors.toList());
    }

    public HotTableRepairTask getSuccessTask(String batchName, String scriptType){
        List<HotTableRepairTask> successTasks = taskList.stream()
                .filter(t -> TaskExecStatus.SUCCESS == TaskExecStatus.get(t.getTaskExecStatus()))
                .filter(t -> t.getTaskBatchName().equals(batchName))
                .filter(t -> t.getTaskScriptType().equals(scriptType))
                .collect(Collectors.toList());
        if(BIUtil.isNotEmpty(successTasks)){
            return successTasks.get(0);
        }
        return null;
    }

    public boolean isEnd() {
        long count = taskList.stream().filter(batch -> TaskExecStatus.get(batch.getTaskExecStatus()).isEnd()).count();
        return taskList.size() == count;
    }

    public boolean isWaiting(){
        long count = taskList.stream()
                    .filter(batch -> !TaskExecStatus.get(batch.getTaskExecStatus()).isEnd())
                    .count();
        return count > 0;
    }

    /**
     * 只要有一个任务失败，表标记为失败
     * @return
     */
    public boolean isFailed(){
        return taskList.stream().anyMatch(batch -> TaskExecStatus.FAIL == TaskExecStatus.get(batch.getTaskExecStatus()));
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }


    public Integer getSlaPriority() {
        return slaPriority;
    }

    public void setSlaPriority(Integer slaPriority) {
        this.slaPriority = slaPriority;
    }

    public List<HotTableRepairTask> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<HotTableRepairTask> taskList) {
        this.taskList = taskList;
    }

    @Override
    public int compareTo(Object o) {
        // 按优先级降序
        if(o instanceof HotTableRepairTable){
            HotTableRepairTable other = (HotTableRepairTable) o;
            return other.getSlaPriority().compareTo(this.getSlaPriority());
        }
        return 0;
    }
}
