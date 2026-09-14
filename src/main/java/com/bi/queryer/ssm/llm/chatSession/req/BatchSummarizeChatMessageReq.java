package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

import java.util.List;

@Data
public class BatchSummarizeChatMessageReq {
    private List<Long> chatMessageIds;

    private Integer isSummarized;
}
