package com.bi.queryer.ssm.engine.interceptor;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 11:35 2025/12/9
 * @Description 查询拦截器管理类
 **/
public abstract class QueryInterceptorManager {
    /**
     * 获取所有查询拦截器
     */
    public static List<QueryInterceptor> getInterceptors(){
        Map<String, QueryInterceptor> map = SpringContextUtil.getContext().getBeansOfType(QueryInterceptor.class);
        List<QueryInterceptor> interceptors = new ArrayList<>(map.values());
        AnnotationAwareOrderComparator.sort(interceptors);
        return interceptors;
    }

    /**
     * 刷新缓存
     */
    public static void refreshConfigureCache() {
        List<QueryInterceptor> interceptors = getInterceptors();
        if(BIUtil.isEmpty(interceptors)){
            return ;
        }
        for(QueryInterceptor interceptor : interceptors){
            interceptor.refreshConfigureCache();
        }
    }

    public static QueryInterceptResult intercept(QuerySession querySession, QueryConfigure queryConfigure) {
        QueryInterceptResult result = new QueryInterceptResult();
        List<QueryInterceptor> interceptors = getInterceptors();
        if(BIUtil.isEmpty(interceptors)){
            return result;
        }
        // 白名单
        if(isWhiteListUser(querySession.getCreatedBy())){
            return result;
        }
        try {
            for (QueryInterceptor interceptor : interceptors) {
                result = interceptor.intercept(querySession, queryConfigure);
                if (result.intercepted) {
                    return result;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    // 判断是否是白名单 用户
    private static boolean isWhiteListUser(String userName){
        if(BIUtil.isEmpty(userName)){
            return true;
        }
        // 不限流的白名单用户列表
        String[] whiteListUsers = SC.v("ssm.key.user.list", "chenmin").split(",");
        for (String whiteListUser : whiteListUsers) {
            if(whiteListUser.equals(userName)){
                return true;
            }
        }
        return false;
    }
}
