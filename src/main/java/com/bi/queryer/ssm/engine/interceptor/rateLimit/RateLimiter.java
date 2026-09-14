package com.bi.queryer.ssm.engine.interceptor.rateLimit;

import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.ssm.engine.session.QuerySessionManager;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:57 2025/11/28
 * @Description 限流器
 **/
public class RateLimiter {
    /**
     * 需要限流查询模板列表缓存
     */
    private static final List<RateLimitQueryTemplateView> templateViews = new ArrayList<>();

    public static void refresh(){
        templateViews.clear();
        BaseDao dao = DBUtil.getBaseDao();
        List<RateLimitQueryTemplateView> templateList = dao.queryObjectList("ssm.session.getAllRateLimitQueryTemplateView", null, RateLimitQueryTemplateView.class);
        templateViews.addAll(templateList);
    }

    public static ResponseMessage limit(BaseController controller, Class<?> clazz, MethodSignature ms){
        ResponseMessage limitMessage = new ResponseMessage();
        EnableRateLimit enableRateLimit = ms.getMethod().getAnnotation(EnableRateLimit.class);
        if(enableRateLimit == null){
            return limitMessage;
        }
        User user = UserManager.get();
        // 白名单
        if(user == null || isWhiteListUser(user)){
            return limitMessage;
        }
        // 是否超过系统整体session数
        if(isOverSystemMaxSession()) {
            limitMessage = new ResponseMessage(false, "系统负载过高，请减少查询的维度、指标、时间范围后重试！");
            limitMessage.setCode("warn");
            return limitMessage;
        }

        // 当前模板视图是否超过设置的session数
        // 初始化参数，便于获取模板id和视图id
        String templateId = controller.stringValue("id");
        String viewId = controller.stringValue("viewId");
        if(isOverTemplateViewMaxSession(templateId, viewId)) {
            limitMessage = new ResponseMessage(false, "当前模板视图查询负载过高，请减少查询的维度、指标、时间范围后重试！");
            limitMessage.setCode("warn");
            return limitMessage;
        }

        return limitMessage;
    }

    public static ResponseMessage limit(QuerySession querySession){
        ResponseMessage limitMessage = new ResponseMessage();
        if(!"true".equalsIgnoreCase(SC.v("ssm.query.rate.limit.enable", "true"))){
            return limitMessage;
        }
        User user = UserManager.get();
        // 白名单
        if(user == null || isWhiteListUser(user)){
            return limitMessage;
        }
        // 是否超过系统整体session数
        if(isOverSystemMaxSession()) {
            limitMessage = new ResponseMessage(false, BIConsts.SSM_ERROR_RATE_LIMIT + "，请减少查询的维度、指标、时间范围后重试！");
            limitMessage.setCode("warn");
            return limitMessage;
        }

        // 当前模板视图是否超过设置的session数
        // 初始化参数，便于获取模板id和视图id
        if(isOverTemplateViewMaxSession(querySession.getTemplateId(), querySession.getViewId())) {
            limitMessage = new ResponseMessage(false, BIConsts.SSM_ERROR_RATE_LIMIT + "(模板视图)，请减少查询的维度、指标、时间范围后重试！");
            limitMessage.setCode("warn");
            return limitMessage;
        }

        return limitMessage;
    }

    // 判断当前session数是否已超过设置阈值
    public static boolean isOverSystemMaxSession(){
        List<QuerySession> sessionList = QuerySessionManager.getAll();
        Integer maxSessionCount = Integer.valueOf(SC.v("ssm.query.session.max", "100"));
        return sessionList.size() >= maxSessionCount;
    }

    // 判断是否是白名单 用户
    private static boolean isWhiteListUser(User user){
        // 不限流的白名单用户列表
        String[] whiteListUsers = SC.v("ssm.key.user.list", "chenmin").split(",");
        for (String whiteListUser : whiteListUsers) {
            if(whiteListUser.equals(user.getName())){
                return true;
            }
        }
        return false;
    }

    /**
     * 是否超过了模板的查询限制
     * @return
     */
    public static boolean isOverTemplateViewMaxSession(String templateId, String viewId){
        try {
            if(BIUtil.isEmpty(templateId)){
                return false;
            }

            // 获取当前所有活跃的查询会话
            List<QuerySession> currentSessions = QuerySessionManager.getAll();

            // 若视图id=all，则表示当前模板的所有视图都受限
            String allViewIdFlag = "all";
            // 检查是否超出配置的最大会话数
            for (RateLimitQueryTemplateView limitTemplateView : templateViews) {
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
    private static boolean isInTimeRange(RateLimitQueryTemplateView template) {
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
