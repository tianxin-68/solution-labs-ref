package com.bi.queryer.ssm.governance.vo;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.governance.entity.GovernanceViewTask;
import lombok.Data;

/**
 * 视图治理-列表/详情出参（对应前端「视图治理」表格与详情抽屉）。
 * 日期统一格式化为字符串返回前端。
 */
@Data
public class GovernanceViewVO {

    private static final String DATE = "yyyy-MM-dd";
    private static final String DATETIME = "yyyy-MM-dd HH:mm:ss";

    private Long id;

    /** 视图ID */
    private String viewId;

    /** 视图名称 */
    private String viewName;

    /** 视图类型：personal/shared/snapshot */
    private String viewType;

    /** 公域：1=是 0=否 */
    private Integer isPublicDomain;

    /** 快照：1=是 0=否（由视图类型派生） */
    private Integer isSnapshot;

    /** 路径（模块/目录） */
    private String viewPath;

    /** 所属模板（详情抽屉） */
    private String tplName;

    /** 负责人 */
    private String viewOwner;

    /** 状态：pending/noticed/exempted/offline/rolled_back/purged */
    private String status;

    /** 命中策略类型 */
    private String policyType;

    /** 命中策略名称 */
    private String policyName;

    /** 命中原因（详情抽屉） */
    private String hitReason;

    /** 距今未访问天数 */
    private Integer daysNoVisit;

    /** 最近一次访问时间 */
    private String lastVisitTime;

    /** 计划下线日期 */
    private String planOfflineDate;

    // —— 状态流转时间线（详情抽屉） ——
    private String noticeTime;
    private String offlineTime;
    private String protectExpireDate;
    private String exemptExpireDate;
    private String rollbackTime;

    public static GovernanceViewVO from(GovernanceViewTask t) {
        if (t == null) {
            return null;
        }
        GovernanceViewVO vo = new GovernanceViewVO();
        vo.setId(t.getId());
        vo.setViewId(t.getViewId());
        vo.setViewName(t.getViewName());
        vo.setViewType(t.getViewType());
        vo.setIsPublicDomain(t.getIsPublicDomain());
        vo.setIsSnapshot(t.getTplType() != null && t.getTplType().toLowerCase().contains("snapshot") ? 1 : 0);
        vo.setViewPath(t.getViewPath());
        vo.setTplName(t.getTplName());
        vo.setViewOwner(t.getViewOwner());
        vo.setStatus(t.getStatus());
        vo.setPolicyType(t.getPolicyType());
        vo.setPolicyName(t.getPolicyName());
        vo.setHitReason(t.getHitReason());
        vo.setDaysNoVisit(t.getDaysNoVisit());
        vo.setLastVisitTime(t.getLastVisitTime());
        vo.setPlanOfflineDate(DateUtil.format(t.getPlanOfflineDate(), DATE));
        vo.setNoticeTime(DateUtil.format(t.getNoticeTime(), DATETIME));
        vo.setOfflineTime(DateUtil.format(t.getOfflineTime(), DATETIME));
        vo.setProtectExpireDate(DateUtil.format(t.getProtectExpireDate(), DATE));
        vo.setExemptExpireDate(DateUtil.format(t.getExemptExpireDate(), DATE));
        vo.setRollbackTime(DateUtil.format(t.getRollbackTime(), DATETIME));
        return vo;
    }
}
