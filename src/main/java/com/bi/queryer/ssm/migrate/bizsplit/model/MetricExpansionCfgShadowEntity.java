package com.bi.queryer.ssm.migrate.bizsplit.model;

/**
 * ?????????????? ssm_query_template_cfg_shadow?
 * ??? cfg_id ???shadow_cfg_id ???????????????
 */
public class MetricExpansionCfgShadowEntity {

    /** ???? id???? */
    private String shadowCfgId;
    /** ???? id???? */
    private String cfgId;
    /** ?? id */
    private String viewId;
    /** ?? id */
    private String tplId;
    /** ???? tplConfig ?? JSON */
    private String tplConfig;
    /** ??????? */
    private String sourceBusinessline;
    /** ??????????? */
    private String targetBusinessline;
    /** ??? */
    private String createdBy;
    /** ???? */
    private String createdTime;
    /** ??? */
    private String updatedBy;
    /** ???? */
    private String updatedTime;

    public String getShadowCfgId() {
        return shadowCfgId;
    }

    public void setShadowCfgId(String shadowCfgId) {
        this.shadowCfgId = shadowCfgId;
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

    public String getSourceBusinessline() {
        return sourceBusinessline;
    }

    public void setSourceBusinessline(String sourceBusinessline) {
        this.sourceBusinessline = sourceBusinessline;
    }

    public String getTargetBusinessline() {
        return targetBusinessline;
    }

    public void setTargetBusinessline(String targetBusinessline) {
        this.targetBusinessline = targetBusinessline;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }
}
