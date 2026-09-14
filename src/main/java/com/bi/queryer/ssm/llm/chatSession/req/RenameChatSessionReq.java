package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

@Data
public class RenameChatSessionReq {
    private Long chatId;
    private String chatName;
}


