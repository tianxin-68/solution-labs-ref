package com.bi.queryer.ssm.api.ticket;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录下高敏指标/维度TopN
 */
public class CtgSensitiveFieldRsp {

    /**
     * TOP20高敏指标（C3/C4）
     */
    private List<SensitiveFieldItem> metrics = new ArrayList<>();

    /**
     * TOP5高敏维度（C3/C4）
     */
    private List<SensitiveFieldItem> dimensions = new ArrayList<>();

    public List<SensitiveFieldItem> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<SensitiveFieldItem> metrics) {
        this.metrics = metrics;
    }

    public List<SensitiveFieldItem> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<SensitiveFieldItem> dimensions) {
        this.dimensions = dimensions;
    }
}