package com.bi.queryer.ssm.portal.template.entity;

import java.sql.Timestamp;

/**
 * 看板 AI 解读运行日志实体类
 * @Auther: contributor
 * @Date: 2026/3/6
 * @Description: ssm_analysis_tpl_ai_exec_log
 */
public class AnalysisTplAiExecLogEntity {

    /**
     * 运行日志 id
     */
    private Integer runLogId;

    /**
     * 分析模板 id
     */
    private String analysisTplId;

    /**
     * 解读方式：模板解读和自由解读
     */
    private String analysisMode;

    /**
     * 解读入参
     */
    private String analysisInput;

    /**
     * 运行结果状态：成功失败
     */
    private String execResult;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 解读耗时（秒）
     */
    private Long execDuration;

    // 环境
    private String env;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private Timestamp createdTime;

    public Integer getRunLogId() {
        return runLogId;
    }

    public void setRunLogId(Integer runLogId) {
        this.runLogId = runLogId;
    }

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public String getAnalysisInput() {
        return analysisInput;
    }

    public void setAnalysisInput(String analysisInput) {
        this.analysisInput = analysisInput;
    }

    public String getExecResult() {
        return execResult;
    }

    public void setExecResult(String execResult) {
        this.execResult = execResult;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getExecDuration() {
        return execDuration;
    }

    public void setExecDuration(Long execDuration) {
        this.execDuration = execDuration;
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

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
