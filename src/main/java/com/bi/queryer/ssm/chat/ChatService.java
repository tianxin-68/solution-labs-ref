package com.bi.queryer.ssm.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.chat.entity.AgentPlatChatDataConfig;
import com.bi.queryer.ssm.chat.entity.AgentPlatChatDataTemplateViewConfig;
import com.bi.queryer.ssm.chat.req.ChatCheckSnapshotDataChangeReq;
import com.bi.queryer.ssm.chat.req.ChatCreateWithSnapshotReq;
import com.bi.queryer.ssm.chat.req.ChatDataRsp;
import com.bi.queryer.ssm.chat.req.ChatDataSnapshotReq;
import com.bi.queryer.ssm.chat.req.ChatSnapshotUpdateReq;
import com.bi.queryer.ssm.engine.config.ui.check.UICheckQueryConfigure;
import com.bi.queryer.ssm.enums.ChatBusinessType;
import com.bi.queryer.ssm.util.OssUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent 平台会话服务：创建会话、更新快照、检查快照变更，并落库快照配置与查询结果。
 */
@Service
@Slf4j
public class ChatService {

    @Autowired
    private BaseDao dao;

    /**
     * 创建会话并保存数据快照。
     * <p>
     * 1. 调用 Agent 服务创建会话，获取 chatId；
     * 2. 写入 agent_plat_chat_data_config；
     * 3. 上传 queryConfig 至 OSS 后批量写入 agent_plat_chat_data_template_view_config。
     *
     * @param req 创建会话及快照入参
     * @return 会话 ID
     */
    public String createWithSnapshot(ChatCreateWithSnapshotReq req) {
        validateCreateWithSnapshotReq(req);

        // Step 1：调用 Agent 接口创建会话
        String chatId = createChatSession(req);

        // Step 2：写入快照配置主表
        String chatDataId = Guid.id();
        AgentPlatChatDataConfig config = new AgentPlatChatDataConfig();
        config.setChatDataId(chatDataId);
        config.setChatId(chatId);
        config.setDataSnapshotType(req.getChatBusinessType());
        config.setDataSnapshotViewId(req.getDataSnapshotViewId());
        config.setIsLatest(Enabled.YES.getId());
        config.setCreatedBy(UserManager.get().getName());
        dao.insert("chat.insertDataConfig", config);

        // Step 3：上传 queryConfig 并批量写入快照结果表
        List<ChatDataSnapshotReq> snapshotReqList = req.getChatDataSnapshotReqList();
        if (CollUtil.isNotEmpty(snapshotReqList)) {
            List<AgentPlatChatDataTemplateViewConfig> dataResults = buildDataResults(snapshotReqList, chatDataId, chatId);
            dao.insert("chat.insertDataResults", dataResults);
        }

        return chatId;
    }

    /**
     * 更新会话数据快照：将旧配置标记为历史，插入新配置及快照结果。
     * <p>
     * 1. 将同一 chatId 下 is_latest=1 的记录置为 0；
     * 2. 插入新的 agent_plat_chat_data_config；
     * 3. 上传 queryConfig 至 OSS 后批量写入 agent_plat_chat_data_template_view_config。
     *
     * @param req 快照更新入参
     */
    public void snapshotUpdate(ChatSnapshotUpdateReq req) {
        validateSnapshotUpdateReq(req);

        String chatId = req.getChatId();
        String chatDataId = Guid.id();
        AgentPlatChatDataConfig config = new AgentPlatChatDataConfig();
        config.setChatDataId(chatDataId);
        config.setChatId(chatId);
        config.setDataSnapshotType(req.getChatBusinessType());
        config.setDataSnapshotViewId(req.getDataSnapshotViewId());
        config.setIsLatest(Enabled.YES.getId());
        config.setCreatedBy(UserManager.get().getName());

        List<ChatDataSnapshotReq> snapshotReqList = req.getChatDataSnapshotReqList();
        List<AgentPlatChatDataTemplateViewConfig> dataResults = CollUtil.isEmpty(snapshotReqList)
                ? new ArrayList<AgentPlatChatDataTemplateViewConfig>()
                : buildDataResults(snapshotReqList, chatDataId, chatId);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                Map<String, Object> param = new HashMap<>();
                param.put("chatId", chatId);
                dao.update("chat.updateDataConfigIsLatest", param);

                dao.insert("chat.insertDataConfig", config);

                if (CollUtil.isNotEmpty(dataResults)) {
                    dao.insert("chat.insertDataResults", dataResults);
                }
            }
        });

        // 事务提交后，通知 Agent 平台更新会话 Meta
        updateChatSessionMeta(req);
    }

    /**
     * 对比当前会话已落库快照与入参：单视图比 query 配置；看板按 widgetId 对齐比配置。
     *
     * @param req 快照变更检查入参
     * @return {@link Enabled#YES} 表示已变更，{@link Enabled#NO} 表示未变更
     */
    public Integer checkSnapshotDataChange(ChatCheckSnapshotDataChangeReq req) {
        validateCheckSnapshotDataChangeReq(req);

        Map<String, Object> param = new HashMap<>();
        param.put("chatId", req.getChatId());
        AgentPlatChatDataConfig config = (AgentPlatChatDataConfig) dao.queryObject("chat.selectLatestDataConfig", param);
        if (config == null) {
            return Enabled.YES.getId();
        }

        param.clear();
        param.put("chatDataId", config.getChatDataId());
        List<AgentPlatChatDataTemplateViewConfig> results = (List<AgentPlatChatDataTemplateViewConfig>) dao.queryObjectList(
                "chat.selectDataResultListByDataId", param);
        if (CollUtil.isEmpty(results)) {
            return Enabled.YES.getId();
        }

        ChatBusinessType chatBusinessType = ChatBusinessType.get(config.getDataSnapshotType());
        switch (chatBusinessType) {
            case QUERY_TEMPLATE:
                String dataQueryConfig = OssUtil.fetchOssUrlBody(results.get(0).getDataSnapshotQueryConfig());
                String normalizedConfig = normalizeDecryptTplConfigForCompare(req.getConfig());
                String compareConfig = normalizeDecryptTplConfigForCompare(dataQueryConfig);
                if (!normalizedConfig.equalsIgnoreCase(compareConfig)) {
                    return Enabled.YES.getId();
                }
                break;
            case ANALYSIS_TEMPLATE:
            case ANALYSIS_TMP_TEMPLATE:
                List<String> queryConfigUrls = new ArrayList<>();
                Map<String, String> widgetIdConfigMap = new HashMap<>();
                for (AgentPlatChatDataTemplateViewConfig result : results) {
                    queryConfigUrls.add(result.getDataSnapshotQueryConfig());
                    widgetIdConfigMap.put(result.getWidgetId(), result.getDataSnapshotQueryConfig());
                }

                Map<String, String> queryConfigMap = OssUtil.getDataByOssUrls(queryConfigUrls);

                Set<String> snapshotWidgetIds = new HashSet<>(widgetIdConfigMap.keySet());
                if (CollUtil.isEmpty(req.getWidgetConfigs())) {
                    if (!snapshotWidgetIds.isEmpty()) {
                        return Enabled.YES.getId();
                    }
                    break;
                }

                Set<String> requestWidgetIds = new HashSet<>();
                for (ChatCheckSnapshotDataChangeReq.WidgetConfig wc : req.getWidgetConfigs()) {
                    if (wc == null || StrUtil.isEmpty(wc.getWidgetId())) {
                        return Enabled.YES.getId();
                    }
                    String widgetId = wc.getWidgetId();
                    requestWidgetIds.add(widgetId);
                    if (!widgetIdConfigMap.containsKey(widgetId)) {
                        return Enabled.YES.getId();
                    }
                    String ossUrl = widgetIdConfigMap.get(widgetId);
                    String savedBody = queryConfigMap.get(ossUrl);

                    String compare1 = normalizeDecryptTplConfigForCompare(savedBody);
                    String compare2 = normalizeDecryptTplConfigForCompare(wc.getConfig());
                    if (!compare1.equalsIgnoreCase(compare2)) {
                        return Enabled.YES.getId();
                    }
                }
                if (!requestWidgetIds.equals(snapshotWidgetIds)) {
                    return Enabled.YES.getId();
                }
                break;
            default:
                break;
        }

        return Enabled.NO.getId();
    }

    /**
     * 通过 chatId 查询最新数据快照配置及结果列表。
     *
     * @param chatId 会话 ID
     * @return 最新快照配置与结果；无数据时返回 null
     */
    public ChatDataRsp getChatData(String chatId) {
        if (StrUtil.isBlank(chatId)) {
            throw new BIException("chatId不能为空");
        }

        Map<String, Object> param = new HashMap<>();
        param.put("chatId", chatId);
        AgentPlatChatDataConfig config =
                (AgentPlatChatDataConfig) dao.queryObject("chat.selectLatestDataConfig", param);
        if (config == null) {
            return null;
        }

        Map<String, Object> resultParam = new HashMap<>();
        resultParam.put("chatDataId", config.getChatDataId());
        List<AgentPlatChatDataTemplateViewConfig> results =
                dao.queryObjectList("chat.selectDataResultListByDataId", resultParam, AgentPlatChatDataTemplateViewConfig.class);

        ChatDataRsp rsp = new ChatDataRsp();
        rsp.setChatDataId(config.getChatDataId());
        rsp.setChatId(config.getChatId());
        rsp.setDataSnapshotType(config.getDataSnapshotType());
        rsp.setDataSnapshotViewId(config.getDataSnapshotViewId());
        rsp.setCreatedBy(config.getCreatedBy());
        rsp.setResults(CollUtil.isEmpty(results) ? Collections.emptyList() : results);
        return rsp;
    }

    /**
     * 通过 chatId 查询最新快照对应的 viewId。
     *
     * @param chatId 会话 ID
     * @return 最新快照 viewId；无数据时返回 null
     */
    public String getDataSnapshotViewId(String chatId) {
        if (StrUtil.isBlank(chatId)) {
            throw new BIException("chatId不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("chatId", chatId);
        AgentPlatChatDataConfig config =
                (AgentPlatChatDataConfig) dao.queryObject("chat.selectLatestDataConfig", param);
        return config == null ? null : config.getDataSnapshotViewId();
    }

    private void validateCheckSnapshotDataChangeReq(ChatCheckSnapshotDataChangeReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (StrUtil.isBlank(req.getChatId())) {
            throw new IllegalArgumentException("chatId不能为空");
        }
    }

    /**
     * 解密模板配置后仅保留 filter、result、analysis 再序列化，用于快照是否变更的对比。
     * 非 JSON 或解析失败时退回明文，避免误判为「无变更」。
     */
    private String normalizeDecryptTplConfigForCompare(String config) {
        if (config == null) {
            return "";
        }
        String plain = SSDUtil.decryptTplConfig(config);
        if (StrUtil.isBlank(plain)) {
            return "";
        }
        try {
            UICheckQueryConfigure uiCheckQueryConfigure = JSONObject.parseObject(plain, UICheckQueryConfigure.class);
            return JSONObject.toJSONString(uiCheckQueryConfigure);
        } catch (Exception e) {
            log.debug("normalizeDecryptTplConfigForCompare parse skip, use raw plain text");
        }
        return plain;
    }

    private void validateSnapshotUpdateReq(ChatSnapshotUpdateReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (StrUtil.isBlank(req.getChatId())) {
            throw new IllegalArgumentException("chatId不能为空");
        }
    }

    private void validateCreateWithSnapshotReq(ChatCreateWithSnapshotReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (StrUtil.isBlank(req.getChatBusinessType())) {
            throw new IllegalArgumentException("chatBusinessType不能为空");
        }
    }

    /**
     * 将快照入参转为落库实体：queryConfig 上传 OSS 后写入 dataSnapshotQueryConfig。
     *
     * @param snapshotReqList 快照列表
     * @param chatDataId      快照配置主键
     * @param chatId          会话 ID
     * @return 待批量插入的快照结果列表
     */
    private List<AgentPlatChatDataTemplateViewConfig> buildDataResults(List<ChatDataSnapshotReq> snapshotReqList,
                                                           String chatDataId, String chatId) {
        List<AgentPlatChatDataTemplateViewConfig> dataResults = new ArrayList<>(snapshotReqList.size());
        for (ChatDataSnapshotReq item : snapshotReqList) {
            String queryConfig = StrUtil.nullToEmpty(item.getQueryConfig());
            String queryConfigOssUrl = OssUtil.upload(
                    queryConfig.getBytes(StandardCharsets.UTF_8), "", ".json", "bigdata/ssm/chat");

            AgentPlatChatDataTemplateViewConfig result = new AgentPlatChatDataTemplateViewConfig();
            result.setChatDataId(chatDataId);
            result.setChatId(chatId);
            result.setDataSnapshotId(item.getViewId());
            result.setDataSnapshotQueryConfig(queryConfigOssUrl);
            result.setWidgetId(item.getWidgetId());
            dataResults.add(result);
        }
        return dataResults;
    }

    /**
     * 调用 Agent 服务创建会话。
     * <p>
     * 请求字段按 Agent 约定：businessId 传 chatBusinessType，businessType 传 chatBusinessId。
     *
     * @param req 创建会话入参
     * @return 会话 ID
     * @throws BIException Agent 接口响应异常或 chatId 为空时抛出
     */
    private String createChatSession(ChatCreateWithSnapshotReq req) {
        String url = SC.v("ssm.agent.base.url") + "/ai/chat/session/create";

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("platform", "ssm");
        reqMap.put("businessId", req.getChatBusinessId());
        reqMap.put("businessType", req.getChatBusinessType());

        User user = UserManager.get();
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

        String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 30000);
        if (StrUtil.isBlank(response)) {
            throw new BIException("调用Agent创建会话异常，响应为空");
        }

        JSONObject responseData = JSONObject.parseObject(response);
        if (responseData == null) {
            throw new BIException("调用Agent创建会话异常，响应解析失败");
        }
        if (!responseData.getBooleanValue("success")) {
            throw new BIException("调用Agent创建会话失败，" + responseData.getString("message"));
        }

        String chatId = responseData.getString("data");
        if (StrUtil.isBlank(chatId)) {
            throw new BIException("调用Agent创建会话失败，chatId为空");
        }
        return chatId;
    }

    /**
     * 调用 Agent 服务更新会话 Meta。
     *
     * @param req 快照更新入参
     * @throws BIException Agent 接口响应异常时抛出
     */
    private void updateChatSessionMeta(ChatSnapshotUpdateReq req) {
        String url = SC.v("ssm.agent.base.url") + "/ai/chat/session/meta/update";

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("chatId",req.getChatId());
        reqMap.put("platform", "ssm");
        reqMap.put("businessId", req.getChatBusinessId());
        reqMap.put("businessType", req.getChatBusinessType());

        User user = UserManager.get();
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

        String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 30000);
        if (StrUtil.isBlank(response)) {
            throw new BIException("调用Agent更新会话Meta异常，响应为空");
        }
        JSONObject responseData = JSONObject.parseObject(response);
        if (responseData == null) {
            throw new BIException("调用Agent更新会话Meta异常，响应解析失败");
        }
        if (!responseData.getBooleanValue("success")) {
            throw new BIException("调用Agent更新会话Meta失败，" + responseData.getString("message"));
        }
    }

}
