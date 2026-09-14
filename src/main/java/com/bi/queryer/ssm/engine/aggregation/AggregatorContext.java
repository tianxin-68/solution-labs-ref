package com.bi.queryer.ssm.engine.aggregation;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;

/**
 * @Author contributor
 * @Date 17:22 2023-09-22
 * @Description 聚合器上下文环境
 **/
public class AggregatorContext {
    protected QueryConfigure config;

    /**
     * 自定义对比的索引号
     */
    protected Integer compareIndex = -1;

    protected AnalysisCalcMode analysisCalcMode;

    /**
     * 是否是同环比
     */
    protected Boolean isThb = false;

    public AggregatorContext(QueryConfigure config) {
        this.config = config;
    }

    public AggregatorContext(QueryConfigure config, Integer compareIndex) {
        this.config = config;
        this.compareIndex = compareIndex;
    }

    public AggregatorContext(QueryConfigure config, AnalysisCalcMode analysisCalcMode, Integer compareIndex, Boolean isThb) {
        this.config = config;
        this.analysisCalcMode = analysisCalcMode;
        this.compareIndex = compareIndex;
        this.isThb = isThb;
    }

    public QueryConfigure getConfig() {
        return config;
    }

    public void setConfig(QueryConfigure config) {
        this.config = config;
    }

    public Integer getCompareIndex() {
        return compareIndex;
    }

    public void setCompareIndex(Integer compareIndex) {
        this.compareIndex = compareIndex;
    }

    public Boolean getThb() {
        return isThb;
    }

    public void setThb(Boolean thb) {
        isThb = thb;
    }
}
