package com.bi.queryer.sys.cache;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author contributor
 */
@Service
public class CacheService {

    ExecutorService cachedThreadPool = null;

    public CacheService(){
        cachedThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * 删除缓存
     * @param type
     * @param list
     */
    public void deleteCacheByKeys(CacheType type, List<String> list){
        if(BIUtil.isEmpty(list)){
            return;
        }
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (String key:list ) {
                    CacheManager.remove(type,key);
                }
            }
        });
    }

    public void deleteCacheByKeys(CacheType type, String key) {

        if(StringUtil.isEmpty(key)){
            return;
        }
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                CacheManager.remove(type, key);
            }
        });
    }


    /**
     * 用于系统管理刷新缓存用户模块
     * @param userName
     */
    public void flushByUserName(String userName){

        if(BIUtil.isEmpty(userName)){
            return;
        }

        if("all".equalsIgnoreCase(userName)){
            CacheManager.remove(CacheType.Authority);
        }else{
            deleteCacheByKeys(CacheType.Authority,userName);
        }
    }

    /**
     * 刷新所有用户权限缓存
     */
    public void flushAuthorityCache(){
        CacheManager.remove(CacheType.Authority);
    }
}
