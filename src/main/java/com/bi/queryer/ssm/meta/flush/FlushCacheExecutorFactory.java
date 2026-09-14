package com.bi.queryer.ssm.meta.flush;

import com.bi.queryer.ssm.enums.CacheFlushType;

import java.util.Map;

/**
 * 刷新缓存工厂类
 */
public class FlushCacheExecutorFactory {

    public static FlushCacheExecutor getFlushCacheEexcutor(CacheFlushType cacheFlushType, Map<String, ?> initParams) {

        switch (cacheFlushType) {

            case FLUSH_FIELD_BY_CODE:
                return new FlushFieldByCodeCacheExecutor(initParams);
            case FLUSH_CTG_SORT_ID:
                return new FlushCtgSortIdCacheExecutor(initParams);
        }

        throw new RuntimeException("不支持的刷新缓存类型");
    }

}
