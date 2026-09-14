package com.bi.queryer.ssm.engine.session;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.interceptor.QueryInterceptResult;
import com.bi.queryer.ssm.engine.interceptor.QueryInterceptorManager;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import com.tx.cache.common.exception.KVException;
import com.tx.cache.redis.RedisCacheClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Author contributor
 * @Date 14:05 2022-11-08
 * @Description 查询会话管理
 **/
public abstract class QuerySessionManager {
    private static final Logger logger = LoggerFactory.getLogger(QuerySessionManager.class);
    private static final Object lock = new Object();

    public static final String redisSessionKeyPrefix = QuerySessionManager.class.getName() + "@";

    private static ExecutorService threadPool = Executors.newFixedThreadPool(1);

    public static ResponseMessage add(String sessionId, String queryId, String dsKey) {
        return add(sessionId, queryId, dsKey, null);
    }

    public static ResponseMessage add(String sessionId, String queryId, String dsKey, QueryEngine engine) {
        ResponseMessage sessionMessage = new ResponseMessage();
        if(BIUtil.isEmpty(sessionId)){
            return sessionMessage;
        }
        synchronized (lock){
            // 先组装 session 本地字段，不涉及跨线程共享状态
            QuerySession session = new QuerySession();
            session.setCreatedBy(UserManager.get().getName());
            session.setQueryId(queryId);
            session.setDsKey(dsKey);
            session.setServerIp(BIUtil.getServerIP());
            session.setSessionId(sessionId);
            session.setCreatedTime(DateUtil.now());

            QueryConfigure configure = null;
            if(engine != null){
                configure = engine.getConfig();
                session.setOlapApiKey(configure.getSettings().getOlapApiKey());
                if(configure.getTemplateEntity() != null) {
                    session.setTemplateId(configure.getTemplateEntity().getId());
                    session.setViewId(configure.getTemplateEntity().getViewId());
                }
                session.setTableNames(engine.buildQueryTableNames());
            }

            // 外部 OLAP API（非系统 key）需要 Redis 分布式锁，保证跨实例并发计数准确
            // 系统 key、Web 查询、BaseDao 直查等场景不走分布式锁
            String olapApiKey = session.getOlapApiKey();
            boolean needOlapApiRateLimitLock = engine != null
                    && StrUtil.isNotEmpty(olapApiKey)
                    && configure != null
                    && !configure.getSettings().isSystemOlapApiKey();
            if (needOlapApiRateLimitLock) {
                String lockKey = olapApiKey + ":" + session.getCreatedBy();
                try (RedisCacheManager.TryLockResult rateLimitLock = RedisCacheManager.tryLock(
                        BIConsts.Locks.OLAP_API_RATE_LIMIT_LOCK, lockKey, 5, TimeUnit.SECONDS)) {
                    // 锁被其他实例占用：与数量超限返回相同提示，避免用户感知差异
                    if (rateLimitLock.isContentionFailed()) {
                        throw new BIException(BIConsts.OLAP_API_ERROR_RATE_LIMIT, BIException.CODE_WARN);
                    }
                    // Redis 异常降级：跳过分布式锁，仅依赖 JVM synchronized + 单次数量校验
                    if (rateLimitLock.isDegraded()) {
                        logger.warn("OLAP API rate limit lock degraded, sessionId={}, lockKey={}", sessionId, lockKey);
                    }
                    interceptAndPersistSession(session, configure);
                }
            } else if (engine != null) {
                // QueryEngine 多维查询：有完整 configure，走限流拦截后再写入
                interceptAndPersistSession(session, configure);
            } else {
                // BaseDao 底层 SQL 直查：无 configure，仅登记 session 供 kill / 监控使用
                persistSession(session);
            }
        }
        return sessionMessage;
    }

    // 先走 QueryInterceptorManager 限流校验，通过后写入 Redis 与 DB
    private static void interceptAndPersistSession(QuerySession session, QueryConfigure configure) {
        QueryInterceptResult interceptResult = QueryInterceptorManager.intercept(session, configure);
        if(interceptResult.isIntercepted()){
            throw new BIException(interceptResult.getMessage(), interceptResult.getCode());
        }
        persistSession(session);
    }

    // 写入 Redis（10 分钟）与 DB，供运行监控、kill 查询、限流计数使用
    private static void persistSession(QuerySession session) {
        try{
            RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
            redisCacheClient.hset(redisSessionKeyPrefix, session.getSessionId(), BIUtil.toJSONString(session), 10 * 60);

            BaseDao dao = DBUtil.getBaseDao();
            dao.insert("ssm.session.add", session);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void update(String sessionId, String queryId){
        try{
            BaseDao dao = DBUtil.getBaseDao();
            QuerySession session = new QuerySession();
            session.setSessionId(sessionId);
            session.setQueryId(queryId);
            dao.update("ssm.session.update", session);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static QuerySession get(String sessionId){
        try{
            // 先从redis中获取，若没有则从数据库获取
            RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
            String value = redisCacheClient.hget(redisSessionKeyPrefix, sessionId);
            if(BIUtil.isNotEmpty(value)){
                QuerySession session = JSON.parseObject(value, QuerySession.class);
                return session;
            }else {
                BaseDao dao = DBUtil.getBaseDao();
                QuerySession session = (QuerySession) dao.queryObject("ssm.session.get", sessionId);
                return session;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    // 获取所有session
    public static List<QuerySession> getAll() {
        List<QuerySession> sessions = new ArrayList<>();
        try {
            RedisCacheClient client = RedisCacheManager.getRedisClient();
            Set<String> sessionIds = RedisCacheManager.hscanKeys(redisSessionKeyPrefix,200);
            long now = System.currentTimeMillis();

            //Redis Hash（HSET）的 TTL 只能设置在整个 Key 上，不能对单个 field 独立过期。
            //每次有新 add() 调用，都会重置整个 Hash Key 的过期时间为 10 分钟，导致 Hash Key 永远不会过期。
            //解决办法：对 Redis 返回结果按 createdTime 过滤掉超过 10 分钟的 stale session

            if(CollUtil.isNotEmpty(sessionIds)) {
                String[] keysArray = sessionIds.toArray(new String[sessionIds.size()]);
                List<Object> data = client.hmget(redisSessionKeyPrefix, keysArray);

                for (Object value : data) {

                    if (value == null) {
                        continue;
                    }

                    QuerySession session = JSON.parseObject(value.toString(), QuerySession.class);
                    if (session.getCreatedTime() == null) {
                        continue;
                    }
                    long age = now - DateUtil.parseDateTime(session.getCreatedTime()).getTime();
                    if (age <= 10 * 60 * 1000) {
                        sessions.add(session);
                    } else {
                        client.hdel(redisSessionKeyPrefix, session.getSessionId());
                    }
                }
            }

            threadPool.execute(() -> {
                try {
                    BaseDao dao = DBUtil.getBaseDao();
                    List<QuerySession> dbSessions = dao.queryObjectList("ssm.session.getAll", new HashMap<>(), QuerySession.class);
                    Set<String> keys = RedisCacheManager.hscanKeys(redisSessionKeyPrefix);
//                    // 更新写入redis
//                    for (QuerySession session : dbSessions){
//                        try {
//                            // 过期时间
//                            int expireInSecond = 10 * 60 - (int) (System.currentTimeMillis() - DateUtil.parseDateTime(session.getCreatedTime()).getTime()) / 1000 ;
//                            expireInSecond = expireInSecond < 0 ? 0 : expireInSecond;
//
//                            // redis不为空
//                            if(BIUtil.isNotEmpty(keys)){
//                                // 已在redis中，则更新过期时间
//                                if(keys.contains(session.getSessionId())) {
//                                    client.hset(redisSessionKeyPrefix, session.getSessionId(), BIUtil.toJSONString(session), expireInSecond);
//                                }else {
//                                    // 不存在，则删除
//                                    client.hdel(redisSessionKeyPrefix, session.getSessionId());
//                                }
//                            }else{
//                                client.hset(redisSessionKeyPrefix, session.getSessionId(), BIUtil.toJSONString(session), expireInSecond);
//                            }
//                        } catch (KVException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
                    // 数据库没有，但redis有，则清空redis
                    if (BIUtil.isEmpty(dbSessions) && BIUtil.isNotEmpty(keys)) {
                        List<String> deleteKeys = new ArrayList<>();
                        deleteKeys.addAll(keys);
                        List<List<String>> splitDeleteKeys = BIUtil.splitList(deleteKeys, 100);
                        for (List<String> groupDeleteKeyList : splitDeleteKeys) {
                            if (CollUtil.isEmpty(groupDeleteKeyList)) {
                                continue;
                            }
                            Set<String> setKeys = new HashSet<>(groupDeleteKeyList);
                            client.hdel(redisSessionKeyPrefix, setKeys.toArray(new String[setKeys.size()]));
                        }

                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        return sessions;
    }

    public static void delete(String sessionId) {
        if (BIUtil.isEmpty(sessionId)) {
            return;
        }
        try {
            RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
            redisCacheClient.hdel(redisSessionKeyPrefix, sessionId);
            threadPool.execute(() -> {
                BaseDao dao = DBUtil.getBaseDao();
                dao.delete("ssm.session.delete", sessionId);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
