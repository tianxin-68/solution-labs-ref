package com.bi.queryer.ssm.llm.chatSession.req;

/**
 * 问答请求体
 */
public class ChatExecuteReq {

    /**
     * 会话id
     */
    public Long chatId;

    /**
     * 提问内容
     */
    public String questionContent;

    /**
     * 模型名称
     */
    public String aiModel;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
}
