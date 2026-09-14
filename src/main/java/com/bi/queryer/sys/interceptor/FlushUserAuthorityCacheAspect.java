package com.bi.queryer.sys.interceptor;

import com.bi.queryer.sys.cache.CacheService;
import com.bi.queryer.sys.common.ResponseMessage;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 刷新用户切面
 * @author contributor
 */
@Aspect
@Component
public class FlushUserAuthorityCacheAspect {

    @Autowired
    private CacheService cacheService = null;

    @AfterReturning(value = "@annotation(com.bi.queryer.sys.interceptor.FlushUserAuthorityCache)", returning="retVal")
    public void doFlushUser(Object retVal){

        if(retVal!=null){
            ResponseMessage responseMessage = (ResponseMessage)retVal;

            if(responseMessage.getSuccess()){
                //刷新缓存
                cacheService.flushAuthorityCache();
            }
        }

    }
}
