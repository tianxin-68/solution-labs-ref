package com.bi.queryer.ssm.engine.interceptor;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.session.QuerySession;

/**
 * @Author contributor
 * @Date 11:33 2025/12/5
 * @Description 查询拦截器
 **/
public abstract class QueryInterceptor {

    /**
     * 刷新配置换成缓存
     */
    public void refreshConfigureCache(){

    }

    public abstract QueryInterceptResult intercept(QuerySession querySession, QueryConfigure queryConfigure);
}
