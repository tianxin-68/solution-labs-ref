package com.bi.queryer.ssm.governance.vo;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.governance.entity.GovernanceFieldTask;
import lombok.Data;

/**
 * 指标/维度治理-列表/详情出参（对应前端「指标维度治理」表格与详情抽屉）。
 * 日期统一格式化为字符串返回前端。
 */
@Data
public class GovernanceFieldVO {

    private static final String DATE = "yyyy-MM-dd";
    private static final String DATETIME = "yyyy-MM-dd HH:mm:ss";

    private Long id;

    /** 编码（白皮书编码，最小治理单元） */
    private String whitePaperCode;

    /** 名称 */
    private String wpName;

    /** 类型：field/dim */
    private String objectType;

    /** 等级：核心/重要/普通 */
    private String wpLevel;

    /** 负责人 */
    private String wpOwner;

    /** 目录（前台目录路径） */
    private String ctgPath;

    /** 状态：pending/noticed/exempted */
    private String status;

    /** 命中策略类型 */
    private String policyType;

    /** 命中策略名称 */
    private String policyName;

    /** 命中原因（详情抽屉） */
    private String hitReason;

    /** 距今未引用天数 */
    private Integer daysNoVisit;

    /** 最近一次被引用日期 */
    private String lastVisitTime;

    // 使用该字段的产品
    private String usedProducts;

    // —— 通知信息（详情抽屉） ——
    private String noticeTime;
    private String lastNoticeTime;
    private Integer noticeCount;
    private String exemptExpireDate;

    public static GovernanceFieldVO from(GovernanceFieldTask t) {
        if (t == null) {
            return null;
        }
        GovernanceFieldVO vo = new GovernanceFieldVO();
        vo.setId(t.getId());
        vo.setWhitePaperCode(t.getWhitePaperCode());
        vo.setWpName(t.getWpName());
        vo.setObjectType(t.getObjectType());
        vo.setWpLevel(t.getWpLevel());
        vo.setWpOwner(t.getWpOwner());
        vo.setCtgPath(t.getCtgPath());
        vo.setStatus(t.getStatus());
        vo.setPolicyType(t.getPolicyType());
        vo.setPolicyName(t.getPolicyName());
        vo.setHitReason(t.getHitReason());
        vo.setDaysNoVisit(t.getDaysNoVisit());
        vo.setLastVisitTime(t.getLastVisitTime());
        vo.setNoticeTime(DateUtil.format(t.getNoticeTime(), DATETIME));
        vo.setLastNoticeTime(DateUtil.format(t.getLastNoticeTime(), DATETIME));
        vo.setNoticeCount(t.getNoticeCount());
        vo.setExemptExpireDate(DateUtil.format(t.getExemptExpireDate(), DATE));
        vo.setUsedProducts(t.getUsedProducts());
        return vo;
    }
}
