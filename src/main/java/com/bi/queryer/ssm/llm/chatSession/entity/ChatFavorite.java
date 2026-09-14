package com.bi.queryer.ssm.llm.chatSession.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatFavorite {
    private Integer favId;
    private String chatId;
    private Double favSortId;
    private String createdBy;
    private Date createdTime;
    private String updatedBy;
    private Date updatedTime;
}


