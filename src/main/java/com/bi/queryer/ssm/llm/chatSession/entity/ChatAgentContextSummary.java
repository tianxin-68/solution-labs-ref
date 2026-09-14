package com.bi.queryer.ssm.llm.chatSession.entity;

import java.util.ArrayList;
import java.util.List;

public class ChatAgentContextSummary {

    private boolean success;
    private String summary;

    private List<String> summarizedMessageIds = new ArrayList<>();
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSummarizedMessageIds() {
        return summarizedMessageIds;
    }

    public void setSummarizedMessageIds(List<String> summarizedMessageIds) {
        this.summarizedMessageIds = summarizedMessageIds;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
