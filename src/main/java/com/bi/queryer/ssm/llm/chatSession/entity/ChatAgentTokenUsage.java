package com.bi.queryer.ssm.llm.chatSession.entity;

public class ChatAgentTokenUsage {

    private Long maxTokens;

    private Long inputTokens;

    private Long outputTokens;

    private Long totalTokens;

    public Long getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Long maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }
}
