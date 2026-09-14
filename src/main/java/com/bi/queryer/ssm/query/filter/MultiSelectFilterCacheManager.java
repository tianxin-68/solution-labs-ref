package com.bi.queryer.ssm.query.filter;

import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.meta.filter.FieldFilterCacheConfig;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.MemoryCacheSyncSSMService;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.sys.startup.InitializableModule;
import com.bi.queryer.sys.startup.SystemInitializer;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:21 2024/12/5
 * @Description 多选数据集管理器
 * 1、负责top字段缓存
 * 2、负责top字段下拉值定时缓存更新
 **/
@Component
public class MultiSelectFilterCacheManager implements Initializable {

    public static final String cache_key_prefix = "filter_select@";

    public static final String cache_key_total_suffix = "@total";

    public static final String cache_space_key = "MultiSelectFilterDatasetProvider";

    public static final String cache_space_schedule_key = "MultiSelectFilterDatasetProvider@Schedule";

    public static final String cache_space_keys[] = new String[]{cache_space_key, cache_space_schedule_key};

    private static Map<String, FieldFilterCacheConfig> fieldFilterCacheConfigs = new LinkedHashMap<>(100);
    @Override
    public void initialize(Map<String, ?> initParams) throws BIException {
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        List<FieldFilterCacheConfig> filterCacheCfglist =  (List<FieldFilterCacheConfig>) dao.queryObjectList("queryAllFieldFilterCacheConfig", null, DataSourceType.Default);
        Map<String, FieldFilterCacheConfig> fieldFilterCacheConfigsTmp = new LinkedHashMap<>(100);
        for(FieldFilterCacheConfig cfg : filterCacheCfglist){
            fieldFilterCacheConfigsTmp.put(cfg.getFieldId(), cfg);
        }
        fieldFilterCacheConfigs.clear();
        fieldFilterCacheConfigs.putAll(fieldFilterCacheConfigsTmp);
        fieldFilterCacheConfigsTmp.clear();
    }

    /**
     * 刷新所有服务器配置缓存
     */
    public synchronized static void flushConfigAllServer(){
        Map<String, String> params = new HashMap<>();
        params.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.MultiSelect.toString());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, params);
    }

    public static List<FieldFilterCacheConfig> getFieldFilterCacheConfigs(){
        return fieldFilterCacheConfigs.values().stream().collect(Collectors.toList());
    }

    public static void setRowsCache(String spaceKey, String fieldId, String rows, long expireMinute){
        RedisCacheManager.setAsyncCurrentDayActive(spaceKey, getRowsKey(fieldId), rows, expireMinute);
    }

    public static void setTotalCache(String spaceKey, String fieldId, String total, long expireMinute){
        RedisCacheManager.setAsyncCurrentDayActive(spaceKey, getTotalKey(fieldId), total, expireMinute);
    }

    public static String getRowsCache(String fieldId){
        for(String spaceKey : cache_space_keys){
            String value = RedisCacheManager.get(spaceKey, getRowsKey(fieldId));
            if(BIUtil.isNotEmpty(value)){
                return value;
            }
        }
        return null;
    }

    public static String getTotalCache(String fieldId){
        for(String spaceKey : cache_space_keys){
            String value = RedisCacheManager.get(spaceKey, getTotalKey(fieldId));
            if(BIUtil.isNotEmpty(value)){
                return value;
            }
        }
        return null;
    }

    public static String getRowsKey(String fieldId){
        return cache_key_prefix +  fieldId;//metaField.getCode();
    }

    public static String getTotalKey(String fieldId){
        return getRowsKey(fieldId) + cache_key_total_suffix;
    }

    public static void clearCache(){
        for (String spaceKey : cache_space_keys) {
            RedisCacheManager.delete(spaceKey);
        }
    }

    public static void clearCache(String fieldId){
        for (String spaceKey : cache_space_keys) {
            RedisCacheManager.delete(spaceKey, getRowsKey(fieldId));
            RedisCacheManager.delete(spaceKey, getTotalKey(fieldId));
        }
    }
}
