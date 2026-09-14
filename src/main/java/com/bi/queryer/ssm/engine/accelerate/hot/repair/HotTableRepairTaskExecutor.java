package com.bi.queryer.ssm.engine.accelerate.hot.repair;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.enums.DataUpdateMode;
import com.bi.queryer.ssm.engine.accelerate.hot.repair.executor.*;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTaskLog;
import com.bi.queryer.ssm.mgr.hot.HotTableRepairTaskLogService;
import com.bi.queryer.ssm.mgr.hot.HotTableRepairTaskService;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:16 2024/10/11
 * @Description 补数脚本执行器
 **/
public class HotTableRepairTaskExecutor {
    protected HotTableRepairTaskService repairTaskService;

    protected HotTableRepairTaskLogService repairTaskLogService;
    public HotTableRepairTaskExecutor(){
        this.repairTaskService = this.getRepairTaskService();
        this.repairTaskLogService = this.getRepairTaskLogService();
    }

    /**
     * 执行逻辑：
     * 1、多个热表，按优先级依次执行补数
     * 2、同一个热表统一批次，先补hive再补doris
     * 3、每个批次补完后，核对数据，若无误，则更新热表的最小时间，否则BI Queryer通告，并停止该表的后续补数 -- BI Queryer通告：通过监控告警处理
     * 4、当一个表所有批次都补完后或所有表补完后，BI Queryer通通知 -- 通过监控告警处理
     * 5、可控制并发补数的表、同一个表并发补数的批次
     * @return 是否已补完毕
     */
    public List<HotTableRepairTask> execute(String sourceTableName){
        // 等待补数的批次
        List<HotTableRepairTask> waitRepairTasks = this.getWaitTasks(sourceTableName);
        if(BIUtil.isEmpty(waitRepairTasks)){
            return waitRepairTasks;
        }

        waitRepairTasks = this.execute(waitRepairTasks);
        return waitRepairTasks;
    }

    protected List<HotTableRepairTask> execute(List<HotTableRepairTask> waitRepairTasks){
        for(HotTableRepairTask task : waitRepairTasks) {
            BaseScriptExecutor scriptExecutor = ScriptExecutorFactory.createScriptExecutor(task.getTaskScriptType());
            if(scriptExecutor == null) {
                continue;
            }
            ScriptExecuteResult result = scriptExecutor.execute(task);
            // 新增任务日志记录
            this.addLog(task, result);
        }
        return waitRepairTasks;
    }

    /**
     * 先kill正在执行的批次再修改状态为取消
     * @param sourceTableName
     */
    public void cancel(String sourceTableName){
        if(BIUtil.isEmpty(sourceTableName)){
            return;
        }
        // 先kill 任务
        // 获取所有正在执行的任务
        List<HotTableRepairTask> runningTasks = repairTaskService.queryRepairTasksByStatus(sourceTableName, Arrays.asList(TaskExecStatus.RUNNING));
        for(HotTableRepairTask task : runningTasks){
            BaseScriptExecutor scriptExecutor = ScriptExecutorFactory.createScriptExecutor(task.getTaskScriptType());
            scriptExecutor.kill(task);
        }

        repairTaskLogService.cancelBySourceTable(sourceTableName);
    }

    public void cancelAll(){

    }

    public void cancelByTaskId(List<String> taskIdList){
        if(BIUtil.isEmpty(taskIdList)){
            return;
        }
        // 先kill 任务
        // 获取所有正在执行的任务
        List<HotTableRepairTask> runningTasks = repairTaskService.queryRepairTasksById(taskIdList);
        runningTasks = runningTasks.stream().filter(task -> TaskExecStatus.get(task.getTaskExecStatus()) == TaskExecStatus.RUNNING).collect(Collectors.toList());
        for(HotTableRepairTask task : runningTasks){
            BaseScriptExecutor scriptExecutor = ScriptExecutorFactory.createScriptExecutor(task.getTaskScriptType());
            scriptExecutor.kill(task);
        }
        repairTaskLogService.updateStatusById(taskIdList, TaskExecStatus.CANCEL.getCode(), "取消");
    }

    /**
     * 重置状态
     * @param sourceTableName
     * @param status
     */
    public void resetStatusBySourceTable(String sourceTableName, String status){
        TaskExecStatus execStatus = TaskExecStatus.get(status);
        if(execStatus == TaskExecStatus.CANCEL){
            this.cancel(sourceTableName);
        }else {
            repairTaskLogService.updateStatusBySourceTable(sourceTableName, status);
        }
    }

    /**
     * 重置状态
     */
    public void resetStatusById(List<String> taskIdList, String status){
        TaskExecStatus execStatus = TaskExecStatus.get(status);
        if(execStatus == TaskExecStatus.CANCEL){
            this.cancelByTaskId(taskIdList);
        }else {
            repairTaskLogService.updateStatusById(taskIdList, status, "");
        }
    }

    public List<HotTableRepairTask> redoById(List<String> taskIdList){
        List<HotTableRepairTask> taskList = repairTaskService.queryRepairTasksById(taskIdList);
        if(BIUtil.isEmpty(taskList)){
            return null;
        }
        // 先取消
        this.cancelByTaskId(taskIdList);

        // 再执行
        this.execute(taskList);

        return taskList;
    }

    /**
     * 获取当前表批次中，已经ready的任务列表
     * doris任务需要判断hive任务是否已完成，若已完成则返回，否则不返回
     * @param waitRepairTable
     * @return
     */
    protected List<HotTableRepairTask> getReadyTask(HotTableRepairTable waitRepairTable){
        List<HotTableRepairTask> raedyTaskList = waitRepairTable.getReadyTasks();
        List<HotTableRepairTask> newReadyTaskList = new ArrayList<>();
        for(HotTableRepairTask task : raedyTaskList){
            if(task.isDorisTask()) {
                String batchName = task.getTaskBatchName();
                HotTableRepairTask successTask = waitRepairTable.getSuccessTask(batchName, DBType.Hive.toString().toLowerCase());
                if(successTask != null){
                    newReadyTaskList.add(task);
                }
            }else {
                newReadyTaskList.add(task);
            }
        }
        return newReadyTaskList;
    }

    /**
     * 创建补数热表
     * @param taskList
     * @return
     */
    public Map<String, HotTableRepairTable> createRepairTables(List<HotTableRepairTask> taskList) {
        // 按source表进行分组
        Map<String, HotTableRepairTable> repairTables = new LinkedHashMap<>();
        for(HotTableRepairTask task : taskList){
            HotTableRepairTable repairTable = repairTables.get(task.getSourceTableName());
            if(repairTable == null){
                repairTable = new HotTableRepairTable(task.getSourceTableName(), task.getSlaPriority());
                repairTables.put(repairTable.getSourceTableName(), repairTable);
            }
            repairTable.getTaskList().add(task);
        }

        return repairTables;
    }

    /**
     * 获取等待补数的表
     * @param taskList
     * @return
     */
    public Map<String, HotTableRepairTable> getWaitRepairTables(List<HotTableRepairTask> taskList){
        Map<String, HotTableRepairTable> repairTables = this.createRepairTables(taskList);
        return repairTables.entrySet().stream()
                .filter(entry -> entry.getValue().isWaiting())
                .filter(entry -> !entry.getValue().isFailed()) // 排除掉失败的表
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 记录执行日志
     * @param task
     * @param result
     * @return
     */
    protected HotTableRepairTaskLog addLog(HotTableRepairTask task, ScriptExecuteResult result){
        // 新增任务日志记录
        HotTableRepairTaskLog log = new HotTableRepairTaskLog();
        log.setLogId(Guid.id());
        log.setTaskId(task.getTaskId());
        log.setSourceTableName(task.getSourceTableName());
        log.setTaskBatchNo(task.getTaskBatchNo());
        log.setTaskScriptType(task.getTaskScriptType());
        log.setTaskExecId(result.getExecId());
        log.setTaskExecStatus(result.isSuccess() ? TaskExecStatus.RUNNING.getCode() : TaskExecStatus.FAIL.getCode());
        log.setTaskExecInfo(result.getExecInfo());
        log.setTaskServerIp(task.getTaskServerIp());
        log.setTaskExecStartTime(DateUtil.date().toString("yyyy-MM-dd HH:mm:ss"));

        // 失败，则设置结束时间
        if(!result.isSuccess()){
            log.setTaskExecEndTime(DateUtil.date().toString("yyyy-MM-dd HH:mm:ss"));
        }

        repairTaskLogService.add(log);
        return log;
    }

    /**
     * 获取待补数任务
     * @param sourceTableName
     * @return
     */
    protected List<HotTableRepairTask> getWaitTasks(String sourceTableName){
        // 等待补数的批次
        List<HotTableRepairTask> waitRepairTasks = new ArrayList<>();

        // 已按优先级和批次排序
        List<HotTableRepairTask> taskList = this.repairTaskService.queryRepairTasksByStatus(sourceTableName, Arrays.asList(TaskExecStatus.READY, TaskExecStatus.RUNNING, TaskExecStatus.SUCCESS, TaskExecStatus.FAIL));

        // 获取待补数的表
        Map<String, HotTableRepairTable> waitRepairTables = this.getWaitRepairTables(taskList);

        if(BIUtil.isEmpty(waitRepairTables)){
            return waitRepairTasks;
        }

        // 并行补数的表个数
        Integer parallelRepairTableCount =  Integer.valueOf(SC.v("hot.table.repair.parallel.table.count", "2"));
        List<String> runningTableNames = this.getRunningTableNames(waitRepairTables);
        List<String> readyTableNames = this.getReadyTableNames(waitRepairTables);
        List<String> paddingTableNames = new ArrayList<>();
        // 若正在运行的表小于并发表个数，则添加表和批次
        if(runningTableNames.size() < parallelRepairTableCount){
            paddingTableNames = readyTableNames.stream().limit(parallelRepairTableCount - runningTableNames.size()).collect(Collectors.toList());
        }


        /**
         * 新补数批次：根据单表批次阈值添加批次
         **/
        List<String> waitRepairTableNames = new ArrayList<>();
        waitRepairTableNames.addAll(runningTableNames);
        waitRepairTableNames.addAll(paddingTableNames);
        // 去重
        waitRepairTableNames = waitRepairTableNames.stream().distinct().collect(Collectors.toList());

        // 每个表可并行执行的任务数
        Integer parallelTableTaskCount =  Integer.valueOf(SC.v("hot.table.repair.parallel.table.task.count", "2"));

        for(String tableName : waitRepairTableNames){
            HotTableRepairTable table = waitRepairTables.get(tableName);
            if(table == null){
                continue;
            }
            List<HotTableRepairTask> runningTasks = table.getRunningTasks();
            // 获取ready时，需要校验doris依赖的hive是否已完成
            List<HotTableRepairTask> readyTask = this.getReadyTask(table);
            if(runningTasks.size() < parallelRepairTableCount){
                List<HotTableRepairTask> newBatches = readyTask.stream().limit(parallelTableTaskCount - runningTasks.size()).collect(Collectors.toList());
                waitRepairTasks.addAll(newBatches);
            }
        }
        return waitRepairTasks;
    }

    /**
     * 获取正在running的补数表名：只要有一个批次是running，则表为running
     * @param repairTables
     * @return
     */
    protected List<String> getRunningTableNames(Map<String, HotTableRepairTable> repairTables){
        List<String> runningTableNames = new ArrayList<>();
        for(String tableName : repairTables.keySet()){
            List<HotTableRepairTask> runningRepairs = repairTables.get(tableName).getRunningTasks();
            if(BIUtil.isNotEmpty(runningRepairs)) {
                runningTableNames.add(tableName);
            }
        }
        runningTableNames = runningTableNames.stream().distinct().collect(Collectors.toList());
        return runningTableNames;
    }

    /**
     * 获取ready的补数表名
     * @param repairTables
     * @return
     */
    protected List<String> getReadyTableNames(Map<String, HotTableRepairTable> repairTables){
        List<String> tableNames = new ArrayList<>();
        for(String tableName : repairTables.keySet()){
            // 若补数表当天是全量热化，必须待热化脚本跑完后再进行补数。避免因全量删除后，后续补数数据丢失
            HotTableDdl hotTableDdl = HotTableCacheManager.getHotTableDdlBySourceTable(tableName);
            if(hotTableDdl == null){
                continue;
            }
            List<HotTableInfo> hotTables = HotTableCacheManager.getHotTablesBySource(tableName);
            if(DataUpdateMode.get(hotTableDdl.getDdlEtlMode()) == DataUpdateMode.FULL){
                boolean isAllTodayFinished = true;
                for(HotTableInfo hotTable : hotTables){
                    if(BIUtil.isEmpty(hotTable.getDataFinishTime())){
                        isAllTodayFinished = false;
                        break;
                    }
                    boolean isTodayFinished = DateUtil.today().equals(DateUtil.parse(hotTable.getDataFinishTime(), "yyyy-MM-dd").toDateStr());
                    isAllTodayFinished = isAllTodayFinished && isTodayFinished;
                }
                if(!isAllTodayFinished){
                    continue;
                }
            }
            List<HotTableRepairTask> tasks = repairTables.get(tableName).getReadyTasks();
            if(BIUtil.isNotEmpty(tasks)) {
                tableNames.add(tableName);
            }
        }
        tableNames = tableNames.stream().distinct().collect(Collectors.toList());
        return tableNames;
    }

    protected HotTableRepairTaskService getRepairTaskService(){
        return (HotTableRepairTaskService) SpringContextUtil.getBean("hotTableRepairTaskService");
    }

    protected HotTableRepairTaskLogService getRepairTaskLogService(){
        return (HotTableRepairTaskLogService) SpringContextUtil.getBean("hotTableRepairTaskLogService");
    }
}