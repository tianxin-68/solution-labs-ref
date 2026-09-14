package com.bi.queryer.ssm.llm.chatSession.req;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SaveChatMessageReq {
    private Long chatId;
    private String chatDataId;
    private String messageType;
    private String aiModel;
    private Long contextUsageByte;
    private Long contextTotalByte;
    private String questionContent;
    private String answerContent;
    private Date chatBeginTime;
    private Date chatEndTime;
    private Integer answerDurationSeconds;
    private Integer answerStatus;
    /** 未传时由服务层默认 0 */
    private Integer isSummarized;
    private List<Long> summarizedMsgIds;
}
