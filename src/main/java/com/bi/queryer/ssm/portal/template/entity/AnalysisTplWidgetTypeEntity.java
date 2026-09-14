package com.bi.queryer.ssm.portal.template.entity;

import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/19 16:07
 * @Description:
 */

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTplWidgetTypeEntity {
    private String widgetTypeCode;
    private String widgetTypeName;
    private double sortId;
    private byte isActive;
    private String createdBy;
    private Timestamp createdTime;
    private String updatedBy;
    private Timestamp updatedTime;
}
