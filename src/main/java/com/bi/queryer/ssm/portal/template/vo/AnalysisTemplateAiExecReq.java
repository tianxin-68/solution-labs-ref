package com.bi.queryer.ssm.portal.template.vo;

/**
 * @Auther: contributor
 * @Date: 2026/3/10 14:36
 * @Description:
 */
public class AnalysisTemplateAiExecReq extends AnalysisTemplateAiPreviewReq {
    // 执行环境，prod: 上线后的版本/local：本地草稿版本
    private String execMode;

    public String getExecMode() {
        return execMode;
    }

    public void setExecMode(String execMode) {
        this.execMode = execMode;
    }
}
