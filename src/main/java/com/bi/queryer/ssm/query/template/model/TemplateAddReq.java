package com.bi.queryer.ssm.query.template.model;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class TemplateAddReq {

    /**
     * 模版ID
     */
    private String tplId;

    /**
     * 模版名称
     */
    private String tplName;

    /**
     * 模版描述
     */
    private String tplDesc;

    /**
     * 模版owner
     */
    private String tplOwner;

    /**
     * 模版配置
     */
    private String tplConfig;

    /**
     * 模版目录ID
     */
    private String ctgId;

    /**
     * 模版类型--（前台不用传）
     */
    private String tplType = "normal";

    /**
     * 数据集id
     */
    private String datasetId;

    /**
     * 模版视图ID
     */
    private String viewId;

    /**
     * 模版视图名称
     */
    private String viewName;

    /**
     *
     */
    private String viewType;

    /**
     * 模版视图是否有效
     */
    private Integer isViewValid = Enabled.YES.getId();

    /**
     * 视图状态 生效 active 临时 temp
     */
    private String viewStatus;

    /**
     * 模版字段维度编码集合
     */
    private String tplConfigFieldDimCodes;

    /**
     * 模版字段指标编码集合
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
     * 配置的跳转模板集合
     */
    private List<TemplateLinkAddReq> templateLinkList = new ArrayList<>();

    /**
     * 失效维度集合
     */
    private List<String> invalidFieldCodeList = new ArrayList<>();

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getTplDesc() {
        return tplDesc;
    }

    public void setTplDesc(String tplDesc) {
        this.tplDesc = tplDesc;
    }

    public String getTplOwner() {
        return tplOwner;
    }

    public void setTplOwner(String tplOwner) {
        this.tplOwner = tplOwner;
    }

    public String getTplConfig() {
        return tplConfig;
    }

    public void setTplConfig(String tplConfig) {
        this.tplConfig = tplConfig;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

    public String getTplType() {
        return tplType;
    }

    public void setTplType(String tplType) {
        this.tplType = tplType;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Integer getIsViewValid() {
        return isViewValid;
    }

    public void setIsViewValid(Integer isViewValid) {
        this.isViewValid = isViewValid;
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

    public List<TemplateLinkAddReq> getTemplateLinkList() {
        return templateLinkList;
    }

    public void setTemplateLinkList(List<TemplateLinkAddReq> templateLinkList) {
        this.templateLinkList = templateLinkList;
    }

    public List<String> getInvalidFieldCodeList() {
        return invalidFieldCodeList;
    }

    public void setInvalidFieldCodeList(List<String> invalidFieldCodeList) {
        this.invalidFieldCodeList = invalidFieldCodeList;
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

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public String getViewStatus() {
        return viewStatus;
    }

    public void setViewStatus(String viewStatus) {
        this.viewStatus = viewStatus;
    }
}
