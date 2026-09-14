package com.bi.queryer.sys.authapply.model.ssm;

import com.bi.queryer.sys.authapply.model.BaseTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 多维分析权限申请工单主表字段（图2参数定义）
 */
public class SsmWorkOrderMainTemplate {

    private String applyUserEmail;
    private String directLeader;
    private String applyerName;
    private String applyerDept;
    private String applyDataset;
    private String applyReason;
    private String dataOwner;
    private String dataOwnerLeader;
    private String dataSensitiveLevel;
    private String securityOwner;
    private String businessOwner;

    public List<BaseTemplate> buildTemplateList() {
        List<BaseTemplate> list = new ArrayList<>();
        list.add(new BaseTemplate("applyUserEmail", applyUserEmail));
        list.add(new BaseTemplate("directLeader", directLeader));
        list.add(new BaseTemplate("applyerName", applyerName));
        list.add(new BaseTemplate("applyerDept", applyerDept));
        list.add(new BaseTemplate("applyDataset", applyDataset));
        list.add(new BaseTemplate("applyReason", applyReason));
        list.add(new BaseTemplate("businessOwner", businessOwner));
        list.add(new BaseTemplate("dataOwner", dataOwner));
        list.add(new BaseTemplate("dataOwnerLeader", dataOwnerLeader));
        list.add(new BaseTemplate("dataSensitiveLevel", dataSensitiveLevel));
        list.add(new BaseTemplate("securityOwner", securityOwner));
        return list;
    }

    public String getApplyUserEmail() {
        return applyUserEmail;
    }

    public void setApplyUserEmail(String applyUserEmail) {
        this.applyUserEmail = applyUserEmail;
    }

    public String getDirectLeader() {
        return directLeader;
    }

    public void setDirectLeader(String directLeader) {
        this.directLeader = directLeader;
    }

    public String getApplyerName() {
        return applyerName;
    }

    public void setApplyerName(String applyerName) {
        this.applyerName = applyerName;
    }

    public String getApplyerDept() {
        return applyerDept;
    }

    public void setApplyerDept(String applyerDept) {
        this.applyerDept = applyerDept;
    }

    public String getApplyDataset() {
        return applyDataset;
    }

    public void setApplyDataset(String applyDataset) {
        this.applyDataset = applyDataset;
    }

    public String getApplyReason() {
        return applyReason;
    }

    public void setApplyReason(String applyReason) {
        this.applyReason = applyReason;
    }

    public String getDataOwner() {
        return dataOwner;
    }

    public void setDataOwner(String dataOwner) {
        this.dataOwner = dataOwner;
    }

    public String getDataOwnerLeader() {
        return dataOwnerLeader;
    }

    public void setDataOwnerLeader(String dataOwnerLeader) {
        this.dataOwnerLeader = dataOwnerLeader;
    }

    public String getDataSensitiveLevel() {
        return dataSensitiveLevel;
    }

    public void setDataSensitiveLevel(String dataSensitiveLevel) {
        this.dataSensitiveLevel = dataSensitiveLevel;
    }

    public String getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner(String securityOwner) {
        this.securityOwner = securityOwner;
    }

    public String getBusinessOwner() { return businessOwner; }

    public void setBusinessOwner(String businessOwner) { this.businessOwner = businessOwner; }
}
