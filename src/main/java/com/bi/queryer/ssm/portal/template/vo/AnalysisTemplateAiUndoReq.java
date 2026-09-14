package com.bi.queryer.ssm.portal.template.vo;

/**
 * @Auther: contributor
 * @Date: 2026/3/10 14:36
 * @Description:
 */
public class AnalysisTemplateAiUndoReq {
    private String analysisMode;

    private String analysisTplId;

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }
}
