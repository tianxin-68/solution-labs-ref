package com.bi.queryer.ssm.llm.chatSession.req;

import java.util.ArrayList;
import java.util.List;

public class ChatAgentQueryReq {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话滚动摘要
     */
    private String summary;

    /**
     * 提问内容
     */
    private String query;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 会话数据快照id
     */
    private String chatDataId;

    private List<ChatAgentQueryDatasetReq> datasets = new ArrayList<>();

    private List<ChatAgentQueryMessageReq> messages = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public List<ChatAgentQueryDatasetReq> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<ChatAgentQueryDatasetReq> datasets) {
        this.datasets = datasets;
    }

    public List<ChatAgentQueryMessageReq> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatAgentQueryMessageReq> messages) {
        this.messages = messages;
    }

    public String getChatDataId() {
        return chatDataId;
    }

    public void setChatDataId(String chatDataId) {
        this.chatDataId = chatDataId;
    }
}
