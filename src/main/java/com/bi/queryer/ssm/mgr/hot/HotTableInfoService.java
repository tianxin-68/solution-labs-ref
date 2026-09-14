package com.bi.queryer.ssm.mgr.hot;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.enums.DataUpdateMode;
import com.bi.queryer.ssm.meta.SysEtlJobInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableSource;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @Author contributor
 * @Date 17:09 2024/8/19
 * @Description 热数据基础表service：负责基础表增删改查
 **/

@Service
@Scope("prototype")
public class HotTableInfoService {
    @Autowired
    protected BaseDao dao = null;

    /**
     * 获取所有热化表（不分页）
     * @return
     */
    public List<HotTableInfo> queryAllHotTableInfo(){
        String sqlId = "ssm.hot.info.queryAllHotTableInfo";
        List<HotTableInfo> hotTableInfoList = (List<HotTableInfo>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        return hotTableInfoList;
    }

    /**
     * 通过源表名获取热化表信息
     * @return
     */
    public List<HotTableInfo> getHotTableInfoBySourceTableName(String sourceTableName){
        String sqlId = "ssm.hot.info.getHotTableInfoBySourceTableName";
        List<HotTableInfo> hotTableInfoList = (List<HotTableInfo>) dao.queryObjectList(sqlId, sourceTableName, DataSourceType.Default);
        return hotTableInfoList;
    }

    public void disable(String hotTableName){
        String sqlId = "ssm.hot.info.disable";
        Map<String, String> param = new HashMap<>();
        param.put("hotTableName", hotTableName);
        dao.update(sqlId, param);
    }

    public void initialize(String sourceTableName) {
        // 删除热数据基础信息表
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                Map<String, Object> param = new HashMap<>();
                param.put("hotTableOwner", HotUtil.getHotTableOwner());

                List<HotTableSource> hotTableSourceList = HotTableCacheManager.getHotTableSource();

                //所有需要初始化的热表信息
                List<HotTableSource> allInitHotTableSourceList = new ArrayList<>();
                if (StrUtil.isNotEmpty(sourceTableName)) {
                    Optional<HotTableSource> optionalHotTableSource = hotTableSourceList.stream().filter(h -> h.getSourceTableName().equalsIgnoreCase(sourceTableName)).findAny();
                    if (optionalHotTableSource.isPresent()) {
                        allInitHotTableSourceList.add(optionalHotTableSource.get());
                    }
                } else {
                    allInitHotTableSourceList.addAll(hotTableSourceList);
                }

                //区分增量和全量
                if (CollUtil.isNotEmpty(allInitHotTableSourceList)) {

                    List<String> fullList = new ArrayList<>();
                    List<String> incrementalList = new ArrayList<>();
                    for (HotTableSource hotTableSource : allInitHotTableSourceList) {
                        DataUpdateMode dataUpdateMode = DataUpdateMode.get(hotTableSource.getDataUpdateMode());
                        if (DataUpdateMode.FULL == dataUpdateMode) {
                            fullList.add(hotTableSource.getSourceTableName());
                        } else {
                            incrementalList.add(hotTableSource.getSourceTableName());
                        }
                    }

                    //处理全量
                    if (CollUtil.isNotEmpty(fullList)) {

                        param.put("sourceTableNameList",fullList);

                        String sqlId = "ssm.hot.info.deleteHotTableInfo";
                        dao.delete(sqlId, param, DataSourceType.Default);

                        // 重新按源表初始化基础信息表
                        sqlId = "ssm.hot.info.insertHotTableInfo";
                        dao.insert(sqlId, param, DataSourceType.Default);
                    }

                    //处理增量
                    if (CollUtil.isNotEmpty(incrementalList)) {
                        param.put("sourceTableNameList",incrementalList);
                        dao.update("ssm.hot.info.updateHotTableInfo",param, DataSourceType.Default);

                        //处理达到热化数据的最大天数的场景
                        dao.update("ssm.hot.info.updateHotTableMinDate",param, DataSourceType.Default);

                    }

                }

            }
        });
    }

    public void updateFinishTime(String etlJobName, String dataFinishTime){
        Map<String, String> param = new HashMap<>();
        param.put("etlJobName", etlJobName);
        param.put("dataFinishTime", dataFinishTime);
        param.put("hotTableOwner", HotUtil.getHotTableOwner());

        String sqlId = "ssm.hot.info.updateFinishTime";
        dao.update(sqlId, param, DataSourceType.Default);
    }

    public List<SysEtlJobInfo> queryEtlJobByNames(List<String> etlJobNames){
        Map<String, Object> param = new HashMap<>();
        param.put("etlJobNames", etlJobNames);
        String sqlId = "ssm.hot.info.queryEtlJobByNames";
        List<SysEtlJobInfo> etlJobInfos = (List<SysEtlJobInfo>) dao.queryObjectList( sqlId, param, DataSourceType.ETL);
        return etlJobInfos;
    }

    public void updateMinDateBySourceTable(String sourceTableName, String minDate){
        Map<String, String> param = new HashMap<>();
        param.put("sourceTableName", sourceTableName);
        param.put("minDate", minDate);

        String sqlId = "ssm.hot.info.updateMinDateBySourceTable";
        dao.update(sqlId, param, DataSourceType.Default);
    }

}
