package com.bi.queryer.ssm.engine.accelerate.cache;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.accelerate.cache.model.QueryTemplateCacheOperateLog;
import com.bi.queryer.ssm.engine.accelerate.cache.model.QueryTemplateCacheResult;
import com.bi.queryer.ssm.engine.accelerate.cache.model.QueryTemplateCacheSource;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.query.template.cache.QueryTemplateCacheService;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.MemoryCacheSyncSSMService;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.sys.startup.InitializableModule;
import com.bi.queryer.sys.startup.SystemInitializer;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 20:29 2025/1/8
 * @Description 查询结果集缓存
 **/
public class QueryTemplateCacheManager implements Initializable {
    protected static Map<String, QueryTemplateCacheSource> cacheSourceMap = new HashMap<>();

    protected static Map<String, QueryTemplateCacheResult> cacheResultMap = new HashMap<>();

    @Override
    public void initialize(Map<String, ?> initParams) throws BIException {
        refresh();
    }

    /**
     * 刷新缓存
     */
    public static void refresh() {
        // 缓存源
        QueryTemplateCacheService cacheService = (QueryTemplateCacheService) SpringContextUtil.getBean("queryTemplateCacheService");
        List<QueryTemplateCacheSource> cacheSourceList = cacheService.queryCacheSource();
        if(BIUtil.isNotEmpty(cacheSourceList)){
            cacheSourceMap.clear();
            for(QueryTemplateCacheSource cacheSource : cacheSourceList){
                cacheSourceMap.put(cacheSource.getQueryTplId(), cacheSource);
                // 添加交叉表表头查询key
                QueryTemplateCacheSource crossHeaderSource = JSONObject.parseObject(BIUtil.toJSONString(cacheSource), QueryTemplateCacheSource.class);
                crossHeaderSource.setQueryTplId(cacheSource.getQueryTplId() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
                crossHeaderSource.setQueryTplName(cacheSource.getQueryTplName() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
                cacheSourceMap.put(crossHeaderSource.getQueryTplId(), crossHeaderSource);
            }
        }

        // 缓存结果
        List<QueryTemplateCacheResult> cacheResultList = cacheService.queryCacheResult();
        if(BIUtil.isNotEmpty(cacheResultList)){
            cacheResultMap.clear();
            for(QueryTemplateCacheResult cacheResult : cacheResultList){
                cacheResultMap.put(cacheResult.getCacheKey(), cacheResult);
            }
        }
    }

    /**
     * 添加缓存(异步)
     * @param queryTplId
     * @param sql
     * @return
     */
    public static void addCache(String queryTplId, String sql, List<StarModel> models, ResultDataSet dataSet){
        if(!isEnable() || dataSet == null || CollUtil.isEmpty(dataSet.getRows())){
            return;
        }
        try {
            // 暂时只对模拟用户进行缓存
            String currentUserName = UserManager.get().getName();
            List<String> apiUserNames = Arrays.asList(SC.v("ssm.api.invoke.user.list").split(","));
            if(!apiUserNames.contains(currentUserName)){
               return;
            }
            QueryTemplateCacheSource source = cacheSourceMap.get(queryTplId);
            if (source == null) {
                return ;
            }
            String newSql = replaceSql(sql);
            String sqlHashKey = SSDUtil.hash(newSql);
            QueryTemplateCacheResult result = cacheResultMap.get(sqlHashKey);
            // 已缓存，则不处理
            if(result != null) {
                String cacheObject = LocalCacheManager.getCache(sqlHashKey); //RedisCacheManager.get(sqlHashKey);
                if(BIUtil.isNotEmpty(cacheObject)) {
                    return;
                }
            }
            //RedisCacheManager.setAsync(sqlHashKey, JSONObject.toJSONString(dataSet), 60 * 60 * 24, TimeUnit.SECONDS);
            User user = UserManager.get();
            new Thread(()->{
                UserManager.set(user);
                long cacheSize = LocalCacheManager.addCache(JSONObject.toJSONString(dataSet), sqlHashKey);
                // 存储缓存结果
                QueryTemplateCacheService cacheService = (QueryTemplateCacheService) SpringContextUtil.getBean("queryTemplateCacheService");
                cacheService.addCacheResult(currentUserName, sqlHashKey, cacheSize, newSql, models, source);

                // 刷新所有服务器缓存
                refreshAllServer();
            }).start();
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("QueryTemplateCacheManager.Exception=" + e.getMessage());
        }
    }

    /**
     * 获取缓存
     * @param queryTplId
     * @param sql
     * @return
     */
    public static ResultDataSet getCache(String queryTplId, String sql, QueryContext ctx){
        if(!isEnable()){
            return null;
        }
        try {
            QueryTemplateCacheSource source = cacheSourceMap.get(queryTplId);
            if (source == null) {
                return null;
            }
            String newSql = replaceSql(sql);
            String sqlHashKey = SSDUtil.hash(newSql);
            QueryTemplateCacheResult result = cacheResultMap.get(sqlHashKey);
            if (result == null || Enabled.isFalse(result.getIsActive())) {
                return null;
            }
            // String cacheObject = RedisCacheManager.get(sqlHashKey);
            String cacheObject = LocalCacheManager.getCache(sqlHashKey);
            if (BIUtil.isEmpty(cacheObject)) {
                return null;
            }
            ResultDataSet dataSet = JSONObject.parseObject(cacheObject, ResultDataSet.class);
            if (dataSet == null || !hasAllColumnAuth(dataSet.getColumns(), ctx.getAclFields())) {
                return null;
            }
            dataSet.getProperties().put("cache", true);
            return dataSet;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    // 判断用户是否有所有的列权限
    private static boolean hasAllColumnAuth(List<ResultDataSetColumn> columns, Map<String, String> aclCodes) {
        if (BIUtil.isEmpty(columns)) {
            return true;
        }

        for (ResultDataSetColumn column : columns) {
            if (!aclCodes.containsKey(column.getRawCode())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 刷新所有服务器缓存
     */
    public synchronized static void refreshAllServer(){
        Map<String, String> params = new HashMap<>();
        params.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.QueryTemplate.toString());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, params);
    }

    /**
     * 1、去掉sql注释
     * 2、去掉sql中的limit
     * @param sql
     * @return
     */
    protected static String replaceSql(String sql){
        String newSql = sql.replaceAll("/\\*.*?\\*/", "");
        String limitStr = " LIMIT";
        if(newSql.contains(limitStr)){
            newSql = newSql.substring(0, newSql.indexOf(limitStr));
        }
        newSql = newSql.trim();
        return newSql;
    }

    protected static boolean isEnable(){
        return "true".equalsIgnoreCase(SC.v("ssm.query.template.cache.enable", "true"));
    }

    /**
     * 通过表名清除缓存
     * @param hasChangedTableNames
     */
    public static void deleteByTableName(List<String> hasChangedTableNames, String operator){
        if(BIUtil.isEmpty(hasChangedTableNames)){
            return;
        }
        List<String> etlJobs = new ArrayList<>();
        for(String tableName : hasChangedTableNames){
            MetaTable mt = SSDMetaCacheManager.getTableByFullName(tableName);
            if(mt != null){
                etlJobs.addAll(mt.getEtlJobs());
            }
        }
        deleteByEtlJob(etlJobs, operator);
    }
    /**
     * 通过etljob清除缓存
     */
    public static void deleteByEtlJob(List<String> hasChangedEtlJobs, String operator){
        if(BIUtil.isEmpty(hasChangedEtlJobs)){
            return ;
        }
        List<QueryTemplateCacheOperateLog> operateLogs = new ArrayList<>();
        Set<String> deleteKeys = new HashSet<>();
        for(QueryTemplateCacheResult result : cacheResultMap.values()){
            if(BIUtil.isEmpty(result.getEtlJobs())){
                continue;
            }
            List<String> etlJobs = Arrays.asList(result.getEtlJobs().split(","));

            List<String> intersectionJobs = etlJobs.stream().filter(hasChangedEtlJobs::contains).collect(Collectors.toList());
            if(BIUtil.isNotEmpty(intersectionJobs)){
                deleteKeys.add(result.getCacheKey());
                // 日志
                operateLogs.add(new QueryTemplateCacheOperateLog(result.getQueryTplId(), result.getQueryTplName(), result.getCacheKey(), "delete", BIUtil.listToStr(hasChangedEtlJobs), "delete by etljob", operator));
            }
        }
        deleteByCacheKey(deleteKeys, operateLogs);
    }

    protected static void deleteByCacheKey(Set<String> deleteKeys, List<QueryTemplateCacheOperateLog> operateLogs){
        if(BIUtil.isEmpty(deleteKeys)){
            return;
        }
        //RedisCacheManager.delete(deleteKeys);
        LocalCacheManager.deleteCache(deleteKeys);
        // 删除缓存结果
        QueryTemplateCacheService cacheService = (QueryTemplateCacheService) SpringContextUtil.getBean("queryTemplateCacheService");
        cacheService.deleteByCacheKey(deleteKeys);

        // 记录删除日志
        cacheService.addOperateLog(operateLogs);

        // 刷新缓存
        refreshAllServer();
    }

    /**
     * 清理分析模板缓存：只清理缓存，不删除配置
     * @param analysisTplId
     */
    public static void clearAnalysisTemplateCache(String analysisTplId){
        if(BIUtil.isEmpty(analysisTplId)){
            return;
        }
        List<QueryTemplateCacheOperateLog> operateLogs = new ArrayList<>();
        Set<String> deleteKeys = new HashSet<>();
        for(QueryTemplateCacheResult result : cacheResultMap.values()){
            if(!analysisTplId.equals(result.getAnalysisTplId())){
                continue;
            }
            deleteKeys.add(result.getCacheKey());
            // 日志
            operateLogs.add(new QueryTemplateCacheOperateLog(result.getQueryTplId(), result.getQueryTplName(), result.getCacheKey(), "delete", "analysisTplId=" + analysisTplId, "手动ui删除", UserManager.get().getName()));
        }
        deleteByCacheKey(deleteKeys, operateLogs);
    }
}
