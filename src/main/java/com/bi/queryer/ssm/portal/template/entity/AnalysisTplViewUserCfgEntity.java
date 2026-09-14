package com.bi.queryer.ssm.portal.template.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * 看板视图用户配置表 ssm_analysis_tpl_view_user_cfg
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTplViewUserCfgEntity {

    private String analysisTplId;

    private String viewId;

    /**
     * 是否默认视图，0 否 1 是
     */
    private Integer isDefault;

    private String createdBy;

    private Timestamp createdTime;
}
