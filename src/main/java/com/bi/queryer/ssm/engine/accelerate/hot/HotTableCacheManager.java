package com.bi.queryer.ssm.engine.accelerate.hot;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.enums.DataUpdateMode;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableField;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.meta.SysEtlJobInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdlChange;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableSource;
import com.bi.queryer.ssm.mgr.hot.HotTableDdlChangeService;
import com.bi.queryer.ssm.mgr.hot.HotTableDdlService;
import com.bi.queryer.ssm.mgr.hot.HotTableInfoService;
import com.bi.queryer.ssm.mgr.hot.HotTableSourceService;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.MemoryCacheSyncSSMService;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.sys.startup.InitializableModule;
import com.bi.queryer.sys.startup.SystemInitializer;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:56 2024/8/19
 * @Description 热表管理器：负责热表基础信息缓存、模型匹配、定时调度等。是热表对外使用的入口。
 **/
public class HotTableCacheManager implements Initializable {

    //热表配置表缓存
    protected static Map<String, HotTableSource> hotTableSourceMap = new HashMap<>();

    protected static Map<String, HotTableDdl>  hotTableDdlMap = new HashMap<>();

    protected static Map<String,HotTableDdlChange> hotTableDdlChangeMap = new HashMap<>();

    /**
     * 热表cache
     */
    protected static List<HotTableInfo> hotTablesCache = new CopyOnWriteArrayList<>();
    @Override
    public void initialize(Map<String, ?> initParams) throws BIException {
        refresh();
    }

    /**
     * 刷新缓存
     */
    public static void refresh() {

        clear();

        //热表ddl信息
        HotTableDdlService hotTableDdlService = (HotTableDdlService)SpringContextUtil.getBean("hotTableDdlService");
        List<HotTableDdl> hotTableDdlList = hotTableDdlService.getAll();
        buildHotTableDdl(hotTableDdlList);

        //热表变更信息
        HotTableDdlChangeService hotTableDdlChangeService = (HotTableDdlChangeService)SpringContextUtil.getBean("hotTableDdlChangeService");
        List<HotTableDdlChange> hotTableDdlChangeList = hotTableDdlChangeService.queryHotTableDdlChange();
        buildHotTableDdlChange(hotTableDdlChangeList);

        //初始化热表配置
        HotTableSourceService hotTableSourceService = (HotTableSourceService) SpringContextUtil.getBean("hotTableSourceService");
        List<HotTableSource> hotTableSourceList = hotTableSourceService.getAllHotTableSource();
        buildHotTableSource(hotTableSourceList);

        HotTableInfoService service = (HotTableInfoService) SpringContextUtil.getBean("hotTableInfoService");
        List<HotTableInfo> hotTableInfoList = service.queryAllHotTableInfo();
        buildHotTableInfo(hotTableInfoList);

    }

    public static void buildHotTableSource(List<HotTableSource> hotTableSourceList){
        if(CollUtil.isEmpty(hotTableSourceList)){
            return;
        }

        for(HotTableSource hotTableSource : hotTableSourceList){
            //处理数据处理模式是增量还是全量
            hotTableSource.setDataUpdateMode(getTableUpdateMode(hotTableSource));
            hotTableSourceMap.put(hotTableSource.getSourceTableName(),hotTableSource);
        }
    }

    public static void buildHotTableDdl(List<HotTableDdl> hotTableDdlList) {
        if (CollUtil.isEmpty(hotTableDdlList)) {
            return;
        }

        for (HotTableDdl hotTableDdl : hotTableDdlList) {
            hotTableDdlMap.put(hotTableDdl.getSourceTableName(), hotTableDdl);
        }
    }

    public static void buildHotTableDdlChange(List<HotTableDdlChange> hotTableDdlChangeList) {
        if(CollUtil.isEmpty(hotTableDdlChangeList)){
            return;
        }

        for(HotTableDdlChange hotTableDdlChange : hotTableDdlChangeList){
            hotTableDdlChangeMap.put(hotTableDdlChange.getSourceTableName(),hotTableDdlChange);
        }
    }

    /**
     * 构建表的更新模式
     */
    public static String getTableUpdateMode(HotTableSource hotTableSource) {

        String sourceTableName = hotTableSource.getSourceTableName();

        //ssm_hot_table_ddl是否存在记录  否-> 全量
        HotTableDdl hotTableDdl = hotTableDdlMap.get(sourceTableName);
        if (hotTableDdl == null) {
            return DataUpdateMode.FULL.getCode();
        }

        //ssm_hot_table_ddl_change是否存在is_active = 1 的记录   否-> 全量
        HotTableDdlChange hotTableDdlChange = hotTableDdlChangeMap.get(sourceTableName);
        if (hotTableDdlChange != null && hotTableDdlChange.getDdlChangeCnt() > 0) {
            return DataUpdateMode.FULL.getCode();
        }

        //ssm_hot_table_source表data_update_mode是否为增量 否-> 全量
        if (DataUpdateMode.INCREMENTAL != DataUpdateMode.get(hotTableSource.getDataUpdateMode())) {
            return DataUpdateMode.FULL.getCode();
        }

        //ssm_hot_table_source表是否为分区表 否-> 全量
        if (!Enabled.value(hotTableSource.getIsNeedPartition())) {
            return DataUpdateMode.FULL.getCode();
        }

        //时间粒度是否为日  否-> 全量
        DateGranularity dateGranularity = DateGranularity.get(hotTableSource.getDateGranularity());
        if (DateGranularity.DAY != dateGranularity) {
            return DataUpdateMode.FULL.getCode();
        }

        return DataUpdateMode.INCREMENTAL.getCode();
    }

    public static void buildHotTableInfo(List<HotTableInfo> hotTableInfoList){

        if(CollUtil.isEmpty(hotTableInfoList)){
            return;
        }

        List<String> etlJobNames = new ArrayList<>();
        for(HotTableInfo hotTableInfo : hotTableInfoList){
            HotTableSource hotTableSource = hotTableSourceMap.get(hotTableInfo.getSourceTableName());
            hotTableInfo.setHotTableSource(hotTableSource);

            etlJobNames.add(hotTableInfo.getEtlJobName());
        }

        // 更新最近作业完成时间
        HotTableInfoService service = (HotTableInfoService) SpringContextUtil.getBean("hotTableInfoService");
        List<SysEtlJobInfo> etlJobEntities = service.queryEtlJobByNames(etlJobNames);
        if(BIUtil.isNotEmpty(etlJobEntities)) {
            Map<String, SysEtlJobInfo> etlJobEntityMap = etlJobEntities.stream().collect(Collectors.toMap(SysEtlJobInfo::getEtlJob, s->s, (s1, s2)->s1));
            Map<String, HotTableInfo> hotTableEtlJobMap = hotTableInfoList.stream().collect(Collectors.toMap(HotTableInfo::getEtlJobName, t->t, (t1,t2)->t1));
            for(String etlJobName : hotTableEtlJobMap.keySet()){
                HotTableInfo tableInfo = hotTableEtlJobMap.get(etlJobName);
                SysEtlJobInfo etlJobEntity = etlJobEntityMap.get(etlJobName);
                if(etlJobEntity != null && tableInfo != null) {
                    tableInfo.setRecentDataFinishTime(etlJobEntity.getLastEndTime());
                }
            }
        }

        hotTablesCache.addAll(hotTableInfoList);
    }


    public static void disable(String hotTableName){
        // 删除缓存
        List<HotTableInfo> tableInfos = hotTablesCache.stream().filter(t->t.getHotTableName().equalsIgnoreCase(hotTableName)).collect(Collectors.toList());
        if(BIUtil.isEmpty(tableInfos)){
            return;
        }
        for(HotTableInfo t : tableInfos){
            hotTablesCache.remove(t);
        }
    }

    public static void disableBySource(String sourceTableName){
        // 删除缓存
        List<HotTableInfo> tableInfos = hotTablesCache.stream().filter(t->t.getSourceTableName().equalsIgnoreCase(sourceTableName)).collect(Collectors.toList());
        if(BIUtil.isEmpty(tableInfos)){
            return;
        }
        for(HotTableInfo t : tableInfos){
            hotTablesCache.remove(t);
        }
    }

    /**
     * 清空缓存
     */
    public static void clear() {
        hotTableSourceMap.clear();
        hotTableDdlMap.clear();
        hotTableDdlChangeMap.clear();
        hotTablesCache.clear();
    }

    public static List<HotTableInfo> getHotTables() {
        // 避免调用方修改
        return hotTablesCache.stream().collect(Collectors.toList());
    }

    public static List<HotTableInfo> getHotTablesByName(String hotTableName){
        // 避免调用方修改
        return hotTablesCache.stream().filter(t->t.getHotTableName().equalsIgnoreCase(hotTableName)).collect(Collectors.toList());
    }

    public static List<HotTableSource> getHotTableSource(){
        return hotTableSourceMap.values().stream().collect(Collectors.toList());
    }

    public static List<HotTableInfo> getHotTablesBySource(String sourceTableName) {
        // 避免调用方修改
        return hotTablesCache.stream().filter(t->t.getSourceTableName().equalsIgnoreCase(sourceTableName)).collect(Collectors.toList());
    }

    public static HotTableInfo getHotTablesBySource(String sourceTableName, DBType dbType) {
        // 避免调用方修改
        List<HotTableInfo> tables = hotTablesCache.stream().filter(t->t.getSourceTableName().equalsIgnoreCase(sourceTableName)).collect(Collectors.toList());
        for(HotTableInfo t : tables){
            if(DBType.getType(t.getHotDbEngine()) == dbType){
                return t;
            }
        }
        return null;
    }

    public static HotTableInfo getHotTableByEtlJob(String etlJobName){
        List<HotTableInfo> tables = hotTablesCache.stream().filter(t->t.getEtlJobName().equalsIgnoreCase(etlJobName)).collect(Collectors.toList());
        if(BIUtil.isEmpty(tables)){
            return null;
        }
        return tables.get(0);
    }

    /**
     * 通过作业名获取多个热表
     * @param etlJobNames
     * @return
     */
    public static List<HotTableInfo> getHotTablesByEtlJobs(List<String> etlJobNames){
        return hotTablesCache.stream().filter(t->etlJobNames.contains(t.getEtlJobName())).collect(Collectors.toList());
    }

    public static void updateFinishTime(String etlJobName, String finishTime){
        HotTableInfo hotTable = getHotTableByEtlJob(etlJobName);
        if(hotTable != null){
            hotTable.setDataFinishTime(finishTime);
        }
    }

    /**
     * 刷新所有服务器缓存
     */
    public synchronized static void refreshAllServer(){
        Map<String, String> params = new HashMap<>();
        params.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.HOT.toString());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, params);
    }

    public static HotTableDdlChange getHotTableDdlChangeBySourceTable(String sourceTableName){
        return hotTableDdlChangeMap.get(sourceTableName);
    }

    public static HotTableDdl getHotTableDdlBySourceTable(String sourceTableName){
        return hotTableDdlMap.get(sourceTableName);
    }

    public static String getPartitionFieldName(String sourceTableName){
        HotTableDdl hotTableDdl = hotTableDdlMap.get(sourceTableName);
        if (hotTableDdl == null) {
            return null;
        }
        String partitionBy = hotTableDdl.getPartitionBy();
        if(BIUtil.isEmpty(partitionBy)){
            return null;
        }
        return partitionBy;
    }
}
