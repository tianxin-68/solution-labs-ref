package com.bi.queryer.ssm.engine.accelerate.hot;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlManager;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.doris.Hive2DorisManager;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.hive.Trino2HiveManager;
import com.bi.queryer.ssm.engine.accelerate.hot.repair.HotTableRepairManager;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdlChange;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.mgr.hot.HotTableDdlChangeService;
import com.bi.queryer.ssm.mgr.hot.HotTableInfoService;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 13:58 2024/8/21
 * @Description 热表管理器：所有热表相关操作入口类
 **/
public abstract class HotTableManager {

    /**
     * 热表信息重置
     * 每天01:00定时调度（配置到datastudio-作业调度中）
     */
    public static List<String> initialize(){
        return initialize(null); // 重置全部
    }


    /**
     * 初始化源表热化数据信息
     * @param sourceTableName
     * @return
     */
    public static List<String> initialize(String sourceTableName){
        List<String> logs = new ArrayList<>();
        // 重置基础信息表
        String tag = "重置热数据基础信息表";
        logs.add(HotUtil.logFormat(tag, false));

        // 先请缓存再初始化表数据
        HotTableCacheManager.clear();
        HotTableCacheManager.refresh();

        // 需要判断表的更新模式
        getService().initialize(sourceTableName);

        logs.add(HotUtil.logFormat(tag, true));

        // 重新初始化缓存
        tag = "重置热数据表缓存(所有服务器)";
        logs.add(HotUtil.logFormat(tag, false));

        HotTableCacheManager.refreshAllServer();

        logs.add(HotUtil.logFormat(tag, true));

        Map<String, HotTableSourceRelation> sourceRelations = new LinkedHashMap<>();
        List<HotTableInfo> hotTables = HotTableCacheManager.getHotTables();
        for(HotTableInfo t : hotTables){
            DBType dbType = DBType.getType(t.getHotDbEngine());
            HotTableSourceRelation relation = sourceRelations.get(t.getSourceTableName());
            if(relation == null) {
                relation = new HotTableSourceRelation(t.getSourceTableName());
            }
            if(dbType == DBType.Trino) {
                relation.trinoHotTable = t;
            }
            if(dbType == DBType.Doris) {
                relation.dorisHotTable = t;
            }
            sourceRelations.put(t.getSourceTableName(), relation);
        }

        // 若源表名不为空，则只处理特定源表
        if(BIUtil.isNotEmpty(sourceTableName)) {
            HotTableSourceRelation relation = sourceRelations.get(sourceTableName);
            sourceRelations.clear();
            sourceRelations.put(sourceTableName, relation);
        }

        // 重置建表&etl脚本
        for(HotTableSourceRelation relation : sourceRelations.values()){
            if(relation == null){
                continue;
            }
            List<String> etlLogs = initializeEtl(relation.trinoHotTable, relation.dorisHotTable);
            logs.addAll(etlLogs);
            sleep();
        }

        return logs;
    }

    protected static List<String> initializeEtl(HotTableInfo trinoHotTable, HotTableInfo dorisHotTable){
        List<String> logs = new ArrayList<>();

        // check源表是否存在，不存在不处理后续逻辑
        MetaTable metaTable = SSDMetaCacheManager.getTableByFullName(trinoHotTable.getSourceTableName());
        if(metaTable == null){
            logs.add(String.format("源表不存在，不处理：%s", trinoHotTable.getSourceTableName()));
            return logs;
        }

        String tag = "etl(trino->hive)";
        logs.add(HotUtil.logFormat(tag, false));

        HotEtlManager etlManager = createEtlManager(trinoHotTable); //new Trino2HiveManager();
        List<String> etlLogs = etlManager.execute(trinoHotTable);

        logs.addAll(etlLogs);
        logs.add(HotUtil.logFormat(tag, true));

        sleep();

        // etl:hive->doris
        tag = "etl(hive->doris)";
        logs.add(HotUtil.logFormat(tag, false));

        etlManager = createEtlManager(dorisHotTable); //new Hive2DorisManager();
        etlLogs = etlManager.execute(dorisHotTable);

        logs.addAll(etlLogs);
        logs.add(HotUtil.logFormat(tag, true));

        //重新初始化后，将历史的表变更设置为失效
        HotTableDdlChangeService hotTableDdlChangeService = (HotTableDdlChangeService)SpringContextUtil.getBean("hotTableDdlChangeService");
        HotTableDdlChange hotTableDdlChange = new HotTableDdlChange();
        hotTableDdlChange.setSourceTableName(trinoHotTable.getSourceTableName());
        hotTableDdlChange.setIsActive(Enabled.NO.getId());
        hotTableDdlChange.setUpdatedBy(HotUtil.getHotTableOwner());
        hotTableDdlChangeService.update(hotTableDdlChange);

        return logs;
    }

    public static void disable(String hotTableName){
        // 修改库表
        getService().disable(hotTableName);

        // 同步刷新服务器缓存
       HotTableCacheManager.refreshAllServer();
    }

    public static void disableBySource(String sourceTableName,String operator) {
        List<HotTableInfo> tables = HotTableCacheManager.getHotTablesBySource(sourceTableName);

        //从缓存没有找到，再从数据库查询一次
        if (CollUtil.isEmpty(tables)) {
            tables = getService().getHotTableInfoBySourceTableName(sourceTableName);
        }

        if (CollUtil.isEmpty(tables)) {
            return;
        }
        // 添加操作记录
        HotTableDdlChangeService hotTableDdlChangeService = (HotTableDdlChangeService) SpringContextUtil.getBean("hotTableDdlChangeService");
        hotTableDdlChangeService.add(sourceTableName, operator);

        // 停用并删除补数记录
        HotTableRepairManager.delete(sourceTableName);

        // 禁用热表和刷新缓存
        for (HotTableInfo t : tables) {
            disable(t.getHotTableName());
        }
    }

    /**
     * 更新作业完成时间
     * @param etlJobName
     * @param etlJobFinishTime
     */
    public static void onEtlJobFinish(String etlJobName, String etlJobFinishTime) {

        //判断作业名是否是热表作业，不是则不处理
        List<HotTableInfo> hotTables = HotTableCacheManager.getHotTables();
        long isJobNameExist = hotTables.stream().filter(h -> h.getEtlJobName().equalsIgnoreCase(etlJobName)).count();
        if (isJobNameExist == 0) {
            return;
        }

        // 更新表
        getService().updateFinishTime(etlJobName, etlJobFinishTime);

        // 更新缓存
        HotTableCacheManager.refreshAllServer();
    }

    public static void updateMinDateBySourceTables(Map<String, String> tableMinDates){
        for(String sourceTableName : tableMinDates.keySet()){
            String minDate = tableMinDates.get(sourceTableName);
            getService().updateMinDateBySourceTable(sourceTableName, minDate);
        }

        // 更新缓存
        HotTableCacheManager.refreshAllServer();
    }

    protected static void sleep(){
        try {
            Thread.sleep( 100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static HotTableInfoService getService(){
        return (HotTableInfoService) SpringContextUtil.getBean("hotTableInfoService");
    }

    public static HotEtlManager createEtlManager(HotTableInfo hotTableInfo){
        HotEtlManager etlManager = null;
        DBType dbType = DBType.getType(hotTableInfo.getHotDbEngine());
        switch (dbType){
            case Trino:
                etlManager = new Trino2HiveManager();
                break;
            case Doris:
                etlManager = new Hive2DorisManager();
                break;
        }
        return etlManager;
    }
}
