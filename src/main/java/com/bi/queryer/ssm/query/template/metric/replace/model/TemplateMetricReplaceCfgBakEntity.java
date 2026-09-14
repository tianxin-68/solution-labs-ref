package com.bi.queryer.ssm.query.template.metric.replace.model;

/**
 * 模板字段替换配置备份（template_metric_replace_cfg_bak）
 */
public class TemplateMetricReplaceCfgBakEntity {

    private Long id;
    private String tplId;
    /** 与 ssd_query_template_cfg.cfg_id 一致；本表主键 */
    private String cfgId;
    private String viewId;
    private String tplConfig;
    private String createdBy;
    private String createdTime;
    private String updatedBy;
    private String updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
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
