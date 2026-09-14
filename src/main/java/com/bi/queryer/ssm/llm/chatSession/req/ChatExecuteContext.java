package com.bi.queryer.ssm.llm.chatSession.req;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ChatExecuteContext {

    private Long chatId;

    private SseEmitter emitter;

    private String token ;

    private String userName;

    private String chatDataId;

    public SseEmitter getEmitter() {
        return emitter;
    }

    public void setEmitter(SseEmitter emitter) {
        this.emitter = emitter;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getChatDataId() {
        return chatDataId;
    }

    public void setChatDataId(String chatDataId) {
        this.chatDataId = chatDataId;
    }
}
