package com.bi.queryer.ssm.meta.accelerate.hot;

import com.bi.queryer.ssm.enums.TaskExecStatus;

/**
 * @Author contributor
 * @Date 19:55 2024/10/11
 * @Description 将hive和doris脚本内容放在一起，便于hive和doris作为同一批次进行管理
 **/
public class HotTableRepairTaskBatch extends HotTableRepairTask implements Comparable<HotTableRepairTaskBatch> {
    private HotTableRepairTask hiveTask;

    private HotTableRepairTask dorisTask;

    public boolean isExecSuccess(){
        if(hiveTask == null || dorisTask == null){
            return false;
        }
        if(TaskExecStatus.get(hiveTask.getTaskExecStatus()) == TaskExecStatus.SUCCESS &&
                TaskExecStatus.get(dorisTask.getTaskExecStatus()) == TaskExecStatus.SUCCESS){
            return true;
        }
        return false;
    }

    public boolean isDataCheckSuccess(){
        if(hiveTask == null || dorisTask == null){
            return false;
        }
        if(TaskExecStatus.get(hiveTask.getDataCheckResult()) == TaskExecStatus.SUCCESS &&
                TaskExecStatus.get(dorisTask.getDataCheckResult()) == TaskExecStatus.SUCCESS){
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(HotTableRepairTaskBatch compareTask) {
        return Integer.valueOf(this.getTaskBatchNo()).compareTo(Integer.valueOf(compareTask.getTaskBatchNo()));
    }

    public HotTableRepairTask getHiveTask() {
        return hiveTask;
    }

    public void setHiveTask(HotTableRepairTask hiveTask) {
        this.hiveTask = hiveTask;
    }

    public HotTableRepairTask getDorisTask() {
        return dorisTask;
    }

    public void setDorisTask(HotTableRepairTask dorisTask) {
        this.dorisTask = dorisTask;
    }
}
