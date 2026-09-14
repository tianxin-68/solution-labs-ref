package com.bi.queryer.ssm.query.template.view.model;

import com.bi.queryer.sys.enums.Enabled;
import org.jetbrains.annotations.NotNull;

/**
 * 模板视图
 */
public class TemplateViewEntity   implements Comparable{

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
     * 模板ID
     */
    private String tplId;

    /**
     * 模板名称
     */
    private String tplName;

    /**
     * 配置ID
     */
    private String cfgId;

    private String ctgId;

    private String ctgPath;

    /**
     * 排序ID
     */
    private Double sortId;

    private String datasetId;

    /**
     * 是否默认
     */
    private Integer isDefault = Enabled.NO.getId();

    /**
     * 是否有效
     */
    private Integer isActive = Enabled.YES.getId();

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 用户真实名
     */
    private String userRealName;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 模板配置
     */
    private String tplConfig;

    /**
     * 最近资产变更申请ID
     */
    private Long lastAssetChangeApplyId;

    /**
     * 审批状态
     */
    private String approvalStatus;

    /**
     * 过期天数
     */
    private Integer expirationDays;

    /**
     * 资产到期时间
     */
    private String assetExpiresTime;

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


    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

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

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getTplConfig() {
        return tplConfig;
    }

    public void setTplConfig(String tplConfig) {
        this.tplConfig = tplConfig;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o == null) {
            return -1;
        }

        Double num1 = this.sortId;
        if(num1 == null){
            num1 = -1.0;
        }

        Double num2 = ((TemplateViewEntity) o).getSortId();
        if(num2 == null){
            num2 = -1.0;
        }

        Double num = num1 - num2;
        if(num == 0){
            return 0;
        }
        return num > 0 ? 1 : -1;
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

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expirationDays = expirationDays;
    }

    public Long getLastAssetChangeApplyId() {
        return lastAssetChangeApplyId;
    }

    public void setLastAssetChangeApplyId(Long lastAssetChangeApplyId) {
        this.lastAssetChangeApplyId = lastAssetChangeApplyId;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getAssetExpiresTime() {
        return assetExpiresTime;
    }

    public void setAssetExpiresTime(String assetExpiresTime) {
        this.assetExpiresTime = assetExpiresTime;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

    public String getCtgPath() {
        return ctgPath;
    }

    public void setCtgPath(String ctgPath) {
        this.ctgPath = ctgPath;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }


    public String getUserRealName() {
        return userRealName;
    }

    public void setUserRealName(String userRealName) {
        this.userRealName = userRealName;
    }
}
