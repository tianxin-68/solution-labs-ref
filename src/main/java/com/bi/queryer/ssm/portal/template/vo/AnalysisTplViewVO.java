package com.bi.queryer.ssm.portal.template.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 看板视图配置 请求/响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTplViewVO {
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

    /**
     * 是否当前登录用户在该看板下的默认视图：1 是、0 否（对齐 ssm_analysis_tpl_view_user_cfg.is_default）
     */
    private Integer isDefault;

    /**
     * 是否可编辑：1 是、0 否
     */
    private Integer canEdit;

}
