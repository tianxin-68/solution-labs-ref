package com.bi.queryer.ssm.portal.template.entity;

import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 10:50
 * @Description:
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTplCfgProdEntity {
    private String analysisTplCfgId;
    private String analysisTplId;
    private String analysisTplContent;
    private long versionNo;
    private int isLatest;
    private String createdBy;
    private Timestamp createdTime;
    private String updatedBy;
    private Timestamp updatedTime;
}
