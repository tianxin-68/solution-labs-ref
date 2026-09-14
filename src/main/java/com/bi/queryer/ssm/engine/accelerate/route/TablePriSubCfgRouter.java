package com.bi.queryer.ssm.engine.accelerate.route;


import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.SpringContextUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主子表路由
 */
public class TablePriSubCfgRouter {


    /**
     * 主子表替换
     * 条件1 ：当天主表作业未完成，子表作业完成，
     * 条件2 ：子表字段包含主表所有的查询字段
     * @param models
     */
    public static void route(QueryConfigure configure,List<StarModel> models) {

        try {

            boolean isEnableTablePriSubRouter = "true".equalsIgnoreCase(SC.v("ssm.table.pri.sub.router.enable", "true"));
            if(!isEnableTablePriSubRouter){
                return ;
            }

            //实时数据集没有主子表替换逻辑
            if(configure.getSettings().isRtDataset()){
                return ;
            }

            if(!configure.getSettings().isEnableTablePriSubRouter()){
                return ;
            }

            if(CollUtil.isEmpty(models)){
                return;
            }

            //查询所有的查询表
            List<QueryTable> tables = new ArrayList<>();
            for (StarModel model : models) {
                tables.addAll(model.getTables());
            }

            List<String> etlJobs = new ArrayList<>();
            //查询主子表依赖的作业
            for(QueryTable qt : tables){

                List<MetaTablePriSubCfg> tablePriSubCfgs = SSDMetaCacheManager.getTablePriSubCfgByTableId(qt.getId());
                if(CollUtil.isEmpty(tablePriSubCfgs)){
                    continue;
                }

                //添加主表依赖作业
                etlJobs.addAll(qt.getMeta().getEtlJobs());

                //添加子表依赖作业
                for(MetaTablePriSubCfg tablePriSubCfg : tablePriSubCfgs){
                    MetaTable subTable = SSDMetaCacheManager.getTable(tablePriSubCfg.getSubTableId());
                    if(subTable == null){
                        continue;
                    }

                    etlJobs.addAll(subTable.getEtlJobs());
                }

            }

            etlJobs = etlJobs.stream().distinct().collect(Collectors.toList());

            if(CollUtil.isEmpty(etlJobs)) {
                return;
            }

            //查询作业执行情况
            BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
            Map<String,Object> queryParam = new HashMap<>();
            queryParam.put("etlJobs", etlJobs);
            List<SysEtlJobInfo> etlJobInfoList = (List<SysEtlJobInfo>)dao.queryObjectList("ssm.etl.info.getEtlInfo", queryParam, DataSourceType.ETL);

            Map<String,SysEtlJobInfo> etlJobMap = new HashMap<>();
            for(SysEtlJobInfo etlJob : etlJobInfoList){
                etlJobMap.put(etlJob.getEtlJob(), etlJob);
            }


            for (StarModel model : models) {
                routeByTablePriSubCfg(model,etlJobMap);
            }


        }catch(Exception e){
           e.printStackTrace();
        }


    }

    /**
     * 替换主子表
     * @param etlJobMap
     * @return
     */
    public static void routeByTablePriSubCfg(StarModel model, Map<String,SysEtlJobInfo> etlJobMap) {

        MetaTable subMetaTable = null;
        Map<String,QueryTable> subDimQueryTableMap = new HashMap<>();

        QueryTable qt = model.getFactTable();
        List<MetaTablePriSubCfg> tablePriSubCfgs = SSDMetaCacheManager.getTablePriSubCfgByTableId(qt.getId());
        if (CollUtil.isEmpty(tablePriSubCfgs)) {
            return;
        }

        //主表已完成，不处理
        if (Enabled.value(isJobCompleted(qt.getMeta().getEtlJobs(), etlJobMap))) {
            return;
        }

        for (MetaTablePriSubCfg tablePriSubCfg : tablePriSubCfgs) {
            MetaTable subTable = SSDMetaCacheManager.getTable(tablePriSubCfg.getSubTableId());
            if (subTable == null) {
                continue;
            }

            //子表已完成，再校验字段是否都存在
            if (Enabled.value(isJobCompleted(subTable.getEtlJobs(), etlJobMap))) {

                List<MetaField> subFields = subTable.getFields();

                //构建子表关联的维度表信息
                List<MetaTable> refDimTables = SSDMetaCacheManager.getRelationTables(subTable.getId());
                Map<String,MetaTable> refDimTableMap = new HashMap<>();
                for(MetaTable refDimTable : refDimTables){
                    if(CollUtil.isEmpty(refDimTable.getFields())){
                        continue;
                    }

                    for (MetaField refDimField : refDimTable.getFields()){
                        refDimTableMap.put(refDimField.getCode(), refDimTable);
                    }
                }

                boolean canReplace = true;
                for (QueryField qf : qt.getFields()) {

                    //虚拟的不校验
                    if(qf.isVirtual()){
                        continue;
                    }

                    //如果字段没有绑定白皮书，code=表名+字段名，此时主子表code不一致
                    //20250902改为通过字段名查找
                    boolean isExist = subFields.stream()
                            .filter(f -> f.getName().equalsIgnoreCase(qf.getName()))
                            .findAny().isPresent();

                    //如果是维度，在子表不存在。再从关联的维度表中查找，如果存在，也可以从子表查询
                    if (!isExist && !qf.isMeasure()) {
                        if (refDimTableMap.containsKey(qf.getCode())) {
                            isExist = true;

                            //将维度表加入到子表关联的维度表中
                            MetaTable dimMateTable = refDimTableMap.get(qf.getCode());
                            QueryTable queryDimTable = subDimQueryTableMap.get(dimMateTable.getId());
                            if (queryDimTable == null) {
                                queryDimTable = new QueryTable(dimMateTable.clone());
                                queryDimTable.setModel(model);
                            }
                            queryDimTable.addField(qf);
                            subDimQueryTableMap.put(dimMateTable.getId(), queryDimTable);
                        }
                    }

                    if (!isExist) {
                        canReplace = false;
                        break;
                    }
                }

                if (canReplace) {
                    subMetaTable = subTable.clone();
                    break;
                }

            }
        }

        //替换事实表与维度表
        if(subMetaTable != null) {
            model.getFactTable().setMeta(subMetaTable);
            //20260326 替换模型id
            //在表关联场景，会通过modelId来获取维表集合，此处需要将星型模型的modelId设置为子表的modelId
            model.setModelId(subMetaTable.getId());
            for (QueryTable subDimQueryTable : subDimQueryTableMap.values()) {
                model.addDimTable(subDimQueryTable);
            }
        }

    }

    /**
     * 作业是否完成
     * @param etlJobs
     * @param etlJobMap
     * @return
     */
    public static Integer isJobCompleted(List<String> etlJobs, Map<String,SysEtlJobInfo> etlJobMap) {

        if (CollUtil.isEmpty(etlJobs)) {
            return Enabled.YES.getId();
        }

        //模拟测试使用
//        if(etlJobs.contains("EXP_DRS2_ADS_ORD_ORDER_DETAIL_DTL_DI")){
//            return Enabled.NO.getId();
//        }
//
//        if(etlJobs.contains("EXP_DRS2_ADS_ORD_ORDER_DETAIL_SUB_DTL_DI")){
//            return Enabled.YES.getId();
//        }

        for (String etlJob : etlJobs) {

            SysEtlJobInfo etlJobInfo = etlJobMap.get(etlJob);
            if (etlJobInfo == null) {
                return Enabled.NO.getId();
            }

            if (!Enabled.value(etlJobInfo.getIsTodayCompleted())) {
                return Enabled.NO.getId();
            }

        }

        return Enabled.YES.getId();
    }

}
