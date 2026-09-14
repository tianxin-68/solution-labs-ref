package com.bi.queryer.ssm.engine.interceptor;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.OlapApiManager;
import com.bi.queryer.ssm.api.entity.OlapApiKeyEntity;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.ssm.engine.session.QuerySessionManager;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(3)
public class QueryOlapApiRateLimitInterceptor extends QueryInterceptor{
    @Override
    public QueryInterceptResult intercept(QuerySession querySession, QueryConfigure queryConfigure) {
        QueryInterceptResult limitMessage = new QueryInterceptResult();

        if (!"true".equalsIgnoreCase(SC.v("ssm.query.olap.api.rate.limit.enable", "true"))) {
            return limitMessage;
        }

        //没有olap_api_key不处理
        String olapApiKey = queryConfigure.getSettings().getOlapApiKey();
        if (StrUtil.isEmpty(olapApiKey)) {
            return limitMessage;
        }

        Integer singleOlapApiSessionMaxCount = Integer.valueOf(SC.v("ssm.query.single.olap.api.session.max", "3"));

        // 从olap_api_key中获取最大查询会话数
        OlapApiKeyEntity olapApiKeyEntity = OlapApiManager.get(olapApiKey);
        if (olapApiKeyEntity != null && olapApiKeyEntity.getMaxQueryConcurrency() > 0) {
            singleOlapApiSessionMaxCount = olapApiKeyEntity.getMaxQueryConcurrency();
        }

        // 获取当前所有活跃的查询会话
        List<QuerySession> currentSessions = QuerySessionManager.getAll();

        String userName = UserManager.get().getName();

        // 统计同一 api_key + 用户下当前活跃 session 数，含本次即将写入的 session 前已有数量
        int apiKeySessionCount = 0;
        for (QuerySession session : currentSessions) {
            if (!olapApiKey.equalsIgnoreCase(session.getOlapApiKey())) {
                continue;
            }

            if(!session.getCreatedBy().equalsIgnoreCase(userName)){
                continue;
            }
            apiKeySessionCount++;
        }

        // 已达上限则拦截；使用 >= 避免 max=3 时第 4 个请求漏拦（off-by-one）
        if (apiKeySessionCount >= singleOlapApiSessionMaxCount) {
            String rateLimitMessage = String.format("%s:olap_api_key:%s最多同时查询数为%s,请稍后重试！",
                    BIConsts.OLAP_API_ERROR_RATE_LIMIT_PREFIX,
                    BIUtil.desensitizeGuid32(olapApiKey),
                    singleOlapApiSessionMaxCount);
            limitMessage = new QueryInterceptResult(true, rateLimitMessage, BIException.CODE_WARN);
            return limitMessage;
        }

        return limitMessage;
    }
}
