package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

import java.util.List;

@Data
public class ChatSnapshotUpdateReq {
    private Long chatId;
    private String dataSnapshotId;
    private String dataFilterText;
    private List<CreateChatSessionSnapshotReq> dataSnapshots;
}
