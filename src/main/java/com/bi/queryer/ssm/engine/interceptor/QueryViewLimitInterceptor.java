package com.bi.queryer.ssm.engine.interceptor;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:02 2026/5/12
 * @Description 查询视图查询阻断拦截器
 **/
@Component
@Order(0)
public class QueryViewLimitInterceptor extends QueryInterceptor {

    private static final String VIEW_BLOCK_WHITE_LIST_CONFIG = "ssm.query.view.limit.white.list";

    @Override
    public QueryInterceptResult intercept(QuerySession querySession, QueryConfigure queryConfigure) {
        QueryInterceptResult result = new QueryInterceptResult();
        if (!"true".equalsIgnoreCase(SC.v("ssm.query.view.limit.enable", "false"))) {
            return result;
        }
        if (BIUtil.isBossUser()) {
            return result;
        }

        String viewId = querySession.getViewId();
        if (BIUtil.isEmpty(viewId)) {
            return new QueryInterceptResult(true, BIConsts.SSM_ERROR_VIEW_LIMIT, BIException.CODE_WARN);
        }

        Set<String> whiteListViewIds = getWhiteListViewIds();
        if(BIUtil.isEmpty(whiteListViewIds)){
            return result;
        }
        if (!whiteListViewIds.contains(viewId)) {
            return new QueryInterceptResult(true, BIConsts.SSM_ERROR_VIEW_LIMIT, BIException.CODE_WARN);
        }

        return result;
    }

    private Set<String> getWhiteListViewIds() {
        String configValue = SC.v(VIEW_BLOCK_WHITE_LIST_CONFIG, "");
        return Arrays.stream(configValue.split(","))
                .map(String::trim)
                .filter(BIUtil::isNotEmpty)
                .collect(Collectors.toSet());
    }
}
