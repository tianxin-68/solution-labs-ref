package com.bi.queryer.ssm.portal.ai.vo;

/**
 * @Auther: contributor
 * @Date: 2026/4/7 16:34
 * @Description:
 */
public class AiAnalysisFavRsp extends TemplateRecentVisitRsp{
    private Long favId;

    private String chatId;

    private String chatName;

    /** 最新一条消息内容摘要（用于卡片预览） */
    private String latestMessageContent;


    private Long messageCount;

    private String lastMessageTime;

    private Double favSortId;

    /**
     * 是否为 agent 平台会话：0=否（本地收藏），1=是（agent历史会话）
     */
    private Integer isAgentPlat;

    public Long getFavId() {
        return favId;
    }

    public void setFavId(Long favId) {
        this.favId = favId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public Double getFavSortId() {
        return favSortId;
    }

    public void setFavSortId(Double favSortId) {
        this.favSortId = favSortId;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLatestMessageContent() {
        return latestMessageContent;
    }

    public void setLatestMessageContent(String latestMessageContent) {
        this.latestMessageContent = latestMessageContent;
    }

    public Long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Long messageCount) {
        this.messageCount = messageCount;
    }

    public Integer getIsAgentPlat() {
        return isAgentPlat;
    }

    public void setIsAgentPlat(Integer isAgentPlat) {
        this.isAgentPlat = isAgentPlat;
    }
}
