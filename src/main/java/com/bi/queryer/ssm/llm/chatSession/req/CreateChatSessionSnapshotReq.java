package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

@Data
public class CreateChatSessionSnapshotReq {
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
