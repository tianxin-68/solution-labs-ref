package com.bi.queryer.ssm.llm.chatSession.req;

import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

public class ChatCreateWithSnapshotReq {

    /**
     * 是否需要查询数据
     */
    private Integer isNeedQueryData = Enabled.NO.getId();

    /**
     * 查询模板 query_template /门户看板 analysis_template/个人多模板 analysis_tmp_template
     */
    private String chatBusinessType;

    /**
     * 查询模板 = 视图id / 门户看板 = 门户看板id / 个人多模板 = 个人多模板 id
     */
    private String chatBusinessId;

    /**
     * 快照视图id
     */
    public String dataSnapshotViewId;

    /**
     * 查询数据配置
     */
    private List<ChatDataSnapshotReq> chatDataSnapshotReqList = new ArrayList<>();


    public Integer getIsNeedQueryData() {
        return isNeedQueryData;
    }

    public void setIsNeedQueryData(Integer isNeedQueryData) {
        this.isNeedQueryData = isNeedQueryData;
    }

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
