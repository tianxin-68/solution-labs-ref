package com.bi.queryer.ssm.chat.entity;

public class AgentPlatChatDataTemplateViewConfig {

    private String chatDataId;
    private String chatId;
    private String dataSnapshotId;
    private String dataSnapshotQueryConfig;
    private String widgetId;

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

    public String getDataSnapshotId() {
        return dataSnapshotId;
    }

    public void setDataSnapshotId(String dataSnapshotId) {
        this.dataSnapshotId = dataSnapshotId;
    }

    public String getDataSnapshotQueryConfig() {
        return dataSnapshotQueryConfig;
    }

    public void setDataSnapshotQueryConfig(String dataSnapshotQueryConfig) {
        this.dataSnapshotQueryConfig = dataSnapshotQueryConfig;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }
}
