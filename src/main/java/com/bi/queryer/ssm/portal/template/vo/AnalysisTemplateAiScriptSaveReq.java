package com.bi.queryer.ssm.portal.template.vo;

/**
 * @Auther: contributor
 * @Date: 2026/3/10 16:53
 * @Description:
 */
public class AnalysisTemplateAiScriptSaveReq {
    private String analysisTplId;
    private String scriptId;
    private String script;
    private String scriptStatus;

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getScriptStatus() {
        return scriptStatus;
    }

    public void setScriptStatus(String scriptStatus) {
        this.scriptStatus = scriptStatus;
    }
}
