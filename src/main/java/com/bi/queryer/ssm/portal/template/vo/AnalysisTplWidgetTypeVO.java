package com.bi.queryer.ssm.portal.template.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/19 16:09
 * @Description:
 */

@Data
public class AnalysisTplWidgetTypeVO {
    private String widgetTypeCode;
    private String widgetTypeName;
    private String createdBy;
    private Timestamp createdTime;
    private String updatedBy;
    private Timestamp updatedTime;
}
