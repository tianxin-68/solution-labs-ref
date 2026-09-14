package com.bi.queryer.ssm.engine.accelerate.cache.model;

/**
 * @Author contributor
 * @Date 10:44 2025/1/10
 * @Description 查询模板操作日志
 **/
public class QueryTemplateCacheOperateLog {
    private String queryTplId;
    private String queryTplName;
    private String cacheKey;
    private String operateType;

    private String operateContent;

    private String remark;

    private String operateBy;

    public QueryTemplateCacheOperateLog() {
    }

    public QueryTemplateCacheOperateLog(String queryTemplateId, String queryTemplateName, String cacheKey) {
        this.queryTplId = queryTemplateId;
        this.queryTplName = queryTemplateName;
        this.cacheKey = cacheKey;
    }

    public QueryTemplateCacheOperateLog(String queryTemplateId, String queryTemplateName, String cacheKey, String operateType, String operateContent, String remark, String operateBy) {
        this.queryTplId = queryTemplateId;
        this.queryTplName = queryTemplateName;
        this.cacheKey = cacheKey;
        this.operateType = operateType;
        this.operateContent = operateContent;
        this.remark = remark;
        this.operateBy = operateBy;
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

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getOperateType() {
        return operateType;
    }

    public void setOperateType(String operateType) {
        this.operateType = operateType;
    }

    public String getOperateContent() {
        return operateContent;
    }

    public void setOperateContent(String operateContent) {
        this.operateContent = operateContent;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOperateBy() {
        return operateBy;
    }

    public void setOperateBy(String operateBy) {
        this.operateBy = operateBy;
    }
}
