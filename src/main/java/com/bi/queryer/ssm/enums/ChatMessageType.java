package com.bi.queryer.ssm.enums;

public enum ChatMessageType {

    QA("qa", "问答"),
    SUMMARY("summary", "摘要");

    private String code;

    private String desc;

    ChatMessageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static ChatMessageType get(String code) {
        for (ChatMessageType value : ChatMessageType.values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return QA;
    }
}
