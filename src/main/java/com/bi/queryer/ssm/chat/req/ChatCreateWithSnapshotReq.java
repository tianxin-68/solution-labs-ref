package com.bi.queryer.ssm.chat.req;

import java.util.ArrayList;
import java.util.List;

public class ChatCreateWithSnapshotReq {

    /**
     * query_template / analysis_template / analysis_tmp_template
     */
    private String chatBusinessType;

    /**
     * 对应业务ID
     */
    private String chatBusinessId;

    /**
     * 快照视图 ID
     */
    private String dataSnapshotViewId;

    /**
     * 快照数据配置列表
     */
    private List<ChatDataSnapshotReq> chatDataSnapshotReqList = new ArrayList<>();

    public String getChatBusinessType() {
        return chatBusinessType;
    }

    public void setChatBusinessType(String chatBusinessType) {
        this.chatBusinessType = chatBusinessType;
    }

    public String getChatBusinessId() {
        return chatBusinessId;
    }

    public void setChatBusinessId(String chatBusinessId) {
        this.chatBusinessId = chatBusinessId;
    }

    public String getDataSnapshotViewId() {
        return dataSnapshotViewId;
    }

    public void setDataSnapshotViewId(String dataSnapshotViewId) {
        this.dataSnapshotViewId = dataSnapshotViewId;
    }

    public List<ChatDataSnapshotReq> getChatDataSnapshotReqList() {
        return chatDataSnapshotReqList;
    }

    public void setChatDataSnapshotReqList(List<ChatDataSnapshotReq> chatDataSnapshotReqList) {
        this.chatDataSnapshotReqList = chatDataSnapshotReqList;
    }
}
