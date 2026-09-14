package com.bi.queryer.ssm.portal.template.entity;

import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/20 11:34
 * @Description:
 */

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTplWidgetQueryTplRelEntity {
    private long id;
    private String analysisTplCfgId;
    private String analysisTplId;
    private String localQueryTplId;
    private String prodQueryTplId;
    private byte isActive;
    private String createdBy;
    private Timestamp createdTime;
    private String updatedBy;
    private Timestamp updatedTime;
}
