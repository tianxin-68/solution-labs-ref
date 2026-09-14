package com.bi.queryer.ssm.query.template.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-01-12  10:23
 * @Description: 模板跳转数据库实体
 */
public class TemplateLinkEntity {

    private String linkId;

    /**
     * 来源模板id
     */
    private String sourceTplId;

    /**
     * 来源模版视图id
     */
    private String sourceTplViewId;

    /**
     * 目标模板id
     */
    private String targetTplId;

    /**
     * 目标模版视图id
     */
    private String targetTplViewId;

    /**
     * 排序
     */
    private Double sortId;

    /**
     * 创建人
     */
    private String createdBy;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getSourceTplId() {
        return sourceTplId;
    }

    public void setSourceTplId(String sourceTplId) {
        this.sourceTplId = sourceTplId;
    }

    public String getSourceTplViewId() {
        return sourceTplViewId;
    }

    public void setSourceTplViewId(String sourceTplViewId) {
        this.sourceTplViewId = sourceTplViewId;
    }

    public String getTargetTplViewId() {
        return targetTplViewId;
    }

    public void setTargetTplViewId(String targetTplViewId) {
        this.targetTplViewId = targetTplViewId;
    }

    public String getTargetTplId() {
        return targetTplId;
    }

    public void setTargetTplId(String targetTplId) {
        this.targetTplId = targetTplId;
    }

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    private List<TemplateLinkFieldEnity> templateLinkFieldList = new ArrayList<>();

    public List<TemplateLinkFieldEnity> getTemplateLinkFieldList() {
        return templateLinkFieldList;
    }

    public void setTemplateLinkFieldList(List<TemplateLinkFieldEnity> templateLinkFieldList) {
        this.templateLinkFieldList = templateLinkFieldList;
    }

}
