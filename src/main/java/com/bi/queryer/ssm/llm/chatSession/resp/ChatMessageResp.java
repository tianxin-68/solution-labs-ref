package com.bi.queryer.ssm.llm.chatSession.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ChatMessageResp {
    private Long chatMessageId;
    private Long chatId;
    private String chatDataId;
    private String questionContent;
    private String answerContent;
    private Integer answerDurationSeconds;
    private Integer answerStatus;
    private String aiModel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date chatBeginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date chatEndTime;

    private Integer isSummarized;
    private String chatMessageType;
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;
}
