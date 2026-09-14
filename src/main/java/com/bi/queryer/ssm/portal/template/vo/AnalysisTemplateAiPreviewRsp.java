package com.bi.queryer.ssm.portal.template.vo;

import com.bi.queryer.util.BIUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Auther: contributor
 * @Date: 2026/3/9 16:48
 * @Description:
 */
public class AnalysisTemplateAiPreviewRsp {

    private String itemId;
    /**
     * 脚本id
     */
    private String scriptId;

    /**
     * 提示语
     */
    private String prompt;

    /**
     * 脚本内容
     */
    private String script;
    /**
     * 脚本预览结果
     */
    private String summary;

    private String previewStatus;

    private String errorMsg;

    private String updateTime;

    private String trace;

    public AnalysisTemplateAiPreviewRsp() {
        this.updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPreviewStatus() {
        return previewStatus;
    }

    public void setPreviewStatus(String previewStatus) {
        this.previewStatus = previewStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }
}
