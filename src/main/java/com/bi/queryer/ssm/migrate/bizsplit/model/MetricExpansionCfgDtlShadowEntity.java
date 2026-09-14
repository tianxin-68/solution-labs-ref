package com.bi.queryer.ssm.migrate.bizsplit.model;

/**
 * ???????????????? ssm_query_template_cfg_dtl_shadow?
 * ??? cfg ?? shadow_cfg_id??????? cfg_id?
 */
public class MetricExpansionCfgDtlShadowEntity {

    /** ???? id???? */
    private String shadowCfgId;
    /** ???? id???? */
    private String cfgId;
    /** ????? */
    private String tplConfigFieldDimCodes;
    /** ????? */
    private String tplConfigFieldMeasureCodes;
    /** ???? JSON */
    private String tplConfigFieldDimAsset;
    /** ???? JSON */
    private String tplConfigFieldMeasureAsset;
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

    public String getTplConfigFieldDimCodes() {
        return tplConfigFieldDimCodes;
    }

    public void setTplConfigFieldDimCodes(String tplConfigFieldDimCodes) {
        this.tplConfigFieldDimCodes = tplConfigFieldDimCodes;
    }

    public String getTplConfigFieldMeasureCodes() {
        return tplConfigFieldMeasureCodes;
    }

    public void setTplConfigFieldMeasureCodes(String tplConfigFieldMeasureCodes) {
        this.tplConfigFieldMeasureCodes = tplConfigFieldMeasureCodes;
    }

    public String getTplConfigFieldDimAsset() {
        return tplConfigFieldDimAsset;
    }

    public void setTplConfigFieldDimAsset(String tplConfigFieldDimAsset) {
        this.tplConfigFieldDimAsset = tplConfigFieldDimAsset;
    }

    public String getTplConfigFieldMeasureAsset() {
        return tplConfigFieldMeasureAsset;
    }

    public void setTplConfigFieldMeasureAsset(String tplConfigFieldMeasureAsset) {
        this.tplConfigFieldMeasureAsset = tplConfigFieldMeasureAsset;
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
