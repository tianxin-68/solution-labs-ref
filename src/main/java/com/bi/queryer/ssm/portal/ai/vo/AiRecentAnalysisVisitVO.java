package com.bi.queryer.ssm.portal.ai.vo;

/** genAI_feature/v3.15.0_start */

public class AiRecentAnalysisVisitVO {

    private String analysisTplId;

    private String portalId;

    private String analysisTplName;

    /** ssm_analysis_tpl_base.analysis_tpl_type：normal / tmp */
    private String analysisTplType;

    private String lastVisitTime;

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public String getPortalId() {
        return portalId;
    }

    public void setPortalId(String portalId) {
        this.portalId = portalId;
    }

    public String getAnalysisTplName() {
        return analysisTplName;
    }

    public void setAnalysisTplName(String analysisTplName) {
        this.analysisTplName = analysisTplName;
    }

    public String getAnalysisTplType() {
        return analysisTplType;
    }

    public void setAnalysisTplType(String analysisTplType) {
        this.analysisTplType = analysisTplType;
    }

    public String getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(String lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }
}
/** genAI_feature/v3.15.0_end */
