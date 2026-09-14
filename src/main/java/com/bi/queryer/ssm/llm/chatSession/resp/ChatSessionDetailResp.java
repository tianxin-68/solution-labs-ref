package com.bi.queryer.ssm.llm.chatSession.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ChatSessionDetailResp {
    private Long chatId;
    private String chatName;
    private String chatBusinessType;
    private String chatBusinessId;
    private String boundViewId;
    private String boundViewName;
    private Long lastChatMessageId;
    private String lastChatMessageAiModel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date updatedTime;

    private ChatSessionDataConfigResp dataConfig;
}
