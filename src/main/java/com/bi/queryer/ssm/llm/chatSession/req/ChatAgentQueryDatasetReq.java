package com.bi.queryer.ssm.llm.chatSession.req;

public class ChatAgentQueryDatasetReq {

    private String url;

    private String name;

    private String description;

    private ChatAgentQueryDataTimeReq dataTime;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChatAgentQueryDataTimeReq getDataTime() {
        return dataTime;
    }

    public void setDataTime(ChatAgentQueryDataTimeReq dataTime) {
        this.dataTime = dataTime;
    }
}
