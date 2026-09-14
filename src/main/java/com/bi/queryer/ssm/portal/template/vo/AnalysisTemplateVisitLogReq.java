package com.bi.queryer.ssm.portal.template.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  16:29
 * @Description: 看板访问日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTemplateVisitLogReq {

    /**
     * 看板id
     */
    private String analysisTplId;

    /**
     * 门户id
     */
    private String portalId;

    /**
     * 访问资源类型，默认看板 {@link com.bi.queryer.ssm.portal.enums.AnalysisTplVisitType#ANALYSIS_TEMPLATE}
     */
    private String visitType;

    /**
     * 资源版本号（专题分析报告 detail 访问时可选）
     */
    private Integer resourceVersion;

    /**
     * 访问开始时间
     */
    private String visitBeginTime;

    /**
     * 访问结束时间
     */
    private String visitEndTime;

}
