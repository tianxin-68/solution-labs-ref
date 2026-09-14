package com.bi.queryer.ssm.engine.accelerate.cache;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.tx.cache.AutoReleaseLock;
import com.tx.cache.common.exception.CacheParamException;
import com.tx.cache.common.exception.KVException;
import com.tx.cache.redis.RScanArgs;
import com.tx.cache.redis.RedisCacheClient;
import com.tx.cache.redis.TxRedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RedisCacheManager {

    protected static Logger logger = LoggerFactory.getLogger(RedisCacheManager.class);

    protected static String spaceKey = "";

    private static final int MAX_CACHE_SIZE = 1024 * 1000;

    /**
     * Redis 异步写线程池。
     *
     * setAsync 是缓存加速能力，不能每次调用都创建新线程；使用有界队列限制瞬时并发，
     * Redis 慢或写入高峰时队列满则丢弃本次缓存写入，避免反向拖垮业务请求。
     */
    private static final ThreadPoolExecutor REDIS_CACHE_WRITE_EXECUTOR = new ThreadPoolExecutor(
            5,
            60,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new NamedThreadFactory("redis-cache-writer"),
            new ThreadPoolExecutor.AbortPolicy()
    );

    public static void initialize(){
        getRedisClient();
    }

    public static RedisCacheClient<String, Object> getRedisClient(){
        String cacheName = SC.v("redis.cache.name", "tx-ssm");
        RedisCacheClient<String, Object> redisClient = TxRedisManager.getClient(cacheName);
        return redisClient;
    }

    public static String get(String spaceKey, String valueKey) {
        RedisCacheClient client = getRedisClient();
        try {
            Object value = client.hget(spaceKey, valueKey);
            if(value != null){
                value = SSDUtil.decompress((String) value);
            }
            return value + "";
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("获取缓存失败", e);
        }
        return null;
    }

    public static String get(String valueKey) {
        RedisCacheClient client = getRedisClient();
        try {
            Object value = client.get(valueKey);
            if(value != null){
                value = SSDUtil.decompress((String) value);
            }
            return value + "";
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("获取缓存失败", e);
        }
        return null;
    }

    public static void setAsync(String spaceKey, String valueKey, String cacheObject, long expireAfterWrite, TimeUnit timeUnit) {
        if(cacheObject == null) {
            return;
        }
        submitRedisCacheWriteTask(valueKey, () -> {
            RedisCacheClient client = getRedisClient();
            String cacheString = buildCacheString(valueKey, cacheObject);
            if (cacheString == null) {
                return;
            }
            client.hset(spaceKey, valueKey, cacheString, new Long(toSeconds(expireAfterWrite, timeUnit)).intValue());
        });
    }

    public static void setAsync(String valueKey, String cacheObject, long expireAfterWrite, TimeUnit timeUnit) {
        if(cacheObject == null) {
            return;
        }
        submitRedisCacheWriteTask(valueKey, () -> {
            set(valueKey, cacheObject, expireAfterWrite, timeUnit);
        });
    }

    public static void set(String valueKey, String cacheObject, long expireAfterWrite, TimeUnit timeUnit) throws KVException {
        RedisCacheClient client = getRedisClient();
        String cacheString = buildCacheString(valueKey, cacheObject);
        if (cacheString == null) {
            return;
        }
        client.set(valueKey, cacheString, new Long(toSeconds(expireAfterWrite, timeUnit)).intValue(), TimeUnit.SECONDS);
    }

    private static void submitRedisCacheWriteTask(String valueKey, RedisCacheWriteTask writeTask) {
        try {
            REDIS_CACHE_WRITE_EXECUTOR.execute(() -> {
                try {
                    writeTask.write();
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.error("缓存设置失败", e);
                }
            });
        } catch (RejectedExecutionException e) {
            // 队列满说明当前 Redis 写入已经堆积；缓存写入可丢弃，不能阻塞调用方业务流程。
            logger.error(valueKey + "缓存设置失败,Redis异步写线程池队列已满", e);
        }
    }

    private static String buildCacheString(String valueKey, String cacheObject) {
        // Redis 仍沿用原有压缩格式，避免影响已有 get/decompress 读取逻辑。
        String cacheString = SSDUtil.compress(cacheObject);
        // 只需要判断写入 Redis 的字符串大小，避免再走 Java 对象序列化造成额外 CPU 和内存开销。
        if(cacheString != null && cacheString.getBytes(StandardCharsets.UTF_8).length >= MAX_CACHE_SIZE){
            logger.error(valueKey + "缓存设置失败,大小超限");
            return null;
        }
        return cacheString;
    }

    private static long toSeconds(long expireAfterWrite, TimeUnit timeUnit) {
        if (timeUnit == null) {
            return expireAfterWrite;
        }
        switch (timeUnit){
            case SECONDS:
                return expireAfterWrite;
            case MINUTES:
                return expireAfterWrite * 60;
            case HOURS:
                return expireAfterWrite * 60 * 60;
            case DAYS:
                return expireAfterWrite * 24 * 60 * 60;
            default:
                return timeUnit.toSeconds(expireAfterWrite);
        }
    }

    public static byte[] objectToByte(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bo = null;
        ObjectOutputStream oo = null;
        try {
            bo = new ByteArrayOutputStream();
            oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

        } catch (Exception ae) {
            ae.printStackTrace();
        } finally {
            try {
                bo.close();
                oo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return (bytes);
    }

    public static Set<String> hscanKeys(String spaceKey) {
        return hscanKeys(spaceKey, 1000);
    }

    public static Set<String> hscanKeys(String spaceKey, int limit) {

        Set<String> keys = new HashSet<>();

        RedisCacheClient client = getRedisClient();
        try {
            RScanArgs rScanArgs = new RScanArgs();
            rScanArgs.limit(limit);
            Map<String, Object> resultMap = client.hscan(spaceKey).getMap();
            if (resultMap == null) {
                return keys;
            }

            return resultMap.keySet();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return keys;
    }

    /**
     * 设置缓存：有效期为截止到当天->2小时
     */
    public static void setAsyncCurrentDayActive(String spaceKey, String valueKey, String needCacheObject, long expireMinute)  {
        try{
            Calendar now = Calendar.getInstance();
            Calendar todayLastTime = Calendar.getInstance();
            todayLastTime.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE), 23,59);
            long diff = DateUtil.between(now.getTime(), todayLastTime.getTime(), DateUnit.MINUTE);
            diff = expireMinute;//Long.valueOf(SC.v("redis.cache.expire", "120"));
            setAsync(spaceKey, valueKey, needCacheObject, diff, TimeUnit.MINUTES);
        }catch (Throwable e) {
            e.printStackTrace();
            logger.error("缓存设置失败[setAsyncCurrentDayActive]", e);
        }
    }

    public static void delete(String spaceKey){
        try {
            getRedisClient().deleteAsync(spaceKey);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("缓存删除失败", e);
        }
    }

    public static void delete(Set<String> valueKeys){
        try {
            getRedisClient().multiDelete(valueKeys);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("缓存删除失败", e);
        }
    }

    public static void delete(String spaceKey, String valueKey) {
        try {

            if (StrUtil.isEmpty(valueKey)) {
                return;
            }

            getRedisClient().hdel(spaceKey, valueKey);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("缓存删除失败", e);
        }
    }

    // 尝试获取 Redis 分布式锁。竞争失败与 Redis 异常都会返回未持锁结果，通过 TryLockResult 区分两种 null 场景。
    // Redis 不可用时降级放行，由调用方跳过分布式锁保护，仅保留 JVM 内 synchronized 兜底。
    public static TryLockResult tryLock(String lockName, String key, long expire, TimeUnit timeUnit) {
        try {
            AutoReleaseLock lock = getRedisClient().tryLock(getKey(lockName, key), expire, timeUnit);
            if (lock == null) {
                logger.warn("Redis tryLock contention failed, lockName={}, key={}", lockName, key);
                return TryLockResult.contentionFailed();
            }
            return TryLockResult.acquired(lock);
        } catch (CacheParamException e) {
            logger.warn("Redis tryLock failed, degrade to local lock only, lockName={}, key={}", lockName, key, e);
            return TryLockResult.degraded();
        } catch (Exception e) {
            logger.error("Redis tryLock error, degrade to local lock only, lockName={}, key={}", lockName, key, e);
            return TryLockResult.degraded();
        }
    }

    // 锁完整 key：命名空间 + 业务 key
    private static String getKey(String lockName, String key) {
        return lockName + ":" + key;
    }

    // Redis 分布式锁尝试结果，配合 try-with-resources 自动释放
    public static class TryLockResult implements AutoCloseable {
        private final AutoReleaseLock lock;
        // true 表示 Redis 异常导致降级，false 且 lock 为空表示正常竞争失败
        private final boolean degraded;

        private TryLockResult(AutoReleaseLock lock, boolean degraded) {
            this.lock = lock;
            this.degraded = degraded;
        }

        public static TryLockResult acquired(AutoReleaseLock lock) {
            return new TryLockResult(lock, false);
        }

        public static TryLockResult contentionFailed() {
            return new TryLockResult(null, false);
        }

        public static TryLockResult degraded() {
            return new TryLockResult(null, true);
        }

        public boolean isAcquired() {
            return lock != null;
        }

        public boolean isDegraded() {
            return degraded;
        }

        public boolean isContentionFailed() {
            return !isAcquired() && !degraded;
        }

        @Override
        public void close() {
            if (lock != null) {
                try {
                    lock.close();
                } catch (KVException e) {
                    logger.warn("Redis lock release failed", e);
                }
            }
        }
    }


    private interface RedisCacheWriteTask {
        void write() throws Throwable;
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String threadNamePrefix;
        private final AtomicInteger threadIndex = new AtomicInteger(1);

        private NamedThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, threadNamePrefix + "-" + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }


    public static void main(String[] args) {
        Calendar now = Calendar.getInstance();
        Calendar todayLastTime = Calendar.getInstance();
        todayLastTime.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE), 23,59);
        System.out.println(DateUtil.format(todayLastTime.getTime(), "yyyy-MM-dd HH:mm:ss"));
        long diff = DateUtil.between(now.getTime(), todayLastTime.getTime(), DateUnit.MINUTE);
        System.out.println(diff);

        System.out.println("now:" + DateUtil.now());
    }

}
