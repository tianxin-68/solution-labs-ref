package com.bi.queryer.ssm.chat.req;

import com.bi.queryer.ssm.chat.entity.AgentPlatChatDataTemplateViewConfig;

import java.util.List;

public class ChatDataRsp {

    private String chatDataId;
    private String chatId;
    private String dataSnapshotType;
    private String dataSnapshotViewId;
    private String createdBy;
    private String createdTime;

    private List<AgentPlatChatDataTemplateViewConfig> results;

    public String getChatDataId() {
        return chatDataId;
    }

    public void setChatDataId(String chatDataId) {
        this.chatDataId = chatDataId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getDataSnapshotType() {
        return dataSnapshotType;
    }

    public void setDataSnapshotType(String dataSnapshotType) {
        this.dataSnapshotType = dataSnapshotType;
    }

    public String getDataSnapshotViewId() {
        return dataSnapshotViewId;
    }

    public void setDataSnapshotViewId(String dataSnapshotViewId) {
        this.dataSnapshotViewId = dataSnapshotViewId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public List<AgentPlatChatDataTemplateViewConfig> getResults() {
        return results;
    }

    public void setResults(List<AgentPlatChatDataTemplateViewConfig> results) {
        this.results = results;
    }
}
