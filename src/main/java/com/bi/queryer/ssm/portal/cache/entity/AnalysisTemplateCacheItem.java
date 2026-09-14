package com.bi.queryer.ssm.portal.cache.entity;

/**
 * @Author contributor
 * @Date 16:19 2025/1/2
 * @Description 缓存item：用户 + 日期粒度
 **/
public class AnalysisTemplateCacheItem extends AnalysisTemplateCacheEntity{

    public AnalysisTemplateCacheItem(){

    }

    private String menuUrl;

    private String analysisDateGranularity;

    private String cacheBy;


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

    public String getCacheBy() {
        return cacheBy;
    }

    public void setCacheBy(String cacheBy) {
        this.cacheBy = cacheBy;
    }
}
