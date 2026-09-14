package com.bi.queryer.ssm.llm.chatSession.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ChatSessionDataConfigResp {
    private String chatDataId;
    private Long chatId;
    private String dataSnapshotType;
    private String snapshotId;
    private String dataQueryConfig;
    private String dataFilterText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;

    private List<ChatSessionDataResultResp> dataResults;
}
