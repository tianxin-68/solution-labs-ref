package com.bi.queryer.ssm.portal.template.vo;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/6/17 15:18
 * @Description:
 */
@Data
public class AnalysisTemplateVO {

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
     * 业务门户ID
     */
    private String portalId;
    /**
     * 父级菜单名称
     */
    private String parentMenuId;

    /**
     * 在线状态
     */
    private String onlineStatus;

    /**
     * 是否有草稿
     */
    private Integer hasDraft;

    /**
     * 草稿更新人
     */
    private String draftUpdatedBy;

    /**
     * 草稿更新时间
     */
    private Long draftUpdatedTime;

    /**
     * 更新人
     */
    private String publishUser;

    /**
     * 更新时间
     */
    private String publishTime;

    /**
     * 对应门户下的目录ID
     */
    private String menuId;

    /**
     * 发布/下线原因
     */
    private String reason;

    /**
     * 组件配置
     */
    private List<WidgetNodeVO> widgetConfigs;

    /**
     * 是否有全局筛选器
     */
    private Integer hasGlobalFilter = Enabled.NO.getId();

    /**
     * 是否有全局时间筛选器
     */
    private Integer hasGlobalDateFilter = Enabled.NO.getId();

    // 使用数据的类型
    private String sourceDataType;

    private Integer hasAiSummary;

    private String analysisTplType;

    private String viewId;

    private String viewName;

}
