package com.bi.queryer.ssm.llm.chatSession.req;

import java.util.ArrayList;
import java.util.List;

public class CheckSnapshotDataChangeReq {

    private Long chatId;

    /**
     * 查询数据配置
     * 模版AI解答使用
     */
    public String config;

    /**
     * 组件配置
     * 看板AI解答使用
     */
    private List<WidgetConfig> widgetConfigs = new ArrayList<>();

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public List<WidgetConfig> getWidgetConfigs() {
        return widgetConfigs;
    }

    public void setWidgetConfigs(List<WidgetConfig> widgetConfigs) {
        this.widgetConfigs = widgetConfigs;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }


    public static class WidgetConfig {
        private String widgetId;
        private String config;

        public WidgetConfig() {

        }

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
