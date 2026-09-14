package com.bi.queryer.ssm.portal.template.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  16:43
 * @Description: 看板访问日志实体
 */
@Data
@Builder
public class AnalysisTemplateVisitLogEntity {

    private String analysisTplId;

    private String portalId;

    /** {@link com.bi.queryer.ssm.portal.enums.AnalysisTplVisitType#getCode()} */
    private String visitType;

    /** 专题分析报告等多版本资源时的版本号 */
    private Integer resourceVersion;

    private String userName;

    private String visitBeginTime;

    private String visitEndTime;

}
