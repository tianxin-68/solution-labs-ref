package com.bi.queryer.ssm.enums;

public enum CalendarType {

    NATURAL("natural","自然日历"),
    BUSINESS("business","业务日历");

    private String code;

    private String desc;

    CalendarType(String code, String desc){
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

    public static CalendarType get(String code) {
        for (CalendarType t : values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return NATURAL;
    }

}
