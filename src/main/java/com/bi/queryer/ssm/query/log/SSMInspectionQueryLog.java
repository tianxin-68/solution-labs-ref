package com.bi.queryer.ssm.query.log;

import com.bi.queryer.sys.enums.Enabled;

import java.util.Date;

public class SSMInspectionQueryLog {

    private String tplId;

    private String viewId;

    private Integer querySuccess = Enabled.YES.getId();

    private Integer queryRows;

    private Date queryBeginTime;

    private Date queryEndTime;

    private String remark;

    private String createdBy;

    private String queryLogId;

    private String dsKey;

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public Integer getQuerySuccess() {
        return querySuccess;
    }

    public void setQuerySuccess(Integer querySuccess) {
        this.querySuccess = querySuccess;
    }

    public Integer getQueryRows() {
        return queryRows;
    }

    public void setQueryRows(Integer queryRows) {
        this.queryRows = queryRows;
    }

    public Date getQueryBeginTime() {
        return queryBeginTime;
    }

    public void setQueryBeginTime(Date queryBeginTime) {
        this.queryBeginTime = queryBeginTime;
    }

    public Date getQueryEndTime() {
        return queryEndTime;
    }

    public void setQueryEndTime(Date queryEndTime) {
        this.queryEndTime = queryEndTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getQueryLogId() {
        return queryLogId;
    }

    public void setQueryLogId(String queryLogId) {
        this.queryLogId = queryLogId;
    }

    public String getDsKey() {
        return dsKey;
    }

    public void setDsKey(String dsKey) {
        this.dsKey = dsKey;
    }
}
