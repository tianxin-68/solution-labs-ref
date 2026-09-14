package com.bi.queryer.ssm.portal.template.entity;

/**
 * 看板 AI 解读脚本与查询模板关系实体类
 * @Auther: contributor
 * @Date: 2026/3/12
 * @Description: ssm_analysis_tpl_ai_query_tpl_rel
 */
public class AnalysisTplAiQueryTplRelEntity {

    /**
     * 关系 id
     */
    private Integer relId;

    /**
     * 脚本 id
     */
    private String scriptId;

    /**
     * 组件 id
     */
    private String widgetId;

    /**
     * 分析模板配置 id
     */
    private String analysisTplCfgId;

    /**
     * 分析模板 id
     */
    private String analysisTplId;

    /**
     * 本地查询模板 id
     */
    private String localTplId;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private String createdTime;

    /**
     * 修改人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private String updatedTime;

    public Integer getRelId() {
        return relId;
    }

    public void setRelId(Integer relId) {
        this.relId = relId;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    public String getAnalysisTplCfgId() {
        return analysisTplCfgId;
    }

    public void setAnalysisTplCfgId(String analysisTplCfgId) {
        this.analysisTplCfgId = analysisTplCfgId;
    }

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public String getLocalTplId() {
        return localTplId;
    }

    public void setLocalTplId(String localTplId) {
        this.localTplId = localTplId;
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
