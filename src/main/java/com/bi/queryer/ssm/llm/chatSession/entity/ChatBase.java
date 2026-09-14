package com.bi.queryer.ssm.llm.chatSession.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatBase {
    private Long chatId;
    private String chatName;
    private String chatBusinessType;
    private String chatBusinessId;
    private String boundViewId;
    private Long lastChatMessageId;
    private Double sortId;
    private Integer isActive;
    private String createdBy;
    private Date createdTime;
    private String updatedBy;
    private Date updatedTime;
}
