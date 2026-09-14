package com.bi.queryer.ssm.llm.chatSession.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatUserConfig {
    private Long pkid;
    private String userName;
    private String lastUsedAiModel;
    private String createdBy;
    private Date createdTime;
}

