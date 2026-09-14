package com.bi.queryer.ssm.engine.accelerate.cache.model;

/**
 * @Author contributor
 * @Date 15:39 2025/1/9
 * @Description 查询模板缓存源：来源可以是分析模板或查询模板，但最终都是查询模板
 **/
public class QueryTemplateCacheSource {
    private String queryTplId;
    private String queryTplName;
    private String analysisTplId;
    private String analysisTplName;
    private String analysisMenuId;
    private String analysisMenuName;

    public String getQueryTplId() {
        return queryTplId;
    }

    public void setQueryTplId(String queryTplId) {
        this.queryTplId = queryTplId;
    }

    public String getQueryTplName() {
        return queryTplName;
    }

    public void setQueryTplName(String queryTplName) {
        this.queryTplName = queryTplName;
    }

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public String getAnalysisTplName() {
        return analysisTplName;
    }

    public void setAnalysisTplName(String analysisTplName) {
        this.analysisTplName = analysisTplName;
    }

    public String getAnalysisMenuId() {
        return analysisMenuId;
    }

    public void setAnalysisMenuId(String analysisMenuId) {
        this.analysisMenuId = analysisMenuId;
    }

    public String getAnalysisMenuName() {
        return analysisMenuName;
    }

    public void setAnalysisMenuName(String analysisMenuName) {
        this.analysisMenuName = analysisMenuName;
    }
}
