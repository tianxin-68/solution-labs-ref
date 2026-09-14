package com.bi.queryer.ssm.llm.entity;

import java.util.ArrayList;
import java.util.List;

public class LLMQueryResult {

    private List<LLMQueryField> dimensions = new ArrayList<>();

    private List<LLMQueryField> metrics = new ArrayList<>();

    //原始的指标配置
    private List<LLMQueryField> originalMetrics = new ArrayList<>();

    public List<LLMQueryField> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<LLMQueryField> dimensions) {
        this.dimensions = dimensions;
    }

    public List<LLMQueryField> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<LLMQueryField> metrics) {
        this.metrics = metrics;
    }

    public List<LLMQueryField> getOriginalMetrics() {
        return originalMetrics;
    }

    public void setOriginalMetrics(List<LLMQueryField> originalMetrics) {
        this.originalMetrics = originalMetrics;
    }
}
