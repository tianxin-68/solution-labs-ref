package com.bi.queryer.ssm.engine.accelerate.route;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.accelerate.route.balance.TrinoClusterBalancer;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.ssm.util.SqlExpressionUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @Author contributor
 * @Date 10:13 2024-05-14
 * @Description 数据源路由器
 **/
public class DataSourceRouter {

    /**
     * 当前数据源
     */
    private final static ThreadLocal<DataSourceType> current = new ThreadLocal<>();

    private final static ThreadLocal<Boolean> enable = new ThreadLocal<Boolean>();

    /**
     * 获取最优数据源：会检查负载情况，获取负载最低数据源
     * @return
     */
    public static DataSourceType getBestDataSourceType(){
        if(enable.get() != null && !enable.get()){
            return DataSourceType.Trino_Master;
        }

        DataSourceType bestDataSource = getCurrentDataSourceType();
        if(bestDataSource == null){
            bestDataSource = DataSourceType.Trino_Master;
        }

        // 若是trino，则获取负载最低的数据源
        if(bestDataSource == DataSourceType.Trino_Master){
            TrinoClusterBalancer clusterBalancer = new TrinoClusterBalancer();
            bestDataSource = clusterBalancer.getBesetDataSourceType();
        }

        return bestDataSource;
    }

    /**
     * 通过模型获取主节点数据源
     * @param models
     * @return
     */
    protected static DataSourceType getDefaultDataSourceType(List<StarModel> models){

        DataSourceType defaultMasterDataSourceType = DataSourceType.Trino_Master;
        if(enable.get() != null && !enable.get()){
            return defaultMasterDataSourceType;
        }
        // 是否支持doris路由
        String routeEngines = SC.v("route.engine.list");
        if(BIUtil.isEmpty(routeEngines) || !routeEngines.toLowerCase().contains(DataSourceType.Doris_Master.getDialect().toLowerCase())) {
            return defaultMasterDataSourceType;
        }

        if(BIUtil.isEmpty(models)) {
            return defaultMasterDataSourceType;
        }
        // 判断表支持的查询引擎
        List<MetaTable> allMetaTables = new ArrayList<>();
        for(StarModel model : models){
            List<QueryTable> tables = model.getTables();
            if(BIUtil.isEmpty(tables)){
                continue;
            }
            for(QueryTable table : tables){
                if(table.getMeta() != null) {
                    allMetaTables.add(table.getMeta());
                }
            }
        }

        boolean isAllSupportDoris = true;
        for(MetaTable metaTable : allMetaTables){
            String supportQueryEngines = metaTable.getSupportQueryEngines();
            isAllSupportDoris = isAllSupportDoris && supportQueryEngines.toLowerCase().contains(DataSourceType.Doris_Master.getDialect().toLowerCase());
        }

        if(isAllSupportDoris){
            return DataSourceType.Doris_Master;
        }else {
            return DataSourceType.Trino_Master;
        }
    }

    public static void setCurrentDataSourceType(DataSourceType ds){
        current.set(ds);
    }

    public static DataSourceType getCurrentDataSourceType(){
        return current.get();
    }

    public static void removeCurrentDataSourceType(){
        current.remove();
        enable.remove();
    }

    /**
     * 设置引擎数据源
     * @param queryEngine
     * @return
     */
    public static void setQueryEngineDefaultDataSource(QueryEngine queryEngine){
        DataSourceType dataSourceType = DataSourceType.Trino_Master;
        if(queryEngine == null) {
            return;
        }

        List<StarModel> models = queryEngine.getModels();//new ArrayList<>();
        /*
        if(queryEngine.getType() != QueryEngineType.Lod){
            models = queryEngine.getModels();
        }else {
            // 重新创建查询配置，避免查询配置重复使用
            QueryConfigure configure = queryEngine.getConfig().clone();
            configure.load();
            LodQueryConfigureCreator lodQueryConfigureCreator = new LodQueryConfigureCreator(configure, queryEngine.getCxt());
            LodQueryConfigure lodQueryConfigure = lodQueryConfigureCreator.create();
            List<LodQueryConfigureItem> items = lodQueryConfigure.getItems();
            for (LodQueryConfigureItem item : items) {
                QueryEngine engine = QueryFactory.createEngine(item.getConfig(), queryEngine.getCxt());
                models.addAll(engine.getModels());
            }
        }
         */

        // 通过模型获取默认数据源
        dataSourceType = getDefaultDataSourceType(models);

        // 是否需要降级到trino
        if(isRelegate2Trino(queryEngine.getConfig(), dataSourceType)){
            dataSourceType = DataSourceType.Trino_Master;
        }
        // 获取模版id
        String templateId = Optional.ofNullable(queryEngine).map(QueryEngine::getConfig).map(QueryConfigure::getTemplateEntity).map(SSDQueryTemplate::getId).orElse(null);
        // 获取巡检指定数据源Key
        String inspectDsKey = Optional.ofNullable(queryEngine).map(QueryEngine::getConfig).map(QueryConfigure::getSettings).map(QuerySettings::getInspectDsKey).orElse(null);
        dataSourceType = switchDorisDataSource(dataSourceType, templateId, inspectDsKey);

        setCurrentDataSourceType(dataSourceType);
    }

    /**
     * 按用户分流的配置了熔断规则doris账号中
     * 目的：熔断规则上线初期，避免规则过严导致大面积熔断，影响用户体验
     * @param dataSourceType
     * @return
     */
    protected static DataSourceType switchDorisDataSource(DataSourceType dataSourceType, String templateId, String inspectDsKey){
        // 巡检指定数据源优先，仅限当前数据源为 Doris 时生效
        if (StrUtil.isNotEmpty(inspectDsKey) && dataSourceType != null && dataSourceType.isDoris()) {
            return DataSourceType.getType(inspectDsKey);
        }
        // 按域账号Hash分流
        if(dataSourceType != DataSourceType.Doris_Master) {
            return dataSourceType;
        }
        User user = UserManager.get();
        if(user == null){
            return dataSourceType;
        }

        /**
        Double rateLimitThreshold = Double.valueOf(SC.v("doris.block.threshold.by.uv.percent", "0.02"));
        List<String> whiteList = Arrays.asList(SC.v("ssm.key.user.list", "chenmin").split(","));
        List<String> blackList = Arrays.asList(SC.v("doris.force.block.users", "").split(","));
        boolean isHitRateLimit = SSDUtil.isHitRateLimitByUserName(user.getName(), rateLimitThreshold, whiteList,  blackList);
        if(isHitRateLimit){
            return DataSourceType.Doris_Slave01;
        }else {
            return dataSourceType;
        }*/

        /**
        Double rateLimitThreshold = Double.valueOf(SC.v("hw_doris.route.threshold.by.uv.percent", "0"));
        List<String> whiteList = Arrays.asList(SC.v("ssm.key.user.list", "chenmin").split(","));
        List<String> blackList = Arrays.asList(SC.v("hw_doris.route.force.users", "").split(","));
        boolean isRouteHWDoris = SSDUtil.isHitRateLimitByUserName(user.getName(), rateLimitThreshold, whiteList,  blackList);
        if(isRouteHWDoris){
            return getHwDorisDataSource(user.getName(), dataSourceType);
        }else {
            return getIdcDorisDataSource(user.getName(), dataSourceType);
        }
         */
        return getFinalDorisDataSource(user, dataSourceType, templateId);
    }
    public static DataSourceType getFinalDorisDataSource(User user, DataSourceType defaultDataSourceType){
        return getFinalDorisDataSource(user, defaultDataSourceType, null);
    }
    public static DataSourceType getFinalDorisDataSource(User user, DataSourceType defaultDataSourceType, String templateId){
        if(user == null){
            return defaultDataSourceType;
        }
        if(defaultDataSourceType == null) {
            return DataSourceType.Doris_Slave01;
        }
        /**
        Double rateLimitThreshold = Double.valueOf(SC.v("hw_doris.route.threshold.by.uv.percent", "0"));
        List<String> whiteList = Arrays.asList(SC.v("ssm.key.user.list", "chenmin").split(","));
        List<String> blackList = Arrays.asList(SC.v("hw_doris.route.force.users", "").split(","));
        boolean isRouteHWDoris = SSDUtil.isHitRateLimitByUserName(user.getName(), rateLimitThreshold, new ArrayList<>(),  blackList);
         */
        DataSourceType finalDataSourceType =  getHwDorisDataSource(user.getName(), defaultDataSourceType, templateId);
        /**
        if(isRouteHWDoris){
            finalDataSourceType = getHwDorisDataSource(user.getName(), defaultDataSourceType, templateId);
        }else {
            finalDataSourceType = getIdcDorisDataSource(user.getName(), defaultDataSourceType, templateId);
        }*/
        if(finalDataSourceType == null){
            finalDataSourceType = DataSourceType.Doris_Slave01;
        }
        return finalDataSourceType;
    }

    /**
     * 获取idc的doris集群数据源
     * @param userName
     * @param dataSourceType
     * @return
     */
    public static DataSourceType getIdcDorisDataSource(String userName, DataSourceType dataSourceType, String templateId){
        // 若是配置了非阻塞的模版，则走非阻塞的数据源（默认数据源）
        if(isForceDorisMaster(templateId)){
            return DataSourceType.HW_Doris_Master;
        }
        Double rateLimitThreshold = Double.valueOf(SC.v("doris.block.threshold.by.uv.percent", "0.02"));
        List<String> whiteList = Arrays.asList(SC.v("ssm.key.user.list", "chenmin").split(","));
        List<String> blackList = Arrays.asList(SC.v("doris.force.block.users", "").split(","));
        boolean isHitRateLimit = SSDUtil.isHitRateLimitByUserName(userName, rateLimitThreshold, whiteList,  blackList);
        if(isHitRateLimit){
            return DataSourceType.Doris_Slave01;
        }else {
            return dataSourceType;
        }
    }

    /**
     * 是否强制走doris master数据源
     * @param templateId
     * @return
     */
    protected static boolean isForceDorisMaster(String templateId){
        if(BIUtil.isEmpty(templateId)){
            return false;
        }
        List<String> nonblockTemplateIds = Arrays.asList(SC.v("ssm.nonblock.template.ids", "") .split(","));
        if(BIUtil.isNotEmpty(nonblockTemplateIds) && BIUtil.isNotEmpty(templateId) && nonblockTemplateIds.contains(templateId)){
            return true;
        }
        return false;
    }
    /**
     * 获取华为云的doris集群数据源
     * @param userName
     * @param dataSourceType
     * @return
     */
    public static DataSourceType getHwDorisDataSource(String userName, DataSourceType dataSourceType, String templateId){
        // 若是配置了非阻塞的模版，则走非阻塞的数据源（默认数据源）
        if(isForceDorisMaster(templateId)){
            return DataSourceType.HW_Doris_Master;
        }

        Double rateLimitThreshold = Double.valueOf(SC.v("hw_doris.block.threshold.by.uv.percent", "1"));
        List<String> whiteList = Arrays.asList(SC.v("ssm.key.user.list", "chenmin").split(","));
        List<String> blackList = Arrays.asList(SC.v("hw_doris.force.block.users", "").split(","));
        boolean isHitRateLimit = SSDUtil.isHitRateLimitByUserName(userName, rateLimitThreshold, whiteList,  blackList);

        if(isHitRateLimit){
            return DataSourceType.HW_Doris_Slave01;
        }else {
            return DataSourceType.HW_Doris_Master;
        }
    }

    /**
     * 降级到trino：若自定义表达式中有特定的trino语法，则降级到trino
     * @param configure
     * @return
     */
    public static boolean isRelegate2Trino(QueryConfigure configure, DataSourceType dataSourceType) {
        DBType dbType = DBType.getType(dataSourceType.getDialect());
        if (dbType == DBType.Trino) {
            return false;
        }
        List<QueryField> resultFields = configure.getResult().getFields();
        if (BIUtil.isEmpty(resultFields)) {
            return false;
        }

        try {
            for (QueryField f : resultFields) {
                String expression = "";
                // 先取前端用户自定义字段表达式
                CustomFieldConfigure customFieldConfigure = f.getCustomFieldConfigure();
                if (customFieldConfigure != null && BIUtil.isNotEmpty(customFieldConfigure.getExpression())) {
                    expression = customFieldConfigure.getExpression();
                }
                // 再取后台配置的派生或复合指标表达式
                if (BIUtil.isEmpty(expression)) {
                    expression = f.getMeta().getAggExpression();
                    if (BIUtil.isEmpty(expression)) {
                        continue;
                    }
                    if (!expression.contains("[")) {
                        continue;
                    }
                }
                if (BIUtil.isEmpty(expression)) {
                    continue;
                }

                //解析出表达式的函数
                List<String> funcs = SqlExpressionUtil.parseFunction(expression);

                String[] keywords = SC.v("route.engine.trino.keywords", "||").split(",");
                for (String kw : keywords) {
                    if (BIUtil.isEmpty(kw)) {
                        continue;
                    }

                    //如果关键字不是函数，则判断表达式中是否包含
                    //如果关键字是函数，则判断表达式中是否包含该函数
                    //函数关键字格式为：函数名@Func
                    if (kw.contains(BIConsts.KEYWORD_FUNC_FLAG)) {
                        String kwFuncValue = kw.replace(BIConsts.KEYWORD_FUNC_FLAG, "").toLowerCase();
                        if (funcs.contains(kwFuncValue)) {
                            return true;
                        }

                    } else {
                        if (expression.contains(kw)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 若数据源是doris，则需再次判断其数据是否都已ready
        /*
        if(DBType.getType(dataSourceType.getDialect()) == DBType.Doris) {
            DataAvailableTimeResp checkResult = SSDUtil.checkDataAvailableTime(queryEngine.getConfig(), queryEngine);
            // 若无不可用字段，则表示数据已ready
            if(BIUtil.isNotEmpty(checkResult.getUnAvailableFieldList())) {
                dataSourceType = DataSourceType.Trino_Master;
            }
        }
         */

        return false;
    }

    public static void setEnable(Boolean isEnable){
        enable.set(isEnable);
    }

    public static void removeEnable(){
        enable.remove();
    }

}
