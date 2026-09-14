package com.bi.queryer.sys.user.vo;

import java.util.List;

public class SsmAuthApplyVo {

    /**
     * 申请数据集 id，仅行级权限申请时用于定位用户已有模块权限
     */
    private String datasetId;

    private String applyReason;
    //模块权限
    private List<SsdDataAuthVo> ctgList;
    //数据权限
    private List<SsdDataAuthVo> dataAuthList;
    //用户已有数据权限
    private List<SsdDataAuthVo> ownAuthList;
    //数据权限审批人
    private String rowAuthOwner;

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getApplyReason() {
        return applyReason;
    }

    public void setApplyReason(String applyReason) {
        this.applyReason = applyReason;
    }

    public List<SsdDataAuthVo> getCtgList() {
        return ctgList;
    }

    public void setCtgList(List<SsdDataAuthVo> ctgList) {
        this.ctgList = ctgList;
    }

    public List<SsdDataAuthVo> getDataAuthList() {
        return dataAuthList;
    }

    public void setDataAuthList(List<SsdDataAuthVo> dataAuthList) {
        this.dataAuthList = dataAuthList;
    }

    public String getRowAuthOwner() {
        return rowAuthOwner;
    }

    public void setRowAuthOwner(String rowAuthOwner) {
        this.rowAuthOwner = rowAuthOwner;
    }

    public List<SsdDataAuthVo> getOwnAuthList() {
        return ownAuthList;
    }

    public void setOwnAuthList(List<SsdDataAuthVo> ownAuthList) {
        this.ownAuthList = ownAuthList;
    }
}
