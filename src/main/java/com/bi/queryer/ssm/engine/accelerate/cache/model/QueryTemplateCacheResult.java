package com.bi.queryer.ssm.engine.accelerate.cache.model;

import com.bi.queryer.sys.enums.Enabled;

/**
 * @Author contributor
 * @Date 15:43 2025/1/9
 * @Description 查询模板缓存结果
 **/
public class QueryTemplateCacheResult {
    private String pkId                            ;
    private String cacheId                     ;
    private String queryTplId               ;
    private String queryTplName         ;
    private String analysisMenuId      ;
    private String analysisMenuName;
    private String analysisTplId           ;
    private String analysisTplName     ;
    private String etlJobs                       ;
    private String cacheSql                    ;
    private String cacheKey                   ;

    private Long cacheSize  = 0L;
    private String remark                        ;
    private Integer isActive  = Enabled.YES.getId();
    private String createdBy                  ;
    private String createdTime              ;
    private String updatedBy                ;
    private String updatedTime             ;

    public String getPkId() {
        return pkId;
    }

    public void setPkId(String pkId) {
        this.pkId = pkId;
    }

    public String getCacheId() {
        return cacheId;
    }

    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

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

    public String getEtlJobs() {
        return etlJobs;
    }

    public void setEtlJobs(String etlJobs) {
        this.etlJobs = etlJobs;
    }

    public String getCacheSql() {
        return cacheSql;
    }

    public void setCacheSql(String cacheSql) {
        this.cacheSql = cacheSql;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(Long cacheSize) {
        this.cacheSize = cacheSize;
    }
}
