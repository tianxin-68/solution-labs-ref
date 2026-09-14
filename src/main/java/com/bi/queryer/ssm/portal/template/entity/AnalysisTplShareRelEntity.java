package com.bi.queryer.ssm.portal.template.entity;

import lombok.Data;

import java.util.Date;

/**
 * 看板分享关系（分享给他人）表 ssm_analysis_tpl_share_rel
 */
@Data
public class AnalysisTplShareRelEntity {

    private String sourceAnalysisTplId;

    private String targetAnalysisTplId;

    private String rootAnalysisTplId;

    private String createdBy;

    private Date createdTime;
}
