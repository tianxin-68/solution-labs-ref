package com.bi.queryer.ssm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.enums.DataSliceGranularity;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SysEtlJobInfo;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.period.DateUtil;
import lombok.SneakyThrows;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

public abstract class TableDataUpdateTimeUtil {


    /**
     * 构建表数据更新时间
     * 结果key = tableFullName,value = dataUpdateTime
     * @param metaTableList
     * @return
     */
    public static Map<String,String> buildTableDataUpdateTime(List<MetaTable> metaTableList) {

        Map<String, String> tableDataUpdateTimeMap = new HashMap<>();

        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");

        List<String> viewDependTableList = new ArrayList<>();
        List<String> etljobs = new ArrayList<>();
        for (MetaTable metaTable : metaTableList) {
            DataTypeEnum dataType = DataTypeEnum.codeOf(metaTable.getDataProcessType());
            if (DataTypeEnum.OFFLINE == dataType) {
                continue;
            }

            if (StrUtil.isNotEmpty(metaTable.getViewDependTables())) {
                viewDependTableList.add(metaTable.getViewDependTables());
            } else {
                etljobs.addAll(metaTable.getEtlJobs());
            }
        }

        Map<String, String> dorisTableDataUpdateTimeMap = new HashMap<>();
        //数据更新依据-依赖数据表从doris元数据 information_schema.tables 获取更新时间
        if (CollUtil.isNotEmpty(viewDependTableList)) {
//            List<DorisTableInformation> dorisTableInformationList = (List<DorisTableInformation>)  dao.queryObjectList("ssm.doris.information.schema.getTableDataUpdateTime",viewDependTableList, DataSourceType.Doris_Master);
//            dorisTableInformationList.stream().forEach(dorisTableInformation ->{
//                dorisTableDataUpdateTimeMap.put(dorisTableInformation.getTableFullName(),dorisTableInformation.getDataUpdateTime());
//            });
            dorisTableDataUpdateTimeMap = buildDorisTableDataUpdateTimeMap(viewDependTableList);
        }

        Map<String, String> etlJobDataUpdateTimeMap = new HashMap<>();
        //数据更新依据-etljob从etl元数据获取更新时间
        if (CollUtil.isNotEmpty(etljobs)) {
            List<SysEtlJobInfo> etlJobInfoList = (List<SysEtlJobInfo>) dao.queryObjectList("ssm.etl.info.batchQueryJobFinishTime", etljobs);
            etlJobInfoList.stream().forEach(etlJobInfo -> {
                etlJobDataUpdateTimeMap.put(etlJobInfo.getEtlJob(), etlJobInfo.getLastTxdate());
            });
        }
        DataSourceType dataSourceType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
        //获取准实时的更新时间
        String nearRealtimeLastBatch = (String) dao.queryObject("ssm.doris.information.schema.getNearRealtimeLastBatch", null, dataSourceType);

        //设置模型数据更新时间
        for (MetaTable metaTable : metaTableList) {

            String dataUpdateTime = "";
            if (StrUtil.isNotEmpty(metaTable.getViewDependTables())) {
                dataUpdateTime = dorisTableDataUpdateTimeMap.get(metaTable.getViewDependTables());
            } else {
                //取etljob更新时间的max
                for (String etljob : metaTable.getEtlJobs()) {
                    dataUpdateTime = DateUtil.getMaxDate(dataUpdateTime, etlJobDataUpdateTimeMap.get(etljob));

                }
            }

            //数仓规范准实时的表，更新时间一致
            //准实时判定条件，数据切分粒度 = 1h
            DataSliceGranularity sliceType = DataSliceGranularity.get(metaTable.getDataSliceCfgObj().getSliceType());
            if (DataSliceGranularity.ONE_HOUR == sliceType) {
                dataUpdateTime = nearRealtimeLastBatch;
            }

            tableDataUpdateTimeMap.put(metaTable.getFullName(), dataUpdateTime);
        }

        return tableDataUpdateTimeMap;
    }

    private static final int THREAD_NUM = 5;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static Map<String,String> buildDorisTableDataUpdateTimeMap(List<String> viewDependTableList) {

        Map<String, String> dorisTableDataUpdateTimeMap = new HashMap<>();
        ExecutorService executor = null;

        try {

            // 1. 均分表名列表到各线程
            List<List<String>> splitTables = BIUtil.averageAssign(viewDependTableList, THREAD_NUM);

            // 2. 创建固定线程池（核心/最大线程数=10，无空闲线程超时，无界队列）
            executor = new ThreadPoolExecutor(
                    THREAD_NUM,
                    THREAD_NUM,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    // 自定义线程名，方便日志排查
                    r -> new Thread(r, "Doris-Query-Thread-" + Thread.activeCount())
            );

            CompletableFuture<Map<String, String>> array[] = new CompletableFuture[splitTables.size()];
            // 3. 提交任务到线程池
            int k = 0;
            for (List<String> tableList : splitTables) {
                BatchQueryTask batchQueryTask = new BatchQueryTask(tableList);
                array[k] = CompletableFuture.supplyAsync(batchQueryTask, executor);
                k++;
            }

            for (CompletableFuture<Map<String, String>> f : array) {
                Map<String, String> map2 = f.join();

                if (map2 != null) {
                    dorisTableDataUpdateTimeMap.putAll(map2);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 确保线程池关闭
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        }

        return dorisTableDataUpdateTimeMap;
    }

    /**
     * 单线程批量处理表列表
     */
    static class BatchQueryTask implements Supplier<Map<String,String>> {

        private List<String> tableList;
        BatchQueryTask(List<String> tableList){
            this.tableList = tableList;
        }

        @SneakyThrows
        @Override
        public Map<String, String> get() {

            Map<String, String> result = new HashMap<>();
            if (CollUtil.isEmpty(tableList)) {
                return result;
            }

            BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");

            for (String tableName : tableList) {
                // 过滤空表名
                if (tableName == null || StrUtil.isEmpty(tableName)) {
                    continue;
                }

                String[] tableNameArr = tableName.split("\\.");

                //表格式必须为库名.表名
                if(tableNameArr.length != 2){
                    continue;
                }

                Map<String, String> params = new HashMap<>();
                params.put("dbName", tableNameArr[0]);
                params.put("tableName", tableNameArr[1]);
                DataSourceType dataSourceType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
                Map<String, Object> dorisTableInformationList = (Map<String, Object>) dao.queryObject("ssm.doris.information.schema.getTableInfo", params, dataSourceType);
                if (dorisTableInformationList != null) {
                    //去掉时间最后的毫秒.0
                    Timestamp updateTimestamp = (Timestamp)dorisTableInformationList.get("Update_time");
                    String updateTime = "";
                    if(updateTimestamp != null){
                        LocalDateTime ldt = updateTimestamp.toLocalDateTime();
                        updateTime = ldt.format(FORMATTER);
                    }
                    result.put(tableName, updateTime);
                }
            }

            return result;
        }

    }

}
