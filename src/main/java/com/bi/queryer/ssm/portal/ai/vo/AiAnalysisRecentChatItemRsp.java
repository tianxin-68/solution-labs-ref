package com.bi.queryer.ssm.portal.ai.vo;


public class AiAnalysisRecentChatItemRsp {
    private String chatId;
    private String chatName;
    /** 最新一条消息内容摘要（用于卡片预览） */
    private String latestMessageContent;
    private Long messageCount;
    /** yyyy-MM-dd HH:mm:ss */
    private String lastMessageTime;

    private Long favId;

    /**
     * 是否为 agent 平台会话：0=否（本地），1=是（agent历史会话）
     */
    private Integer isAgentPlat;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
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

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Long getFavId() {
        return favId;
    }

    public void setFavId(Long favId) {
        this.favId = favId;
    }

    public Integer getIsAgentPlat() {
        return isAgentPlat;
    }

    public void setIsAgentPlat(Integer isAgentPlat) {
        this.isAgentPlat = isAgentPlat;
    }
}
