package com.bi.queryer.ssm.engine.accelerate.hot.repair;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.HotTableManager;
import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.enums.DataUpdateMode;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlManager;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlScriptBuilder;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdlChange;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableSource;
import com.bi.queryer.ssm.mgr.hot.HotTableRepairTaskService;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:07 2024/10/11
 * @Description 补数脚本构建器
 **/
public class HotTableRepairTaskCreator {

    public List<HotTableRepairTask> buildRepairTask(String sourceTableName){
        List<HotTableRepairTask> taskList = new ArrayList<>();

        List<HotTableInfo> hotTables = this.getRepairTables(sourceTableName);
        for(HotTableInfo hotTable : hotTables) {
            taskList.addAll(this.buildSingleTableRepairTask(hotTable));
        }
        if(BIUtil.isEmpty(taskList)){
            return taskList;
        }
        // 存储补数信息
        HotTableRepairTaskService service = this.getService();
        service.saveRepairTasks(taskList);

        return taskList;
    }

    protected List<HotTableInfo> getRepairTables(String sourceTableName){
        List<HotTableInfo> hotTables = null;
        if(BIUtil.isEmpty(sourceTableName)){
            // 为空时，则处理所有表
            hotTables = HotTableCacheManager.getHotTables();
        }else {
            hotTables = HotTableCacheManager.getHotTablesBySource(sourceTableName);
        }
        List<HotTableRepairTask> repairTasks = getService().queryRepairTasksBySourceTable(sourceTableName);
        // 按sourceTable分组
        Map<String, List<HotTableRepairTask>> tableRepairTasks = repairTasks.stream().collect(Collectors.groupingBy(HotTableRepairTask::getSourceTableName));
        List<HotTableInfo> newHotTables = new ArrayList<>();
        for(HotTableInfo hotTable : hotTables){
            List<HotTableRepairTask> tasks = tableRepairTasks.get(hotTable.getSourceTableName());
            // 若有补数任务，则不处理，避免跨天补数（ddl改变后，系统会删除对应源表补数任务）
            if(BIUtil.isNotEmpty(tasks)){
                continue;
            }
            if(isNeedRepair(hotTable)){
                newHotTables.add(hotTable);
            }
        }
        return newHotTables;
    }

    public List<HotTableRepairTask> buildSingleTableRepairTask(HotTableInfo hotTable){
        List<HotTableRepairTask> tableRepairList = new ArrayList<>();
        if(!this.isNeedRepair(hotTable)){
            return tableRepairList;
        }
        HotEtlManager etlManager = HotTableManager.createEtlManager(hotTable);
        if(etlManager == null) {
            return tableRepairList;
        }
        // 复用建表和etl脚本，便于后面补数脚本使用
        HotEtlScriptBuilder builder = etlManager.createScriptBuilder(hotTable);
        EtlTableDdl ddl = builder.buildScheduleScript();

        // 补数脚本
        ddl = builder.buildRepairScript(ddl);

        // 创建补数批次
        tableRepairList = buildSingleTableRepairTask(ddl, hotTable);

        return tableRepairList;
    }

    public List<HotTableRepairTask> buildSingleTableRepairTask(EtlTableDdl ddl, HotTableInfo hotTableInfo){
        List<HotTableRepairTask> tableRepairList = new ArrayList<>();
        DateTime startDate = DateUtil.offset(DateUtil.parseDate(hotTableInfo.getDataMinDate()), DateField.DAY_OF_YEAR, -1);
        Long hotDays = DateUtil.between(DateUtil.parseDate(hotTableInfo.getDataMinDate()), DateUtil.parseDate(hotTableInfo.getDataMaxDate()), DateUnit.DAY) + 1;
        if(hotDays >= hotTableInfo.getHotTableSource().getHotDataRequireDays()){
            return tableRepairList;
        }
        Integer repairDays = hotTableInfo.getHotTableSource().getHotDataRequireDays() - hotDays.intValue();
        DateTime endDate =  DateUtil.offset(startDate, DateField.DAY_OF_YEAR, -1 * repairDays);//DateUtil.offset(DateUtil.date(), DateField.DAY_OF_YEAR, -750);

        DBType dbType = DBType.getType(hotTableInfo.getHotDbEngine());
        int index = 1;
        // 按30天进行分配补数
        while(startDate.getTime() > endDate.getTime()){
            String script = ddl.getRepairScriptContent();
            int batchDays = hotTableInfo.getHotTableSource().getRepairDataBatchDays();
            DateTime batchEndDate = DateUtil.offset(startDate,  DateField.DAY_OF_YEAR, -1 * batchDays);
            // 如果批次结束日期小于补数的结束日期，则批次结束日期=补数结束日期
            if(batchEndDate.getTime() < endDate.getTime()){
                batchEndDate = endDate;
            }
            String startStr = startDate.toString("yyyy-MM-dd");
            String endStr = batchEndDate.toString("yyyy-MM-dd");

            String scriptType = dbType == DBType.Trino ? "hive" : "doris";
            String batchNo =  index + "";//batchEndDate.toString("yyyyMMdd") + "_" + startDate.toString("yyyyMMdd");
            index++;

            if(dbType == DBType.Trino){
                // 替换where
                script = StringUtils.replaceOnce(script,"${hivevar:v_date}", endStr);
                script = StringUtils.replaceOnce(script,"${hivevar:v_date}", startStr);
            }
            if(dbType == DBType.Doris){
                // 替换delete
                script = StringUtils.replaceOnce(script,"${v_date}", endStr);
                script = StringUtils.replaceOnce(script,"${v_date}", startStr);

                // 替换where
                script = StringUtils.replaceOnce(script,"${v_date}", endStr);
                script = StringUtils.replaceOnce(script,"${v_date}", startStr);

                // v_label
                // 此处不处理，再最终执行时替换：避免重复调用时label被使用
                //String loadLabel = hotTableInfo.getEtlJobName() + "_" + batchNo;
                //script = StringUtils.replaceOnce(script,"${v_label}", loadLabel);

                // data infile
                String dataInFiles = BIUtil.listToStr(HotUtil.buildDataFileListByMonth(hotTableInfo.getHotTableName(), endStr, startStr), ",", "\"");
                String replacement = String.format(" infile(%s)", dataInFiles);
                String pattern = " infile\\([^\\)]*\\)";
                script = script.replaceAll(pattern, replacement);
            }

            HotTableRepairTask repairTask = new HotTableRepairTask();
            repairTask.setTaskId(Guid.id());
            repairTask.setSourceTableName(hotTableInfo.getSourceTableName());
            repairTask.setTaskBatchNo(batchNo);
            repairTask.setTaskStartDate(endStr);
            repairTask.setTaskEndDate(startStr);
            repairTask.setTaskScriptType(scriptType);
            repairTask.setTaskScriptContent(script);
            repairTask.setTaskExecStatus(TaskExecStatus.READY.getCode());
            repairTask.setCreatedBy(HotUtil.getHotTableOwner());
            repairTask.setTaskServerIp(BIUtil.getServerIP());

            tableRepairList.add(repairTask);

            startDate = DateUtil.offset(batchEndDate,  DateField.DAY_OF_YEAR, -1);;
        }

        return tableRepairList;
    }

    protected boolean isNeedRepair(HotTableInfo hotTable){
        HotTableSource source = hotTable.getHotTableSource();
        Integer hotDataRequireDays = source.getHotDataRequireDays();
        if(hotDataRequireDays == null || hotDataRequireDays <= 0){
            return false;
        }

        if(Enabled.isFalse(source.getIsAutoRepairData())){
            return false;
        }

        // 只处理增量表
        if(DataUpdateMode.get(source.getDataUpdateMode()) != DataUpdateMode.INCREMENTAL){
            return false;
        }

        String hotMinDate = hotTable.getDataMinDate();
        String hotMaxDate = hotTable.getDataMaxDate();

        // 若需要热化的天数大于已热化的天数(hotMaxDate - hotMinDate)，则需补数
        Long hotDays = DateUtil.between(DateUtil.parseDate(hotMaxDate), DateUtil.parseDate(hotMinDate), DateUnit.DAY) + 1;

        // 需要补数的差异天数
        int repairDiffDays = Integer.valueOf(SC.v("hot.table.repair.diff.days","10"));
        if(hotDataRequireDays - hotDays <= repairDiffDays){
            return false;
        }

        // 当前视图ddl若有变化，则不补数
        HotTableDdlChange ddlChange = HotTableCacheManager.getHotTableDdlChangeBySourceTable(hotTable.getSourceTableName());
        if(ddlChange != null && ddlChange.getDdlChangeCnt() > 0){
            return false;
        }
        return true;
    }

    public void deleteRepairTask(String sourceTableName){
        HotTableRepairTaskService service = getService();
        List<String> tableNames = new ArrayList<>();
        tableNames.add(sourceTableName);
        service.delete(tableNames);
    }

    protected HotTableRepairTaskService getService(){
        return (HotTableRepairTaskService) SpringContextUtil.getBean("hotTableRepairTaskService");
    }
}
