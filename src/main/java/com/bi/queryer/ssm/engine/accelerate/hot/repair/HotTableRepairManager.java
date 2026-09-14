package com.bi.queryer.ssm.engine.accelerate.hot.repair;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:38 2024/10/10
 * @Description 热化表补数管理类
 **/

@Component
public class HotTableRepairManager {
    /**
     * 初始化构建脚本
     * @param sourceTableName
     * @return
     */
    public static List<String> initialize(String sourceTableName){
        // 再初始化
        List<String> result = new ArrayList<>();
        HotTableRepairTaskCreator scriptBuilder = new HotTableRepairTaskCreator();
        List<HotTableRepairTask> tableRepairList = scriptBuilder.buildRepairTask(sourceTableName);
        if(BIUtil.isEmpty(tableRepairList)){
            result.add("无新增热表需补数！");
            return result;
        }

        // 返回批次信息
        for(HotTableRepairTask repair : tableRepairList){
            String info = repair.getSourceTableName() + ":" + repair.getTaskScriptType() + ":" + repair.getTaskStartDate() + "~" + repair.getTaskEndDate();
            result.add(info);
        }
        return result;
    }

    /**
     * 执行补数脚本：先检查并更新已运行任务的状态，最后再提交任务
     * @return
     */
    public static List<String> submit(String sourceTableName){
        List<String> result = new ArrayList<>();
        /** 更新正在运行任务的进度(状态) **/
        HotTableRepairProgressProcessor progressProcessor = new HotTableRepairProgressProcessor();
        progressProcessor.process();

        /** 执行补数任务 **/
        HotTableRepairTaskExecutor executor = new HotTableRepairTaskExecutor();
        // 补数时间段
        String repairActiveTime = SC.v("hot.table.repair.active.time", "09:00~23:00");
        DateTime repairStartTime = DateUtil.parse(String.format("%s %s:00", DateUtil.today(), repairActiveTime.split("~")[0]));
        DateTime repairEndTime = DateUtil.parse(String.format("%s %s:00", DateUtil.today(), repairActiveTime.split("~")[1]));
        if(!DateUtil.isIn(DateUtil.date(), repairStartTime, repairEndTime)){
            result.add(String.format("当前时间不在补数时间段:%s，无法提交补数任务", repairActiveTime));
            return result;
        }
        List<HotTableRepairTask> tasks = executor.execute(sourceTableName);
        if(BIUtil.isEmpty(tasks)){
            result.add("本次提交无补数任务，任务已完成或有失败任务！");
        }else {
            result.add("本次提交任务：");
            tasks.stream().forEach(t -> result.add(t.getTaskId() + ":" + t.getTaskBatchName()));
        }

        return result;
    }

    public static List<String> cancel(String sourceTableName){
        // 先取消：避免重复执行导致数据错误
        HotTableRepairTaskExecutor executor = new HotTableRepairTaskExecutor();
        if(BIUtil.isEmpty(sourceTableName)){
            executor.cancelAll();
        }else {
            executor.cancel(sourceTableName);
        }
        List<String> logs = new ArrayList<>();
        logs.add("取消成功");
        return logs;
    }

    public static List<String> resetStatusBySourceTable(String sourceTableName, String execStatus){
        // 先取消：避免重复执行导致数据错误
        HotTableRepairTaskExecutor executor = new HotTableRepairTaskExecutor();
        executor.resetStatusBySourceTable(sourceTableName, execStatus);
        List<String> logs = new ArrayList<>();
        logs.add("重置成功");
        return logs;
    }

    public static List<String> resetStatusById(List<String> taskIdList, String status){
        HotTableRepairTaskExecutor executor = new HotTableRepairTaskExecutor();
        executor.resetStatusById(taskIdList, status);
        List<String> logs = new ArrayList<>();
        logs.add("重置成功");
        return logs;
    }

    public static List<String> redoById(List<String> taskIdList){
        HotTableRepairTaskExecutor executor = new HotTableRepairTaskExecutor();
        List<String> logs = new ArrayList<>();
        executor.redoById(taskIdList);
        logs.add("重新执行成功");
        return logs;
    }

    public static void delete(String sourceTableName){
        if(BIUtil.isEmpty(sourceTableName)){
            return;
        }
        // 先取消
        cancel(sourceTableName);

        // 再删除
        HotTableRepairTaskCreator taskManager = new HotTableRepairTaskCreator();
        taskManager.deleteRepairTask(sourceTableName);
    }
}
