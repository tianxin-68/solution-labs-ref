package com.bi.queryer.ssm.engine.accelerate.hot.mq;

import com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionByMQ;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.cache.CacheAction;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.cache.CacheActionParameter;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.disable.DisableAction;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.disable.DisableActionParameter;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.user.UserService;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONObject;
import com.tx.mq.TxMqClient;
import com.tx.mq.exception.TxMqClientException;
import com.tx.mq.iface.consumer.IPushConsumer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:54 2024/8/19
 * @Description 接受datastudio-adhoc模块的修改mq
 **/
@Configuration
public class AdhocMQReceiver extends HotMQReceiver {

    public List<ActionByMQ> accept(JSONObject jsonObject){
        String dbType = StringUtils.lowerCase(jsonObject.getString("dbType"));
        String tableName = jsonObject.getString("tableName");
        String operation = jsonObject.getString("operation");
        String operator = jsonObject.getString("operator");
        if (BIUtil.isEmpty(tableName) || BIUtil.isEmpty(operation)) {
            return Collections.emptyList();
        }
        tableName = tableName.toLowerCase();
        boolean isRepairMQ = false;
        if("ds_api".equalsIgnoreCase(operator)){
            isRepairMQ = true;
        }
        // 若是数据系统开发中心操作，则不处理
        if(!isRepairMQ && BIUtil.isNotEmpty(operator)) {
            UserService userService = (UserService) SpringContextUtil.getBean("userService");
            User usr = userService.queryByName(operator);
            if(usr != null){
                if(usr.getDeptPathName() != null && usr.getDeptPathName().contains("数据系统开发中心")){
                    isRepairMQ = true;
                }
            }
        }
        // 补数触发的mq，不走后续逻辑
        if(isRepairMQ){
            return Collections.emptyList();
        }

        List<ActionByMQ> actions = new ArrayList<>();
        actions.addAll(this.createHotAction(dbType, tableName, operation, operator));
        actions.addAll(this.createCacheAction(dbType, tableName, operation, operator));
        return actions;
    }

    protected List<ActionByMQ> createHotAction(String dbType, String tableName, String operation, String operator){
        List<ActionByMQ> actions = new ArrayList<>();
        List<String> sourceTables = null;
        //如果是视图的修改
        String viewSchema = getViewSchema();
        if (tableName.startsWith(viewSchema)) {
            List<String> changeOperations = getViewChangeOperations();
            if (changeOperations.contains(operation.toLowerCase())) {
                //sourceTables = Collections.singletonList(tableName);
                if (getSupportViewEngines().contains(dbType)) {
                    // 热表action
                    actions.add(new DisableAction(new DisableActionParameter(tableName, operator)));
                }
            }
        } else {
            List<String> changeOperations = getTableChangedOperations();
            if (!changeOperations.contains(operation.toLowerCase())) {
                return Collections.emptyList();
            }

            //如果是视图依赖hive表内容的修改
            List<String> etlJobNames = getDownstreamEtlJobByTableName(tableName);

            // 热化基础表
            if(getSupportTableEngines().contains(dbType)) {
                List<HotTableInfo> hotTableInfos = HotTableCacheManager.getHotTablesByEtlJobs(etlJobNames);
                if (BIUtil.isNotEmpty(hotTableInfos)) {
                    sourceTables = hotTableInfos.stream().map(HotTableInfo::getSourceTableName).collect(Collectors.toList());
                    sourceTables.forEach(t -> {
                        actions.add(new DisableAction(new DisableActionParameter(t.toLowerCase(), operator)));
                    });
                }
            }
        }
        return actions;
    }

    protected List<ActionByMQ> createCacheAction(String dbType, String tableName, String operation, String operator){
        List<ActionByMQ> actions = new ArrayList<>();
        String viewSchema = getViewSchema();
        if (tableName.startsWith(viewSchema)) {
            List<String> changeOperations = getViewChangeOperations();
            if (!changeOperations.contains(operation.toLowerCase())) {
                return actions;
            }
            actions.add(new CacheAction(new CacheActionParameter(Arrays.asList(tableName), null, operator)));
        } else {
            List<String> changeOperations = getTableChangedOperations();
            if (!changeOperations.contains(operation.toLowerCase())) {
                return actions;
            }
            actions.add(new CacheAction(new CacheActionParameter(Arrays.asList(tableName), null, operator)));
        }
        return actions;
    }

    private String getViewSchema() {
        return SC.v("hot.view.schema", "bi_view");
    }

    private List<String> getSupportViewEngines() {
        String supportViewEngine = SC.v("hot.view.support.engine", "trino");
        String[] supportEngines = supportViewEngine.toLowerCase().split(",");
        return Arrays.asList(supportEngines);
    }

    private List<String> getSupportTableEngines() {
        String supportEngine = SC.v("hot.table.support.engine", "hive3,spark,trino");
        String[] supportEngines = supportEngine.toLowerCase().split(",");
        return Arrays.asList(supportEngines);
    }

    private List<String> getViewChangeOperations() {
        String changeOperation = SC.v("hot.view.change.operation", "alter,drop,create");
        String[] changeOperations = changeOperation.toLowerCase().split(",");
        return Arrays.asList(changeOperations);
    }

    private List<String> getTableChangedOperations() {
        String changeOperation = SC.v("hot.table.data.change.operation", "insert,drop,create,truncate");
        String[] changeOperations = changeOperation.toLowerCase().split(",");
        return Arrays.asList(changeOperations);
    }

    @SuppressWarnings("unchecked")
    private List<String> getDownstreamEtlJobByTableName(String tableName) {
        BaseDao dao = DBUtil.getBaseDao();
        String sqlId = "ssm.etl.info.getDownstreamEtlJobByTableName";
        List<String> etlJobNames = (List<String>) dao.queryObjectList(sqlId, tableName, DataSourceType.ETL);
        if (BIUtil.isEmpty(etlJobNames)) {
            return Collections.emptyList();
        }
        return etlJobNames;
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public IPushConsumer tableOperateConsumer() throws TxMqClientException {
        if (applicationContext.getParent() == null) {
            return new EmptyPushConsumer();
        }
        RuntimeEnv env = BIUtil.getRuntimeEnv();
//        if (RuntimeEnv.UT == env) {
        if (RuntimeEnv.UT == env || RuntimeEnv.Test == env || RuntimeEnv.Dev == env) {
            return new EmptyPushConsumer();
        } else {
            return TxMqClient.createPushConsumer("bi.ssm.table.operate.sub", consumeCallback);
        }
    }

    public List<ActionByMQ> accept2(JSONObject jsonObject) {
        return null;
        /*
        String dbType = StringUtils.lowerCase(jsonObject.getString("dbType"));
        String tableName = jsonObject.getString("tableName");
        String operation = jsonObject.getString("operation");
        String operator = jsonObject.getString("operator");
        if (BIUtil.isEmpty(tableName) || BIUtil.isEmpty(operation)) {
            return Collections.emptyList();
        }
        tableName = tableName.toLowerCase();
        boolean isRepairMQ = false;
        if("ds_api".equalsIgnoreCase(operator)){
            isRepairMQ = true;
        }
        // 若是数据系统开发中心操作，则不处理
        if(!isRepairMQ && BIUtil.isNotEmpty(operator)) {
            UserService userService = (UserService) SpringContextUtil.getBean("userService");
            User usr = userService.queryByName(operator);
            if(usr != null){
                if(usr.getDeptPathName() != null && usr.getDeptPathName().contains("数据系统开发中心")){
                    isRepairMQ = true;
                }
            }
        }
        // 补数触发的mq，不走后续逻辑
        if(isRepairMQ){
            return Collections.emptyList();
        }

        List<ActionByMQ> actions = new ArrayList<>();

        List<String> sourceTables = null;
        //如果是视图的修改
        String viewSchema = getViewSchema();
        if (tableName.startsWith(viewSchema)) {
            List<String> changeOperations = getViewChangeOperations();
            if (changeOperations.contains(operation.toLowerCase())) {
                //sourceTables = Collections.singletonList(tableName);
                if (getSupportViewEngines().contains(dbType)) {
                    // 热表action
                    actions.add(new DisableAction(new DisableActionParameter(tableName, operator)));
                }
                actions.add(new CacheAction(new CacheActionParameter(Arrays.asList(tableName), null, operator)));
            }
        } else {
            List<String> changeOperations = getTableChangedOperations();
            if (!changeOperations.contains(operation.toLowerCase())) {
                return Collections.emptyList();
            }

            //如果是视图依赖hive表内容的修改
            List<String> etlJobNames = getDownstreamEtlJobByTableName(tableName);

            // 缓存action
            actions.add(new CacheAction(new CacheActionParameter(null, etlJobNames, operator)));

            // 热化基础表
            if(getSupportTableEngines().contains(dbType)) {
                List<HotTableInfo> hotTableInfos = HotTableCacheManager.getHotTablesByEtlJobs(etlJobNames);
                if (BIUtil.isNotEmpty(hotTableInfos)) {
                    sourceTables = hotTableInfos.stream().map(HotTableInfo::getSourceTableName).collect(Collectors.toList());
                    sourceTables.forEach(t -> {
                        actions.add(new DisableAction(new DisableActionParameter(t.toLowerCase(), operator)));
                    });
                }
            }
        }
        return actions;

         */
        /*
        if (BIUtil.isEmpty(sourceTables)) {
            return Collections.emptyList();
        }
        return sourceTables.stream()
                .map(v -> new DisableAction(new DisableActionParameter(v.toLowerCase(),operator)))
                .collect(Collectors.toList());

         */
    }
}
