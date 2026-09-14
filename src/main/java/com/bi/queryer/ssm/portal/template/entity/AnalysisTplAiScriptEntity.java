package com.bi.queryer.ssm.portal.template.entity;

import java.sql.Timestamp;

/**
 * 看板 AI 解读脚本存储实体类
 * @Auther: contributor
 * @Date: 2026/3/6
 * @Description: ssm_analysis_tpl_ai_script
 */
public class AnalysisTplAiScriptEntity {

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
     * 脚本类型
     */
    private String scriptType;

    /**
     * 脚本状态：未生成 (not_generated)、生成中 (generating)、已生成 (generated)、已过期 (outdated)、手动修改 (manual_edited)、生成失败 (error)
     */
    private String scriptStatus;

    /**
     * 脚本内容
     */
    private String scriptContent;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private Timestamp createdTime;

    /**
     * 修改人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private Timestamp updatedTime;

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

    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }

    public String getScriptStatus() {
        return scriptStatus;
    }

    public void setScriptStatus(String scriptStatus) {
        this.scriptStatus = scriptStatus;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Timestamp getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Timestamp updatedTime) {
        this.updatedTime = updatedTime;
    }
}
