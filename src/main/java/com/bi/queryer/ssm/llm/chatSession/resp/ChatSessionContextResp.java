package com.bi.queryer.ssm.llm.chatSession.resp;

import lombok.Data;

@Data
public class ChatSessionContextResp {
    private Double contextMaxByte;
    private Double contextUsageByte;
}
