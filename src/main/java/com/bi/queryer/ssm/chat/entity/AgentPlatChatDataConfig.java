package com.bi.queryer.ssm.chat.entity;

import java.util.Date;

public class AgentPlatChatDataConfig {

    private String chatDataId;
    private String chatId;
    private String dataSnapshotType;
    private String dataSnapshotViewId;
    private Integer isLatest;
    private String createdBy;
    private Date createdTime;

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

    public Integer getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Integer isLatest) {
        this.isLatest = isLatest;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
