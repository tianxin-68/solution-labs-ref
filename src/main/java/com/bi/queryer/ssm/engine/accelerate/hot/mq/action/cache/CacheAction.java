package com.bi.queryer.ssm.engine.accelerate.hot.mq.action.cache;

import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionByMQ;

/**
 * @Author contributor
 * @Date 11:20 2025/1/10
 * @Description 缓存相关action
 **/
public class CacheAction extends ActionByMQ<CacheActionParameter> {
    public CacheAction(CacheActionParameter parameter) {
        super(parameter);
    }

    @Override
    public Object action() {
        /**
        if(BIUtil.isNotEmpty(parameter.getTableNames())){
            QueryTemplateCacheManager.deleteByTableName(parameter.getTableNames(), parameter.getOperator());
        }

        if(BIUtil.isNotEmpty(parameter.getEtlJobs())){
            QueryTemplateCacheManager.deleteByEtlJob(parameter.getEtlJobs(), parameter.getOperator());
        }*/
        return null;
    }
}
