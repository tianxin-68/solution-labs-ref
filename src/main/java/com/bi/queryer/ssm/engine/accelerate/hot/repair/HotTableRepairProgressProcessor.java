package com.bi.queryer.ssm.engine.accelerate.hot.repair;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.HotTableManager;
import com.bi.queryer.ssm.engine.accelerate.hot.repair.executor.BaseScriptExecutor;
import com.bi.queryer.ssm.engine.accelerate.hot.repair.executor.ScriptExecuteResult;
import com.bi.queryer.ssm.engine.accelerate.hot.repair.executor.ScriptExecutorFactory;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTaskBatch;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.ssm.mgr.hot.HotTableRepairTaskLogService;
import com.bi.queryer.ssm.mgr.hot.HotTableRepairTaskService;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 19:15 2024/10/14
 * 职责：
 * 1、更新单个任务的执行进度和状态
 * 2、核对已完成的任务数据量
 * 3、更新热表的最小热化日期
 * @Description 补数进度处理器
 **/
public class HotTableRepairProgressProcessor {

    protected HotTableRepairTaskService repairTaskService;

    protected HotTableRepairTaskLogService repairTaskLogService;
    public HotTableRepairProgressProcessor(){
        this.repairTaskService = this.getRepairTaskService();
        this.repairTaskLogService = this.getRepairTaskLogService();
    }

    /**
     * 处理内容：
     * 1、更新运行状态
     * 2、check数据
     * 3、更新最小热化日期
     */
    public void process(){
        // 更新执行状态
        this.updateExecStatus();

        // 异步校验数据
        this.checkData();

        // 更新最小热化日期
        this.updateHotMinDate();
    }

    public void updateExecStatus(){
        // 更新正在运行任务状态
        List<HotTableRepairTask> runningTasks = repairTaskService.queryRepairTasksByStatus(null, Arrays.asList(TaskExecStatus.RUNNING));
        for(HotTableRepairTask task : runningTasks){
            BaseScriptExecutor scriptExecutor = ScriptExecutorFactory.createScriptExecutor(task.getTaskScriptType());
            ScriptExecuteResult execResult = scriptExecutor.getProgress(task);
            if(execResult.isSuccess()){
                task.setTaskExecStatus(execResult.getExecStatus());
                task.setTaskExecInfo(execResult.getExecInfo());
                task.setTaskExecEndTime(execResult.getExecEndTime());
                repairTaskLogService.updateStatusByTask(task);
            }
        }
    }

    /**
     * 校验逻辑：
     * 1、trino_view vs hot_hive
     * 2、hot_hive vs hot_doris
     * @param
     */
    protected void checkData(){
        // 核对已完成且未核对的批次数据
        List<HotTableRepairTask> successTasks = repairTaskService.queryRepairTasksByStatus(null, Arrays.asList(TaskExecStatus.SUCCESS));
        List<HotTableRepairTaskBatch> waitDataCheckBatchTasks = convert2Batch(successTasks);
        // 只校验hive和doris都完成的批次
        waitDataCheckBatchTasks = waitDataCheckBatchTasks.stream().filter(b->b.isExecSuccess()).collect(Collectors.toList());

        if(BIUtil.isEmpty(waitDataCheckBatchTasks)){
            return;
        }

        List<HotTableRepairTaskBatch> taskBatchList = waitDataCheckBatchTasks;
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.submit(() -> {
            for(HotTableRepairTaskBatch batchTask : taskBatchList){
                HotTableRepairTask hiveTask = batchTask.getHiveTask();
                if(hiveTask != null && !TaskExecStatus.get(hiveTask.getDataCheckResult()).isEnd()) {
                    this.trinoVsHive(batchTask);
                    this.sleep(1000);
                }

                HotTableRepairTask dorisTask = batchTask.getDorisTask();
                if(dorisTask != null && !TaskExecStatus.get(dorisTask.getDataCheckResult()).isEnd()) {
                    this.hiveVsDoris(batchTask);
                    this.sleep(1000);
                }
            }
        });
        singleThreadExecutor.shutdown();
    }

    /**
     * 更新热化的最小日期
     * 逻辑：按批次号依次从小到大排序，从最近日期依次向前找，若都已完成且校验成功，则更新最小日期。碰到未完成或校验失败或不连续的批次，则停止。
     */
    protected void updateHotMinDate(){
        List<HotTableRepairTask> successTasks = repairTaskService.queryRepairTasksByStatus(null, Arrays.asList(TaskExecStatus.SUCCESS));
        // 筛选未更新最小日期的任务，避免重复更新
        successTasks = successTasks.stream().filter(t -> Enabled.isFalse(t.getIsHotMinDateUpdated())).collect(Collectors.toList());
        List<HotTableRepairTaskBatch> batchTasks = convert2Batch(successTasks);

        // 筛选数据校验通过的批次
        batchTasks = batchTasks.stream().filter(b -> b.isDataCheckSuccess()).collect(Collectors.toList());

        if(BIUtil.isEmpty(batchTasks)){
            return;
        }
        // 按源表分组
        Map<String, List<HotTableRepairTaskBatch>> tableBatchTasks = batchTasks.stream().collect(Collectors.groupingBy(HotTableRepairTaskBatch::getSourceTableName));
        Map<String, String> tableMinDates = new HashMap<>();
        List<String> updatedMinDateTaskIdList = new ArrayList<>();
        for(String sourceTableName : tableBatchTasks.keySet()){
            List<HotTableRepairTaskBatch> batches = tableBatchTasks.get(sourceTableName);
            // 先排序
            Collections.sort(batches);

            // 获取热表当前最小热化日期
            HotTableInfo hotTable = HotTableCacheManager.getHotTablesBySource(sourceTableName, DBType.Doris);
            if(hotTable == null){
                continue;
            }
            String minDate = hotTable.getDataMinDate();

            for(HotTableRepairTaskBatch batch : batches ){
                HotTableRepairTask dorisTask = batch.getDorisTask();
                if(dorisTask == null){
                    break;
                }
                String repairStartDate = dorisTask.getTaskStartDate();
                String repairEndDate = dorisTask.getTaskEndDate();
                long diff = DateUtil.between(DateUtil.parseDate(repairEndDate), DateUtil.parseDate(minDate), DateUnit.DAY, false);
                // minDate - 补数结束日期 = 1 ，则minDate = 补数开始日期
                if(diff == 1){
                    minDate = repairStartDate;
                    updatedMinDateTaskIdList.add(dorisTask.getTaskId());
                    HotTableRepairTask hiveTask = batch.getHiveTask();
                    if(hiveTask != null){
                        updatedMinDateTaskIdList.add(hiveTask.getTaskId());
                    }
                }else {
                    break;
                }
            }
            if(!hotTable.getDataMinDate().equals(minDate)) {
                tableMinDates.put(sourceTableName, minDate);
            }
        }

        // 更新任务标识
        repairTaskLogService.updateMinDateStatus(updatedMinDateTaskIdList, Enabled.YES.getId());

        // 更新热表minDate
        HotTableManager.updateMinDateBySourceTables(tableMinDates);
    }

    protected void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 核对每天数据量
     * @param batchTask
     */
    protected void trinoVsHive(HotTableRepairTaskBatch batchTask){
        String sourceTableName = batchTask.getSourceTableName();
        // 获取hive任务
        HotTableRepairTask hiveTask = batchTask.getHiveTask();
        HotTableInfo hiveHotTable = HotTableCacheManager.getHotTablesBySource(sourceTableName, DBType.Trino);

        String partitionFieldName = HotTableCacheManager.getPartitionFieldName(sourceTableName);
        if(BIUtil.isEmpty(partitionFieldName)){
            return;
        }

        String trinoVsHiveSql = getTrinoVsHiveSql(sourceTableName, hiveHotTable.getHotTableName(), partitionFieldName, hiveTask.getTaskStartDate(), hiveTask.getTaskEndDate());
        BaseDao dao = DBUtil.getBaseDao();
        try {
            List checkResult = dao.queryObjectListByTrinoSQL(trinoVsHiveSql, new HashMap(), DataSourceType.Trino_Slave02);
            if(BIUtil.isNotEmpty(checkResult)){
                hiveTask.setDataCheckResult(TaskExecStatus.FAIL.getCode());
                hiveTask.setDataCheckInfo(String.format("trino vs hive 记录数不一致天数：%s", checkResult.size()));
            }else {
                hiveTask.setDataCheckResult(TaskExecStatus.SUCCESS.getCode());
            }
        }catch(Exception e){
            hiveTask.setDataCheckResult(TaskExecStatus.FAIL.getCode());
            hiveTask.setDataCheckInfo(e.getMessage());
        }

        // 修改状态
        repairTaskLogService.updateDataCheckStatusByTask(hiveTask);
    }

    /**
     * 核对总数据量
     * @param batchTask
     */
    protected void hiveVsDoris(HotTableRepairTaskBatch batchTask){
        String sourceTableName = batchTask.getSourceTableName();
        HotTableRepairTask dorisTask = batchTask.getDorisTask();
        HotTableInfo hotTable = HotTableCacheManager.getHotTablesBySource(sourceTableName, DBType.Doris);

        String partitionFieldName = HotTableCacheManager.getPartitionFieldName(sourceTableName);
        if(BIUtil.isEmpty(partitionFieldName)){
            return;
        }
        String sql = this.getHiveVsDorisSql(hotTable.getHotTableName(), partitionFieldName, dorisTask.getTaskStartDate(), dorisTask.getTaskEndDate());
        BaseDao dao = DBUtil.getBaseDao();
        try {
            List hiveResult = dao.queryObjectListByTrinoSQL(sql, new HashMap(), DataSourceType.Trino_Slave02);
            List dorisResult = dao.queryObjectListByDorisSQL(sql, DataSourceType.Doris_Master);
            if(BIUtil.isNotEmpty(hiveResult) && BIUtil.isNotEmpty(dorisResult)){
                Integer hiveCount =  Integer.valueOf(((Map)hiveResult.get(0)).get("f_count") + "");
                Integer dorisCount = Integer.valueOf(((Map)dorisResult.get(0)).get("f_count") + "");
                if(!hiveCount.equals(dorisCount)){
                    dorisTask.setDataCheckResult(TaskExecStatus.FAIL.getCode());
                    dorisTask.setDataCheckInfo(String.format("hive和doris数据不一致，hive数据量：%s，doris数据量：%s", hiveCount, dorisCount));
                }else{
                    dorisTask.setDataCheckResult(TaskExecStatus.SUCCESS.getCode());
                }
            }else {
                String msg = (BIUtil.isEmpty(hiveResult) ? "hive无数据" : "") + (BIUtil.isEmpty(dorisResult) ? " doris无数据" : "");
                dorisTask.setDataCheckResult(TaskExecStatus.FAIL.getCode());
                dorisTask.setDataCheckInfo(msg);
            }
        }catch (Exception e){
            dorisTask.setDataCheckResult(TaskExecStatus.FAIL.getCode());
            dorisTask.setDataCheckInfo(e.getMessage());
        }
        // 修改状态
        repairTaskLogService.updateDataCheckStatusByTask(dorisTask);
    }

    protected String getTrinoVsHiveSql(String trinoTableName, String hiveTableName, String partitionFieldName, String startDate, String endDate){
        String sql = "/* ssm:hot-data-repair-check(trino vs hive)*/ select  " +
                " tx1.dt as source_dt, " +
                " tx2.dt as hot_dt, " +
                " tx1.cnt as source_cnt, " +
                " tx2.cnt as hot_cnt, " +
                " (tx1.cnt - tx2.cnt) as diff_cnt " +
                "from ( " +
                " select  " +
                "  t.${partitionFieldName} as dt,  " +
                "  count(1) as cnt  " +
                " from ${trinoTableName} t  " +
                " where t.${partitionFieldName} between '${startDate}' and '${endDate}' " +
                " group by t.${partitionFieldName}  " +
                ") tx1 left join ( " +
                " select  " +
                "  t.${partitionFieldName} as dt,  " +
                "  count(1) as cnt  " +
                " from ${hiveTableName} t " +
                " where t.${partitionFieldName} between '${startDate}' and '${endDate}' " +
                " group by t.${partitionFieldName} " +
                ") tx2 " +
                "on tx1.dt = tx2.dt " +
                "where 1=1 " +
                "  and (tx1.cnt - tx2.cnt) <> 0 or (tx1.cnt - tx2.cnt) is null";

        Map<String, String> replacements = new HashMap<>();
        replacements.put("trinoTableName", trinoTableName);
        replacements.put("hiveTableName", hiveTableName);
        replacements.put("partitionFieldName", partitionFieldName);
        replacements.put("startDate", startDate);
        replacements.put("endDate", endDate);

        for(String key : replacements.keySet()){
            sql = sql.replaceAll(String.format("\\$\\{%s\\}", key), replacements.get(key));
        }
        return sql;
    }

    /**
     * 获取hive 和 doris对比sql
     * @param hotTableName
     * @param partitionFieldName
     * @param startDate
     * @param endDate
     * @return
     */
    protected String getHiveVsDorisSql(String hotTableName, String partitionFieldName, String startDate, String endDate){
        String sql = String.format("/* ssm:hot-data-repair-check(hive vs doris)*/ select count(1) as f_count from %s where %s between '%s' and '%s'", hotTableName, partitionFieldName, startDate, endDate);
        return sql;
    }

    /**
     * 将一个表的hive和doris同批次任务合并
     * @param taskList
     * @return
     */
    public List<HotTableRepairTaskBatch> convert2Batch(List<HotTableRepairTask> taskList){
        List<HotTableRepairTaskBatch> scriptList = new ArrayList<>();
        Map<String, HotTableRepairTaskBatch> scriptMap = new LinkedHashMap<>();

        for(HotTableRepairTask task : taskList){
            String key = String.format("%s_%s", task.getSourceTableName(), task.getTaskBatchNo());
            HotTableRepairTaskBatch batchTask = scriptMap.get(key);
            if(batchTask == null){
                batchTask = new HotTableRepairTaskBatch();
            }
            batchTask.setSourceTableName(task.getSourceTableName());
            batchTask.setTaskBatchNo(task.getTaskBatchNo());
            batchTask.setSlaPriority(task.getSlaPriority());

            if(DBType.getType(task.getTaskScriptType()) == DBType.Hive){
                batchTask.setHiveTask(task);
            }else {
                batchTask.setDorisTask(task);
            }
            scriptMap.put(key, batchTask);
        }
        scriptList = new ArrayList<>(scriptMap.values());
        return scriptList;
    }

    protected HotTableRepairTaskService getRepairTaskService(){
        return (HotTableRepairTaskService) SpringContextUtil.getBean("hotTableRepairTaskService");
    }

    protected HotTableRepairTaskLogService getRepairTaskLogService(){
        return (HotTableRepairTaskLogService) SpringContextUtil.getBean("hotTableRepairTaskLogService");
    }
}
