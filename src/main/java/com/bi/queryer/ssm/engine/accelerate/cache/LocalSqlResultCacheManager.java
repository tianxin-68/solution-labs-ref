package com.bi.queryer.ssm.engine.accelerate.cache;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.enums.QueryModeType;
import com.bi.queryer.ssm.enums.QuerySourceType;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 基于最终执行SQL的短期本地结果缓存。
 */
public abstract class LocalSqlResultCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalSqlResultCacheManager.class);

    /**
     * 本地 SQL 结果缓存异步写线程池。
     *
     * 本地缓存是查询加速能力，不能每次写入都创建新线程；使用有界队列限制瞬时写入堆积，
     * 队列满时丢弃本次缓存写入，避免反向影响主查询链路。
     */
    private static final ThreadPoolExecutor LOCAL_SQL_CACHE_WRITE_EXECUTOR = new ThreadPoolExecutor(
            5,
            50,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    public static ResultDataSet getCache(QueryConfigure config, QueryContext cxt, String sessionId, DataSourceType dataSourceType, String sql) {
        if (!isQueryCacheable(config, cxt, sessionId)) {
            return null;
        }
        try {
            String cacheKey = buildCacheKey(dataSourceType, sql);
            if (BIUtil.isEmpty(cacheKey)) {
                return null;
            }
            String cacheObject = LocalCacheManager.getCache(cacheKey, getTtlSeconds(config));
            if (BIUtil.isEmpty(cacheObject)) {
                return null;
            }
            ResultDataSet dataSet = JSONObject.parseObject(cacheObject, ResultDataSet.class);
            if (dataSet == null) {
                return null;
            }
            if (dataSet.getProperties() != null) {
                dataSet.getProperties().put("cache", true);
                dataSet.getProperties().put("cacheType", "local");
            }
            return dataSet;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void addCache(QueryConfigure config, QueryContext cxt, String sessionId, DataSourceType dataSourceType, String sql, ResultDataSet dataSet) {
        if (!isQueryCacheable(config, cxt, sessionId) || dataSet == null) {
            return;
        }
        try {
            String cacheKey = buildCacheKey(dataSourceType, sql);
            if (BIUtil.isEmpty(cacheKey)) {
                return;
            }

            if (!isDataSetSizeCacheable(cacheKey, dataSet)) {
                return;
            }

            String cacheContent = JSONObject.toJSONString(dataSet);
            submitLocalSqlCacheWriteTask(cacheKey, cacheContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isDataSetSizeCacheable(String cacheKey, ResultDataSet dataSet) {
        int rowSize = dataSet.getSize() == null ? 0 : dataSet.getSize();
        int maxCacheRowSize = SC.getInteger("ssm.query.sql.local.cache.rows", 10000);
        if (rowSize > maxCacheRowSize) {
            logger.error(cacheKey + "本地SQL结果缓存写入跳过,行数超过限制:" + rowSize);
            return false;
        }
        if(rowSize == 0) {
            return true;
        }
        Map<String, Object> row = dataSet.getRows().get(0);
        if(BIUtil.isEmpty(row)){
            return true;
        }
        int columnSize = row.size();
        int maxCacheColumnSize = SC.getInteger("ssm.query.sql.local.cache.columns", 100);
        if (columnSize > maxCacheColumnSize) {
            logger.error(cacheKey + "本地SQL结果缓存写入跳过,列数超过限制:" + columnSize);
            return false;
        }
        return true;
    }

    private static void submitLocalSqlCacheWriteTask(String cacheKey, String cacheContent) {
        try {
            LOCAL_SQL_CACHE_WRITE_EXECUTOR.execute(() -> {
                try {
                    LocalCacheManager.addCache(cacheContent, cacheKey);
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.error(cacheKey + "本地SQL结果缓存写入失败", e);
                }
            });
        } catch (RejectedExecutionException e) {
            // 队列满说明当前本地缓存写入已经堆积；缓存写入可丢弃，不能阻塞调用方业务流程。
            logger.error(cacheKey + "本地SQL结果缓存写入失败,异步写线程池队列已满", e);
        }
    }

    /**
     * 判断当前查询是否适合短期SQL结果缓存。
     *
     * 本缓存以最终执行SQL为粒度，只服务普通离线查询：
     * 1. 实时数据集不缓存，避免返回5分钟内的旧数据；
     * 2. 导出查询不缓存，避免大结果集写入本地文件；
     * 3. 表头、mock、字段校验等非正式查询不缓存，避免污染正式查询结果；
     * 4. header session 不缓存，兼容透视表/表头预查询这类临时查询。
     */
    private static boolean isQueryCacheable(QueryConfigure config, QueryContext cxt, String sessionId) {
        if (!isEnable()) {
            return false;
        }
        if (config == null || config.getSettings() == null || cxt == null) {
            return false;
        }
        // 仅对api起效:all=所有场景、olap_api=api场景
        boolean isOnlyOlapApiEnable = !"all".equalsIgnoreCase(SC.v("ssm.query.sql.local.cache.scene", "olap_api"));
        boolean isOlapApiQuery = QuerySourceType.OLAP_API == QuerySourceType.get(config.getSettings().getQuerySource());
        if(isOnlyOlapApiEnable && !isOlapApiQuery){
            return false;
        }

        if (config.getSettings().isRtDataset()) {
            return false;
        }
        if (cxt.isExport()) {
            return false;
        }
        QueryModeType queryModeType = config.getSettings().getQueryModeType();
        if (QueryModeType.ALL != queryModeType) {
            return false;
        }
        return true;
    }

    /**
    private static boolean isHeaderQuery(String sessionId) {
        if (BIUtil.isEmpty(sessionId)) {
            return false;
        }
        String lowerSessionId = sessionId.toLowerCase();
        return lowerSessionId.contains("header")
                || sessionId.startsWith(BIConsts.HEADER_PREPARE_SESSIONID_PREFIX.toLowerCase());
    }
     */

    private static String buildCacheKey(DataSourceType dataSourceType, String sql) {
        if (dataSourceType == null || BIUtil.isEmpty(sql)) {
            return "";
        }
        String source = dataSourceType.getKey() + "|" + removeHint(sql);
        return "rs_" + getCurrentUserName() + "_" + SSDUtil.hash(source);
    }

    /**
     * SQL hint通常通过块注释追加，只影响引擎执行参数，不影响查询语义。
     * 缓存key中去掉 hint 可以避免相同SQL因用户信息、来源等 hint 差异导致无法命中。
     *
     * 注意：这里只移除块注释，不移除 limit/order by/where 等会影响结果集的SQL片段。
     */
    private static String removeHint(String sql) {
        if (BIUtil.isEmpty(sql)) {
            return "";
        }
        return sql.replaceAll("/\\*.*?\\*/", "").trim();
    }

    private static String getCurrentUserName() {
        User user = UserManager.get();
        if (user == null || BIUtil.isEmpty(user.getName())) {
            return "unknown-user";
        }
        return user.getName();
    }

    public static boolean isEnable() {
        return "true".equalsIgnoreCase(SC.v("ssm.query.sql.local.cache.enable", "true"));
    }

    public static long getTtlSeconds(QueryConfigure config) {
        Long ttl = Long.valueOf(SC.v("ssm.query.sql.local.cache.ttl.seconds", "300"));
        // 仅对api起效
        boolean isOlapApiQuery = QuerySourceType.OLAP_API == QuerySourceType.get(config.getSettings().getQuerySource());
        if(isOlapApiQuery){
            return ttl;
        }
        // 若多所有查询起效，则需要针对非api查询进行时间段的管控
        // 12点前不起效
        if(!isOlapApiQuery){
            int hour = DateUtil.dateNew(new DateTime()).hour(true);
            if(hour < 12){
                ttl = 1L;
            }
        }
        return ttl;
    }
}
