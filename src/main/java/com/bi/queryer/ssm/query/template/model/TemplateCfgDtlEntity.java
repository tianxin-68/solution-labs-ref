package com.bi.queryer.ssm.query.template.model;


/**
 * 模版配置扩展信息
 */
public class TemplateCfgDtlEntity {

    /**
     *
     */
    private String tplId;

    /**
     * 视图id
     */
    private String viewId;

    /**
     * 配置ID
     */
    private String cfgId;

    /**
     * 模版配置字段维度编码信息
     */
    private String tplConfigFieldDimCodes;

    /**
     * 模版配置字段指标编码信息
     */
    private String tplConfigFieldMeasureCodes;

    /**
     * 模版配置字段维度资产
     */
    private String tplConfigFieldDimAsset;

    /**
     * 模版配置字段指标资产
     */
    private String tplConfigFieldMeasureAsset;

    /**
     * 视图绑定的数据集ID（内存传递，不落 cfg_dtl 表）
     */
    private String datasetId;

    /**
     * 视图状态 active=生效 temp=临时
     */
    private String viewStatus;

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
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

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
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

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getViewStatus() {
        return viewStatus;
    }

    public void setViewStatus(String viewStatus) {
        this.viewStatus = viewStatus;
    }

    public TemplateCfgDtlEntity() {
    }

    public TemplateCfgDtlEntity(String cfgId, String tplConfigFieldDimCodes, String tplConfigFieldMeasureCodes) {
        this.cfgId = cfgId;
        this.tplConfigFieldDimCodes = tplConfigFieldDimCodes;
        this.tplConfigFieldMeasureCodes = tplConfigFieldMeasureCodes;
    }

    public TemplateCfgDtlEntity( String tplConfigFieldDimCodes, String tplConfigFieldMeasureCodes, String tplConfigFieldDimAsset, String tplConfigFieldMeasureAsset) {
        this.tplConfigFieldDimCodes = tplConfigFieldDimCodes;
        this.tplConfigFieldMeasureCodes = tplConfigFieldMeasureCodes;
        this.tplConfigFieldDimAsset = tplConfigFieldDimAsset;
        this.tplConfigFieldMeasureAsset = tplConfigFieldMeasureAsset;
    }

}
