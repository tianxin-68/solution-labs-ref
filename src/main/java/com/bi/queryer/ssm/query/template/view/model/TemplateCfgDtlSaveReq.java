package com.bi.queryer.ssm.query.template.view.model;

public class TemplateCfgDtlSaveReq {

    private String viewId;

    private Integer isViewValid;

    /**
     * 模版字段维度编码集合
     */
    private String tplConfigFieldDimCodes;

    /**
     * 模版字段指标编码集合
     */
    private String tplConfigFieldMeasureCodes;

    /**
     * 模版字段维度资产
     */
    private String tplConfigFieldDimAsset;

    /**
     * 模版字段指标资产
     */
    private String tplConfigFieldMeasureAsset;

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
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

    public Integer getIsViewValid() {
        return isViewValid;
    }

    public void setIsViewValid(Integer isViewValid) {
        this.isViewValid = isViewValid;
    }
}
