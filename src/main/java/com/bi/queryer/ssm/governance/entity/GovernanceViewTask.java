package com.bi.queryer.ssm.governance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 视图治理任务（ssm_governance_view_task）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GovernanceViewTask {

    private Long id;

    /** 视图ID */
    private String viewId;

    /** 视图配置ID */
    private String cfgId;

    /** 视图名称 */
    private String viewName;

    /** 视图类型：personal/shared/snapshot */
    private String viewType;

    /** 模板类型（包含 snapshot 即为快照模板） */
    private String tplType;

    /** 模板ID */
    private String tplId;

    /** 模板目录ID */
    private String ctgId;

    /** 模板名称 */
    private String tplName;

    /** 目录路径（模块/目录） */
    private String viewPath;

    /** 视图责任人 */
    private String viewOwner;

    /** 是否公域模板：1=是 0=否 */
    private Integer isPublicDomain;

    /** 是否挂载门户看板：1=是 0=否 */
    private Integer isPortal;

    /** 责任人是否全部离职：1=是 0=否 */
    private Integer isOwnerAllLeft;

    /** 距今未访问天数 */
    private Integer daysNoVisit;

    /** 最近一次访问时间 */
    private String lastVisitTime;

    /** 命中策略类型 */
    private String policyType;

    /** 命中策略名称 */
    private String policyName;

    /** 命中原因 */
    private String hitReason;

    /** 状态 */
    private String status;

    /** 计划下线日期 */
    private Date planOfflineDate;

    /** 通知时间 */
    private Date noticeTime;

    /** 下线时间 */
    private Date offlineTime;

    /** 保护期到期日 */
    private Date protectExpireDate;

    /** 豁免时间 */
    private Date exemptTime;

    /** 豁免到期日 */
    private Date exemptExpireDate;

    /** 回滚时间 */
    private Date rollbackTime;

    /** 扫描批次（数据日期 dt） */
    private String batchNo;

    /** 是否有效：1=在线 0=已下线 */
    private Integer isActive;

    private Date createdTime;

    private Date updatedTime;
}
