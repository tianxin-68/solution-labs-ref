package com.bi.queryer.ssm.portal.ai.vo;

/**
 * AI 最近访问项：字段对齐 {@link com.bi.queryer.ssm.query.template.model.TemplateCtgTreeRsp} 树节点关键信息，仅保留常用字段。
 * <ul>
 *   <li>查询模板：type=query_template，parentId/ctgId 为目录，viewId 为视图</li>
 *   <li>看板：type=analysis_template，parentId 为门户 portalId</li>
 * </ul>
 */

public class TemplateRecentVisitRsp {

    /** 资源 id：查询模板 tplId / 看板 analysisTplId */
    private String tplId;

    /** 展示名称 */
    private String tplName;

    /** 与 {@link com.bi.queryer.ssm.query.template.enums.FavTemplateType} code 一致，如 query_template、analysis_template */
    private String tplType;

    // 目录id
    private String ctgId;

    /**
     * 视图名称
     */
    private String viewName;

    /** 查询模板访问时的视图 id（看板为空） */
    private String viewId;

    /** 最近访问时间 yyyy-MM-dd HH:mm:ss */
    private String lastVisitTime;

    /**
     * 分类路径
     */
    private String ctgNamePath;

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getTplType() {
        return tplType;
    }

    public void setTplType(String tplType) {
        this.tplType = tplType;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(String lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }

    public String getCtgNamePath() {
        return ctgNamePath;
    }

    public void setCtgNamePath(String ctgNamePath) {
        this.ctgNamePath = ctgNamePath;
    }
}
