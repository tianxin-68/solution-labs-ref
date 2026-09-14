package com.bi.queryer.ssm.llm.chatSession.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ChatSessionListItemResp {
    private Long chatId;
    private String chatName;
    private String chatBusinessType;
    private String chatBusinessId;
    private Long lastChatMessageId;
    private Boolean isFavorite;
    private Double favSortId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;

    private String boundViewId;

    private String boundViewName;

    private String lastMessageContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastMessageTime;

    private Integer messageCount;

    private Integer favId;
}
