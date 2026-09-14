package com.bi.queryer.ssm.llm.chatSession.entity;

public class ChatAgentResult {

    private boolean success;

    /**
     * 回答内容
     */
    private String answer;

    /**
     * token使用情况
     */
    private ChatAgentTokenUsage tokenUsage;

    private ChatAgentContextSummary summary;

    private String errorMessage;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public ChatAgentTokenUsage getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(ChatAgentTokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ChatAgentContextSummary getSummary() {
        return summary;
    }

    public void setSummary(ChatAgentContextSummary summary) {
        this.summary = summary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
