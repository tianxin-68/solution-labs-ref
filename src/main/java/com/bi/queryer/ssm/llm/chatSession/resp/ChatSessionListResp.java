package com.bi.queryer.ssm.llm.chatSession.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatSessionListResp {
    private Integer total = 0;
    private List<ChatSessionListItemResp> rows = new ArrayList<>();
}


