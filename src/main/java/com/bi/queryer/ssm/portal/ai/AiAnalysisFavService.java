package com.bi.queryer.ssm.portal.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.ChatBusinessType;
import com.bi.queryer.ssm.llm.chatSession.entity.ChatBase;
import com.bi.queryer.ssm.portal.ai.entity.AiAnalysisFavEntity;
import com.bi.queryer.ssm.portal.ai.vo.AgentSessionItem;
import com.bi.queryer.ssm.portal.ai.vo.AiAnalysisFavReq;
import com.bi.queryer.ssm.portal.ai.vo.AiAnalysisFavRsp;
import com.bi.queryer.ssm.portal.ai.vo.ChatMessageLatestRow;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.query.template.enums.FavTemplateType;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * AI 会话收藏
 */
/** genAI_feature/v3.15.0_start */
@Service
@Scope("prototype")
@Slf4j
public class AiAnalysisFavService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    public void addFav(AiAnalysisFavReq req) {
        checkArgument(req != null, "参数为空");
        checkArgument(req.getChatId() != null, "会话id为空");

        User user = UserManager.get();
        String userName = user.getName();

        Map<String, Object> q = new HashMap<>(4);
        q.put("userName", userName);
        q.put("chatId", req.getChatId());
        Integer cnt = dao.queryObject("ssm.ai.analysis.fav.countByUserAndChat", q, Integer.class);
        checkState(cnt == null || cnt == 0, "已收藏该会话");

        Double nextSort = dao.queryObject("ssm.ai.analysis.fav.selectNextFavSortId", q, Double.class);

        AiAnalysisFavEntity entity = new AiAnalysisFavEntity();
        entity.setChatId(req.getChatId());
        entity.setFavSortId(nextSort);
        entity.setCreatedBy(userName);
        entity.setUpdatedBy(userName);
        dao.insert("ssm.ai.analysis.fav.insert", entity);
    }

    public void removeFav(AiAnalysisFavReq req) {
        checkArgument(req != null, "参数为空");
        checkArgument(req.getChatId() != null, "会话id为空");

        User user = UserManager.get();
        Map<String, Object> q = new HashMap<>(4);
        q.put("userName", user.getName());
        q.put("chatId", req.getChatId());
        dao.delete("ssm.ai.analysis.fav.deleteByUserAndChat", q);
    }

    /**
     * 收藏列表：仅返回 chat_base 中仍存在且未逻辑删除的会话，并补全模板/视图/看板与路径信息；
     * 对已失效的收藏记录从库中删除。
     */
    public List<AiAnalysisFavRsp> listFav() {
        User user = UserManager.get();
        String userName = user.getName();
        Map<String, Object> q = new HashMap<>(2);
        q.put("userName", userName);
        List<AiAnalysisFavEntity> favList = dao.queryObjectList("ssm.ai.analysis.fav.listByUser", q, AiAnalysisFavEntity.class);

        // ① 本地收藏
        List<AiAnalysisFavRsp> localFavList = new ArrayList<>();
        if (CollUtil.isNotEmpty(favList)) {
            List<Long> distinctChatIds = favList.stream()
                    .map(AiAnalysisFavEntity::getChatId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(distinctChatIds)) {
                Set<Long> existingActiveChatIds = fetchExistingActiveChatIdsForUser(userName, distinctChatIds);
                List<AiAnalysisFavEntity> validFavList = new ArrayList<>(favList.size());
                for (AiAnalysisFavEntity fav : favList) {
                    Long chatId = fav.getChatId();
                    if (chatId == null) {
                        continue;
                    }
                    if (existingActiveChatIds.contains(chatId)) {
                        validFavList.add(fav);
                    }
                }

                if (CollUtil.isNotEmpty(validFavList)) {
                    List<Long> validChatIds = validFavList.stream()
                            .map(AiAnalysisFavEntity::getChatId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    List<ChatBase> chatBaseList = loadChatBasesByChatIdsForUser(userName, validChatIds);
                    Map<Long, ChatBase> chatIdToSession = chatBaseList.stream()
                            .filter(Objects::nonNull)
                            .filter(c -> c.getChatId() != null)
                            .collect(Collectors.toMap(ChatBase::getChatId, c -> c, (a, b) -> a));

                    Set<String> analysisTplIds = new HashSet<>();
                    for (ChatBase session : chatBaseList) {
                        if (ChatBusinessType.ANALYSIS_TEMPLATE.getCode().equals(session.getChatBusinessType())
                                || ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(session.getChatBusinessType())) {
                            analysisTplIds.add(session.getChatBusinessId());
                        }
                    }
                    Map<String, TemplateViewEntity> viewIdToTemplateView = aiAnalysisService.batchLoadQueryTemplateViews(chatBaseList);
                    Map<String, AnalysisTemplateEntity> analysisTplIdToEntity = aiAnalysisService.batchLoadAnalysisTemplatesByTplId(analysisTplIds);

                    for (AiAnalysisFavEntity fav : validFavList) {
                        ChatBase session = chatIdToSession.get(fav.getChatId());
                        if (session == null) {
                            continue;
                        }
                        AiAnalysisFavRsp rsp = new AiAnalysisFavRsp();
                        rsp.setFavId(fav.getFavId());
                        rsp.setChatId(String.valueOf(fav.getChatId()));
                        rsp.setFavSortId(fav.getFavSortId());
                        fillTemplateFieldsFromSession(rsp, session, viewIdToTemplateView, analysisTplIdToEntity);
                        localFavList.add(rsp);
                    }

                    List<Long> lastMessagePointerIds = chatBaseList.stream()
                            .map(ChatBase::getLastChatMessageId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    Map<Long, Long> chatIdToMessageCount = aiAnalysisService.aggregateMessageCountByChatId(validChatIds);
                    Map<Long, ChatMessageLatestRow> chatIdToLatestMessage =
                            aiAnalysisService.aggregateLatestMessagePreviewByChatId(lastMessagePointerIds);
                    for (AiAnalysisFavRsp rsp : localFavList) {
                        if (StrUtil.isBlank(rsp.getChatId())) {
                            continue;
                        }
                        Long cid = Long.parseLong(rsp.getChatId());
                        rsp.setMessageCount(chatIdToMessageCount.get(cid));
                        ChatMessageLatestRow latest = chatIdToLatestMessage.get(cid);
                        if (latest != null) {
                            rsp.setLatestMessageContent(latest.getLatestContent());
                            rsp.setLastMessageTime(latest.getLastMessageTime());
                        }
                    }
                }
            }
        }
        localFavList.forEach(rsp -> rsp.setIsAgentPlat(0));

        // ② agent 历史会话，异常降级为空
        List<AiAnalysisFavRsp> agentFavList = Collections.emptyList();
        try {
            agentFavList = buildFavRspFromAgentSessions(fetchAgentFavSessions());
        } catch (Exception e) {
            log.warn("fetchAgentFavSessions failed, skip agent source", e);
        }

        // ③ 直接相加，无去重
        List<AiAnalysisFavRsp> merged = new ArrayList<>(localFavList);
        merged.addAll(agentFavList);

        aiAnalysisService.fillItemPaths(merged);
        return merged;
    }

    private void fillTemplateFieldsFromSession(
            AiAnalysisFavRsp rsp,
            ChatBase session,
            Map<String, TemplateViewEntity> viewIdToTemplateView,
            Map<String, AnalysisTemplateEntity> analysisTplIdToEntity) {
        String businessType = session.getChatBusinessType();
        rsp.setTplType(businessType);
        if (ChatBusinessType.QUERY_TEMPLATE.getCode().equals(businessType)) {
            TemplateViewEntity view = viewIdToTemplateView.get(StringUtils.defaultIfEmpty(session.getBoundViewId(), session.getChatBusinessId()));
            if (view != null) {
                rsp.setTplId(view.getTplId());
                rsp.setViewId(view.getViewId());
                rsp.setViewName(view.getViewName());
                rsp.setTplName(StrUtil.emptyToDefault(view.getTplName(), view.getViewName()));
                rsp.setCtgId(view.getCtgId());
            } else {
                rsp.setViewId(session.getChatBusinessId());
            }
        } else if (ChatBusinessType.ANALYSIS_TEMPLATE.getCode().equals(businessType)) {
            AnalysisTemplateEntity tpl = analysisTplIdToEntity.get(session.getChatBusinessId());
            if (tpl != null) {
                rsp.setTplId(tpl.getAnalysisTplId());
                rsp.setTplName(tpl.getAnalysisTplName());
            } else {
                rsp.setTplId(session.getChatBusinessId());
            }
        } else if (ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(businessType)) {
            rsp.setTplType(FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode());
            AnalysisTemplateEntity tpl = analysisTplIdToEntity.get(session.getChatBusinessId());
            if (tpl != null) {
                rsp.setTplId(tpl.getAnalysisTplId());
                rsp.setTplName(tpl.getAnalysisTplName());
            } else {
                rsp.setTplId(session.getChatBusinessId());
            }
        }
        rsp.setChatName(session.getChatName());
    }

    private List<AgentSessionItem> fetchAgentFavSessions() {
        String url = SC.v("ssm.agent.base.url") + "/ai/chat/session/history";

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("platform", "ssm");
        reqMap.put("isFav", 1);
        reqMap.put("hasBusinessType",true);

        User user = UserManager.get();
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

        String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 30000);
        if (StrUtil.isBlank(response)) {
            log.warn("fetchAgentFavSessions: 响应为空");
            return Collections.emptyList();
        }
        JSONObject resp = JSONObject.parseObject(response);
        if (resp == null) {
            log.warn("fetchAgentFavSessions: 响应解析失败");
            return Collections.emptyList();
        }
        JSONArray data = resp.getJSONArray("data");
        if (data == null) {
            return Collections.emptyList();
        }
        return data.toJavaList(AgentSessionItem.class);
    }

    private List<AiAnalysisFavRsp> buildFavRspFromAgentSessions(List<AgentSessionItem> agentSessions) {
        if (CollUtil.isEmpty(agentSessions)) {
            return Collections.emptyList();
        }

        List<ChatBase> fakeBases = agentSessions.stream().map(s -> {
            ChatBase b = new ChatBase();
            b.setChatBusinessId(s.getBusinessId());
            b.setChatBusinessType(s.getBusinessType());
            return b;
        }).collect(Collectors.toList());

        Map<String, TemplateViewEntity> viewMap = aiAnalysisService.batchLoadQueryTemplateViews(fakeBases);
        Set<String> analysisTplIds = agentSessions.stream()
                .filter(s -> ChatBusinessType.ANALYSIS_TEMPLATE.getCode().equals(s.getBusinessType())
                        || ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(s.getBusinessType()))
                .map(AgentSessionItem::getBusinessId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, AnalysisTemplateEntity> tplMap = aiAnalysisService.batchLoadAnalysisTemplatesByTplId(analysisTplIds);

        List<AiAnalysisFavRsp> result = new ArrayList<>();
        for (AgentSessionItem s : agentSessions) {
            AiAnalysisFavRsp rsp = new AiAnalysisFavRsp();
            rsp.setChatId(s.getChatId());
            rsp.setChatName(s.getChatName());
            rsp.setLatestMessageContent(s.getLastMessageContent());
            rsp.setLastMessageTime(s.getLastMessageTime());
            rsp.setMessageCount(s.getMessageCount() == null ? null : s.getMessageCount().longValue());
            rsp.setIsAgentPlat(1);
            fillTemplateFieldsFromAgentSession(rsp, s, viewMap, tplMap);
            result.add(rsp);
        }
        return result;
    }

    private void fillTemplateFieldsFromAgentSession(
            AiAnalysisFavRsp rsp,
            AgentSessionItem session,
            Map<String, TemplateViewEntity> viewIdToTemplateView,
            Map<String, AnalysisTemplateEntity> analysisTplIdToEntity) {
        String businessType = session.getBusinessType();
        rsp.setTplType(businessType);
        if (ChatBusinessType.QUERY_TEMPLATE.getCode().equals(businessType)) {
            TemplateViewEntity view = viewIdToTemplateView.get(session.getBusinessId());
            if (view != null) {
                rsp.setTplId(view.getTplId());
                rsp.setViewId(view.getViewId());
                rsp.setViewName(view.getViewName());
                rsp.setTplName(StrUtil.emptyToDefault(view.getTplName(), view.getViewName()));
                rsp.setCtgId(view.getCtgId());
            } else {
                rsp.setViewId(session.getBusinessId());
            }
        } else if (ChatBusinessType.ANALYSIS_TEMPLATE.getCode().equals(businessType)) {
            AnalysisTemplateEntity tpl = analysisTplIdToEntity.get(session.getBusinessId());
            if (tpl != null) {
                rsp.setTplId(tpl.getAnalysisTplId());
                rsp.setTplName(tpl.getAnalysisTplName());
            } else {
                rsp.setTplId(session.getBusinessId());
            }
        } else if (ChatBusinessType.ANALYSIS_TMP_TEMPLATE.getCode().equals(businessType)) {
            rsp.setTplType(FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode());
            AnalysisTemplateEntity tpl = analysisTplIdToEntity.get(session.getBusinessId());
            if (tpl != null) {
                rsp.setTplId(tpl.getAnalysisTplId());
                rsp.setTplName(tpl.getAnalysisTplName());
            } else {
                rsp.setTplId(session.getBusinessId());
            }
        }
        rsp.setChatName(session.getChatName());
    }

    private List<ChatBase> loadChatBasesByChatIdsForUser(String userName, List<Long> chatIds) {
        if (CollUtil.isEmpty(chatIds)) {
            return Collections.emptyList();
        }
        Map<String, Object> params = new HashMap<>(4);
        params.put("userName", userName);
        params.put("chatIds", chatIds);
        return dao.queryObjectList("chat.session.base.listChatBaseByChatIdsForUser", params, ChatBase.class);
    }

    private Set<Long> fetchExistingActiveChatIdsForUser(String userName, List<Long> chatIds) {
        Map<String, Object> params = new HashMap<>(4);
        params.put("userName", userName);
        params.put("chatIds", chatIds);
        List<ChatBase> rows = dao.queryObjectList(
                "chat.session.base.listChatBaseByChatIdsForUser", params, ChatBase.class);
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptySet();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .map(ChatBase::getChatId)
                .collect(Collectors.toSet());
    }
}
/** genAI_feature/v3.15.0_end */
