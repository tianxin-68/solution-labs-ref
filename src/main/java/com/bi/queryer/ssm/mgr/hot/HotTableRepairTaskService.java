package com.bi.queryer.ssm.mgr.hot;

import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 11:11 2024/10/11
 * @Description TODO
 **/
@Service
@Scope("prototype")
public class HotTableRepairTaskService {

    @Autowired
    protected BaseDao dao = null;

    public void delete(List<String> sourceTableNames){
        if(BIUtil.isEmpty(sourceTableNames)){
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("sourceTableNames", sourceTableNames);
        dao.delete("ssm.hot.table.repair.task.deleteBySource", params, DataSourceType.Default);
    }

    public void saveRepairTasks(List<HotTableRepairTask> hotTableRepairTaskList){
        if(BIUtil.isEmpty(hotTableRepairTaskList)){
            return;
        }

        // 先删除
        List<String> sourceTableNames = hotTableRepairTaskList.stream().map(HotTableRepairTask::getSourceTableName).distinct().collect(Collectors.toList());
        this.delete(sourceTableNames);

        // 再插入
        dao.insert("ssm.hot.table.repair.task.insertRepairTask", hotTableRepairTaskList, DataSourceType.Default);
    }

    /**
     * 获取待补数的批次
     * @param sourceTableName
     * @return
     */
    public List<HotTableRepairTask> queryWaitRepairTasks(String sourceTableName){
        List<TaskExecStatus> taskExecStatusList = new ArrayList<>();
        taskExecStatusList.add(TaskExecStatus.READY);
        taskExecStatusList.add(TaskExecStatus.PADDING);
        return queryRepairTasksByStatus(sourceTableName, taskExecStatusList);
    }

    public List<HotTableRepairTask> queryRepairTasksByStatus(String sourceTableName, List<TaskExecStatus> taskExecStatusList){
        List<HotTableRepairTask> taskList = this.queryRepairTasksBySourceTable(sourceTableName);
        if(BIUtil.isEmpty(taskExecStatusList)){
            return taskList;
        }
        taskList = taskList.stream().filter(task -> taskExecStatusList.contains(TaskExecStatus.get(task.getTaskExecStatus()))).collect(Collectors.toList());
        return taskList;
    }

    public List<HotTableRepairTask> queryRepairTasksBySourceTable(String sourceTableName){
        Map<String, Object> params = new HashMap<>();
        params.put("sourceTableName", sourceTableName);
        List<HotTableRepairTask> repairList = (List<HotTableRepairTask>) dao.queryObjectList("ssm.hot.table.repair.task.queryRepairTask", params, DataSourceType.Default);
        return repairList;
    }

    public List<HotTableRepairTask> queryRepairTasksById(List<String> taskIdList){
        Map<String, Object> params = new HashMap<>();
        params.put("taskIdList", taskIdList);
        List<HotTableRepairTask> repairList = (List<HotTableRepairTask>) dao.queryObjectList("ssm.hot.table.repair.task.queryRepairTask", params, DataSourceType.Default);
        return repairList;
    }

}
