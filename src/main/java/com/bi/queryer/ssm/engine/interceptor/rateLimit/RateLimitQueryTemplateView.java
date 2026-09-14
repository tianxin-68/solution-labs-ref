package com.bi.queryer.ssm.engine.interceptor.rateLimit;

/**
 * @Author contributor
 * @Date 17:58 2025/11/28
 * @Description 限流的查询模板
 **/
public class RateLimitQueryTemplateView {
    private String templateId;
    private String viewId;
    private Integer maxSession;
    private String startTime;
    private String endTime;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getMaxSession() {
        return maxSession;
    }

    public void setMaxSession(Integer maxSession) {
        this.maxSession = maxSession;
    }
}
