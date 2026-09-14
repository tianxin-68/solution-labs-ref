package com.bi.queryer.ssm.portal.template.entity;

/**
 * 临时分析模板与查询模板目录挂载关系
 *
 * @see ssd_tmp_analysis_tpl_ctg_rel
 */

import com.bi.queryer.sys.enums.Enabled;

/** genAI_feature/v3.15.0_start */
public class TmpAnalysisTplCtgRelEntity {

    /**
     * 关系 id
     */
    private Integer relId;

    /**
     * 目录 id（ssd_query_template_ctg.ctg_id）
     */
    private String ctgId;

    /**
     * 临时看板 id（ssm_analysis_tpl_base.analysis_tpl_id）
     */
    private String analysisTplId;

    /**
     * 看板名称（关联查询 ssm_analysis_tpl_base 时填充）
     */
    private String analysisTplName;

    /**
     * 看板负责人（关联查询时填充）
     */
    private String analysisTplOwner;

    /**
     * 看板描述（关联查询时填充）
     */
    private String analysisTplDesc;

    private Integer hasAiSummary = Enabled.NO.getId();

    private String createdTime;

    private String updatedTime;

    private String createdBy;

    private String updatedBy;

    public Integer getRelId() {
        return relId;
    }

    public void setRelId(Integer relId) {
        this.relId = relId;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
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

    public String getAnalysisTplOwner() {
        return analysisTplOwner;
    }

    public void setAnalysisTplOwner(String analysisTplOwner) {
        this.analysisTplOwner = analysisTplOwner;
    }

    public String getAnalysisTplDesc() {
        return analysisTplDesc;
    }

    public void setAnalysisTplDesc(String analysisTplDesc) {
        this.analysisTplDesc = analysisTplDesc;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getHasAiSummary() {
        return hasAiSummary;
    }

    public void setHasAiSummary(Integer hasAiSummary) {
        this.hasAiSummary = hasAiSummary;
    }
}
/** genAI_feature/v3.15.0_end */
