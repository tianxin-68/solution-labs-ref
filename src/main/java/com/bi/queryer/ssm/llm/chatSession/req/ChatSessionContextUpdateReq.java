package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

@Data
public class ChatSessionContextUpdateReq {
    private Long chatId;
    private Double contextMaxByte;
    private Double contextUsageByte;
}
