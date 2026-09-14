package com.bi.queryer.ssm.portal.template.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @Auther: contributor
 * @Date: 2024/6/23 14:59
 * @Description:
 */
@Data
public class AnalysisTemplateBaseVO {

    /**
     * 看板ID
     */
    private String analysisTplId;
    /**
     * 看板名称
     */
    private String analysisTplName;

    /**
     * 看板描述
     */
    private String analysisTplDesc;

    /**
     * 发布状态
     */
    private String publishStatus;
    /**
     * 在线状态
     */
    private String onlineStatus;

    /**
     * 是否有草稿
     */
    private Integer hasDraft;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private Timestamp updatedTime;
}
