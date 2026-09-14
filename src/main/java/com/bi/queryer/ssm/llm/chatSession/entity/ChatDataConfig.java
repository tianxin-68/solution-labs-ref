package com.bi.queryer.ssm.llm.chatSession.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatDataConfig {
    private String chatDataId;
    private Long chatId;
    private String dataSnapshotType;
    private String dataSnapshotId;
    private String dataFilterText;
    private Integer isLatest;
    private String createdBy;
    private Date createdTime;
}
