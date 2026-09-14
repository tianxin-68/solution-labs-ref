package com.bi.queryer.ssm.query.template.view.model;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.query.template.model.TemplateLinkAddReq;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

/**
 * 模版视图保存请求实体
 */
public class TemplateViewSaveReq {

    /**
     * 模版ID
     */
    private String tplId;

    /**
     * 视图ID
     */
    private String viewId;

    /**
     * 视图名称
     */
    private String viewName;

    /**
     * 视图类型 公共视图=public 个人视图=personal
     */
    private String viewType;

    /**
     * 视图状态 生效 active 临时 temp
     */
    private String viewStatus;

    /**
     * 模版配置
     */
    private String tplConfig;

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

    /**
     * 配置的跳转模板集合
     */
    private List<TemplateLinkAddReq> templateLinkList = new ArrayList<>();

    /**
     * 失效维度集合
     */
    private List<String> invalidFieldCodeList = new ArrayList<>();

    /**
     * 模版视图是否有效
     */
    private Integer isViewValid = Enabled.YES.getId();

    /**
     * 是否保存模版跳转信息
     * @return
     */
    private Integer isSaveTemplateLink = Enabled.YES.getId();

    /**
     * 数据集id
     */
    private String datasetId;

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

    public String getViewName() {

        if (StrUtil.isEmpty(viewName)) {
            viewName = "默认视图";
        }

        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getTplConfig() {
        return tplConfig;
    }

    public void setTplConfig(String tplConfig) {
        this.tplConfig = tplConfig;
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

    public Integer getIsViewValid() {
        return isViewValid;
    }

    public void setIsViewValid(Integer isViewValid) {
        this.isViewValid = isViewValid;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
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

    public Integer getIsSaveTemplateLink() {
        return isSaveTemplateLink;
    }

    public void setIsSaveTemplateLink(Integer isSaveTemplateLink) {
        this.isSaveTemplateLink = isSaveTemplateLink;
    }

    public String getViewStatus() {
        return viewStatus;
    }

    public void setViewStatus(String viewStatus) {
        this.viewStatus = viewStatus;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }
}
