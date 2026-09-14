package com.bi.queryer.ssm.llm.chatSession;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.OlapApiService;
import com.bi.queryer.ssm.api.vo.req.QueryOlapByConfigReq;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.check.UICheckQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.check.UICheckQueryResult;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.enums.ChatBusinessType;
import com.bi.queryer.ssm.enums.ChatMessageType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.SseEventType;
import com.bi.queryer.ssm.llm.chatSession.entity.*;
import com.bi.queryer.ssm.llm.chatSession.req.*;
import com.bi.queryer.ssm.llm.chatSession.resp.*;
import com.bi.queryer.ssm.portal.template.AnalysisTemplateService;
import com.bi.queryer.ssm.portal.template.AnalysisTplViewService;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplViewEntity;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplWidgetEntity;
import com.bi.queryer.ssm.portal.ai.vo.ChatMessageStatRow;
import com.bi.queryer.ssm.portal.template.vo.WidgetNodeVO;
import com.bi.queryer.ssm.query.template.view.TemplateViewService;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.ssm.util.AgentUtil;
import com.bi.queryer.ssm.util.CsvUtil;
import com.bi.queryer.ssm.util.OssUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatSessionService {

    static final int ACTIVE = 1;
    static final int INACTIVE = 0;
    static final double DEFAULT_SORT_ID = 9999D;
    static final int DEFAULT_PAGE_NO = 1;
    static final int DEFAULT_PAGE_SIZE = 99999;
    static final int MAX_PAGE_SIZE = 99999;
    static final int MAX_NAME_LENGTH = 500;
    static final int ANSWER_STATUS_NORMAL = 1;
    static final int ANSWER_STATUS_ERROR = 0;
    static final int NOT_SUMMARIZED = 0;
    static final int SUMMARIZED = 1;
    static final String DEFAULT_CHAT_NAME = "新会话";
    static final String MESSAGE_TYPE_QA = "QA";
    static final String MESSAGE_TYPE_SUMMARY = "SUMMARY";
    /**
     * 与需求约定：未传 is_summarized 时默认 0
     */
    static final int DEFAULT_IS_SUMMARIZED = 0;
    /**
     * 与需求约定：未传 chat_message_type 时默认 QA
     */
    static final String DEFAULT_CHAT_MESSAGE_TYPE = MESSAGE_TYPE_QA;
    /**
     * 与需求约定：未传 answer_status 时默认 1
     */
    static final int DEFAULT_ANSWER_STATUS = ANSWER_STATUS_NORMAL;
    /**
     * 非 OLAP 路径（isNeedQueryData=0）下，前端直传 {@link ChatDataSnapshotReq#getDataset()}。
     * 当行数超过本值时：全量 CSV 仍上传作为 data_content；再单独生成「仅前 N 行」的 CSV 上传，
     * 供 {@link CreateChatSessionSnapshotReq#setDataPreviewContent}（列表/预览拉取，避免超大预览）。
     */
    private static final int SNAPSHOT_DATASET_PREVIEW_MAX_ROWS = 1001;
    /**
     * 多条快照并行构建时的线程上限，避免 OLAP/OSS 同时打满连接池。
     */
    private static final int SNAPSHOT_BUILD_PARALLELISM = 5;

    @Autowired
    private BaseDao dao;

    @Autowired
    private OlapApiService olapApiService;

    @Autowired
    private TemplateViewService templateViewService;

    @Autowired
    private AnalysisTplViewService analysisTplViewService;

    @Autowired
    private AnalysisTemplateService analysisTemplateService;

    public ChatSessionResp createChatSession(final CreateChatSessionReq req) {
        validateCreateReq(req);
        final String userName = getCurrentUserName();
        final Date now = new Date();
        final ChatBase chatBase = buildChatBase(req, userName, now);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                chatBase.setSortId(getNextChatBaseSortId(chatBase.getChatBusinessId(), userName));
                dao.insert("chat.session.base.insertChatBase", chatBase);

                ChatUserConfig userConfig = buildChatUserConfig(req, userName, now);
                if (userConfig != null) {
                    dao.insert("chat.session.user.config.insertChatUserConfig", userConfig);
                }

                ChatDataConfig dataConfig = buildChatDataConfig(chatBase.getChatId(),
                        req.getChatBusinessType().trim(), req.getDataSnapshotId().trim(), req.getDataFilterText(), userName, now);
                dao.insert("chat.data.config.insertChatDataConfig", dataConfig);

                List<ChatDataResult> dataResults = buildChatDataResults(req.getDataSnapshots(), chatBase.getChatId(), dataConfig.getChatDataId(),
                        userName, now);
                batchInsertChatDataResults(dataResults);
            }
        });

        log.info("create chat session success, userName={}, chatId={}", userName, chatBase.getChatId());
        return buildChatSessionResp(chatBase, Boolean.FALSE);
    }

    public Long saveChatMessage(final SaveChatMessageReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        normalizeSaveChatMessageDefaults(req);
        validateSaveChatMessageReq(req);
        final String userName = getCurrentUserName();
        final ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);

        final Date now = new Date();
        final String messageType = req.getMessageType();
        final ChatMessage chatMessage = buildQaChatMessage(req, userName, now);
        final boolean shouldUpdateLastChatMessageId = MESSAGE_TYPE_QA.equals(messageType);
        final boolean shouldGenerateChatName = shouldUpdateLastChatMessageId && chatBase.getLastChatMessageId() == null;

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.insert("chat.message.insertChatMessage", chatMessage);

                syncLastUsedAiModel(userName, req.getAiModel(), now);

                if (shouldUpdateLastChatMessageId) {
                    updateChatBaseLastMessageId(req.getChatId(), chatMessage.getChatMessageId(), userName);

                    if (shouldGenerateChatName) {
                        String generatedChatName = generateChatName(chatBase, req);

                        if (StringUtils.isNotBlank(generatedChatName)) {
                            updateChatBaseName(req.getChatId(), generatedChatName, userName);
                        }
                    }
                }
            }
        });

        log.info("save chat message success, userName={}, chatId={}, chatMessageId={}, messageType={}",
                userName, req.getChatId(), chatMessage.getChatMessageId(), messageType);
        return chatMessage.getChatMessageId();
    }

    public Long saveChatSummarizedMessage(final SaveChatSummarizedMessageReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        final String userName = getCurrentUserName();
        final Date now = new Date();
        final ChatMessage chatMessage = buildSummaryChatMessage(req, userName, now);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.insert("chat.message.insertChatMessage", chatMessage);

                BatchSummarizeChatMessageReq batchSummarizeChatMessageReq = new BatchSummarizeChatMessageReq();
                batchSummarizeChatMessageReq.setChatMessageIds(req.getChatMessageIds());
                batchSummarizeChatMessages(batchSummarizeChatMessageReq);
            }
        });

        return chatMessage.getChatMessageId();
    }

    /**
     * 按会话查询全部对话记录，按 chat_message_id 升序。
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessageResp> listChatMessages(ChatMessageListReq req,boolean isAnswerContentFromOss) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());
        String userName = getCurrentUserName();
        checkChatSessionOwner(req.getChatId(), userName);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", req.getChatId());
        List<ChatMessage> rows =
                (List<ChatMessage>) dao.queryObjectList("chat.message.listChatMessageByChatId", params);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatMessageResp> result = new ArrayList<ChatMessageResp>(rows.size());
        for (ChatMessage row : rows) {
            result.add(toChatMessageResp(row));
        }

        //是否需要将oss文件读取出来
        if(isAnswerContentFromOss){
            fillAnswerContentFromOss(result);
        }

        return result;
    }

    public void deleteChatSession(DeleteChatSessionReq req) {
        validateChatId(req == null ? null : req.getChatId());
        String userName = getCurrentUserName();
        ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);
        if (isInactive(chatBase)) {
            log.info("delete chat session ignored for inactive chat, userName={}, chatId={}", userName, req.getChatId());
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", req.getChatId());
        params.put("createdBy", userName);
        params.put("updatedBy", userName);
        dao.update("chat.session.base.logicDeleteChatBase", params);

        Map<String, Object> favoriteParams = new HashMap<String, Object>();
        favoriteParams.put("chatId", String.valueOf(req.getChatId()));
        dao.delete("chat.session.favorite.deleteFavoriteByChatId", favoriteParams);

        log.info("delete chat session success, userName={}, chatId={}", userName, req.getChatId());
    }

    public void renameChatSession(RenameChatSessionReq req) {
        validateRenameReq(req);
        String userName = getCurrentUserName();
        ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);
        ensureActive(chatBase);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", req.getChatId());
        params.put("chatName", req.getChatName().trim());
        params.put("updatedBy", userName);
        dao.update("chat.session.base.renameChatBase", params);

        log.info("rename chat session success, userName={}, chatId={}", userName, req.getChatId());
    }

    public void favoriteChatSession(FavoriteChatSessionReq req) {
        validateChatId(req == null ? null : req.getChatId());
        String userName = getCurrentUserName();
        ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);
        ensureActive(chatBase);

        Map<String, Object> queryParams = buildFavoriteQueryParams(req.getChatId(), userName);
        ChatFavorite favorite = (ChatFavorite) dao.queryObject("chat.session.favorite.getFavoriteByChatIdAndUser", queryParams);
        if (favorite != null) {
            log.info("favorite chat session ignored because it already exists, userName={}, chatId={}", userName, req.getChatId());
            return;
        }

        Date now = new Date();
        ChatFavorite entity = new ChatFavorite();
        entity.setChatId(String.valueOf(req.getChatId()));
        entity.setFavSortId(DEFAULT_SORT_ID);
        entity.setCreatedBy(userName);
        entity.setCreatedTime(now);
        entity.setUpdatedBy(userName);
        entity.setUpdatedTime(now);
        dao.insert("chat.session.favorite.insertFavorite", entity);

        log.info("favorite chat session success, userName={}, chatId={}", userName, req.getChatId());
    }

    public void unfavoriteChatSession(FavoriteChatSessionReq req) {
        validateChatId(req == null ? null : req.getChatId());
        String userName = getCurrentUserName();
        ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);
        ensureActive(chatBase);

        dao.delete("chat.session.favorite.deleteFavoriteByChatIdAndUser", buildFavoriteQueryParams(req.getChatId(), userName));
        log.info("unfavorite chat session success, userName={}, chatId={}", userName, req.getChatId());
    }

    @SuppressWarnings("unchecked")
    public void batchSummarizeChatMessages(BatchSummarizeChatMessageReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        List<Long> messageIds = validateAndNormalizeChatMessageIds(req.getChatMessageIds());
        String userName = getCurrentUserName();

        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("messageIds", messageIds);
        List<ChatMessage> messages = (List<ChatMessage>) dao.queryObjectList("chat.message.listChatMessageByIds", queryParams);
        if (messages == null || messages.size() != messageIds.size()) {
            throw new IllegalArgumentException("存在不存在的chatMessageId");
        }

        validateChatMessageOwnership(messages, userName);
        validateSummarizableMessages(messages);

        Map<String, Object> updateParams = new HashMap<String, Object>();
        updateParams.put("isSummarized", Optional.ofNullable(req.getIsSummarized()).orElse(SUMMARIZED));
        updateParams.put("messageIds", messageIds);
        dao.update("chat.message.batchUpdateIsSummarizedByIds", updateParams);

        log.info("batch summarize chat messages success, userName={}, chatMessageIds={}", userName, messageIds);
    }

    public void bindChatSessionView(BindChatSessionViewReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());
        validateBoundViewId(req.getBoundViewId());

        String userName = getCurrentUserName();
        ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);
        ensureActive(chatBase);

        String boundViewId = req.getBoundViewId().trim();
        if (StringUtils.equals(boundViewId, chatBase.getBoundViewId())) {
            log.info("bind chat session view ignored because it already exists, userName={}, chatId={}, boundViewId={}",
                    userName, req.getChatId(), boundViewId);
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", req.getChatId());
        params.put("boundViewId", boundViewId);
        params.put("updatedBy", userName);
        dao.update("chat.session.base.bindChatBaseView", params);

        log.info("bind chat session view success, userName={}, chatId={}, boundViewId={}",
                userName, req.getChatId(), boundViewId);
    }

    public void unbindChatSessionView(UnbindChatSessionViewReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());

        String userName = getCurrentUserName();
        ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);
        ensureActive(chatBase);

        if (StringUtils.isBlank(chatBase.getBoundViewId())) {
            log.info("unbind chat session view ignored because no bound view, userName={}, chatId={}",
                    userName, req.getChatId());
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", req.getChatId());
        params.put("updatedBy", userName);
        dao.update("chat.session.base.unbindChatBaseView", params);

        log.info("unbind chat session view success, userName={}, chatId={}", userName, req.getChatId());
    }

    @SuppressWarnings("unchecked")
    public ChatSessionListResp listChatSessions(ChatSessionListQueryReq req) {
        String userName = getCurrentUserName();
        ChatSessionListQueryReq actualReq = req == null ? new ChatSessionListQueryReq() : req;
        int pageNo = normalizePageNo(actualReq.getPageNo());
        int pageSize = normalizePageSize(actualReq.getPageSize());

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userName", userName);
        params.put("favorite", actualReq.getFavorite());
        params.put("pageNo", pageNo);
        params.put("pageSize", pageSize);
        params.put("offset", (pageNo - 1) * pageSize);
        params.put("chatBusinessType", trimToNull(actualReq.getChatBusinessType()));
        params.put("chatBusinessId", trimToNull(actualReq.getChatBusinessId()));
        params.put("boundViewId", trimToNull(actualReq.getBoundViewId()));

        Integer total = dao.queryCount("chat.session.base.countChatBaseByUser", params);
        List<ChatSessionListItemResp> rows =
                (List<ChatSessionListItemResp>) dao.queryObjectList("chat.session.base.listChatBaseByUser", params);
        fillBoundViewName(rows);
        fillLastMessageContent(rows);
        fillMessageCount(rows);

        ChatSessionListResp resp = new ChatSessionListResp();
        resp.setTotal(total == null ? 0 : total);
        resp.setRows(rows == null ? Collections.<ChatSessionListItemResp>emptyList() : rows);
        return resp;
    }

    public ChatSessionContextResp getChatSessionContext(ChatSessionContextReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());
        String userName = getCurrentUserName();
        checkChatSessionOwner(req.getChatId(), userName);

        ChatSessionContextResp resp =
                (ChatSessionContextResp) dao.queryObject("chat.context.getChatContextByChatId", req.getChatId());
        if (resp == null) {
            resp = new ChatSessionContextResp();
            resp.setContextMaxByte(0D);
            resp.setContextUsageByte(0D);
        }
        return resp;
    }

    public void updateChatSessionContext(ChatSessionContextUpdateReq req) {
        validateChatSessionContextUpdateReq(req);
        String userName = getCurrentUserName();
        checkChatSessionOwner(req.getChatId(), userName);

        Date now = new Date();
        ChatContext chatContext = buildChatContext(req, userName, now);
        ChatSessionContextResp existing =
                (ChatSessionContextResp) dao.queryObject("chat.context.getChatContextByChatId", req.getChatId());
        if (existing == null) {
            dao.insert("chat.context.insertChatContext", chatContext);
            return;
        }
        dao.update("chat.context.updateChatContextByChatId", chatContext);
    }

    @SuppressWarnings("unchecked")
    public ChatSessionDetailResp getChatSessionDetail(ChatSessionDetailReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());
        String userName = getCurrentUserName();
        ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);

        ChatDataConfig dataConfig =
                (ChatDataConfig) dao.queryObject("chat.data.config.getChatDataConfigByChatId", req.getChatId());
        List<ChatDataResult> dataResults = Collections.emptyList();
        if (dataConfig != null && StringUtils.isNotBlank(dataConfig.getChatDataId())) {
            List<ChatDataResult> queried = (List<ChatDataResult>) dao.queryObjectList(
                    "chat.data.result.listChatDataResultByChatDataId", dataConfig.getChatDataId());
            if (queried != null) {
                dataResults = queried;
            }
        }

        return buildChatSessionDetailResp(chatBase, dataConfig, dataResults);
    }

    protected ChatBase checkChatSessionOwner(Long chatId, String userName) {
        ChatBase chatBase = (ChatBase) dao.queryObject("chat.session.base.getChatBaseById", chatId);
        if (chatBase == null) {
            throw new IllegalArgumentException("chatId对应会话不存在");
        }
        if (!StringUtils.equalsIgnoreCase(userName, chatBase.getCreatedBy())) {
            throw new SecurityException("无权操作该会话");
        }
        return chatBase;
    }

    protected void ensureActive(ChatBase chatBase) {
        if (isInactive(chatBase)) {
            throw new IllegalStateException("当前会话已失效");
        }
    }

    protected ChatSessionResp buildChatSessionResp(ChatBase entity, Boolean isFavorite) {
        ChatSessionResp resp = new ChatSessionResp();
        resp.setChatId(entity.getChatId());
        resp.setChatName(entity.getChatName());
        resp.setChatBusinessType(entity.getChatBusinessType());
        resp.setChatBusinessId(entity.getChatBusinessId());
        resp.setIsFavorite(isFavorite);
        resp.setCreatedTime(entity.getCreatedTime());
        resp.setUpdatedTime(entity.getUpdatedTime());
        return resp;
    }

    protected ChatSessionDetailResp buildChatSessionDetailResp(ChatBase chatBase, ChatDataConfig dataConfig,
                                                               List<ChatDataResult> dataResults) {
        ChatSessionDetailResp resp = new ChatSessionDetailResp();
        resp.setChatId(chatBase.getChatId());
        resp.setChatName(chatBase.getChatName());
        resp.setChatBusinessType(chatBase.getChatBusinessType());
        resp.setChatBusinessId(chatBase.getChatBusinessId());
        resp.setBoundViewId(chatBase.getBoundViewId());
        resp.setBoundViewName(queryBoundViewName(chatBase.getChatBusinessType(), chatBase.getBoundViewId()));
        resp.setLastChatMessageId(chatBase.getLastChatMessageId());
        resp.setLastChatMessageAiModel(getLastChatMessageAiModel(chatBase.getLastChatMessageId()));
        resp.setCreatedTime(chatBase.getCreatedTime());
        resp.setUpdatedTime(chatBase.getUpdatedTime());
        resp.setDataConfig(buildChatSessionDataConfigResp(dataConfig, dataResults));
        return resp;
    }

    protected String getLastChatMessageAiModel(Long lastChatMessageId) {
        if (lastChatMessageId == null) {
            return null;
        }
        ChatMessage lastChatMessage = (ChatMessage) dao.queryObject("chat.message.getChatMessageById", lastChatMessageId);
        if (lastChatMessage == null) {
            return null;
        }
        return lastChatMessage.getAiModel();
    }

    protected ChatSessionDataConfigResp buildChatSessionDataConfigResp(ChatDataConfig dataConfig,
                                                                       List<ChatDataResult> dataResults) {
        if (dataConfig == null) {
            return null;
        }
        ChatSessionDataConfigResp resp = new ChatSessionDataConfigResp();
        resp.setChatDataId(dataConfig.getChatDataId());
        resp.setChatId(dataConfig.getChatId());
        resp.setDataSnapshotType(dataConfig.getDataSnapshotType());
        resp.setSnapshotId(dataConfig.getDataSnapshotId());
        resp.setDataFilterText(dataConfig.getDataFilterText());
        resp.setCreatedTime(dataConfig.getCreatedTime());
        resp.setDataResults(buildChatSessionDataResultRespList(dataResults));
        return resp;
    }

    protected List<ChatSessionDataResultResp> buildChatSessionDataResultRespList(List<ChatDataResult> dataResults) {
        if (dataResults == null || dataResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatSessionDataResultResp> respList = new ArrayList<ChatSessionDataResultResp>(dataResults.size());
        for (ChatDataResult dataResult : dataResults) {
            ChatSessionDataResultResp resp = new ChatSessionDataResultResp();
            resp.setChatDataId(dataResult.getChatDataId());
            resp.setChatId(dataResult.getChatId());
            resp.setDataSnapshotId(dataResult.getDataSnapshotId());
            resp.setDataQueryViewId(dataResult.getDataQueryViewId());
            resp.setDataQueryConfig(dataResult.getDataQueryConfig());
            resp.setDataQueryRemark(dataResult.getDataQueryRemark());
            resp.setDataContent(dataResult.getDataContent());
            resp.setDataPreviewContent(dataResult.getDataPreviewContent());
            resp.setDataSize(dataResult.getDataSize());
            resp.setDataRowCount(dataResult.getDataRowCount());
            resp.setDataColumnCount(dataResult.getDataColumnCount());
            respList.add(resp);
        }
        return respList;
    }

    protected void batchInsertChatDataResults(List<ChatDataResult> dataResults) {
        if (dataResults == null || dataResults.isEmpty()) {
            return;
        }
        dao.insert("chat.data.result.insertChatDataResults", dataResults);
    }

    protected void validateCreateReq(CreateChatSessionReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (StringUtils.isBlank(req.getChatBusinessType())) {
            throw new IllegalArgumentException("chatBusinessType不能为空");
        }
        if (StringUtils.isBlank(req.getChatBusinessId())) {
            throw new IllegalArgumentException("chatBusinessId不能为空");
        }
        if (req.getDataSnapshots() == null || req.getDataSnapshots().isEmpty()) {
            throw new IllegalArgumentException("dataSnapshots不能为空");
        }
//
//        Set<String> snapshotIds = new HashSet<String>();
//        for (CreateChatSessionSnapshotReq snapshot : req.getDataSnapshots()) {
//            validateSnapshotReq(snapshot);
//            String snapshotId = snapshot.getDataSnapshotId().trim();
//            if (!snapshotIds.add(snapshotId)) {
//                throw new IllegalArgumentException("snapshotId不能重复");
//            }
//        }
        validateNameLength(req.getChatName());
    }

    protected void validateSnapshotUpdateReq(ChatSnapshotUpdateReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());
        validateSnapshotReqList(req.getDataSnapshots());
    }

    protected void validateSnapshotReqList(List<CreateChatSessionSnapshotReq> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalArgumentException("dataSnapshots must not be empty");
        }

//        Set<String> snapshotIds = new HashSet<String>();
//        for (CreateChatSessionSnapshotReq snapshot : snapshots) {
//            validateSnapshotReq(snapshot);
//            String snapshotId = snapshot.getDataSnapshotId().trim();
//            if (!snapshotIds.add(snapshotId)) {
//                throw new IllegalArgumentException("snapshotId must be unique");
//            }
//        }
    }

    protected void normalizeSaveChatMessageDefaults(SaveChatMessageReq req) {
        if (StringUtils.isBlank(req.getMessageType())) {
            req.setMessageType(DEFAULT_CHAT_MESSAGE_TYPE);
        } else {
            req.setMessageType(req.getMessageType().trim().toUpperCase());
        }
    }

    protected void validateSaveChatMessageReq(SaveChatMessageReq req) {
        validateChatId(req.getChatId());
        if (StringUtils.isBlank(req.getChatDataId())) {
            throw new IllegalArgumentException("chatDataId不能为空");
        }
        if (StringUtils.isBlank(req.getAiModel())) {
            throw new IllegalArgumentException("aiModel不能为空");
        }

        String messageType = req.getMessageType();
        if (!MESSAGE_TYPE_QA.equals(messageType) && !MESSAGE_TYPE_SUMMARY.equals(messageType)) {
            throw new IllegalArgumentException("messageType只支持QA或SUMMARY");
        }

        if (MESSAGE_TYPE_QA.equals(messageType)) {
            validateQaMessageReq(req);
        } else {
            validateSummaryMessageReq(req);
        }
    }

    protected void validateQaMessageReq(SaveChatMessageReq req) {
        if (StringUtils.isBlank(req.getQuestionContent())) {
            throw new IllegalArgumentException("questionContent不能为空");
        }
        /*if (StringUtils.isBlank(req.getAnswerContent())) {
            throw new IllegalArgumentException("answerContent不能为空");
        }*/
        if (req.getChatBeginTime() == null) {
            throw new IllegalArgumentException("chatBeginTime不能为空");
        }
        if (req.getChatEndTime() == null) {
            throw new IllegalArgumentException("chatEndTime不能为空");
        }
        if (req.getChatEndTime().before(req.getChatBeginTime())) {
            throw new IllegalArgumentException("chatEndTime不能早于chatBeginTime");
        }
        if (req.getAnswerDurationSeconds() != null && req.getAnswerDurationSeconds() < 0) {
            throw new IllegalArgumentException("answerDurationSeconds不能小于0");
        }
        if (req.getAnswerStatus() != null
                && req.getAnswerStatus() != ANSWER_STATUS_NORMAL
                && req.getAnswerStatus() != ANSWER_STATUS_ERROR) {
            throw new IllegalArgumentException("answerStatus只支持0或1");
        }
    }

    protected void validateSummaryMessageReq(SaveChatMessageReq req) {
        if (StringUtils.isBlank(req.getAnswerContent())) {
            throw new IllegalArgumentException("answerContent不能为空");
        }
        if (req.getSummarizedMsgIds() == null || req.getSummarizedMsgIds().isEmpty()) {
            throw new IllegalArgumentException("summarizedMsgIds不能为空");
        }
        if (req.getAnswerStatus() != null
                && req.getAnswerStatus() != ANSWER_STATUS_NORMAL
                && req.getAnswerStatus() != ANSWER_STATUS_ERROR) {
            throw new IllegalArgumentException("answerStatus只支持0或1");
        }
    }

    protected void validateRenameReq(RenameChatSessionReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());
        if (StringUtils.isBlank(req.getChatName())) {
            throw new IllegalArgumentException("chatName不能为空");
        }
        validateNameLength(req.getChatName());
    }

    protected void validateChatId(Long chatId) {
        if (chatId == null || chatId <= 0) {
            throw new IllegalArgumentException("chatId不能为空");
        }
    }

    protected void validateChatSessionContextUpdateReq(ChatSessionContextUpdateReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        validateChatId(req.getChatId());
        if (req.getContextMaxByte() == null) {
            throw new IllegalArgumentException("contextMaxByte不能为空");
        }
        if (req.getContextUsageByte() == null) {
            throw new IllegalArgumentException("contextUsageByte不能为空");
        }
        if (req.getContextMaxByte() < 0D) {
            throw new IllegalArgumentException("contextMaxByte不能小于0");
        }
        if (req.getContextUsageByte() < 0D) {
            throw new IllegalArgumentException("contextUsageByte不能小于0");
        }
        if (req.getContextUsageByte() > req.getContextMaxByte()) {
            throw new IllegalArgumentException("contextUsageByte不能大于contextMaxByte");
        }
    }

    protected void validateBoundViewId(String boundViewId) {
        if (StringUtils.isBlank(boundViewId)) {
            throw new IllegalArgumentException("boundViewId不能为空");
        }
    }

    protected void validateNameLength(String chatName) {
        if (StringUtils.isNotBlank(chatName) && chatName.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("chatName长度不能超过500");
        }
    }

    protected void validateSnapshotReq(CreateChatSessionSnapshotReq snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("dataSnapshots元素不能为空");
        }
        if (StringUtils.isBlank(snapshot.getDataSnapshotId())) {
            throw new IllegalArgumentException("snapshotId不能为空");
        }
        if (StringUtils.isBlank(snapshot.getDataQueryConfig())) {
            throw new IllegalArgumentException("dataQueryConfig不能为空");
        }
        if (StringUtils.isBlank(snapshot.getDataContent())) {
            throw new IllegalArgumentException("dataContent不能为空");
        }
        if (StringUtils.isBlank(snapshot.getDataPreviewContent())) {
            throw new IllegalArgumentException("dataPreviewContent不能为空");
        }
        if (snapshot.getDataSize() == null || snapshot.getDataSize() < 0L) {
            throw new IllegalArgumentException("dataSize不能为空且不能小于0");
        }
        if (snapshot.getDataRowCount() == null || snapshot.getDataRowCount() < 0) {
            throw new IllegalArgumentException("dataRowCount不能为空且不能小于0");
        }
        if (snapshot.getDataColumnCount() == null || snapshot.getDataColumnCount() < 0) {
            throw new IllegalArgumentException("dataColumnCount不能为空且不能小于0");
        }
    }

    protected List<Long> validateAndNormalizeSummarizedMsgIds(List<Long> summarizedMsgIds) {
        Set<Long> normalized = new LinkedHashSet<Long>();
        for (Long messageId : summarizedMsgIds) {
            if (messageId == null || messageId <= 0) {
                throw new IllegalArgumentException("summarizedMsgIds中存在非法messageId");
            }
            normalized.add(messageId);
        }
        return new ArrayList<Long>(normalized);
    }

    @SuppressWarnings("unchecked")
    protected void validateSummarizedMessages(Long chatId, List<Long> summarizedMsgIds) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("messageIds", summarizedMsgIds);
        List<ChatMessage> messages = (List<ChatMessage>) dao.queryObjectList("chat.message.listChatMessageByIds", params);
        if (messages == null || messages.size() != summarizedMsgIds.size()) {
            throw new IllegalArgumentException("存在不存在的摘要消息");
        }
        for (ChatMessage message : messages) {
            if (!chatId.equals(message.getChatId())) {
                throw new IllegalArgumentException("summarizedMsgIds中存在不属于当前会话的消息");
            }
            if (!MESSAGE_TYPE_QA.equalsIgnoreCase(message.getChatMessageType())) {
                throw new IllegalArgumentException("只有QA消息可以被摘要");
            }
        }
    }

    protected List<Long> validateAndNormalizeChatMessageIds(List<Long> chatMessageIds) {
        if (chatMessageIds == null || chatMessageIds.isEmpty()) {
            throw new IllegalArgumentException("chatMessageIds不能为空");
        }

        LinkedHashSet<Long> normalized = new LinkedHashSet<Long>();

        for (Long chatMessageId : chatMessageIds) {
            if (chatMessageId == null || chatMessageId <= 0) {
                throw new IllegalArgumentException("chatMessageIds中存在非法chatMessageId");
            }
            normalized.add(chatMessageId);
        }

        return new ArrayList<Long>(normalized);
    }

    protected void validateChatMessageOwnership(List<ChatMessage> messages, String userName) {
        Set<Long> checkedChatIds = new HashSet<Long>();
        for (ChatMessage message : messages) {
            if (message.getChatId() == null) {
                throw new IllegalArgumentException("chatMessageId对应会话不存在");
            }
            if (checkedChatIds.add(message.getChatId())) {
                checkChatSessionOwner(message.getChatId(), userName);
            }
        }
    }

    protected void validateSummarizableMessages(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (!MESSAGE_TYPE_QA.equalsIgnoreCase(message.getChatMessageType())) {
                throw new IllegalArgumentException("只有QA消息可以标记为已摘要");
            }
        }
    }

    protected String defaultChatName(String chatName) {
        if (StringUtils.isBlank(chatName)) {
            return DEFAULT_CHAT_NAME;
        }
        return chatName.trim();
    }

    protected String getCurrentUserName() {
        User user = UserManager.get();
        if (user == null || StringUtils.isBlank(user.getName())) {
            throw new IllegalStateException("当前用户未登录");
        }
        return user.getName();
    }

    protected ChatBase buildChatBase(CreateChatSessionReq req, String userName, Date now) {
        ChatBase chatBase = new ChatBase();
        chatBase.setChatName(defaultChatName(req.getChatName()));
        chatBase.setChatBusinessType(req.getChatBusinessType().trim());
        chatBase.setChatBusinessId(req.getChatBusinessId().trim());
        chatBase.setIsActive(ACTIVE);
        chatBase.setCreatedBy(userName);
        chatBase.setCreatedTime(now);
        chatBase.setUpdatedBy(userName);
        chatBase.setUpdatedTime(now);
        return chatBase;
    }

    protected Double getNextChatBaseSortId(String chatBusinessId, String userName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatBusinessId", chatBusinessId);
        params.put("createdBy", userName);
        Double nextSortId = (Double) dao.queryObject("chat.session.base.getNextSortIdByBusinessId", params);
        if (nextSortId == null || nextSortId < 1D) {
            return 1D;
        }
        return nextSortId;
    }

    protected ChatUserConfig buildChatUserConfig(CreateChatSessionReq req, String userName, Date now) {
        if (StringUtils.isBlank(req.getLastUsedAiModel())) {
            return null;
        }

        ChatUserConfig userConfig = new ChatUserConfig();
        userConfig.setUserName(userName);
        userConfig.setLastUsedAiModel(req.getLastUsedAiModel().trim());
        userConfig.setCreatedBy(userName);
        userConfig.setCreatedTime(now);
        return userConfig;
    }

    protected void syncLastUsedAiModel(String userName, String aiModel, Date now) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(aiModel)) {
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userName", userName);
        ChatUserConfig existing = (ChatUserConfig) dao.queryObject("chat.session.user.config.getChatUserConfigByUserName", params);
        if (existing == null) {
            ChatUserConfig userConfig = new ChatUserConfig();
            userConfig.setUserName(userName);
            userConfig.setLastUsedAiModel(aiModel.trim());
            userConfig.setCreatedBy(userName);
            userConfig.setCreatedTime(now);
            dao.insert("chat.session.user.config.insertChatUserConfig", userConfig);
            return;
        }

        params.put("lastUsedAiModel", aiModel.trim());
        dao.update("chat.session.user.config.updateLastUsedAiModel", params);
    }

    protected ChatDataConfig buildChatDataConfig(Long chatId, String dataSnapshotType, String dataSnapshotId, String dataFilterText,
                                                 String userName, Date now) {
        ChatDataConfig dataConfig = new ChatDataConfig();
        dataConfig.setChatDataId(Guid.id());
        dataConfig.setChatId(chatId);
        dataConfig.setDataSnapshotType(dataSnapshotType);
        dataConfig.setDataSnapshotId(dataSnapshotId);
        dataConfig.setDataFilterText(dataFilterText);
        dataConfig.setIsLatest(ACTIVE);
        dataConfig.setCreatedBy(userName);
        dataConfig.setCreatedTime(now);
        return dataConfig;
    }

    protected List<ChatDataResult> buildChatDataResults(List<CreateChatSessionSnapshotReq> snapshots, Long chatId, String chatDataId,
                                                        String userName, Date now) {
        List<ChatDataResult> results = new ArrayList<ChatDataResult>(snapshots.size());
        for (CreateChatSessionSnapshotReq snapshot : snapshots) {
            results.add(buildChatDataResult(snapshot, chatId, userName, now, chatDataId));
        }
        return results;
    }

    protected ChatDataResult buildChatDataResult(CreateChatSessionSnapshotReq snapshot, Long chatId, String userName, Date now,
                                                 String chatDataId) {
        ChatDataResult dataResult = new ChatDataResult();
        dataResult.setChatDataId(chatDataId);
        dataResult.setChatId(chatId);
        dataResult.setDataSnapshotId(snapshot.getDataSnapshotId());
        dataResult.setDataQueryViewId(snapshot.getDataQueryViewId());
        dataResult.setDataQueryConfig(snapshot.getDataQueryConfig());
        dataResult.setDataQueryRemark(snapshot.getDataQueryRemark());
        dataResult.setDataContent(snapshot.getDataContent().trim());
        dataResult.setDataPreviewContent(snapshot.getDataPreviewContent().trim());
        dataResult.setDataSize(snapshot.getDataSize());
        dataResult.setDataRowCount(snapshot.getDataRowCount());
        dataResult.setDataColumnCount(snapshot.getDataColumnCount());
        return dataResult;
    }

    protected ChatMessage buildQaChatMessage(SaveChatMessageReq req, String userName, Date now) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatMessageType(MESSAGE_TYPE_QA);
        chatMessage.setIsSummarized(req.getIsSummarized() != null ? req.getIsSummarized() : DEFAULT_IS_SUMMARIZED);
        chatMessage.setChatId(req.getChatId());
        chatMessage.setChatDataId(req.getChatDataId().trim());
        chatMessage.setQuestionContent(req.getQuestionContent().trim());
        chatMessage.setAnswerContent(req.getAnswerContent().trim());
        chatMessage.setAnswerDurationSeconds(req.getAnswerDurationSeconds());
        chatMessage.setAnswerStatus(req.getAnswerStatus() != null ? req.getAnswerStatus() : DEFAULT_ANSWER_STATUS);
        chatMessage.setAiModel(req.getAiModel().trim());
        chatMessage.setChatBeginTime(req.getChatBeginTime());
        chatMessage.setChatEndTime(req.getChatEndTime());
        chatMessage.setCreatedBy(userName);
        chatMessage.setCreatedTime(now);
        return chatMessage;
    }

    protected ChatMessage buildSummaryChatMessage(SaveChatSummarizedMessageReq req, String userName, Date now) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatMessageType(MESSAGE_TYPE_SUMMARY);
        chatMessage.setIsSummarized(NOT_SUMMARIZED);
        chatMessage.setChatId(req.getChatId());
        chatMessage.setChatDataId(req.getChatDataId().trim());
        chatMessage.setQuestionContent(null);
        chatMessage.setAnswerContent(req.getSummarizedContent().trim());
        chatMessage.setAnswerDurationSeconds(null);
        chatMessage.setAnswerStatus(ANSWER_STATUS_NORMAL);
        chatMessage.setChatBeginTime(null);
        chatMessage.setChatEndTime(null);
        chatMessage.setCreatedBy(userName);
        chatMessage.setCreatedTime(now);
        return chatMessage;
    }

    protected ChatMessageResp toChatMessageResp(ChatMessage entity) {
        ChatMessageResp resp = new ChatMessageResp();
        resp.setChatMessageId(entity.getChatMessageId());
        resp.setChatId(entity.getChatId());
        resp.setChatDataId(entity.getChatDataId());
        resp.setQuestionContent(entity.getQuestionContent());
        resp.setAnswerContent(entity.getAnswerContent());
        resp.setAnswerDurationSeconds(entity.getAnswerDurationSeconds());
        resp.setAnswerStatus(entity.getAnswerStatus());
        resp.setAiModel(entity.getAiModel());
        resp.setChatBeginTime(entity.getChatBeginTime());
        resp.setChatEndTime(entity.getChatEndTime());
        resp.setIsSummarized(entity.getIsSummarized());
        resp.setChatMessageType(entity.getChatMessageType());
        resp.setCreatedBy(entity.getCreatedBy());
        resp.setCreatedTime(entity.getCreatedTime());
        return resp;
    }

    protected void fillAnswerContentFromOss(List<ChatMessageResp> messageList) {
        if (CollUtil.isEmpty(messageList)) {
            return;
        }

        List<String> answerContentUrls = messageList.stream()
                .map(ChatMessageResp::getAnswerContent)
                .filter(this::isOssUrl)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(answerContentUrls)) {
            return;
        }

        Map<String, String> answerMap = OssUtil.getDataByOssUrls(answerContentUrls);
        for (ChatMessageResp chatMessage : messageList) {
            String answerContent = chatMessage.getAnswerContent();
            if (!isOssUrl(answerContent)) {
                continue;
            }

            String realAnswerContent = answerMap.get(answerContent);
            if (realAnswerContent != null) {
                chatMessage.setAnswerContent(realAnswerContent);
            }
        }
    }

    protected boolean isOssUrl(String content) {
        return StringUtils.startsWithIgnoreCase(content, "http://")
                || StringUtils.startsWithIgnoreCase(content, "https://");
    }

    @SuppressWarnings("unchecked")
    protected void fillLastMessageContent(List<ChatSessionListItemResp> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }

        List<Long> lastChatMessageIds = rows.stream()
                .map(ChatSessionListItemResp::getLastChatMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(lastChatMessageIds)) {
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("lastChatMessageIds", lastChatMessageIds);
        List<ChatMessage> latestMessages = (List<ChatMessage>) dao.queryObjectList(
                "chat.session.message.listQuestionMessageByIds", params);
        if (CollUtil.isEmpty(latestMessages)) {
            return;
        }

        Map<Long, ChatMessage> chatIdToLatestMessage = latestMessages.stream()
                .filter(message -> message != null && message.getChatId() != null)
                .collect(Collectors.toMap(ChatMessage::getChatId, message -> message, (left, right) -> left));

        for (ChatSessionListItemResp row : rows) {
            ChatMessage latestMessage = chatIdToLatestMessage.get(row.getChatId());
            if (latestMessage == null) {
                continue;
            }
            row.setLastMessageContent(latestMessage.getQuestionContent());
            row.setLastMessageTime(latestMessage.getChatBeginTime());
        }
    }

    protected void fillBoundViewName(List<ChatSessionListItemResp> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }

        Map<String, String> viewNameCache = new HashMap<String, String>();
        for (ChatSessionListItemResp row : rows) {
            String boundViewId = row.getBoundViewId();
            if (StringUtils.isBlank(boundViewId)) {
                continue;
            }

            String cacheKey = row.getChatBusinessType() + "_" + boundViewId;
            if (!viewNameCache.containsKey(cacheKey)) {
                viewNameCache.put(cacheKey, queryBoundViewName(row.getChatBusinessType(), boundViewId));
            }
            row.setBoundViewName(viewNameCache.get(cacheKey));
        }
    }

    protected String queryBoundViewName(String chatBusinessType, String boundViewId) {
        if (StringUtils.isBlank(boundViewId)) {
            return null;
        }

        if (ChatBusinessType.QUERY_TEMPLATE.getCode().equals(chatBusinessType)) {
            TemplateViewEntity templateViewEntity =
                    (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", boundViewId);
            return templateViewEntity == null ? null : templateViewEntity.getViewName();
        }

        AnalysisTplViewEntity analysisTplViewEntity =
                (AnalysisTplViewEntity) dao.queryObject("ssm.analysis.tpl.view.getById", boundViewId);
        return analysisTplViewEntity == null ? null : analysisTplViewEntity.getViewName();
    }

    @SuppressWarnings("unchecked")
    protected void fillMessageCount(List<ChatSessionListItemResp> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }

        List<Long> chatIds = rows.stream()
                .map(ChatSessionListItemResp::getChatId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(chatIds)) {
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatIds", chatIds);
        List<ChatMessageStatRow> statRows = (List<ChatMessageStatRow>) dao.queryObjectList(
                "chat.session.message.countQaMessagesByChatIds", params);
        Map<Long, Long> chatIdToCount = new HashMap<Long, Long>();
        if (CollUtil.isNotEmpty(statRows)) {
            for (ChatMessageStatRow statRow : statRows) {
                if (statRow != null && statRow.getChatId() != null && statRow.getMessageCount() != null) {
                    chatIdToCount.put(statRow.getChatId(), statRow.getMessageCount());
                }
            }
        }

        for (ChatSessionListItemResp row : rows) {
            Long count = chatIdToCount.get(row.getChatId());
            row.setMessageCount(count == null ? 0 : count.intValue());
        }
    }

    protected void updateChatBaseLastMessageId(Long chatId, Long chatMessageId, String userName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", chatId);
        params.put("lastChatMessageId", chatMessageId);
        params.put("updatedBy", userName);
        dao.update("chat.session.base.updateLastChatMessageId", params);
    }

    protected void updateChatBaseName(Long chatId, String chatName, String userName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", chatId);
        params.put("chatName", chatName);
        params.put("updatedBy", userName);
        dao.update("chat.session.base.renameChatBase", params);
    }

    protected String generateChatName(ChatBase chatBase, SaveChatMessageReq req) {
        try {
            String businessName = queryChatBusinessName(chatBase.getChatBusinessType(), chatBase.getChatBusinessId());
            if (StringUtils.isAnyBlank(businessName, req.getQuestionContent(), req.getAiModel())) {
                return null;
            }

            String url = AgentUtil.getAgentChatServiceBaseUrl()+"/llm/tool/generate-name";
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", businessName);
            requestBody.put("query", req.getQuestionContent());
            requestBody.put("modelName", req.getAiModel());

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"), requestBody.toJSONString()))
                    .build();

            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.body() == null) {
                    return null;
                }
                String responseBody = response.body().string();
                if (!response.isSuccessful() || StringUtils.isBlank(responseBody)) {
                    log.warn("generate chat name failed, chatId={}, httpCode={}, response={}",
                            chatBase.getChatId(), response.code(), responseBody);
                    return null;
                }

                JSONObject jsonObject = JSON.parseObject(responseBody);
                JSONObject data = jsonObject == null ? null : jsonObject.getJSONObject("data");
                return data == null ? null : StringUtils.trimToNull(data.getString("generatedName"));
            }
        } catch (Exception e) {
            System.out.println("generate chat name error"+ e.getMessage());
            log.warn("generate chat name error, chatId={}", chatBase.getChatId(), e);
            return null;
        }
    }

    protected String queryChatBusinessName(String chatBusinessType, String chatBusinessId) {
        if (StringUtils.isBlank(chatBusinessId)) {
            return null;
        }

        if (ChatBusinessType.QUERY_TEMPLATE.getCode().equals(chatBusinessType)) {
            return (String) dao.queryObject("ssm.template.getTemplateNameByTplId", chatBusinessId);
        }

        return (String) dao.queryObject("ssm.analysisTemplate.getAnalysisTplNameById", chatBusinessId);
    }

    protected ChatContext buildChatContext(ChatSessionContextUpdateReq req, String userName, Date now) {
        ChatContext chatContext = new ChatContext();
        chatContext.setChatId(req.getChatId());
        chatContext.setContextMaxByte(req.getContextMaxByte());
        chatContext.setContextUsageByte(req.getContextUsageByte());
        chatContext.setCreatedBy(userName);
        chatContext.setCreatedTime(now);
        chatContext.setUpdatedBy(userName);
        chatContext.setUpdatedTime(now);
        return chatContext;
    }

    private Map<String, Object> buildFavoriteQueryParams(Long chatId, String userName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("chatId", String.valueOf(chatId));
        params.put("userName", userName);
        return params;
    }

    private boolean isInactive(ChatBase chatBase) {
        return chatBase.getIsActive() != null && chatBase.getIsActive() == INACTIVE;
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 执行 Agent 对话：在独立线程中拉取远程 SSE，将增量事件推给前端，结束后落库并关闭流。
     * <p>
     * Controller 可立即返回；实际耗时逻辑在此异步执行，避免阻塞 HTTP 线程。
     */
    public void execute(ChatExecuteReq req, ChatExecuteContext context) {
        context.setUserName(UserManager.get().getName());
        // 单线程执行整条 Agent 调用链，与 SSE 顺序消费一致
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            try {
                queryAgentChatService(req, context);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                long endTime = System.currentTimeMillis();
                // 通知前端结束并附带耗时（如心跳/关闭帧）
                AgentUtil.closeMessage(context, Duration.ofMillis(endTime - startTime));
            }
        });
        executor.shutdown();
    }

    /**
     * 调用 Agent 流式接口：设置线程内用户上下文 → SSE 读行解析 → 中间事件转发前端 →
     * 收到执行结果后保存问答；最后在 finally 中清理 UserManager 与流。
     */
    private void queryAgentChatService(ChatExecuteReq req, ChatExecuteContext context) {
        InputStream is = null;
        try {

            // 子线程无 Web 上下文，显式注入当前用户名供下游 saveChatMessage 等使用
            User user = new User();
            user.setName(context.getUserName());
            UserManager.set(user);

            SaveChatMessageReq saveChatMessageReq = new SaveChatMessageReq();
            saveChatMessageReq.setChatBeginTime(new Date());
            saveChatMessageReq.setChatId(context.getChatId());
            saveChatMessageReq.setAiModel(req.getAiModel());
            saveChatMessageReq.setQuestionContent(req.getQuestionContent());

            ChatAgentQueryReq chatAgentQueryReq = buildChatAgentQueryReq(req);
            long t1 = System.currentTimeMillis();
            System.out.println("开始调用agent服务：chatId:"+context.getChatId());
            is = this.streamInvoke(context, chatAgentQueryReq);
            long t2 = System.currentTimeMillis();
            System.out.println("获取agent服务流：chatId:"+context.getChatId()+"耗时："+(t2-t1)/1000);
            saveChatMessageReq.setChatDataId(context.getChatDataId());

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            ChatAgentResult agentResult = new ChatAgentResult();
            // 按行读取 SSE：仅处理 data: 行，解析 wfEvent 决定是最终结果还是透传前端
            while ((line = reader.readLine()) != null) {
                try {

                    if (StrUtil.isEmpty(line)) {
                        continue;
                    }

                    if (!line.startsWith("data:")) {
                        continue;
                    }

                    String newStr = line.replaceFirst("data:", "").trim();
                    JSONObject msgJson = JSONObject.parseObject(newStr);
                    String event = msgJson.getString("wfEvent");
                    SseEventType sseEventType = SseEventType.get(event);

                    if (SseEventType.EXECUTE_RESULT == sseEventType) {
                        long t3 = System.currentTimeMillis();
                        System.out.println("agent服务执行结果事件：chatId:"+context.getChatId()+"耗时："+(t3-t1)/1000);
                        // 本轮调用的完整结果（含 answer、summary 等），用于后续落库
                        agentResult = JSONObject.parseObject(newStr, ChatAgentResult.class);

                    } else {

                        if (SseEventType.MESSAGE_END == sseEventType) {
                            long t4 = System.currentTimeMillis();
                            System.out.println("agent服务结束事件：chatId:"+context.getChatId()+"耗时："+(t4-t1)/1000);
                            break;
                        }
                        // 思考过程、工具调用等中间事件，原样推给前端 SSE
                        AgentUtil.sendMessage(context, sseEventType, msgJson);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            saveChatMessageReq.setChatEndTime(new Date());
            ///System.out.println("agentResult:" + JSONObject.toJSONString(agentResult));
            saveAgentChatResult(agentResult, saveChatMessageReq);

        } catch (Exception e) {
            System.out.println("queryAgentChatService error"+ e.getMessage());
            e.printStackTrace();
        } finally {
            // 避免线程池复用时串用户
            UserManager.remove();
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * 将 Agent 返回结果持久化：QA 消息（答案可为大文本，存 OSS URL）；
     * 若带摘要则再写一条 SUMMARY 并批量标记被摘要的 QA。
     */
    public void saveAgentChatResult(ChatAgentResult agentResult, SaveChatMessageReq saveChatMessageReq) {

        saveChatMessageReq.setMessageType(ChatMessageType.QA.getCode());
        saveChatMessageReq.setAnswerStatus(agentResult.isSuccess() ? Enabled.YES.getId() : Enabled.NO.getId());

        String answerContentUrl = "";
        if(!agentResult.isSuccess()){
            saveChatMessageReq.setAnswerStatus(Enabled.NO.getId());
            answerContentUrl = StrUtil.isNotEmpty(agentResult.getErrorMessage())?agentResult.getErrorMessage():"agent服务异常";
            answerContentUrl = OssUtil.upload(answerContentUrl.getBytes(StandardCharsets.UTF_8), "", ".json", "bigdata/ssm/chat");
        }
        if (StrUtil.isNotEmpty(agentResult.getAnswer())) {
            // 长答案上传 OSS，库表仅存链接，列表查询时再按需拉取正文
            answerContentUrl = OssUtil.upload(agentResult.getAnswer().getBytes(StandardCharsets.UTF_8), "", ".json", "bigdata/ssm/chat");
        }

        saveChatMessageReq.setAnswerContent(answerContentUrl);

        long AnswerDurationSeconds = Math.abs(saveChatMessageReq.getChatBeginTime().getTime() - saveChatMessageReq.getChatEndTime().getTime());
        saveChatMessageReq.setAnswerDurationSeconds((int) (AnswerDurationSeconds / 1000));
        saveChatMessage(saveChatMessageReq);

        //保存会话token使用情况
        ChatAgentTokenUsage tokenUsage = agentResult.getTokenUsage();
        if(tokenUsage!=null) {
            ChatSessionContextUpdateReq req = new ChatSessionContextUpdateReq();
            req.setChatId(saveChatMessageReq.getChatId());
            req.setContextMaxByte(tokenUsage.getMaxTokens().doubleValue());
            req.setContextUsageByte(tokenUsage.getTotalTokens().doubleValue());
            updateChatSessionContext(req);
        }

        // Agent 返回会话级摘要时，额外落库 SUMMARY 消息并更新被摘要消息的 is_summarized
        ChatAgentContextSummary summary = agentResult.getSummary();
        if (summary != null && StrUtil.isNotEmpty(summary.getSummary())) {
            SaveChatSummarizedMessageReq saveChatSummarizedMessageReq = new SaveChatSummarizedMessageReq();
            saveChatSummarizedMessageReq.setChatId(saveChatMessageReq.getChatId());
            saveChatSummarizedMessageReq.setChatDataId(saveChatMessageReq.getChatDataId());

            //摘要上传oss
            String summaryContentUrl = OssUtil.upload(summary.getSummary().getBytes(StandardCharsets.UTF_8), "", ".json", "bigdata/ssm/chat");
            saveChatSummarizedMessageReq.setSummarizedContent(summaryContentUrl);
            List<Long> summarizedMessageIds = new ArrayList<>();
            for (String summarizedMessageId : summary.getSummarizedMessageIds()) {
                summarizedMessageIds.add(Long.parseLong(summarizedMessageId));
            }
            saveChatSummarizedMessageReq.setChatMessageIds(summarizedMessageIds);
            saveChatSummarizedMessage(saveChatSummarizedMessageReq);
        }

    }

    /**
     * POST 调用 Agent 执行接口，返回响应体字节流（text/event-stream），由调用方按行解析 SSE。
     */
    protected InputStream streamInvoke(ChatExecuteContext context, ChatAgentQueryReq chatAgentQueryReq) {

        String token = context.getToken();
        context.setChatDataId(chatAgentQueryReq.getChatDataId());

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");

        String url = AgentUtil.getAgentChatServiceBaseUrl()+"/llm/tool/execute";
        String params = JSON.toJSONString(chatAgentQueryReq);
        RequestBody body = RequestBody.create(mediaType, params);
        Request request = new Request.Builder().addHeader("u_token", token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(body).url(url).build();
        InputStream result = null;
        try {
            // SSE 可能长时间无完整 body，读/连超时放大避免中途断开
            OkHttpClient httpClient = new OkHttpClient.Builder().readTimeout(6, TimeUnit.HOURS)
                    .connectTimeout(6, TimeUnit.HOURS).build();
            Response response = httpClient.newCall(request).execute();
            result = response.body().byteStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 组装调用 Agent 的完整入参：会话 dataId、各快照数据集 URL 与说明、历史消息（未摘要的 QA）及汇总摘要文案。
     */
    public ChatAgentQueryReq buildChatAgentQueryReq(ChatExecuteReq req) {

        ChatAgentQueryReq chatAgentQueryReq = new ChatAgentQueryReq();
        chatAgentQueryReq.setSessionId(req.getChatId().toString());
        chatAgentQueryReq.setQuery(req.getQuestionContent());
        chatAgentQueryReq.setModelName(req.getAiModel());

        // 会话详情：拿到 chatDataId 与每条快照的 OSS 数据、remark（模板/视图/时间范围等）
        ChatSessionDetailReq chatDetailReq = new ChatSessionDetailReq();
        chatDetailReq.setChatId(req.getChatId());
        ChatSessionDetailResp chatSessionDetailResp = getChatSessionDetail(chatDetailReq);
        chatAgentQueryReq.setChatDataId(chatSessionDetailResp.getDataConfig().getChatDataId());

        for (ChatSessionDataResultResp dataResult : chatSessionDetailResp.getDataConfig().getDataResults()) {
            ChatAgentQueryDatasetReq chatAgentQueryDatasetReq = new ChatAgentQueryDatasetReq();
            chatAgentQueryDatasetReq.setUrl(dataResult.getDataContent());

            /**
             * 补充数据集的说明信息
             */
            String dataQueryRemark = dataResult.getDataQueryRemark();
            if (StrUtil.isNotEmpty(dataQueryRemark)) {
                ChatDataSnapshotRemark chatDataSnapshotRemark = JSON.parseObject(dataQueryRemark, ChatDataSnapshotRemark.class);

                String name = String.format("%s,%s", chatDataSnapshotRemark.getTplName(), chatDataSnapshotRemark.getViewName());
                chatAgentQueryDatasetReq.setName(chatDataSnapshotRemark.getViewId());
                chatAgentQueryDatasetReq.setDescription(name);

                ChatAgentQueryDataTimeReq chatAgentQueryDataTimeReq = new ChatAgentQueryDataTimeReq();

                DateGranularity dateGranularity = DateGranularity.get(chatDataSnapshotRemark.getDateGranularity());
                chatAgentQueryDataTimeReq.setGranularity(dateGranularity.getDesc());

                if (CollUtil.isNotEmpty(chatDataSnapshotRemark.getDataRange())) {
                    chatAgentQueryDataTimeReq.setStart(chatDataSnapshotRemark.getDataRange().get(0));
                    chatAgentQueryDataTimeReq.setEnd(chatDataSnapshotRemark.getDataRange().get(chatDataSnapshotRemark.getDataRange().size() - 1));
                }
                chatAgentQueryDatasetReq.setDataTime(chatAgentQueryDataTimeReq);
            }

            chatAgentQueryReq.getDatasets().add(chatAgentQueryDatasetReq);
        }

        // 历史消息：已摘要的 QA 不再重复传给模型；SUMMARY 类型消息的答案拼成 summary 字段
        ChatMessageListReq chatMessageListReq = new ChatMessageListReq();
        chatMessageListReq.setChatId(req.getChatId());
        List<ChatMessageResp> messageList = listChatMessages(chatMessageListReq,false);
        if (CollUtil.isNotEmpty(messageList)) {

            messageList = messageList.stream().filter(f -> !Enabled.value(f.getIsSummarized()) &&Enabled.value(f.getAnswerStatus())).collect(Collectors.toList());
            String summary = "";
            for (ChatMessageResp chatMessage : messageList) {
                ChatMessageType chatMessageType = ChatMessageType.get(chatMessage.getChatMessageType());

                String answerContent = chatMessage.getAnswerContent();
                if (ChatMessageType.QA == chatMessageType) {
                    ChatAgentQueryMessageReq chatAgentQueryMessageReq = new ChatAgentQueryMessageReq();
                    chatAgentQueryMessageReq.setId(chatMessage.getChatMessageId().toString());
                    chatAgentQueryMessageReq.setQuery(chatMessage.getQuestionContent());
                    chatAgentQueryMessageReq.setAnswer(answerContent);
                    chatAgentQueryReq.getMessages().add(chatAgentQueryMessageReq);
                } else {
                    summary = answerContent;
                }
            }

            chatAgentQueryReq.setSummary(summary);

        }

        return chatAgentQueryReq;
    }

    /**
     * 一步创建会话：根据是否实时查数生成快照列表，写入过滤文案与主快照 viewId，再复用 {@link #createChatSession}。
     * <p>
     * 性能要点：耗时主要在 {@link #buildChatDataSnapshot}（多条 OLAP 与 OSS）；多组件时该方法内部会并行构建各条快照
     * （见 {@link #SNAPSHOT_BUILD_PARALLELISM}）。若仍慢，可优先让前端在「数据未顶格截断」场景传
     * {@code isNeedQueryData=0} 或保证 dataset 行数小于 {@link QuerySessionSettingManager#getQueryRowLimit()} 以跳过 OLAP。
     */
    public Long createWithSnapshot(ChatCreateWithSnapshotReq req) throws IOException {

        List<CreateChatSessionSnapshotReq> chatDataSnapshotReqList = buildChatDataSnapshot(req);

        CreateChatSessionReq createChatSessionReq = new CreateChatSessionReq();
        createChatSessionReq.setChatBusinessType(req.getChatBusinessType());
        createChatSessionReq.setChatBusinessId(req.getChatBusinessId());

        // 构建过滤条件
        createChatSessionReq.setDataFilterText(buildDataFilterText(req.getChatDataSnapshotReqList()));
        createChatSessionReq.setDataSnapshots(chatDataSnapshotReqList);
        createChatSessionReq.setDataSnapshotId(req.getDataSnapshotViewId());
        ChatSessionResp chatSessionResp = createChatSession(createChatSessionReq);

        return chatSessionResp.getChatId();
    }

    /**
     * 构建快照列表。
     * <ul>
     *   <li>{@code isNeedQueryData=false}：仅用入参 {@link ChatDataSnapshotReq#getDataset()}，转 CSV 上传（见
     *   <li>{@code isNeedQueryData=true}：默认走后端 OLAP；若某条快照里前端 {@code dataset} 行数
     *   严格小于 {@link QuerySessionSettingManager#getQueryRowLimit()}（与引擎查询结果上限一致），
     *   则认为数据未被「顶格截断」、与前端全量一致，此时<strong>不再请求 OLAP</strong>，与上一分支同源。</li>
     * </ul>
     * 非查数路径及上述「走前端 dataset」时：若行数大于 {@link #SNAPSHOT_DATASET_PREVIEW_MAX_ROWS}，再单独上传预览 CSV。
     * <p>
     * 多于一条快照时，各条并行构建（有界线程池），以缩短看板多组件场景总耗时；单条仍串行避免无谓调度。
     * OLAP 依赖 {@link UserManager}，子线程会注入调用方用户后执行并在任务结束清理。
     */
    public List<CreateChatSessionSnapshotReq> buildChatDataSnapshot(ChatCreateWithSnapshotReq req) throws IOException {

        // 入参快照列表（多组件 = 多条）
        List<ChatDataSnapshotReq> snapshotReqs = req.getChatDataSnapshotReqList();
        if (CollUtil.isEmpty(snapshotReqs)) {
            return new ArrayList<CreateChatSessionSnapshotReq>();
        }

        // true：需要走后端 OLAP 查数分支；false：仅用前端自带的 dataset
        boolean needOlapBranch = Enabled.value(req.getIsNeedQueryData());
        // 仅在 OLAP 分支有意义：与引擎结果行上限一致，用于判断「前端行数未顶满 → 信任全量、跳过 OLAP」
        Integer searchLimit = needOlapBranch ? QuerySessionSettingManager.getQueryRowLimit() : null;
        int n = snapshotReqs.size();

        // 单条：不建线程池，顺序构建即可
        if (n <= 1) {
            List<CreateChatSessionSnapshotReq> out = new ArrayList<CreateChatSessionSnapshotReq>(n);
            for (ChatDataSnapshotReq snapshotReq : snapshotReqs) {
                out.add(buildOneChatSnapshotItem(req, snapshotReq, needOlapBranch, searchLimit));
            }
            return out;
        }

        // 并行时子线程无 Web 上下文，需把当前用户拷入 UserManager，供 OlapApiService.queryOlapDataByConfig 使用
        User callerUser = UserManager.get();
        int poolSize = Math.min(n, SNAPSHOT_BUILD_PARALLELISM);
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            // 按下标写入，保证返回顺序与入参 chatDataSnapshotReqList 一致
            CreateChatSessionSnapshotReq[] slots = new CreateChatSessionSnapshotReq[n];
            List<CompletableFuture<Void>> futureList = new ArrayList<CompletableFuture<Void>>(n);
            for (int i = 0; i < n; i++) {
                int idx = i;
                ChatDataSnapshotReq snapshotReq = snapshotReqs.get(i);
                futureList.add(CompletableFuture.runAsync(() -> {
                    if (callerUser != null) {
                        UserManager.set(callerUser);
                    }
                    try {
                        slots[idx] = buildOneChatSnapshotItem(req, snapshotReq, needOlapBranch, searchLimit);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    } finally {
                        UserManager.remove();
                    }
                }, pool));
            }
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture<?>[0])).join();
            List<CreateChatSessionSnapshotReq> out = new ArrayList<CreateChatSessionSnapshotReq>(n);
            Collections.addAll(out, slots);
            return out;
        } catch (CompletionException e) {
            // 子线程里用 CompletionException 包装了 IOException，此处还原为受检异常或运行时异常
            Throwable c = e.getCause();
            if (c instanceof IOException) {
                throw (IOException) c;
            }
            if (c instanceof Error) {
                throw (Error) c;
            }
            if (c instanceof RuntimeException) {
                throw (RuntimeException) c;
            }
            throw new RuntimeException(c);
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(30, TimeUnit.MINUTES)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }
    }

    /**
     * 单条快照：非 OLAP 分支或「行数未顶满 searchLimit」时用前端 dataset；否则 OLAP。
     */
    private CreateChatSessionSnapshotReq buildOneChatSnapshotItem(ChatCreateWithSnapshotReq req,
                                                                  ChatDataSnapshotReq snapshotReq,
                                                                  boolean needOlapBranch,
                                                                  Integer searchLimit) throws IOException {

        //模板临时快照，赋值快照视图的属性
        if (StrUtil.isEmpty(snapshotReq.getViewId())) {
            snapshotReq.setViewId(req.getDataSnapshotViewId());
            snapshotReq.setViewName("默认视图");
            snapshotReq.setTplId(req.getChatBusinessId());
        }

        if (!needOlapBranch) {
            return buildSnapshotItemFromClientDataset(snapshotReq,req.getDataSnapshotViewId());
        }
        ResultDataSet prefetchDataset = snapshotReq.getDataset();
        int prefetchRowCount = prefetchDataset != null && prefetchDataset.getRows() != null
                ? prefetchDataset.getRows().size() : 0;
        if (searchLimit != null && prefetchRowCount < searchLimit) {
            return buildSnapshotItemFromClientDataset(snapshotReq,req.getDataSnapshotViewId());
        }
        return buildSnapshotItemFromOlap(req, snapshotReq);
    }

    private CreateChatSessionSnapshotReq buildSnapshotItemFromOlap(ChatCreateWithSnapshotReq req,
                                                                   ChatDataSnapshotReq snapshotReq) throws IOException {
        QueryOlapByConfigReq queryOlapByConfigReq = new QueryOlapByConfigReq();
        queryOlapByConfigReq.setConfig(snapshotReq.getQueryConfig());
        queryOlapByConfigReq.setViewId(StrUtil.isEmpty(snapshotReq.getViewId()) ? req.getDataSnapshotViewId() : snapshotReq.getViewId());
        ResultDataSet resultDataSet = olapApiService.queryOlapDataByConfig(queryOlapByConfigReq);
        CsvUtil.CsvDataset csvDataset = JSON.parseObject(JSON.toJSONString(resultDataSet.getProperties().get("csvDataset")), CsvUtil.CsvDataset.class);
        return buildChatDataSnapshotReq(csvDataset, snapshotReq, req.getDataSnapshotViewId());
    }

    /**
     * 使用请求体中已携带的 {@link ChatDataSnapshotReq#getDataset()} 生成一条快照：
     * 全量转 CSV 上传 OSS；行数过多时预览单独截断。与 {@code isNeedQueryData=0} 主流程一致。
     */
    private CreateChatSessionSnapshotReq buildSnapshotItemFromClientDataset(ChatDataSnapshotReq snapshotReq,String dataSnapshotViewId) {
        ResultDataSet fullDataset = snapshotReq.getDataset();
        CsvUtil.CsvDataset csvDataset = CsvUtil.toCsv(fullDataset, '\t');
        String csvDatasetUrl = OssUtil.upload(csvDataset.csvContent.getBytes(StandardCharsets.UTF_8), "", ".csv", "bigdata/agent_new/dataset");
        csvDataset.csvDatasetUrl = csvDatasetUrl;

//        int rowCount = fullDataset != null && fullDataset.getRows() != null
//                ? fullDataset.getRows().size() : 0;
//        if (rowCount > SNAPSHOT_DATASET_PREVIEW_MAX_ROWS) {
//            ResultDataSet previewDataset = copyResultDataSetWithMaxRows(fullDataset, SNAPSHOT_DATASET_PREVIEW_MAX_ROWS);
//            CsvUtil.CsvDataset previewCsv = CsvUtil.toCsv(previewDataset, '\t');
//            String previewUrl = OssUtil.upload(previewCsv.csvContent.getBytes(StandardCharsets.UTF_8), "", ".csv", "bigdata/agent_new/dataset");
//            csvDataset.previewCsvDatasetUrl = previewUrl;
//        } else {
        csvDataset.previewCsvDatasetUrl = csvDatasetUrl;
//        }
        return buildChatDataSnapshotReq(csvDataset, snapshotReq, dataSnapshotViewId);
    }

    /**
     * 快照更新请求体中已带好的快照列表，直接透出（无额外查数）。
     */
    public List<CreateChatSessionSnapshotReq> buildChatDataSnapshot(ChatSnapshotUpdateReq req) {
        return req == null ? Collections.<CreateChatSessionSnapshotReq>emptyList() : req.getDataSnapshots();
    }

    /**
     * 将单次快照请求 + 查数得到的 CSV 转为落库结构：查询配置与数据内容均用 OSS URL，remark 存模板/视图/时间粒度等元数据。
     */
    public CreateChatSessionSnapshotReq buildChatDataSnapshotReq(CsvUtil.CsvDataset csvDataset, ChatDataSnapshotReq snapshotReq, String dataSnapshotViewId) {

        CreateChatSessionSnapshotReq createChatSessionSnapshotReq = new CreateChatSessionSnapshotReq();
        createChatSessionSnapshotReq.setDataSnapshotId(dataSnapshotViewId);

        String dataQueryConfig = OssUtil.upload(snapshotReq.getQueryConfig().getBytes(StandardCharsets.UTF_8), "", ".json", "bigdata/ssm/chat");
        createChatSessionSnapshotReq.setDataQueryConfig(dataQueryConfig);
        createChatSessionSnapshotReq.setDataContent(csvDataset.csvDatasetUrl);
        createChatSessionSnapshotReq.setDataPreviewContent(StrUtil.isNotEmpty(csvDataset.previewCsvDatasetUrl) ? csvDataset.previewCsvDatasetUrl : csvDataset.csvDatasetUrl);
        createChatSessionSnapshotReq.setDataQueryViewId(snapshotReq.getViewId());

        //构建查询说明
        createChatSessionSnapshotReq.setDataQueryRemark(buildDataQueryRemark(snapshotReq));

        createChatSessionSnapshotReq.setDataSize(Double.valueOf(String.valueOf(csvDataset.storageSize)));
        createChatSessionSnapshotReq.setDataRowCount(csvDataset.recordSize);
        createChatSessionSnapshotReq.setDataColumnCount(csvDataset.headers.length);

        return createChatSessionSnapshotReq;
    }


    /**
     * 构建数据过滤条件（多条快照）。若各条生成的文案不一致，返回第一条文案 + "..."
     *
     * @param snapshotReqList 快照请求集合
     * @return 过滤文案
     */
    public String buildDataFilterText(List<ChatDataSnapshotReq> snapshotReqList) {
        if (CollUtil.isEmpty(snapshotReqList)) {
            return "";
        }
        String first = buildDataFilterTextSingle(snapshotReqList.get(0));
        for (int i = 1; i < snapshotReqList.size(); i++) {
            if (!Objects.equals(first, buildDataFilterTextSingle(snapshotReqList.get(i)))) {
                return first + "...";
            }
        }
        return first;
    }

    private String buildDataFilterTextSingle(ChatDataSnapshotReq snapshotReq) {
        if (snapshotReq == null || CollUtil.isEmpty(snapshotReq.getDataRange())) {
            return "";
        }
        DateGranularity granularity = DateGranularity.get(snapshotReq.getDateGranularity());
        return String.format("%s|%s ~ %s",
                granularity.getDesc(),
                snapshotReq.getDataRange().get(0),
                snapshotReq.getDataRange().get(snapshotReq.getDataRange().size() - 1));
    }

    /**
     * 构建数据查询说明
     *
     * @param snapshotReq
     * @return
     */
    public String buildDataQueryRemark(ChatDataSnapshotReq snapshotReq) {

        ChatDataSnapshotRemark dataQueryRemark = new ChatDataSnapshotRemark();
        dataQueryRemark.setTplId(snapshotReq.getTplId());
        dataQueryRemark.setTplName(snapshotReq.getTplName());
        dataQueryRemark.setViewId(snapshotReq.getViewId());
        dataQueryRemark.setViewName(snapshotReq.getViewName());
        dataQueryRemark.setDataRange(snapshotReq.getDataRange());
        dataQueryRemark.setDateGranularity(snapshotReq.getDateGranularity());
        dataQueryRemark.setWidgetId(snapshotReq.getWidgetId());

        return JSONObject.toJSONString(dataQueryRemark);
    }

    /**
     * 对外「带快照更新会话」入口：先 {@link #buildChatDataSnapshot(ChatCreateWithSnapshotReq)}，再委托 {@link #updateChatSnapshotData}。
     */
    public void snapshotUpdate(ChatUpdateWithSnapshotReq req) throws IOException {

        List<CreateChatSessionSnapshotReq> chatDataSnapshotReqList = buildChatDataSnapshot(req);

        ChatSnapshotUpdateReq updateReq = new ChatSnapshotUpdateReq();
        updateReq.setChatId(req.getChatId());

        updateReq.setDataFilterText(buildDataFilterText(req.getChatDataSnapshotReqList()));

        updateReq.setDataSnapshots(chatDataSnapshotReqList);
        updateReq.setDataSnapshotId(req.getDataSnapshotViewId());

        updateChatSnapshotData(updateReq);

    }

    /**
     * 事务内将本会话旧 data_config 标记非最新，插入新的 chat_data_config 与多条 chat_data_result，实现快照切换。
     */
    public void updateChatSnapshotData(final ChatSnapshotUpdateReq req) {
        List<CreateChatSessionSnapshotReq> chatDataSnapshotReqList = buildChatDataSnapshot(req);

        validateSnapshotUpdateReq(req);
        final String userName = getCurrentUserName();
        final ChatBase chatBase = checkChatSessionOwner(req.getChatId(), userName);
        ensureActive(chatBase);
        final Date now = new Date();
        final ChatDataConfig latestDataConfig = buildChatDataConfig(chatBase.getChatId(),
                chatBase.getChatBusinessType(), req.getDataSnapshotId(), req.getDataFilterText(), userName, now);
        final List<ChatDataResult> dataResults = buildChatDataResults(chatDataSnapshotReqList, chatBase.getChatId(), latestDataConfig.getChatDataId(),
                userName, now);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("chatId", req.getChatId());
                dao.update("chat.data.config.clearLatestChatDataConfigByChatId", params);
                dao.insert("chat.data.config.insertChatDataConfig", latestDataConfig);
                batchInsertChatDataResults(dataResults);
            }
        });

        log.info("snapshot update success, userName={}, chatId={}, chatDataId={}, snapshotCount={}",
                userName, req.getChatId(), latestDataConfig.getChatDataId(), dataResults.size());
    }

    /**
     * 对比当前会话已落库快照与入参：单视图比 query 配置；看板按 widgetId 对齐比配置。
     *
     * @return {@link com.bi.queryer.sys.enums.Enabled#YES} 表示已变更，{@link com.bi.queryer.sys.enums.Enabled#NO} 表示未变更
     */
    public Integer checkSnapshotDataChange(CheckSnapshotDataChangeReq req) {

        ChatSessionDetailReq chatDetailReq = new ChatSessionDetailReq();
        chatDetailReq.setChatId(req.getChatId());
        ChatSessionDetailResp chatSessionDetailResp = getChatSessionDetail(chatDetailReq);

        ChatBusinessType chatBusinessType = ChatBusinessType.get(chatSessionDetailResp.getChatBusinessType());
        if (CollUtil.isEmpty(chatSessionDetailResp.getDataConfig().getDataResults())) {
            return Enabled.YES.getId();
        }

        switch (chatBusinessType) {
            case QUERY_TEMPLATE:
                // 单视图：拉取 OSS 上已落库的配置，与入参 config 做归一化后比较（仅对比 filter/result/analysis）
                String dataQueryConfig = OssUtil.fetchOssUrlBody(chatSessionDetailResp.getDataConfig().getDataResults().get(0).getDataQueryConfig());
                String config = normalizeDecryptTplConfigForCompare(req.getConfig());
                String compareConfig = normalizeDecryptTplConfigForCompare(dataQueryConfig);
                if (!config.equalsIgnoreCase(compareConfig)) {
                    return Enabled.YES.getId();
                }

                break;
            case ANALYSIS_TEMPLATE:
            case ANALYSIS_TMP_TEMPLATE:
                // 看板多组件：按 widgetId 对齐，比较每个组件的查询配置是否与快照一致
                List<String> queryConfigUrls = new ArrayList<>();

                // key=widgetId，value=该组件配置在 OSS 上的地址
                Map<String, String> widgetIdConfigMap = new HashMap<>();
                for (ChatSessionDataResultResp dataResult : chatSessionDetailResp.getDataConfig().getDataResults()) {
                    queryConfigUrls.add(dataResult.getDataQueryConfig());
                    ChatDataSnapshotRemark dataQueryRemark = JSONObject.parseObject(dataResult.getDataQueryRemark(), ChatDataSnapshotRemark.class);
                    widgetIdConfigMap.put(dataQueryRemark.getWidgetId(), dataResult.getDataQueryConfig());
                }

                // URL -> OSS 拉取后的配置正文
                Map<String, String> queryConfigMap = OssUtil.getDataByOssUrls(queryConfigUrls);

                Set<String> snapshotWidgetIds = new HashSet<>(widgetIdConfigMap.keySet());
                // 请求未带任何组件：若快照里仍有组件则视为已变更
                if (CollUtil.isEmpty(req.getWidgetConfigs())) {
                    if (!snapshotWidgetIds.isEmpty()) {
                        return Enabled.YES.getId();
                    }
                    break;
                }
                // 逐个组件：widgetId 须在快照中存在，且归一化后的配置与快照一致
                Set<String> requestWidgetIds = new HashSet<>();
                for (CheckSnapshotDataChangeReq.WidgetConfig wc : req.getWidgetConfigs()) {
                    if (wc == null || StrUtil.isEmpty(wc.getWidgetId())) {
                        return Enabled.YES.getId();
                    }
                    String widgetId = wc.getWidgetId();
                    requestWidgetIds.add(widgetId);
                    if (!widgetIdConfigMap.containsKey(widgetId)) {
                        // 请求多出快照中不存在的组件
                        return Enabled.YES.getId();
                    }
                    String ossUrl = widgetIdConfigMap.get(widgetId);
                    String savedBody = queryConfigMap.get(ossUrl);

                    String compare1= normalizeDecryptTplConfigForCompare(savedBody);
                    String compare2= normalizeDecryptTplConfigForCompare(wc.getConfig());
                    if (!compare1.equalsIgnoreCase(compare2)) {
                        return Enabled.YES.getId();
                    }
                }
                // 请求与快照的 widgetId 集合须完全一致（防止快照侧多出的组件未在请求中提交）
                if (!requestWidgetIds.equals(snapshotWidgetIds)) {
                    return Enabled.YES.getId();
                }

                break;
        }

        return Enabled.NO.getId();
    }

    
    /**
     * 解密模板配置后仅保留 filter、result、analysis 再序列化，用于快照是否变更的对比。
     * 非 JSON 或解析失败时退回明文，避免误判为“无变更”。
     */
    private String normalizeDecryptTplConfigForCompare(String config) {
        // null 无配置，与 decrypt 前区分，避免 NPE
        if (config == null) {
            return "";
        }
        String plain = SSDUtil.decryptTplConfig(config);
        if (StrUtil.isBlank(plain)) {
            return "";
        }
        try {
            //转为为UICheckQueryResult
            UICheckQueryConfigure uiCheckQueryConfigure = JSONObject.parseObject(plain, UICheckQueryConfigure.class);
            return JSONObject.toJSONString(uiCheckQueryConfigure);

        } catch (Exception e) {
            log.debug("normalizeDecryptTplConfigForCompare parse skip, use raw plain text");
        }
        return plain;
    }

    /**
     * 按会话拉取各快照预览 CSV 的 OSS 内容，与 remark 中的模板名组装为列表返回。
     */
    public List<GetPreviewDataResp> getPreviewDataByChatId(GetPreviewDataByChatIdReq req) {

        List<GetPreviewDataResp> previewDataRespList = new ArrayList<>();

        Long chatId = req.getChatId();
        ChatSessionDetailReq chatDetailReq = new ChatSessionDetailReq();
        chatDetailReq.setChatId(chatId);
        ChatSessionDetailResp chatSessionDetailResp = getChatSessionDetail(chatDetailReq);

        List<String> previewDataUrls = chatSessionDetailResp.getDataConfig().getDataResults()
                .stream().map(ChatSessionDataResultResp::getDataPreviewContent).collect(Collectors.toList());

        Map<String, String> answerMap = OssUtil.getDataByOssUrls(previewDataUrls);

        for (ChatSessionDataResultResp dataResult : chatSessionDetailResp.getDataConfig().getDataResults()) {
            GetPreviewDataResp previewDataResp = new GetPreviewDataResp();
            ChatDataSnapshotRemark dataSnapshotRemark = JSONObject.parseObject(dataResult.getDataQueryRemark(), ChatDataSnapshotRemark.class);
            previewDataResp.setName(dataSnapshotRemark.getTplName());

            String previewData = OssUtil.getDatasetWithLimit(answerMap.get(dataResult.getDataPreviewContent()), SNAPSHOT_DATASET_PREVIEW_MAX_ROWS);
            previewDataResp.setData(previewData);
            previewDataRespList.add(previewDataResp);
        }

        return previewDataRespList;
    }

    /**
     * 浅拷贝 {@link ResultDataSet} 的列定义，行数据仅保留头部连续 {@code maxRows} 条。
     * 与 {@link CsvUtil#toCsv(ResultDataSet, char)} 配合，用于生成截断后的预览 CSV，不修改入参。
     *
     * @param source  完整数据集；为 null 或 maxRows 非法时返回 null
     * @param maxRows 最大保留行数（通常为 {@link #SNAPSHOT_DATASET_PREVIEW_MAX_ROWS}）
     */
    private static ResultDataSet copyResultDataSetWithMaxRows(ResultDataSet source, int maxRows) {
        if (source == null || maxRows <= 0) {
            return null;
        }
        ResultDataSet copy = new ResultDataSet();
        // toCsv 依赖 columns + rows，列结构与原表一致即可
        copy.setColumns(source.getColumns());
        List<Map<String, Object>> rows = source.getRows();
        if (rows == null || rows.isEmpty()) {
            copy.setRows(new ArrayList<Map<String, Object>>());
            return copy;
        }
        int n = Math.min(maxRows, rows.size());
        // subList 包一层 ArrayList，避免持有原 list 的 subList 视图导致后续误改原数据
        copy.setRows(new ArrayList<Map<String, Object>>(rows.subList(0, n)));
        return copy;
    }

}
