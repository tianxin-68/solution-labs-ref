package com.bi.queryer.ssm.engine.accelerate.hot;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdlChange;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:44 2024/8/21
 * @Description 模型热化处理类
 **/
public class HotModelOptimizer {
    /**
     * 模型热化
     * 可热化逻辑：
     * 一、必须条件（同时满足）
     * 1、查询事实可被热化
     * 2、查询时间范围在热表时间范围内
     * 3、查询事实表为Trino引擎
     *
     * 二、Doris引擎选择（同时满足）
     * 1、所有星型模型表都被热化为Doris
     * 2、Doris热化表当天作业已完成
     * 3、星型模型中无维度表
     *
     * 三、Trino引擎选择（同时满足）
     * 1、存在星型模型表都被热化为Trino（注：不需要所有模型表都被热化为Trino)
     * 2、Trino热化表当天作业已完成
     *
     * @param configure
     * @param models
     * @return 被热化的星模型
     */
    public static List<StarModel> hot(QueryConfigure configure, List<StarModel> models) {
        boolean isEnableHotTableQuery = "true".equalsIgnoreCase(SC.v("hot.table.query.enable", "true"));
        if(!isEnableHotTableQuery){
            return models;
        }

        //实时数据集不需要热化
        if(configure.getSettings().isRtDataset()){
            return models;
        }

        if(!configure.getSettings().isEnableHotTableQuery()){
            return models;
        }
        if(BIUtil.isEmpty(models)){
            return models;
        }

        // 创建热模型：源模型与热模型关联关系
        List<HotModel> hotStarModels = createHotStarModels(configure, models);
        if(BIUtil.isEmpty(hotStarModels)){
            return models;
        }

        // 优先选择doris引擎
        boolean isAllHot2Doris = isAllHot2Doris(models, hotStarModels);
        if(isAllHot2Doris){
            hotByEngineType(hotStarModels, DBType.Doris);
        }else {
            if("true".equalsIgnoreCase(SC.v("hot.table.engine.trino.enable", "false"))) {
                hotByEngineType(hotStarModels, DBType.Trino);
            }
        }

        return models;
    }

    /**
     * 对热模型进行最终热化：修改事实表元数据
     * @param hotStarModels
     * @param dbType
     */
    protected static void hotByEngineType(List<HotModel> hotStarModels, DBType dbType){
        for(HotModel hotStarModel : hotStarModels){
            HotTableInfo hotTable = dbType == DBType.Doris ? hotStarModel.getDorisHotTable() : hotStarModel.getTrinoHotTable();
            if(hotTable == null){
                continue;
            }
            // 替换元数据
            StarModel starModel = hotStarModel.getModel();
            QueryTable factTable = starModel.getFactTable();
            // 获取表元数据：此处需clone
            MetaTable metaTable = factTable.getMeta().clone();
            if(BIUtil.isEmpty(metaTable.getRawFullName())) {
                metaTable.setRawFullName(metaTable.getFullName());
            }
            // 设置其schema和表名
            String[] tableInfos = hotTable.getHotTableName().split("\\.");
            String hotTableName = tableInfos[tableInfos.length - 1];
            String hotSchema = hotTable.getHotTableName().replace("." + hotTableName, "");
            metaTable.setTableSchema(hotSchema);
            metaTable.setName(hotTableName);

            // 设置其支持的查询引擎
            List<String> supportQueryEngines = new ArrayList<>();
            switch (dbType){
                case Trino:
                    supportQueryEngines.add(DBType.Trino.toString().toLowerCase());
                    break;
                case Doris:
                    supportQueryEngines.add(DBType.Trino.toString().toLowerCase());
                    supportQueryEngines.add(DBType.Doris.toString().toLowerCase());
                    break;
            }
            metaTable.setSupportQueryEngines(BIUtil.listToStr(supportQueryEngines));

            // 最后设置查询表的元信息
            factTable.setMeta(metaTable);
        }
    }

    /**
     * 判断所有星型模型是否都被热化到doris中
     * @param models
     * @param hotStarModels
     * @return
     */
    protected static boolean isAllHot2Doris(List<StarModel> models, List<HotModel> hotStarModels){
        List<QueryTable> queryTables = new ArrayList<>();
        models.stream().forEach(m->queryTables.addAll(m.getTables()));
        // 所有事实表表都已被热化，且无维度表
        boolean isAllHot2Doris = hotStarModels.size() == models.size() && hotStarModels.size() == queryTables.size();
        for (HotModel hotStarModel : hotStarModels) {
            if(!isAllHot2Doris){
                continue;
            }
            isAllHot2Doris = isAllHot2Doris && hotStarModel.getDorisHotTable() != null;
        }
        return isAllHot2Doris;
    }

    /**
     * 创建热模型
     * @param configure
     * @param models
     * @return
     */
    protected static List<HotModel> createHotStarModels(QueryConfigure configure, List<StarModel> models){
        List<HotModel> hotStarModels = new ArrayList<>();

        Map<String, StarModel> modelMap = new HashMap<>();
        models.forEach(m ->{modelMap.put(m.getFactTable().getId(), m);});

        // 查询时间范围
        List<String> queryDateRange = SSDUtil.getFilterDateRange(configure);
        if(BIUtil.isEmpty(queryDateRange) || queryDateRange.size() < 2){
            return hotStarModels;
        }
        Collections.sort(queryDateRange);
        String queryMinDate = queryDateRange.get(0);
        String queryMaxDate = queryDateRange.get(queryDateRange.size() - 1);

        for(StarModel model : models){
            MetaTable factTable = model.getFactTable().getMeta();
            if(factTable == null){
                continue;
            }
            String supportQueryEngines = factTable.getSupportQueryEngines();
            if(BIUtil.isEmpty(supportQueryEngines) || !supportQueryEngines.contains(DBType.Trino.toString().toLowerCase())){
                continue;
            }
            // 获取热表
            List<HotTableInfo> modelHotTables = getHotTableBySourceTableName(factTable.getFullName(), queryMinDate, queryMaxDate);
            if(BIUtil.isEmpty(modelHotTables)){
                continue;
            }

            // 构建热模型
            HotModel hotStarModel = new HotModel(model);
            for (HotTableInfo hotTable : modelHotTables) {
                DBType dbEngineType = DBType.getType(hotTable.getHotDbEngine());
                if(dbEngineType == DBType.Trino){
                    hotStarModel.setTrinoHotTable(hotTable);
                }
                if(dbEngineType == DBType.Doris){
                    hotStarModel.setDorisHotTable(hotTable);
                }
            }
            hotStarModels.add(hotStarModel);
        }

        return hotStarModels;
    }

    /**
     * 通过源表名获取热表信息
     * @param sourceTableName
     * @param queryMinDate
     * @param queryMaxDate
     * @return
     */
    protected static List<HotTableInfo> getHotTableBySourceTableName(String sourceTableName, String queryMinDate, String queryMaxDate){

        //校验时间是否为yyyy-MM-dd
        if (!BIUtil.validDate(queryMinDate, "yyyy-MM-dd")) {
            return new ArrayList<>();
        }

        //如果最大日期大于昨天，赋值昨天
        DateTime lastDay = DateUtil.yesterday();
        DateTime queryMaxDateTime = DateUtil.parse(queryMaxDate);
        if (queryMaxDateTime.getTime() > lastDay.getTime()) {
            queryMaxDate = lastDay.toDateStr();
        }

        String filterMaxDate = queryMaxDate;

        List<HotTableInfo> hotTablesCache = HotTableCacheManager.getHotTables();
        String today = DateUtil.today();
        List<HotTableInfo> tableInfos = hotTablesCache.stream()
                .filter(t->t.getSourceTableName().equalsIgnoreCase(sourceTableName))
                .filter(t->t.getDataMinDate().compareTo(queryMinDate) <= 0)
                .filter(t->t.getDataMaxDate().compareTo(filterMaxDate) >= 0)
                //.filter(t->t.getDataFinishTime().compareTo(today) >= 0)
                .collect(Collectors.toList());

        // 逻辑：当天完成用当天的数据，否则用昨天的数据
        List<HotTableInfo> newTableInfos = new ArrayList<>();
        for(HotTableInfo tableInfo : tableInfos){
            // 当天作业已完成
            if(BIUtil.isNotEmpty(tableInfo.getDataFinishTime())){
                if(tableInfo.getDataFinishTime().compareTo(today) >= 0){
                    newTableInfos.add(tableInfo);
                }
            }else if(BIUtil.isNotEmpty(tableInfo.getRecentDataFinishTime())){
                // 当天作业完成时间为空，但上一次完成时间不为空，则判断： 最近完成日期（去掉时分秒）= 昨天 &&  查询的最大日期 <= 前天 && ddl变更时间（去掉时分秒） != 昨天
                DateTime today_1 =  DateUtil.yesterday(); // 昨天
                DateTime today_2 = DateUtil.offsetDay(today_1, -1); // 前天
                DateTime recentFinishDate = DateUtil.parse(tableInfo.getRecentDataFinishTime(), "yyyy-MM-dd");
                DateTime maxDate = DateUtil.parseDate(queryMaxDate);
                HotTableDdlChange ddlChange = HotTableCacheManager.getHotTableDdlChangeBySourceTable(tableInfo.getSourceTableName());
                boolean hasDdlChange = false;
                if(ddlChange != null && BIUtil.isNotEmpty(ddlChange.getLastChangeTime())){
                    DateTime ddlChangeLastDate = DateUtil.parse(ddlChange.getLastChangeTime(), "yyyy-MM-dd");
                    hasDdlChange = ddlChange.getDdlChangeCnt() > 0 || ddlChangeLastDate.toString("yyyy-MM-dd").equals(today_1.toString("yyyy-MM-dd"));
                }
                if(!hasDdlChange && today_1.toDateStr().equals(recentFinishDate.toDateStr()) && maxDate.toDateStr().compareTo(today_2.toString("yyyy-MM-dd")) <= 0){
                    newTableInfos.add(tableInfo);
                }
            }
        }
        tableInfos = newTableInfos;

        return tableInfos;
    }

    public static void main(String[] args) {
        String t = "2024-01-04 13:00:11";
        DateTime finishDate = DateUtil.parse(t, "yyyy-MM-dd");
        System.out.println(finishDate.toDateStr());

        System.out.println(DateUtil.yesterday());
    }

}
