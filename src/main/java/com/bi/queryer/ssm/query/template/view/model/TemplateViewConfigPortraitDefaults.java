package com.bi.queryer.ssm.query.template.view.model;

/**
 * 视图配置画像：默认参数（日期范围、查询维度、过滤、TopN、汇总方式等）
 */
public class TemplateViewConfigPortraitDefaults {

    /** 日期范围与粒度合并展示，如 2026-04-14 ~ 2026-04-20 · 按日 */
    private String dateRangeSummary;

    /** 查询维度字段名，顿号分隔 */
    private String queryDimensionsText;

    /** 过滤条件摘要（不含公共日期） */
    private String filterConditionText;

    /** 最多查询行数 / TopN */
    private String topN;

    /** 汇总方式，如 default、aggregate */
    private String aggregationMode;

    public String getDateRangeSummary() {
        return dateRangeSummary;
    }

    public void setDateRangeSummary(String dateRangeSummary) {
        this.dateRangeSummary = dateRangeSummary;
    }

    public String getQueryDimensionsText() {
        return queryDimensionsText;
    }

    public void setQueryDimensionsText(String queryDimensionsText) {
        this.queryDimensionsText = queryDimensionsText;
    }

    public String getFilterConditionText() {
        return filterConditionText;
    }

    public void setFilterConditionText(String filterConditionText) {
        this.filterConditionText = filterConditionText;
    }

    public String getTopN() {
        return topN;
    }

    public void setTopN(String topN) {
        this.topN = topN;
    }

    public String getAggregationMode() {
        return aggregationMode;
    }

    public void setAggregationMode(String aggregationMode) {
        this.aggregationMode = aggregationMode;
    }



}
