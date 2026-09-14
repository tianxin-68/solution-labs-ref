package com.bi.queryer.sys.enums;

/**
 * 消息类型
 * @author contributor
 */
public enum MsgType {

    Public("公共消息"),
    Private("个人消息"),
    Public_Service("公共服务");

    private String desc;
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

    private MsgType(String desc) {
        this.desc = desc;
    }

    public static MsgType get(String code) {
        for(MsgType e : MsgType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return Public;
    }
}
