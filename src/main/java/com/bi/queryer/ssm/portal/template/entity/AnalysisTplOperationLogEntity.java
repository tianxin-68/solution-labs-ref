package com.bi.queryer.ssm.portal.template.entity;

import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/28 11:18
 * @Description:
 */

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTplOperationLogEntity {
    private int id;
    private String analysisTplId;
    private String analysisTplCfgId;
    private String portalId;
    private String operationType;
    private String operationReason;
    private String createdBy;
    private Timestamp createdTime;
}
