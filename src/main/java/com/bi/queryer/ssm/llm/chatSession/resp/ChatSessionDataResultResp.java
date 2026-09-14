package com.bi.queryer.ssm.llm.chatSession.resp;

import lombok.Data;

@Data
public class ChatSessionDataResultResp {
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
