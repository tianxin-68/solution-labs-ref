package com.bi.queryer.ssm.portal.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.ChatBusinessType;
import com.bi.queryer.ssm.llm.chatSession.entity.ChatBase;
import com.bi.queryer.ssm.portal.PortalMenuService;
import com.bi.queryer.ssm.portal.ai.entity.AiAnalysisFavEntity;
import com.bi.queryer.ssm.portal.ai.vo.*;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.AnalysisTplType;
import com.bi.queryer.ssm.portal.enums.PortalMenuType;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.portal.template.entity.TmpAnalysisTplCtgRelEntity;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.template.enums.FavTemplateType;
import com.bi.queryer.ssm.query.template.model.TemplateCtgPathEntity;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isEmpty;
import static com.google.common.base.Preconditions.checkArgument;

/** genAI_feature/v3.15.0_start */
@Service
@Scope("prototype")
@Slf4j
public class AiAnalysisService {

    private static final int DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 100;

    @Autowired
    private BaseDao dao;

    @Autowired
    private QueryTemplateCategoryService categoryService;

    @Autowired
    private PortalMenuService portalMenuService;

    public List<TemplateRecentVisitRsp> listRecentVisits(Integer limit) {
        int lim = limit == null ? DEFAULT_LIMIT : limit;
        checkArgument(lim > 0 && lim <= MAX_LIMIT, "limit 需在 1~100 之间");

        String userName = UserManager.get().getName();
        Map<String, Object> params = new HashMap<>(4);
        params.put("userName", userName);
        params.put("limit", lim);

        List<AiRecentAnalysisVisitVO> analysisRows = dao.queryObjectList(
                "ssm.analysis.template.visit.log.getRecentVisitAnalysisList", params, AiRecentAnalysisVisitVO.class);
        List<AiRecentQueryTplVisitVO> queryRows = dao.queryObjectList(
                "ssm.query.getRecentVisitQueryTplList", params, AiRecentQueryTplVisitVO.class);

        int cap = (isEmpty(analysisRows) ? 0 : analysisRows.size()) + (isEmpty(queryRows) ? 0 : queryRows.size());
        List<TemplateRecentVisitRsp> items = new ArrayList<>(Math.max(cap, 8));
        if (!isEmpty(analysisRows)) {
            analysisRows.forEach(row -> items.add(toItemFromAnalysis(row)));
        }
        if (!isEmpty(queryRows)) {
            queryRows.forEach(row -> items.add(toItemFromQuery(row)));
        }

        items.sort((x, y) -> -1 * x.getLastVisitTime().compareTo(y.getLastVisitTime()));
        List<TemplateRecentVisitRsp> res = items.subList(0, Math.min(cap, lim));

        fillItemPaths(res);
        return res;
    }

    public List<TemplateRecentChatRsp> listRecentChats() {
        String userName = UserManager.get().getName();

        // ① 本地逻辑
        List<TemplateRecentChatRsp> localList = new ArrayList<>();
        List<ChatBase> chatBaseSessions = fetchUserAiChatBaseRows(userName);
        if (CollUtil.isNotEmpty(chatBaseSessions)) {
            Map<String, TemplateViewEntity> viewIdToTemplateView =
                    batchLoadQueryTemplateViews(chatBaseSessions);
            Map<String, AnalysisTemplateEntity> analysisTplIdToEntity =
                    batchLoadAnalysisTemplatesByTplId(extractDistinctAnalysisTplIds(chatBaseSessions));

            List<Long> allChatIds = chatBaseSessions.stream()
                    .map(ChatBase::getChatId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Long> lastMessagePointerIds = chatBaseSessions.stream()
                    .map(ChatBase::getLastChatMessageId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            Map<Long, Long> chatIdToMessageCount = aggregateMessageCountByChatId(allChatIds);
            Map<Long, ChatMessageLatestRow> chatIdToLatestMessageRow =
                    aggregateLatestMessagePreviewByChatId(lastMessagePointerIds);
            Map<Long, AiAnalysisFavEntity> chatIdToFav = getChatFav(userName, allChatIds);

            Map<String, TemplateRecentChatRsp> resourceGroupKeyToRecentChatRsp = new HashMap<>();
            for (ChatBase sessionRow : chatBaseSessions) {
                TemplateRecentChatRsp rsp;
                if (ChatBusinessType.QUERY_TEMPLATE.getCode().equals(sessionRow.getChatBusinessType())) {
                    String id = StringUtils.defaultIfEmpty(sessionRow.getBoundViewId(), sessionRow.getChatBusinessId());
                    TemplateViewEntity queryTemplateView = viewIdToTemplateView.get(id);
                    if (queryTemplateView == null) {
                        continue;
                    }

                    rsp = resourceGroupKeyToRecentChatRsp.get(queryTemplateView.getTplId());
                    if (rsp == null) {
                        rsp = new TemplateRecentChatRsp();
                        rsp.setTplId(queryTemplateView.getTplId());
                        rsp.setCtgId(queryTemplateView.getCtgId());
                        rsp.setTplName(queryTemplateView.getTplName());
                        rsp.setViewId(queryTemplateView.getViewId());
                        rsp.setViewName(queryTemplateView.getViewName());
                        rsp.setTplType(sessionRow.getChatBusinessType());
                        resourceGroupKeyToRecentChatRsp.put(queryTemplateView.getTplId(), rsp);
                        localList.add(rsp);
                    }
                } else {
                    AnalysisTemplateEntity analysisTemplate = analysisTplIdToEntity.get(sessionRow.getChatBusinessId());
                    if (analysisTemplate == null) {
                        continue;
                    }
                    rsp = resourceGroupKeyToRecentChatRsp.get(sessionRow.getChatBusinessId());
                    if (rsp == null) {
                        rsp = new TemplateRecentChatRsp();
                        rsp.setTplId(sessionRow.getChatBusinessId());
                        rsp.setTplName(analysisTemplate.getAnalysisTplName());
                        if (ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(sessionRow.getChatBusinessType())) {
                            rsp.setTplType(FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode());
                        } else {
                            rsp.setTplType(sessionRow.getChatBusinessType());
                        }
                        resourceGroupKeyToRecentChatRsp.put(sessionRow.getChatBusinessId(), rsp);
                        localList.add(rsp);
                    }
                }
                List<AiAnalysisRecentChatItemRsp> nestedSessionCards = rsp.getChats();
                if (nestedSessionCards == null) {
                    nestedSessionCards = new ArrayList<>();
                    rsp.setChats(nestedSessionCards);
                }
                AiAnalysisRecentChatItemRsp itemRsp = new AiAnalysisRecentChatItemRsp();
                itemRsp.setChatId(String.valueOf(sessionRow.getChatId()));
                itemRsp.setChatName(sessionRow.getChatName());
                itemRsp.setIsAgentPlat(0);

                ChatMessageLatestRow latestMessageRow = chatIdToLatestMessageRow.get(sessionRow.getChatId());
                if (latestMessageRow != null) {
                    itemRsp.setLatestMessageContent(latestMessageRow.getLatestContent());
                    itemRsp.setLastMessageTime(latestMessageRow.getLastMessageTime());
                }

                AiAnalysisFavEntity fav = chatIdToFav.get(sessionRow.getChatId());
                if (fav != null) {
                    itemRsp.setFavId(fav.getFavId());
                }

                itemRsp.setMessageCount(chatIdToMessageCount.get(sessionRow.getChatId()));
                nestedSessionCards.add(itemRsp);
                rsp.setBusinessId(sessionRow.getChatBusinessId());
                rsp.setChatBusinessType(sessionRow.getChatBusinessType());
            }

            for (TemplateRecentChatRsp recentChatRsp : localList) {
                List<AiAnalysisRecentChatItemRsp> nestedSessions = recentChatRsp.getChats();
                if (CollUtil.isNotEmpty(nestedSessions)) {
                    AiAnalysisRecentChatItemRsp headSessionCard = nestedSessions.get(0);
                    recentChatRsp.setLatestChatName(headSessionCard.getChatName());
                    recentChatRsp.setLatestChatTime(headSessionCard.getLastMessageTime());
                    recentChatRsp.setChatTotalCount(nestedSessions.size());
                }
            }
        }

        // ② agent 来源，异常降级为空
        List<TemplateRecentChatRsp> agentList = Collections.emptyList();
        try {
            agentList = buildRecentChatRspFromAgentSessions(fetchAgentHistorySessions());
        } catch (Exception e) {
            log.warn("fetchAgentHistorySessions failed, skip agent source", e);
        }

        // ③ 按 businessId 合并 chats
        Map<String, TemplateRecentChatRsp> mergedMap = new LinkedHashMap<>();
        List<TemplateRecentChatRsp> nullBusinessIdItems = new ArrayList<>();

        // 先放 local（保留 local 的模板元信息作为主记录）
        for (TemplateRecentChatRsp rsp : localList) {
            String businessId = rsp.getBusinessId();
            if (businessId == null) {
                nullBusinessIdItems.add(rsp);
            } else {
                mergedMap.put(businessId, rsp);
            }
        }

        // 再合并 agent：businessId 已存在则追加 chats，否则直接加入
        for (TemplateRecentChatRsp rsp : agentList) {
            String businessId = rsp.getBusinessId();
            if (businessId == null) {
                nullBusinessIdItems.add(rsp);
                continue;
            }
            if (mergedMap.containsKey(businessId)) {
                TemplateRecentChatRsp existing = mergedMap.get(businessId);
                List<AiAnalysisRecentChatItemRsp> existingChats = existing.getChats();
                if (existingChats == null) {
                    existingChats = new ArrayList<>();
                }
                if (CollUtil.isNotEmpty(rsp.getChats())) {
                    existingChats.addAll(rsp.getChats());
                }
                existing.setChats(existingChats);
            } else {
                mergedMap.put(businessId, rsp);
            }
        }

        // ④ 合并后重新刷新 latestChatName / latestChatTime / chatTotalCount
        List<TemplateRecentChatRsp> merged = new ArrayList<>(mergedMap.values());
        merged.addAll(nullBusinessIdItems);
        for (TemplateRecentChatRsp rsp : merged) {
            List<AiAnalysisRecentChatItemRsp> chats = rsp.getChats();
            if (CollUtil.isNotEmpty(chats)) {
                // chats 内按 lastMessageTime 倒排，取第一条作为最新
                chats.sort((a, b) -> {
                    if (a.getLastMessageTime() == null) {
                        return 1;
                    }
                    if (b.getLastMessageTime() == null) {
                        return -1;
                    }
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                });
                rsp.setLatestChatName(chats.get(0).getChatName());
                rsp.setLatestChatTime(chats.get(0).getLastMessageTime());
                rsp.setChatTotalCount(chats.size());
            }
        }

        // ⑤ 外层按 latestChatTime 倒排，null 排最后
        merged.sort((a, b) -> {
            String ta = a.getLatestChatTime();
            String tb = b.getLatestChatTime();
            if (ta == null && tb == null) {
                return 0;
            }
            if (ta == null) {
                return 1;
            }
            if (tb == null) {
                return -1;
            }
            return tb.compareTo(ta);
        });

        // ⑥ 填充路径结果
        fillItemPaths(merged);
        return merged;
    }

    public List<AgentSessionItem> fetchAgentHistorySessions() {
        String url = SC.v("ssm.agent.base.url") + "/ai/chat/session/history";
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("platform", "ssm");
        reqMap.put("hasBusinessType",true);

        User user = UserManager.get();
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

        String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 30000);
        if (StrUtil.isBlank(response)) {
            log.warn("fetchAgentHistorySessions: 响应为空");
            return Collections.emptyList();
        }
        JSONObject resp = JSONObject.parseObject(response);
        if (resp == null) {
            return Collections.emptyList();
        }
        JSONArray data = resp.getJSONArray("data");
        if (data == null) {
            return Collections.emptyList();
        }
        return data.toJavaList(AgentSessionItem.class);
    }

    public List<TemplateRecentChatRsp> buildRecentChatRspFromAgentSessions(List<AgentSessionItem> agentSessions) {
        if (CollUtil.isEmpty(agentSessions)) {
            return Collections.emptyList();
        }

        List<ChatBase> fakeBases = agentSessions.stream().map(s -> {
            ChatBase b = new ChatBase();
            b.setChatBusinessId(s.getBusinessId());
            b.setChatBusinessType(s.getBusinessType());
            return b;
        }).collect(Collectors.toList());

        Map<String, TemplateViewEntity> viewIdToTemplateView = batchLoadQueryTemplateViews(fakeBases);
        Set<String> analysisTplIds = agentSessions.stream()
                .filter(s -> ChatBusinessType.ANALYSIS_TEMPLATE.getCode().equals(s.getBusinessType())
                        || ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(s.getBusinessType()))
                .map(AgentSessionItem::getBusinessId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, AnalysisTemplateEntity> analysisTplIdToEntity = batchLoadAnalysisTemplatesByTplId(analysisTplIds);

        List<TemplateRecentChatRsp> resultList = new ArrayList<>();
        Map<String, TemplateRecentChatRsp> resourceGroupKeyToRecentChatRsp = new HashMap<>();
        for (AgentSessionItem sessionRow : agentSessions) {
            TemplateRecentChatRsp rsp;
            if (ChatBusinessType.QUERY_TEMPLATE.getCode().equals(sessionRow.getBusinessType())) {
                String id = sessionRow.getBusinessId();
                TemplateViewEntity queryTemplateView = viewIdToTemplateView.get(id);
                if (queryTemplateView == null) {
                    continue;
                }

                rsp = resourceGroupKeyToRecentChatRsp.get(queryTemplateView.getTplId());
                if (rsp == null) {
                    rsp = new TemplateRecentChatRsp();
                    rsp.setTplId(queryTemplateView.getTplId());
                    rsp.setCtgId(queryTemplateView.getCtgId());
                    rsp.setTplName(queryTemplateView.getTplName());
                    rsp.setViewId(queryTemplateView.getViewId());
                    rsp.setViewName(queryTemplateView.getViewName());
                    rsp.setTplType(sessionRow.getBusinessType());
                    resourceGroupKeyToRecentChatRsp.put(queryTemplateView.getTplId(), rsp);
                    resultList.add(rsp);
                }
            } else {
                AnalysisTemplateEntity analysisTemplate = analysisTplIdToEntity.get(sessionRow.getBusinessId());
                if (analysisTemplate == null) {
                    continue;
                }
                rsp = resourceGroupKeyToRecentChatRsp.get(sessionRow.getBusinessId());
                if (rsp == null) {
                    rsp = new TemplateRecentChatRsp();
                    rsp.setTplId(sessionRow.getBusinessId());
                    rsp.setTplName(analysisTemplate.getAnalysisTplName());
                    if (ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(sessionRow.getBusinessType())) {
                        rsp.setTplType(FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode());
                    } else {
                        rsp.setTplType(sessionRow.getBusinessType());
                    }
                    resourceGroupKeyToRecentChatRsp.put(sessionRow.getBusinessId(), rsp);
                    resultList.add(rsp);
                }
            }
            List<AiAnalysisRecentChatItemRsp> nestedSessionCards = rsp.getChats();
            if (nestedSessionCards == null) {
                nestedSessionCards = new ArrayList<>();
                rsp.setChats(nestedSessionCards);
            }
            AiAnalysisRecentChatItemRsp itemRsp = new AiAnalysisRecentChatItemRsp();
            itemRsp.setChatId(sessionRow.getChatId());
            itemRsp.setChatName(sessionRow.getChatName());
            itemRsp.setIsAgentPlat(1);
            itemRsp.setLatestMessageContent(sessionRow.getLastMessageContent());
            itemRsp.setLastMessageTime(sessionRow.getLastMessageTime());
            itemRsp.setMessageCount(sessionRow.getMessageCount() == null ? null : sessionRow.getMessageCount().longValue());
            nestedSessionCards.add(itemRsp);
            rsp.setBusinessId(sessionRow.getBusinessId());
            rsp.setChatBusinessType(sessionRow.getBusinessType());
        }

        for (TemplateRecentChatRsp recentChatRsp : resultList) {
            List<AiAnalysisRecentChatItemRsp> nestedSessions = recentChatRsp.getChats();
            if (CollUtil.isNotEmpty(nestedSessions)) {
                AiAnalysisRecentChatItemRsp headSessionCard = nestedSessions.get(0);
                recentChatRsp.setLatestChatName(headSessionCard.getChatName());
                recentChatRsp.setLatestChatTime(headSessionCard.getLastMessageTime());
                recentChatRsp.setChatTotalCount(nestedSessions.size());
            }
        }

        return resultList;
    }

    /** 拉取当前用户在 AI 场景下的有效 chat_base 记录（含排序，见对应 MyBatis）。 */
    private List<ChatBase> fetchUserAiChatBaseRows(String userName) {
        return dao.queryObjectList(
                "chat.session.base.listChatBaseByUserForAiRecentConversation",
                Collections.singletonMap("userName", userName),
                ChatBase.class);
    }

    /** 从会话行中收集查询模板场景涉及的 view_id（去重）。 */
    private static Set<String> extractDistinctQueryViewIds(List<ChatBase> chatBaseSessions) {
        Set<String> viewIds = new HashSet<>();
        for (ChatBase sessionRow : chatBaseSessions) {
            if (FavTemplateType.QUERY_TEMPLATE.getCode().equals(sessionRow.getChatBusinessType()) && sessionRow.getBoundViewId() != null) {
                viewIds.add(sessionRow.getBoundViewId());
            }
        }
        return viewIds;
    }

    private static Set<String> extractDistinctQueryQueryTplIds(List<ChatBase> chatBaseSessions) {
        Set<String> queryTplIds = new HashSet<>();
        for (ChatBase sessionRow : chatBaseSessions) {
            if (ChatBusinessType.QUERY_TEMPLATE.getCode().equals(sessionRow.getChatBusinessType()) && sessionRow.getBoundViewId() == null) {
                queryTplIds.add(sessionRow.getChatBusinessId());
            }
        }
        return queryTplIds;
    }

    /** 从会话行中收集正式看板场景涉及的 analysis_tpl_id（去重）。 */
    private static Set<String> extractDistinctAnalysisTplIds(List<ChatBase> chatBaseSessions) {
        Set<String> analysisTplIds = new HashSet<>();
        for (ChatBase sessionRow : chatBaseSessions) {
            if (ChatBusinessType.ANALYSIS_TEMPLATE.getCode().equals(sessionRow.getChatBusinessType()) ||
                    ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(sessionRow.getChatBusinessType())) {
                analysisTplIds.add(sessionRow.getChatBusinessId());
            }
        }
        return analysisTplIds;
    }

    /** 按 view_id 批量加载 ssd_query_template_view（及关联展示字段）。 */
    public Map<String, TemplateViewEntity> batchLoadQueryTemplateViews(List<ChatBase> chatBaseSessions) {
        if (CollUtil.isEmpty(chatBaseSessions)) {
            return Collections.emptyMap();
        }

        Set<String> viewIds = extractDistinctQueryViewIds(chatBaseSessions);
        Set<String> queryTplIds = extractDistinctQueryQueryTplIds(chatBaseSessions);
        viewIds.add("_");
        queryTplIds.add("_");
        Map<String, Object> viewQueryParams = new HashMap<>(2);
        viewQueryParams.put("viewIds", viewIds);
        viewQueryParams.put("queryTplIds", queryTplIds);
        List<TemplateViewEntity> viewRows = dao.queryObjectList(
                "ssm.template.view.listByViewIds", viewQueryParams, TemplateViewEntity.class);
        if (CollUtil.isEmpty(viewRows)) {
            return Collections.emptyMap();
        }
        return viewRows.stream()
                .filter(Objects::nonNull)
                .filter(v -> StrUtil.isNotBlank(v.getViewId()) || StrUtil.isNotBlank(v.getTplId()))
                .collect(Collectors.toMap(v -> StringUtils.defaultIfEmpty(v.getViewId(), v.getTplId()), v -> v, (a, b) -> a));
    }

    /** 按 analysis_tpl_id 批量加载 ssm_analysis_tpl_base。 */
    public Map<String, AnalysisTemplateEntity> batchLoadAnalysisTemplatesByTplId(Set<String> analysisTplIds) {
        if (CollUtil.isEmpty(analysisTplIds)) {
            return Collections.emptyMap();
        }
        Map<String, Object> analysisQueryParams = new HashMap<>(2);
        analysisQueryParams.put("analysisTplIds", new ArrayList<>(analysisTplIds));
        List<AnalysisTemplateEntity> templateRows = dao.queryObjectList(
                "ssm.analysisTemplate.getTemplateBaseList", analysisQueryParams, AnalysisTemplateEntity.class);
        if (CollUtil.isEmpty(templateRows)) {
            return Collections.emptyMap();
        }
        return templateRows.stream()
                .filter(Objects::nonNull)
                .filter(b -> StrUtil.isNotBlank(b.getAnalysisTplId()))
                .collect(Collectors.toMap(AnalysisTemplateEntity::getAnalysisTplId, v -> v, (a, b) -> a));
    }

    /** chat_message 按 chat_id 统计条数。 */
    public Map<Long, Long> aggregateMessageCountByChatId(List<Long> chatIds) {
        if (CollUtil.isEmpty(chatIds)) {
            return Collections.emptyMap();
        }
        Map<String, Object> countQueryParams = new HashMap<>(2);
        countQueryParams.put("chatIds", chatIds);
        List<ChatMessageStatRow> statRows = dao.queryObjectList(
                "chat.session.message.countMessagesByChatIds", countQueryParams, ChatMessageStatRow.class);
        if (CollUtil.isEmpty(statRows)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> chatIdToCount = new HashMap<>();
        for (ChatMessageStatRow statRow : statRows) {
            if (statRow != null && statRow.getChatId() != null && statRow.getMessageCount() != null) {
                chatIdToCount.put(statRow.getChatId(), statRow.getMessageCount());
            }
        }
        return chatIdToCount;
    }

    private Map<Long, AiAnalysisFavEntity> getChatFav(String userName, List<Long> chatIds) {
        if (CollUtil.isEmpty(chatIds)) {
            return Collections.emptyMap();
        }
        Map<String, Object> queryParams = new HashMap<>(2);
        queryParams.put("chatIds", chatIds);
        queryParams.put("userName", userName);
        List<AiAnalysisFavEntity> favEntities = dao.queryObjectList(
                "ssm.ai.analysis.fav.getFavByChatIds", queryParams, AiAnalysisFavEntity.class);
        if (CollUtil.isEmpty(favEntities)) {
            return Collections.emptyMap();
        }
        Map<Long, AiAnalysisFavEntity> res = new HashMap<>();
        for (AiAnalysisFavEntity entity : favEntities) {
            res.put(entity.getChatId(), entity);
        }
        return res;
    }

    /** 按 chat 维度取最新一条消息摘要（参数名与 Mapper 一致）。 */
    public Map<Long, ChatMessageLatestRow> aggregateLatestMessagePreviewByChatId(List<Long> lastChatMessageIds) {
        if (CollUtil.isEmpty(lastChatMessageIds)) {
            return Collections.emptyMap();
        }
        Map<String, Object> latestMessageQueryParams = new HashMap<>(2);
        latestMessageQueryParams.put("lastChatMessageIds", lastChatMessageIds);
        List<ChatMessageLatestRow> latestRows = dao.queryObjectList(
                "chat.session.message.listLatestMessagePerChatIds", latestMessageQueryParams, ChatMessageLatestRow.class);
        if (CollUtil.isEmpty(latestRows)) {
            return Collections.emptyMap();
        }
        Map<Long, ChatMessageLatestRow> chatIdToLatestRow = new HashMap<>();
        for (ChatMessageLatestRow latestRow : latestRows) {
            if (latestRow != null && latestRow.getChatId() != null) {
                chatIdToLatestRow.put(latestRow.getChatId(), latestRow);
            }
        }
        return chatIdToLatestRow;
    }

    private static TemplateRecentVisitRsp toItemFromAnalysis(AiRecentAnalysisVisitVO row) {
        TemplateRecentVisitRsp item = new TemplateRecentVisitRsp();
        item.setTplId(row.getAnalysisTplId());
        item.setTplName(row.getAnalysisTplName());
        item.setTplType(AnalysisTplType.TMP.getCode().equals(row.getAnalysisTplType()) ?
                FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode() : FavTemplateType.ANALYSIS_TEMPLATE.getCode());
        item.setLastVisitTime(row.getLastVisitTime());
        return item;
    }

    private static TemplateRecentVisitRsp toItemFromQuery(AiRecentQueryTplVisitVO row) {
        TemplateRecentVisitRsp item = new TemplateRecentVisitRsp();
        item.setTplId(row.getTplId());
        item.setTplName(row.getTplName());
        item.setTplType(FavTemplateType.QUERY_TEMPLATE.getCode());
        item.setCtgId(row.getCtgId());
        item.setViewId(row.getViewId());
        item.setViewName(row.getViewName());
        item.setLastVisitTime(row.getLastVisitTime());
        return item;
    }

    /**
     * 统一补全展示路径：<br/>
     * 1）临时看板：ssd_tmp_analysis_tpl_ctg_rel → ctgId；<br/>
     * 2）查询模板 + 临时看板：{@link QueryTemplateCategoryService#getCtgPath(List)} → ctgNamePath；<br/>
     * 3）正式看板：{@link PortalMenuService#getPortalMenuByRefIds} → menuUrl → ctgNamePath
     */
    public void fillItemPaths(List<? extends TemplateRecentVisitRsp> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        fillTmpAnalysisCtgId(items);
        applyCtgNamePathByCtgIds(items);
        setAnalysisTplMenuPath(items);
    }

    private void fillTmpAnalysisCtgId(List<? extends TemplateRecentVisitRsp> items) {
        Set<String> tmpTplIds = items.stream()
                .filter(v -> FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode().equals(v.getTplType()))
                .map(TemplateRecentVisitRsp::getTplId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(tmpTplIds)) {
            return;
        }
        Map<String, Object> param = Collections.singletonMap("analysisTplIds", tmpTplIds);
        List<TmpAnalysisTplCtgRelEntity> rels = dao.queryObjectList(
                "ssd.tmpAnalysisTplCtgRel.listCtgRelByAnalysisTplIds", param, TmpAnalysisTplCtgRelEntity.class);
        if (CollUtil.isEmpty(rels)) {
            return;
        }
        Map<String, String> tplIdToCtg = rels.stream()
                .filter(Objects::nonNull)
                .filter(r -> StrUtil.isNotBlank(r.getAnalysisTplId()))
                .collect(Collectors.toMap(TmpAnalysisTplCtgRelEntity::getAnalysisTplId, TmpAnalysisTplCtgRelEntity::getCtgId, (a, b) -> a));
        for (TemplateRecentVisitRsp item : items) {
            String tplId = item.getTplId();
            if (StrUtil.isBlank(tplId) || !FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode().equals(item.getTplType())) {
                continue;
            }
            String ctgId = tplIdToCtg.get(tplId);
            if (StrUtil.isBlank(ctgId)) {
                continue;
            }
            item.setCtgId(ctgId);
        }
    }

    private void applyCtgNamePathByCtgIds(List<? extends TemplateRecentVisitRsp> items) {
        List<String> ctgIds = items.stream()
                .map(TemplateRecentVisitRsp::getCtgId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(ctgIds)) {
            return;
        }
        List<TemplateCtgPathEntity> pathEntities = categoryService.getCtgPath(ctgIds);
        if (CollUtil.isEmpty(pathEntities)) {
            return;
        }
        Map<String, String> ctgIdToNamePath = pathEntities.stream()
                .filter(Objects::nonNull)
                .filter(p -> StrUtil.isNotBlank(p.getCtgId()))
                .collect(Collectors.toMap(TemplateCtgPathEntity::getCtgId, TemplateCtgPathEntity::getCtgNamePath, (a, b) -> a));
        for (TemplateRecentVisitRsp item : items) {
            String ctgId = item.getCtgId();
            if (StrUtil.isBlank(ctgId) || FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(item.getTplType())) {
                continue;
            }
            item.setCtgNamePath(ctgIdToNamePath.get(ctgId));
        }
    }

    private void setAnalysisTplMenuPath(List<? extends TemplateRecentVisitRsp> items) {
        Set<String> tplIds = items.stream()
                .filter(v -> FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(v.getTplType()))
                .map(TemplateRecentVisitRsp::getTplId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(tplIds)) {
            return;
        }
        List<PortalMenu> menus = portalMenuService.getPortalMenuByRefIds(
                Collections.singletonList(PortalMenuType.ANALYSIS_TEMPLATE.getCode()),
                new ArrayList<>(tplIds));
        if (CollUtil.isEmpty(menus)) {
            return;
        }
        Map<String, PortalMenu> refToMenuUrl = menus.stream()
                .filter(Objects::nonNull)
                .filter(m -> StrUtil.isNotBlank(m.getContentRefId()))
                .collect(Collectors.toMap(PortalMenu::getContentRefId, v -> v, (a, b) -> a));
        for (TemplateRecentVisitRsp item : items) {
            String tplId = item.getTplId();
            if (StrUtil.isBlank(tplId) || !FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(item.getTplType())) {
                continue;
            }
            PortalMenu menu = refToMenuUrl.get(tplId);
            if (menu == null) {
                continue;
            }
            item.setCtgId(menu.getMenuId());
            item.setCtgNamePath(menu.getPortalName() + menu.getMenuUrl());
        }
    }
}
/** genAI_feature/v3.15.0_end */
