package com.bi.queryer.ssm.portal.template.vo;

public class AgentQueryRsp {

    private String code;

    private String value;

    /**
     * 提示词
     */
    private String prompt;

    private Integer generateDuration;

    private Integer executeDuration;

    private Integer duration;

    private String trace;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getGenerateDuration() {
        return generateDuration;
    }

    public void setGenerateDuration(Integer generateDuration) {
        this.generateDuration = generateDuration;
    }

    public Integer getExecuteDuration() {
        return executeDuration;
    }

    public void setExecuteDuration(Integer executeDuration) {
        this.executeDuration = executeDuration;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }
}
