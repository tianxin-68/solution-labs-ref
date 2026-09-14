package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

import java.util.List;

@Data
public class CreateChatSessionReq {
    private String chatName;
    private String chatBusinessType;
    private String chatBusinessId;
    private String lastUsedAiModel;
    private String dataSnapshotId;
    private String dataFilterText;
    private List<CreateChatSessionSnapshotReq> dataSnapshots;
}

