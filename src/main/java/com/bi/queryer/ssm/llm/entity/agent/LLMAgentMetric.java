package com.bi.queryer.ssm.llm.entity.agent;

public class LLMAgentMetric {

    private String metricCode;

    private String metricName;

    private String metricConfig;

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricConfig() {
        return metricConfig;
    }

    public void setMetricConfig(String metricConfig) {
        this.metricConfig = metricConfig;
    }
}
