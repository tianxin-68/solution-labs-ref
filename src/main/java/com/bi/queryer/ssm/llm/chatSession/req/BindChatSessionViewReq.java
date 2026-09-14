package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

@Data
public class BindChatSessionViewReq {
    private Long chatId;
    private String boundViewId;
}
