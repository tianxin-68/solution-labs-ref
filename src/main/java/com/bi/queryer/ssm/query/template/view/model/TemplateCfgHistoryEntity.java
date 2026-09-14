package com.bi.queryer.ssm.query.template.view.model;

public class TemplateCfgHistoryEntity {

    private Long pkid;

    private String cfgId;

    private String viewId;

    private String tplId;

    private String tplConfig;

    private String createdBy;

    public Long getPkid() {
        return pkid;
    }

    public void setPkid(Long pkid) {
        this.pkid = pkid;
    }

    public String getCfgId() {
        return cfgId;
    }

    public void setCfgId(String cfgId) {
        this.cfgId = cfgId;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getTplConfig() {
        return tplConfig;
    }

    public void setTplConfig(String tplConfig) {
        this.tplConfig = tplConfig;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
