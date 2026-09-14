package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

import java.util.List;

@Data
public class SaveChatSummarizedMessageReq {
    private Long chatId;
    private String chatDataId;
    private String summarizedContent;
    private List<Long> chatMessageIds;
}
