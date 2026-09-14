package com.bi.queryer.sys.enums;

/**
 * 消息显示方式
 * @author contributor
 */
public enum MsgShowType {

    Link("只显示超链接（点击弹出新窗口）"),
    Attachment("只显示附件（点击会直接下载）"),
    All("展示全部（点击弹出详情）"),
    Menu("内联菜单");

    private String desc;
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

    private MsgShowType(String desc) {
        this.desc = desc;
    }

    public static MsgShowType get(String code) {
        for(MsgShowType e : MsgShowType.values()) {
            if(e.toString().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return Link;
    }
}
