package com.bi.queryer.ssm.portal.template.entity;

import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 10:20
 * @Description:
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTplCfgLocalEntity {
    private String analysisTplCfgId;
    private String analysisTplId;
    private String analysisTplContent;
    private long localVersionNo;
    private Long prodVersionNo;
    private String createdBy;
    private Timestamp createdTime;
    private String updatedBy;
    private Timestamp updatedTime;
}
