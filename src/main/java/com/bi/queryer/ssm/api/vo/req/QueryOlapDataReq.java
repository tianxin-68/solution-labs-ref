package com.bi.queryer.ssm.api.vo.req;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询多维数据请求参数
 */
public class QueryOlapDataReq {

    /**
     * 域账号
     */
    private String userName;

    /**
     * 模板视图ID
     */
    private String templateViewId;


    /**
     * 查询时间参数
     */
    private QueryOlapDataQueryDateReq queryDate = new QueryOlapDataQueryDateReq();

    /**
     * 查询过滤参数
     */
    private List<QueryOlapDataQueryFilterReq> filters = new ArrayList<>();

    /**
     * 指定需要在结果中展示的行/列维度（按 fieldName 与维度 title 匹配）。
     * 为空或未传时保持视图默认 isShow；非空时：在列表中的维度 isShow=1，否则置空。
     */
    private List<QueryOlapDataQueryDimensionReq> queryDimensions;

    /**
     * 指定需要在结果中展示的指标（按 fieldName 与指标 title 匹配）。
     * 为空或未传时保持视图默认 isShow；非空时：在列表中的指标 isShow=1，否则置空。
     */
    private List<QueryOlapDataQueryMetricReq> queryMetrics;

    /**
     * 限制返回的最大行数，对应查询配置中的 queryRowLimit；未传则不改视图默认。
     */
    private Integer topN;

    /**
     * 分析：同环比
     */
    private QueryOlapDataAnalysisReq  analysis = new QueryOlapDataAnalysisReq();

    /**
     * 结果数据集格式相关参数
     */
    private QueryOlapDataSetReq dataSet = new QueryOlapDataSetReq();

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTemplateViewId() {
        return templateViewId;
    }

    public void setTemplateViewId(String templateViewId) {
        this.templateViewId = templateViewId;
    }

    public QueryOlapDataQueryDateReq getQueryDate() {
        return queryDate;
    }

    public void setQueryDate(QueryOlapDataQueryDateReq queryDate) {
        this.queryDate = queryDate;
    }

    public List<QueryOlapDataQueryFilterReq> getFilters() {
        return filters;
    }

    public void setFilters(List<QueryOlapDataQueryFilterReq> filters) {
        this.filters = filters;
    }

    public List<QueryOlapDataQueryDimensionReq> getQueryDimensions() {
        return queryDimensions;
    }

    public void setQueryDimensions(List<QueryOlapDataQueryDimensionReq> queryDimensions) {
        this.queryDimensions = queryDimensions;
    }

    public List<QueryOlapDataQueryMetricReq> getQueryMetrics() {
        return queryMetrics;
    }

    public void setQueryMetrics(List<QueryOlapDataQueryMetricReq> queryMetrics) {
        this.queryMetrics = queryMetrics;
    }

    public Integer getTopN() {
        return topN;
    }

    public void setTopN(Integer topN) {
        this.topN = topN;
    }

    public QueryOlapDataSetReq getDataSet() {
        return dataSet;
    }

    public void setDataSet(QueryOlapDataSetReq dataSet) {
        this.dataSet = dataSet;
    }

    public QueryOlapDataAnalysisReq getAnalysis() {
        return analysis;
    }

    public void setAnalysis(QueryOlapDataAnalysisReq analysis) {
        this.analysis = analysis;
    }

    public boolean hasAnalysis(){
        return analysis != null && !analysis.isEmpty();
    }

    public boolean hasQueryDate(){
        return queryDate != null && !queryDate.isEmpty();
    }
}
