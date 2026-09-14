package com.bi.queryer.ssm.enums;

public enum TerminalType {

    PC("pc","pc"),
    MOBILE("mobile","mobile") ;

    private String code;

    private String desc;

    TerminalType(String code, String desc) {
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

    public static TerminalType get(String code) {
        for (TerminalType t : TerminalType.values()) {
            if (t.getCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return PC;
    }

}
