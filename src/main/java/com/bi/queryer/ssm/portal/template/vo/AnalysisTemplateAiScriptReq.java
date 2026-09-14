package com.bi.queryer.ssm.portal.template.vo;

/**
 * @Auther: contributor
 * @Date: 2026/3/9 14:27
 * @Description:
 */
public class AnalysisTemplateAiScriptReq {
    /**
     * 脚本 id
     */
    private String scriptId;

    /**
     * 分析模板配置 id
     */
    private String analysisTplCfgId;

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


    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getAnalysisTplCfgId() {
        return analysisTplCfgId;
    }

    public void setAnalysisTplCfgId(String analysisTplCfgId) {
        this.analysisTplCfgId = analysisTplCfgId;
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
}
