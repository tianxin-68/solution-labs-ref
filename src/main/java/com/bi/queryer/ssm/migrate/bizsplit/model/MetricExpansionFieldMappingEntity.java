package com.bi.queryer.ssm.migrate.bizsplit.model;

/**
 * 指标膨胀/替换字段 id 映射明细，对应 ssm_metric_expansion_field_mapping。
 */
public class MetricExpansionFieldMappingEntity {

    /** 主键 */
    private Long pkid;
    /** 关联影子配置 id */
    private String shadowCfgId;
    /** 视图 id（膨胀/替换后的视图） */
    private String viewId;
    /** 模板 id（膨胀/替换后的模板） */
    private String tplId;
    /** 膨胀/替换前的源视图 id */
    private String oldViewId;
    /** 膨胀/替换前的源模板 id */
    private String oldTplId;
    /** 执行时正式 cfg id */
    private String cfgId;
    /** 源业务线 */
    private String sourceBusinessline;
    /** 本条新指标对应的目标业务线 */
    private String targetBusinessline;
    /** 老指标 field id */
    private String oldFieldId;
    /** 老指标 code */
    private String oldFieldCode;
    /** 老指标 title */
    private String oldFieldTitle;
    /** 新指标 field id */
    private String newFieldId;
    /** 新指标 code */
    private String newFieldCode;
    /** 新指标 title */
    private String newFieldTitle;
    /** 映射表 source_metric_code（普通指标） */
    private String sourceMetricCode;
    /** 映射表 target_metric_code（普通指标） */
    private String targetMetricCode;
    /** 创建人 */
    private String createdBy;
    /** 创建时间 */
    private String createdTime;
    /** 更新人 */
    private String updatedBy;
    /** 更新时间 */
    private String updatedTime;

    public Long getPkid() {
        return pkid;
    }

    public void setPkid(Long pkid) {
        this.pkid = pkid;
    }

    public String getShadowCfgId() {
        return shadowCfgId;
    }

    public void setShadowCfgId(String shadowCfgId) {
        this.shadowCfgId = shadowCfgId;
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

    public String getOldViewId() {
        return oldViewId;
    }

    public void setOldViewId(String oldViewId) {
        this.oldViewId = oldViewId;
    }

    public String getOldTplId() {
        return oldTplId;
    }

    public void setOldTplId(String oldTplId) {
        this.oldTplId = oldTplId;
    }

    public String getCfgId() {
        return cfgId;
    }

    public void setCfgId(String cfgId) {
        this.cfgId = cfgId;
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

    public String getOldFieldId() {
        return oldFieldId;
    }

    public void setOldFieldId(String oldFieldId) {
        this.oldFieldId = oldFieldId;
    }

    public String getOldFieldCode() {
        return oldFieldCode;
    }

    public void setOldFieldCode(String oldFieldCode) {
        this.oldFieldCode = oldFieldCode;
    }

    public String getOldFieldTitle() {
        return oldFieldTitle;
    }

    public void setOldFieldTitle(String oldFieldTitle) {
        this.oldFieldTitle = oldFieldTitle;
    }

    public String getNewFieldId() {
        return newFieldId;
    }

    public void setNewFieldId(String newFieldId) {
        this.newFieldId = newFieldId;
    }

    public String getNewFieldCode() {
        return newFieldCode;
    }

    public void setNewFieldCode(String newFieldCode) {
        this.newFieldCode = newFieldCode;
    }

    public String getNewFieldTitle() {
        return newFieldTitle;
    }

    public void setNewFieldTitle(String newFieldTitle) {
        this.newFieldTitle = newFieldTitle;
    }

    public String getSourceMetricCode() {
        return sourceMetricCode;
    }

    public void setSourceMetricCode(String sourceMetricCode) {
        this.sourceMetricCode = sourceMetricCode;
    }

    public String getTargetMetricCode() {
        return targetMetricCode;
    }

    public void setTargetMetricCode(String targetMetricCode) {
        this.targetMetricCode = targetMetricCode;
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
