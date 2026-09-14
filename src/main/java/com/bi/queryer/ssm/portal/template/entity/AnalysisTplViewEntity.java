package com.bi.queryer.ssm.portal.template.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * 看板视图配置表 ssm_analysis_tpl_view
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTplViewEntity {
    /**
     *  视图id
     */
    private String viewId;

    /**
     *  视图名称
     */
    private String viewName;

    /**
     *  视图类型
     */
    private String viewType;

    /**
     *  分析模板id
     */
    private String analysisTplId;

    /**
     *  分析模板配置id
     */
    private String analysisTplCfgId;
    private Integer isActive;
    private Double sortId;
    private String createdBy;
    private String createdTime;
    private String updatedBy;
    private String updatedTime;


}
