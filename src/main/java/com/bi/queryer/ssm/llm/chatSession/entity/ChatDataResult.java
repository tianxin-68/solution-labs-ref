package com.bi.queryer.ssm.llm.chatSession.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatDataResult {
    private String chatDataId;
    private Long chatId;
    private String dataSnapshotId;
    private String dataQueryViewId;
    private String dataQueryConfig;
    private String dataQueryRemark;
    private String dataContent;
    private String dataPreviewContent;
    private Double dataSize;
    private Integer dataRowCount;
    private Integer dataColumnCount;
}
