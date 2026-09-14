package com.bi.queryer.ssm.llm.chatSession.req;

public class ChatUpdateWithSnapshotReq extends ChatCreateWithSnapshotReq{

    private Long chatId;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}
