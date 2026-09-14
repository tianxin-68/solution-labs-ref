package com.bi.queryer.ssm.engine.accelerate.hot.etl.hive;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-09-09  20:15
 * @Description: hive增量脚本构建
 */
public class HiveIncrementalScriptBuilder extends HiveFullScriptBuilder {
    public HiveIncrementalScriptBuilder(HotTableInfo hotTable) {
        super(hotTable);
    }

    public EtlTableDdl buildScheduleScript(){
        // 是否是分区表
        boolean isPartitionTable = hotTable.isPartitionTable();

        // 建表
        HiveTableCreator tableCreator = new HiveTableCreator(hotTable);
        EtlTableDdl ddl = tableCreator.create();

        // 拼接清理分区+insert sql
        List<String> fragments = new ArrayList<>();

        fragments.add("\n-- ==========前置api==========\n");
        fragments.add(buildPreApiSql()+ ";");

        if(isPartitionTable) {
            fragments.add(buildPartitionCleanSql(ddl));
        }

        fragments.add("\n-- ==========更新热表数据==========\n");
        // 构建插入sql
        String insertSql = buildInsertOverwrite(ddl);
        fragments.add(insertSql+ ";");

        if("true".equalsIgnoreCase(SC.v("hot.table.add.analyze.enable", "false"))) {
            fragments.add("\n-- ==========执行分析热表==========\n");
            fragments.add(buildAnalyzeTableSql()+";");
        }

        String etlScriptContent = BIUtil.listToStr(fragments, "");
        ddl.setEtlScriptContent(etlScriptContent);

        return ddl;
    }

    protected String buildInsertOverwrite(EtlTableDdl ddl) {
        return super.buildInsertOverwrite(ddl);
    }

    public String buildWhereSql(String partitionFieldName) {

        Integer dataIncrementalDays = hotTable.getHotTableSource().getDataIncrementalDays();
        String startDate = DateUtil.offsetDay(DateUtil.date(), -dataIncrementalDays).toDateStr();
        String endDate = DateUtil.offsetDay(DateUtil.date(), -1).toDateStr();

        List<String> filterDateRange = this.getFilterDateRangeByDateGranularity(startDate, endDate);
        startDate = filterDateRange.get(0);
        endDate = filterDateRange.get(1);

        String whereSql = String.format(" where t1.%s between '%s' and '%s' ",
                partitionFieldName,
                startDate,
                endDate);
        return whereSql;
    }

    protected List<String> getSetParameters() {
        return super.getSetParameters();
    }

    /**
     * 清理分区
     * @return
     */
    public String buildPartitionCleanSql(EtlTableDdl ddl){
        String partitionCleanSql = "";
        Integer hotDataMaxDays = this.hotTable.getHotTableSource().getHotDataMaxDays();
        if(hotDataMaxDays == -1) {
            return partitionCleanSql;
        }

        String partitionCleanDate = DateUtil.offsetDay(DateUtil.date(), -hotDataMaxDays-1).toDateStr();
        String partitionFieldName = ddl.getPartitionField().getName();
        partitionCleanSql = "\n-- ==========清理分区==========\n";
        partitionCleanSql += String.format("ALTER TABLE %s DROP IF EXISTS PARTITION (%s='%s');",
                this.hotTable.getHotTableName(),
                partitionFieldName,
                partitionCleanDate);
        return partitionCleanSql;
    }

    protected String buildAnalyzeTableSql(){
        return super.buildAnalyzeTableSql();
    }

    protected  String buildPreApiSql() {
        return super.buildPreApiSql();
    }

    public static void main(String[] args) {
        String day = DateUtil.offsetDay(DateUtil.date(), -1).toDateStr();
        String day2 = DateUtil.offsetDay(DateUtil.date(), -750-1).toDateStr();
        System.out.println(day);
        System.out.println(day2);
    }


}
