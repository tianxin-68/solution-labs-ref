package com.bi.queryer.ssm.portal.template.vo;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2026/3/10 16:53
 * @Description:
 */
public class AnalysisTemplateAiScriptQueryReq {
    // 执行环境，prod: 上线后的版本/local：本地草稿版本
    private String execMode; // local/prod
    private String analysisTplId;
    // 视图id
    private String viewId;

    private List<String> scriptIds;

    public String getExecMode() {
        return execMode;
    }

    public void setExecMode(String execMode) {
        this.execMode = execMode;
    }

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public List<String> getScriptIds() {
        return scriptIds;
    }

    public void setScriptIds(List<String> scriptIds) {
        this.scriptIds = scriptIds;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }
}
