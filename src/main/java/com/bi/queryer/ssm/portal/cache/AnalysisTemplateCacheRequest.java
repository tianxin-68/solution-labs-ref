package com.bi.queryer.ssm.portal.cache;

/**
 * @Author contributor
 * @Date 11:42 2024/12/27
 * @Description TODO
 **/
public class AnalysisTemplateCacheRequest {
    private String analysisTplId;

    private String menuId;

    public String getAnalysisTplId() {
        return analysisTplId;
    }

    public void setAnalysisTplId(String analysisTplId) {
        this.analysisTplId = analysisTplId;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }
}
