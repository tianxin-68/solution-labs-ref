package com.bi.queryer.ssm.portal.cache.entity;

/**
 * @Author contributor
 * @Date 15:39 2024/12/31
 * @Description 缓存日志实体类
 **/
public class AnalysisTemplateCacheLogEntity {
    private String portalId;
    private String portalName;
    private String menuName;
    private String menuId;

    private String menuUrl ;

    private String analysisTplId;
    private String analysisTplName;

    private String analysisDateGranularity;

    private Integer isCacheSuccess;

    private String cacheInfo ;

    private String cacheBeginTime ;

    private String cacheEndTime ;

    private String cacheBy;

    public String getPortalId() {
        return portalId;
    }

    public void setPortalId(String portalId) {
        this.portalId = portalId;
    }

    public String getPortalName() {
        return portalName;
    }

    public void setPortalName(String portalName) {
        this.portalName = portalName;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
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

    public Integer getIsCacheSuccess() {
        return isCacheSuccess;
    }

    public void setIsCacheSuccess(Integer isCacheSuccess) {
        this.isCacheSuccess = isCacheSuccess;
    }

    public String getCacheInfo() {
        return cacheInfo;
    }

    public void setCacheInfo(String cacheInfo) {
        this.cacheInfo = cacheInfo;
    }

    public String getCacheBeginTime() {
        return cacheBeginTime;
    }

    public void setCacheBeginTime(String cacheBeginTime) {
        this.cacheBeginTime = cacheBeginTime;
    }

    public String getCacheEndTime() {
        return cacheEndTime;
    }

    public void setCacheEndTime(String cacheEndTime) {
        this.cacheEndTime = cacheEndTime;
    }

    public String getCacheBy() {
        return cacheBy;
    }

    public void setCacheBy(String cacheBy) {
        this.cacheBy = cacheBy;
    }

    public String getMenuUrl() {
        return menuUrl;
    }

    public void setMenuUrl(String menuUrl) {
        this.menuUrl = menuUrl;
    }

    public String getAnalysisDateGranularity() {
        return analysisDateGranularity;
    }

    public void setAnalysisDateGranularity(String analysisDateGranularity) {
        this.analysisDateGranularity = analysisDateGranularity;
    }
}
