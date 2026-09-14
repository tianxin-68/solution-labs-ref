package com.bi.queryer.ssm.chat.req;

import java.util.ArrayList;
import java.util.List;

public class ChatCheckSnapshotDataChangeReq {

    /**
     * 会话ID
     */
    private String chatId;

    /**
     * 查询数据配置（单视图场景）
     */
    private String config;

    /**
     * 组件配置列表（看板场景）
     */
    private List<WidgetConfig> widgetConfigs = new ArrayList<>();

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public List<WidgetConfig> getWidgetConfigs() {
        return widgetConfigs;
    }

    public void setWidgetConfigs(List<WidgetConfig> widgetConfigs) {
        this.widgetConfigs = widgetConfigs;
    }

    public static class WidgetConfig {

        private String widgetId;
        private String config;

        public String getWidgetId() {
            return widgetId;
        }

        public void setWidgetId(String widgetId) {
            this.widgetId = widgetId;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }
    }
}
