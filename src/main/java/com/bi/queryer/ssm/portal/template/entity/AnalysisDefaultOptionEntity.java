package com.bi.queryer.ssm.portal.template.entity;

import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/22 10:54
 * @Description:
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisDefaultOptionEntity {
    private long id;
    private String defaultOption;
    private byte isActive;
    private String createdBy;
    private Timestamp createdTime;
    private String updatedBy;
    private Timestamp updatedTime;
}
