package com.bi.queryer.sys.authapply.model.rpt;

import com.bi.queryer.sys.authapply.model.BaseTemplate;

import java.util.ArrayList;
import java.util.List;

public class RptTemplate {
    //申请人邮箱
    private String applyUser;
    //申请人
    private String applyer;
    //申请部门
    private String applyerDpt;
    //申请组别
    private String applyerGroup;
    //报表文件夹
    private String rptPath;
    //数据归属部门负责人 一级审批人
    private String dataOwnerPrincipal;
    //数据归属部门负责人 二级审批人
    private String dataOwnerPrincipal2;
    //行级权限审批人
    private String rowAuthOwner;
    //分析师
    private String analyst;
    //权限时长
    private Integer activeDuration;
    //申请原因
    private String applyReason;

    public List<BaseTemplate> buildRptTemplate() {
        List<BaseTemplate> list = new ArrayList<>();
        list.add(new BaseTemplate("applyUser", getApplyUser()));
        list.add(new BaseTemplate("applyer", getApplyer()));
        list.add(new BaseTemplate("applyerDpt", getApplyerDpt()));
        list.add(new BaseTemplate("applyerGroup", getApplyerGroup()));
        list.add(new BaseTemplate("rptPath", getRptPath()));
        list.add(new BaseTemplate("dataOwnerPrincipal", getDataOwnerPrincipal()));
        list.add(new BaseTemplate("dataOwnerPrincipal2", getDataOwnerPrincipal2()));
        list.add(new BaseTemplate("rowAuthOwner", getRowAuthOwner()));
        list.add(new BaseTemplate("analyst", getAnalyst()));
        list.add(new BaseTemplate("isNeedExpire", "是"));
        list.add(new BaseTemplate("activeDuration", String.valueOf(getActiveDuration())));
        list.add(new BaseTemplate("applyReason", getApplyReason()));
        return list;
    }

    public String getApplyUser() {
        return applyUser;
    }

    public void setApplyUser(String applyUser) {
        this.applyUser = applyUser;
    }

    public Integer getActiveDuration() {
        return activeDuration;
    }

    public void setActiveDuration(Integer activeDuration) {
        this.activeDuration = activeDuration;
    }

    public String getApplyer() {
        return applyer;
    }

    public void setApplyer(String applyer) {
        this.applyer = applyer;
    }

    public String getApplyerDpt() {
        return applyerDpt;
    }

    public void setApplyerDpt(String applyerDpt) {
        this.applyerDpt = applyerDpt;
    }

    public String getApplyerGroup() {
        return applyerGroup;
    }

    public void setApplyerGroup(String applyerGroup) {
        this.applyerGroup = applyerGroup;
    }

    public String getRptPath() {
        return rptPath;
    }

    public void setRptPath(String rptPath) {
        this.rptPath = rptPath;
    }

    public String getDataOwnerPrincipal() {
        return dataOwnerPrincipal;
    }

    public void setDataOwnerPrincipal(String dataOwnerPrincipal) {
        this.dataOwnerPrincipal = dataOwnerPrincipal;
    }

    public String getDataOwnerPrincipal2() {
        return dataOwnerPrincipal2;
    }

    public void setDataOwnerPrincipal2(String dataOwnerPrincipal2) {
        this.dataOwnerPrincipal2 = dataOwnerPrincipal2;
    }

    public String getRowAuthOwner() {
        return rowAuthOwner;
    }

    public void setRowAuthOwner(String rowAuthOwner) {
        this.rowAuthOwner = rowAuthOwner;
    }

    public String getAnalyst() {
        return analyst;
    }

    public void setAnalyst(String analyst) {
        this.analyst = analyst;
    }

    public String getApplyReason() {
        return applyReason;
    }

    public void setApplyReason(String applyReason) {
        this.applyReason = applyReason;
    }
}
