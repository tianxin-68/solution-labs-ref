package com.bi.queryer.ssm.portal.template.entity;

import com.bi.queryer.ssm.query.template.model.TemplateViewMapping;
import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 13:38
 * @Description:
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTplWidgetEntity {
    private String widgetId;
    private String analysisTplCfgId;
    private String analysisTplId;
    private String widgetTypeCode;
    private String parentWidgetId;
    private String widgetTitle;
    private String widgetDesc;
    private String widgetSettings;
    private String widgetOptions;
    private String queryTplId;
    private String queryTplViewIdMapping;
    private List<TemplateViewMapping> queryTplViewIdMappingList = new ArrayList<>();
    private double sortId;
    private int isActive;
    private String createdBy;
    private Timestamp createdTime;
    private String updatedBy;
    private Timestamp updatedTime;
}
