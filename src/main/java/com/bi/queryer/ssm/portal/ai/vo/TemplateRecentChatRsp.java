package com.bi.queryer.ssm.portal.ai.vo;

import java.util.List;


public class TemplateRecentChatRsp extends TemplateRecentVisitRsp {
    /** 对应 chat_base.chat_business_id（全局唯一） */
    private String businessId;
    /** 对应 chat_base.chat_business_type，如 query_template / analysis_template / tmp_analysis_template */
    private String chatBusinessType;
    /** 该资源下会话总数 */
    private Integer chatTotalCount;
    /** 最新一条会话名称（用于「最新对话：xxx」） */
    private String latestChatName;
    /** 最新一条会话时间 */
    private String latestChatTime;
    /** 该资源下全部会话卡片（顺序与 SQL listChatBaseByUserForAiRecentConversation 中按 last_chat_message_id 排序一致） */
    private List<AiAnalysisRecentChatItemRsp> chats;

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getChatBusinessType() {
        return chatBusinessType;
    }

    public void setChatBusinessType(String chatBusinessType) {
        this.chatBusinessType = chatBusinessType;
    }

    public Integer getChatTotalCount() {
        return chatTotalCount;
    }

    public void setChatTotalCount(Integer chatTotalCount) {
        this.chatTotalCount = chatTotalCount;
    }

    public String getLatestChatName() {
        return latestChatName;
    }

    public void setLatestChatName(String latestChatName) {
        this.latestChatName = latestChatName;
    }

    public String getLatestChatTime() {
        return latestChatTime;
    }

    public void setLatestChatTime(String latestChatTime) {
        this.latestChatTime = latestChatTime;
    }

    public List<AiAnalysisRecentChatItemRsp> getChats() {
        return chats;
    }

    public void setChats(List<AiAnalysisRecentChatItemRsp> chats) {
        this.chats = chats;
    }

}
