package com.bi.queryer.ssm.engine.interceptor;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.interceptor.rateLimit.RateLimitQueryTemplateView;
import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.ssm.engine.session.QuerySessionManager;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:28 2025/12/5
 * @Description TODO
 **/
@Component
@Order(2)
public class QueryRateLimitInterceptor extends QueryInterceptor{

    /**
     * 需要限流查询模板列表缓存
     */
    private static final List<RateLimitQueryTemplateView> templateViewsCache = new ArrayList<>();

    @Override
    public void refreshConfigureCache() {
        templateViewsCache.clear();
        BaseDao dao = DBUtil.getBaseDao();
        List<RateLimitQueryTemplateView> templateList = dao.queryObjectList("ssm.session.getAllRateLimitQueryTemplateView", null, RateLimitQueryTemplateView.class);
        templateViewsCache.addAll(templateList);
    }

    @Override
    public QueryInterceptResult intercept(QuerySession querySession, QueryConfigure queryConfigure) {
        QueryInterceptResult limitMessage = new QueryInterceptResult();
        if(!"true".equalsIgnoreCase(SC.v("ssm.query.rate.limit.enable", "true"))){
            return limitMessage;
        }
        // 是否超过系统整体session数
        if(isOverSystemMaxSession()) {
            limitMessage = new QueryInterceptResult(true, BIConsts.SSM_ERROR_RATE_LIMIT + "，请减少查询的维度、指标、时间范围后重试！", BIException.CODE_WARN);
            return limitMessage;
        }

        // 当前模板视图是否超过设置的session数
        // 初始化参数，便于获取模板id和视图id
        if(isOverTemplateViewMaxSession(querySession.getTemplateId(), querySession.getViewId())) {
            limitMessage = new QueryInterceptResult(true, BIConsts.SSM_ERROR_RATE_LIMIT + "(模板视图)，请减少查询的维度、指标、时间范围后重试！", BIException.CODE_WARN);
            return limitMessage;
        }

        return limitMessage;
    }

    // 判断当前session数是否已超过设置阈值
    public boolean isOverSystemMaxSession(){
        List<QuerySession> sessionList = QuerySessionManager.getAll();
        Integer maxSessionCount = Integer.valueOf(SC.v("ssm.query.session.max", "100"));
        return sessionList.size() >= maxSessionCount;
    }

    /**
     * 是否超过了模板的查询限制
     * @return
     */
    public boolean isOverTemplateViewMaxSession(String templateId, String viewId){
        try {
            if(BIUtil.isEmpty(templateId)){
                return false;
            }

            // 获取当前所有活跃的查询会话
            List<QuerySession> currentSessions = QuerySessionManager.getAll();

            // 若视图id=all，则表示当前模板的所有视图都受限
            String allViewIdFlag = "all";
            // 检查是否超出配置的最大会话数
            for (RateLimitQueryTemplateView limitTemplateView : templateViewsCache) {
                if (!templateId.equals(limitTemplateView.getTemplateId())) {
                    continue;
                }
                // 检查时间范围是否有效
                if (!isInTimeRange(limitTemplateView)) {
                    continue; // 不在时间范围内，跳过此模板
                }

                // 统计匹配模板ID和视图ID的会话数
                int currentTemplateSessions = 0;
                int currentValueSessions = 0;
                for (QuerySession session : currentSessions) {
                    if (templateId.equals(session.getTemplateId())) {
                        currentTemplateSessions++;
                        // 如果 viewId 为 all 或者与会话中的 viewId 匹配，则计入统计
                        if (allViewIdFlag.equalsIgnoreCase(viewId) || viewId.equals(session.getViewId())) {
                            currentValueSessions++;
                        }
                    }
                }
                // 模板配置的是限制所有视图，即只限制模板粒度
                if (allViewIdFlag.equalsIgnoreCase(limitTemplateView.getViewId())) {
                    return currentTemplateSessions >= limitTemplateView.getMaxSession();
                }
                if (BIUtil.isNotEmpty(viewId) && viewId.equalsIgnoreCase(limitTemplateView.getViewId())) {
                    return currentValueSessions >= limitTemplateView.getMaxSession();
                }
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return true;
        }
    }

    /**
     * 判断当前时间是否在模板限制的时间范围内
     * @param template 限流查询模板
     * @return 是否在时间范围内
     */
    private boolean isInTimeRange(RateLimitQueryTemplateView template) {
        String startTimeStr = template.getStartTime(); // 注意：字段名为starTime而非startTime
        String endTimeStr = template.getEndTime();

        // 如果没有设置时间限制，则默认在时间范围内
        if (BIUtil.isEmpty(startTimeStr) || BIUtil.isEmpty(endTimeStr)) {
            return true;
        }

        try {
            // 解析时间字符串为时间戳进行比较
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long startTime = sdf.parse(startTimeStr).getTime();
            long endTime = sdf.parse(endTimeStr).getTime();
            long currentTime = System.currentTimeMillis();

            // 判断当前时间是否在起止时间范围内
            return currentTime >= startTime && currentTime <= endTime;
        } catch (Exception e) {
            // 时间格式解析异常，默认在时间范围内
            return true;
        }
    }
}
