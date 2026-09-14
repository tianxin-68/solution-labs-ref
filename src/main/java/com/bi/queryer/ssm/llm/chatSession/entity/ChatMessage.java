package com.bi.queryer.ssm.llm.chatSession.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatMessage {
    private Long chatMessageId;
    private String chatMessageType;
    private Integer isSummarized;
    private Long chatId;
    private String chatDataId;
    private String questionContent;
    private String answerContent;
    private Integer answerDurationSeconds;
    private Integer answerStatus;
    private String aiModel;
    private Date chatBeginTime;
    private Date chatEndTime;
    private String createdBy;
    private Date createdTime;
}
