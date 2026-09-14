package com.bi.queryer.ssm.api;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.entity.AgentUserViewEntity;
import com.bi.queryer.ssm.api.vo.req.QueryOlapByConfigReq;
import com.bi.queryer.ssm.api.vo.req.QueryOlapDataReq;
import com.bi.queryer.ssm.api.vo.rsp.OlapDatasetAndMetadataData;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.api.vo.req.QueryViewConfigPortraitReq;
import com.bi.queryer.ssm.migrate.bizsplit.service.BizSplitNotificationService;
import com.bi.queryer.ssm.query.template.view.TemplateViewService;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewConfigPortraitRsp;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多维对外提供的api
 */
@Controller
@Scope("prototype")
@RequestMapping("/olap/api")
@Slf4j
public class OlapApiController {

    private static final String OLAP_QUERY_SUCCESS_MESSAGE = "查询多维数据成功";

    @Autowired
    private OlapApiService olapApiService;

    @Autowired
    private BizSplitNotificationService bizSplitNotificationService;

    /**
     * 通过查询模版视图id获取视图查询数据
     * @param queryOlapDataReq
     * @return
     */
    @RequestMapping(value = "query/dataset", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<String> queryOlapData(@RequestBody QueryOlapDataReq queryOlapDataReq, HttpServletRequest request,
            @RequestHeader(value = "hostname", required = false) String hostname,
            @RequestHeader(value = "accessToken", required = false) String accessToken) {
        String olapApiKey = request.getHeader(BIConsts.OLAP_API_KEY);
        String olapApiUserName = request.getHeader(BIConsts.OLAP_API_USER_NAME);
        String result = olapApiService.queryOlapData(queryOlapDataReq, olapApiKey, olapApiUserName, hostname, accessToken, request);
        String message = resolveOlapResponseMessage(queryOlapDataReq);
        return SSMResponseMessage.success(message, result);
    }

    /** genAI_feature/olap_api_v2_start */
    /**
     * 查询多维数据集 CSV + 列元数据（白皮书）及小计/同环比口径说明
     */
    @RequestMapping(value = "query/datasetAndMetadata", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<OlapDatasetAndMetadataData> queryOlapDataWithMetadata(@RequestBody QueryOlapDataReq queryOlapDataReq, HttpServletRequest request,
            @RequestHeader(value = "hostname", required = false) String hostname,
            @RequestHeader(value = "accessToken", required = false) String accessToken) {
        String olapApiKey = request.getHeader(BIConsts.OLAP_API_KEY);
        String olapApiUserName = request.getHeader(BIConsts.OLAP_API_USER_NAME);
        OlapDatasetAndMetadataData data = olapApiService.queryOlapDataWithMetadata(queryOlapDataReq, olapApiKey, olapApiUserName, hostname, accessToken, request);
        applyBizSplitReminderToRemark(data, queryOlapDataReq);
        return SSMResponseMessage.success(OLAP_QUERY_SUCCESS_MESSAGE, data);
    }
    /** genAI_feature/olap_api_v2_end */

    /**
     * 传入完整 UI 查询 config，返回 ResultDataSet
     */
    @RequestMapping(value = "query/dataset/byConfig", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<ResultDataSet> queryOlapDataByConfig(@RequestBody QueryOlapByConfigReq req) throws IOException {
        ResultDataSet dataSet = olapApiService.queryOlapDataByConfig(req);
        return SSMResponseMessage.success("查询多维数据成功", dataSet);
    }

    /**
     * 查询当前用户在 Agent 平台可见的视图列表，按 sort_order 升序排列
     */
    @RequestMapping(value = "agent/user/views", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<List<AgentUserViewEntity>> queryAgentUserViews(HttpServletRequest request) {
        String userName = resolveUserName(request.getHeader(BIConsts.OLAP_API_USER_NAME));
        System.out.println("agent/user/views入参:" + userName);
        if (StrUtil.isBlank(userName)) {
            throw new BIException("请求头中" + BIConsts.OLAP_API_USER_NAME + "不能为空！");
        }
        List<AgentUserViewEntity> viewList = olapApiService.queryViewListByUserName(userName);
        return SSMResponseMessage.success("查询成功", viewList);
    }

    /**
     * 查询视图配置画像（字段列表、维度、指标、筛选项等元信息）
     */
    @RequestMapping(value = "view/configPortrait", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<TemplateViewConfigPortraitRsp> getViewConfigPortrait(@RequestBody QueryViewConfigPortraitReq req,
            HttpServletRequest request) {
        String olapApiKey = request.getHeader(BIConsts.OLAP_API_KEY);
        String olapApiUserName = request.getHeader(BIConsts.OLAP_API_USER_NAME);
        String viewId = req.getViewId();
        String userName = olapApiService.validateViewConfigPortrait(olapApiKey, olapApiUserName, viewId);
        TemplateViewConfigPortraitRsp rsp = olapApiService.getViewConfigPortrait(viewId, userName);
        return SSMResponseMessage.success("查询成功", rsp);
    }

    /**
     * 根据请求头中的用户名获取其绑定的 olap api_key，需校验 hostname/accessToken 与用户的绑定关系
     */
    @RequestMapping(value = "user/apikey", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<String> getUserApiKey(HttpServletRequest request,
            @RequestHeader(value = "hostname", required = false) String hostname,
            @RequestHeader(value = "accessToken", required = false) String accessToken) {
        String userName = resolveUserName(request.getHeader(BIConsts.OLAP_API_USER_NAME));
        if (StrUtil.isBlank(userName)) {
            throw new BIException("请求头中" + BIConsts.OLAP_API_USER_NAME + "不能为空！");
        }
        String apiKey = olapApiService.getUserApiKey(userName, hostname, accessToken);
        return SSMResponseMessage.success("查询成功", apiKey);
    }

    /**
     * 手动触发指定视图列表的 ssm_user_agent_view_config 同步
     */
    @RequestMapping(value = "agent/view/config/sync", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<Void> syncAgentViewConfig(@RequestBody List<String> viewIdList) {
        if (viewIdList == null || viewIdList.isEmpty()) {
            throw new BIException("viewIdList 不能为空！");
        }
        for (String viewId : viewIdList) {
            if (StrUtil.isBlank(viewId)) {
                continue;
            }
            olapApiService.syncViewConfig(viewId);
        }
        return SSMResponseMessage.success("同步成功", null);
    }

    private String resolveUserName(String userName) {
        return BIUtil.resolveUserName(userName);
    }

    /**
     * 查询成功后检查业务线拆分提醒；命中时将响应 message 替换为提醒文案。
     */
    private String resolveOlapResponseMessage(QueryOlapDataReq queryOlapDataReq) {
        if (queryOlapDataReq == null) {
            return OLAP_QUERY_SUCCESS_MESSAGE;
        }
        return bizSplitNotificationService.resolveResponseMessage(
                OLAP_QUERY_SUCCESS_MESSAGE,
                queryOlapDataReq.getTemplateViewId(),
                queryOlapDataReq.getUserName());
    }

    /**
     * datasetAndMetadata 接口命中拆分提醒时写入 data.remark，不修改响应 message。
     */
    private void applyBizSplitReminderToRemark(OlapDatasetAndMetadataData data, QueryOlapDataReq queryOlapDataReq) {
        if (data == null || queryOlapDataReq == null) {
            return;
        }
        Map<String, Object> remark = bizSplitNotificationService.applyReminderToRemark(
                data.getRemark(),
                queryOlapDataReq.getTemplateViewId(),
                queryOlapDataReq.getUserName());
        data.setRemark(remark);
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能是: client, proxy1, proxy2
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    @RequestMapping(value = "caller", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> caller(HttpServletRequest request) throws Exception {
        String ip = getClientIp(request);
        String hostname = InetAddress.getByName(ip).getHostName();

        Map<String, String> result = new HashMap<>();
        result.put("ip", ip);
        result.put("hostname", hostname);
        return result;
    }
}
