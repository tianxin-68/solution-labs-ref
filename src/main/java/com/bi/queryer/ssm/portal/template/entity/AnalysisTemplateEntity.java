package com.bi.queryer.ssm.portal.template.entity;

import com.bi.queryer.sys.enums.Enabled;
import lombok.*;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/17 17:21
 * @Description:
 */

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTemplateEntity {

    private String analysisTplId;

    private String analysisTplName;

    private String analysisTplOwner;

    private String analysisTplDesc;

    private String draftStatus;

    private String onlineStatus;

    private int isActive;

    private String createdBy;

    private Timestamp createdTime;

    private String updatedBy;

    private Timestamp updatedTime;
    // 使用数据的类型
    private String sourceDataType;

    private Integer hasAiSummary = Enabled.NO.getId();

    private String analysisTplType;
}
