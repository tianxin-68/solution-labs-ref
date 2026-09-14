package com.bi.queryer.ssm.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.validator.QueryMessageType;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.inspection.query.template.InspectionService;
import com.bi.queryer.ssm.query.log.SSDQueryLogEntity;
import com.bi.queryer.ssm.query.template.TemplateLinkService;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.tx.cache.redis.RedisCacheClient;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author contributor
 * @Date 19:54 2025/7/2
 * @Description 服务监控：服务器健康监控、异常查询监控等
 **/
public abstract class ServiceMonitor {
    /**
     * check服务器健康
     * 暂时通过系统选项手工配置控制服务器是否可用
     * @return
     */
    public static ResponseMessage monitorServerHealth(){
        ResponseMessage result = new ResponseMessage();
        // 系统是否已挂起
        boolean isMounting = "true".equalsIgnoreCase(SC.v("system.mounting", "false"));
        if(isMounting){
            result.set(false, SC.v("system.mounting.tip", "当前系统负载过高，正在解决中，请稍后再试。紧急情况，企微联系【数据产品技术支持】"));
        }
        return result;
    }

    /**
     * 监控查询
     * 监控异常，只处理关键用户
     */
    public static void monitorQuery(SSDException modelCheckException){
        monitorKeyUserQuery(modelCheckException);
    }

    /**
     * 监控查询
     */
    public static void monitorQuery(SSDQueryLogEntity log, QueryMessageType msgType) {
        monitorContinuousFailQuery(log, msgType);
        monitorKeyUserQuery(log);

        //cm当日查询纳入巡检
        if (isKeyUser(log.getUserName())) {
            InspectionService inspectionService = (InspectionService) SpringContextUtil.getBean("inspectionService");
            inspectionService.insertCurrentQueryView(log.getViewId());
        }
    }

    /**
     * 监控查询持续失败
     * 告警渠道：
     * 1、机器人
     * 2、电话
     */
    public static void monitorContinuousFailQuery(SSDQueryLogEntity log,QueryMessageType msgType) {
        if (Enabled.isTrue(log.getSuccess())) {
            return;
        }

        //命中了熔断规则，不计入持续失败，避免频繁告警
        //20260416 持续失败告警排除olap_api限流场景
        if (QueryMessageType.ERROR_BLOCK == msgType || QueryMessageType.ERROR_OLAP_API_RATE_LIMIT == msgType) {
            return;
        }

        Integer maxCount = Integer.valueOf(SC.v("ssm.monitor.continuous.fail.max.count", "15"));
        Integer minuteInterval = Integer.valueOf(SC.v("ssm.monitor.continuous.fail.minute.interval", "3"));
        if (maxCount <= 0 || minuteInterval <= 0) {
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        String monitorRuleId = SC.v("ssm.monitor.continuous.fail.rule.id", "ceba3a7e962d483bb2ef6f8ca0061014");
        executorService.execute(() -> {
            String keyPrefix = "monitorContinuousFailQuery@";
            try {
                RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
                Set<String> keys = RedisCacheManager.hscanKeys(keyPrefix);
                if (BIUtil.isNotEmpty(keys) && keys.size() > maxCount) {
                    String msg = String.format("多维分析%s分钟内查询失败次数超过%s次", minuteInterval, maxCount);
                    alarmByDataStudioApi(monitorRuleId, msg);

                    //触发告警后，清理缓存。避免重复告警
                    List<String> deleteKeyList = new ArrayList<>();
                    for (String key : keys) {
                        if (StrUtil.isEmpty(key)) {
                            continue;
                        }
                        deleteKeyList.add(key);
                    }

                    if (CollUtil.isNotEmpty(deleteKeyList)) {

                        List<List<String>> splitDeleteKeys = BIUtil.splitList(deleteKeyList, 100);
                        for (List<String> groupDeleteKeyList : splitDeleteKeys) {
                            if (CollUtil.isEmpty(groupDeleteKeyList)) {
                                continue;
                            }
                            Set<String> setKeys = new HashSet<>(groupDeleteKeyList);
                            redisCacheClient.hdel(keyPrefix, setKeys.toArray(new String[setKeys.size()]));
                        }

                    }
                }
                // 添加当前查询信息到redis中
                redisCacheClient.hset(keyPrefix, log.getId(), log.getId(), minuteInterval * 60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        executorService.shutdown();
    }

    public static void monitorKeyUserQuery(SSDQueryLogEntity log){
        if(!isKeyUser(log.getUserName()) || Enabled.isTrue(log.getSuccess())){
            return;
        }
        List<String> msgList = new ArrayList<>();
        msgList.add("用户：" + log.getUserName());
        msgList.add("报错信息：" + log.getInfo());
        msgList.add("模板ID：" + log.getTemplateId());
        msgList.add("视图ID：" + log.getViewId());
        msgList.add("查询来源：" + log.getQuerySource());
        msgList.add("查询环境：" + log.getEnv());
        msgList.add("数据源：" + log.getDsKey());

        String monitorRuleId = getMonitorKeyUserRuleId();
        String msg = BIUtil.listToStr(msgList, "\n");
        alarmByDataStudioApi(monitorRuleId, msg);
    }

    /**
     * 监控关键人员查询
     * 监控内容：
     * 1、查询失败
     * 告警渠道：
     * 1、机器人
     */
    public static void monitorKeyUserQuery(SSDException exception){
        User user = UserManager.get();
        if(user == null){
            return;
        }
        boolean isKeyUser = isKeyUser(user.getName());
        if(!isKeyUser) {
            return;
        }

        String monitorRuleId = getMonitorKeyUserRuleId();
        String msg = String.format("用户：%s\n错误信息：%s", user.getName(), exception.getMessage());
        alarmByDataStudioApi(monitorRuleId, msg);
    }

    protected static String getMonitorKeyUserRuleId(){
        String monitorRuleId = SC.v("ssm.monitor.key.user.query.rule.id", "8b2b22828834461489a5ab86ee0ae7a5");
        return monitorRuleId;
    }

    protected static boolean isKeyUser(String currentUserName){
        if(BIUtil.isEmpty(currentUserName)){
            return false;
        }
        String keyUsers = SC.v("ssm.monitor.key.user.list", "chenmin");
        if(BIUtil.isEmpty(keyUsers)){
            return false;
        }
        List<String> keyUserList = Arrays.asList(keyUsers.split(","));
        return keyUserList.contains(currentUserName);
    }

    /**
     * 通过调用datastudio api触发告警
     * @param monitorRuleId
     * @param msg
     * @return
     */
    protected static void alarmByDataStudioApi(String monitorRuleId, String msg){
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(()->{
            String monitorServerBaseUrl = SC.v("datastudio.server.monitor.base.url", "http://datastudio.example.com:9010");
            String url = String.format("%s/monitor/monitorRule/executeMonitorRuleByMap?monitorRuleId=%s", monitorServerBaseUrl, monitorRuleId);
            // 请求头
            Map<String, String> headers = new HashMap<>();
            // ds_api
            String token = HotUtil.getHotTableAuthorityToken(); // 和热化表共用一个token
            headers.put("u_token", token);
            Map<String, Object> parameters = new HashMap<>();
            String finalMsg = "";
            if(msg != null){
                finalMsg = msg.replace("'", ""); // 剔除单元
            }
            parameters.put("msg", finalMsg);
            String response = HttpUtil.doPost(url, parameters, "application/json", headers, 60 * 1000);
            JSONObject responseJson = new JSONObject();
            try {
                responseJson = JSONObject.parseObject(response);
            }catch (Exception e){
                e.printStackTrace();
                responseJson.put("success", "false");
                responseJson.put("message", e.getMessage());
            }
        });
        executorService.shutdown();
    }
}
