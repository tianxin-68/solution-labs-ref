package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

@Data
public class ChatSessionListQueryReq {
    private Integer favorite = 0;
    private Integer pageNo = 1;
    private Integer pageSize = 99999;
    private String chatBusinessType;
    private String chatBusinessId;
    private String boundViewId;
}


