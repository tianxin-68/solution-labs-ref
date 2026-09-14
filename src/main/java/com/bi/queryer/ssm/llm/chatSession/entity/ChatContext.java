package com.bi.queryer.ssm.llm.chatSession.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatContext {
    private Long chatId;
    private Double contextMaxByte;
    private Double contextUsageByte;
    private String createdBy;
    private Date createdTime;
    private String updatedBy;
    private Date updatedTime;
}
