package com.bi.queryer.ssm.governance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 指标/维度治理任务（ssm_governance_field_task）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GovernanceFieldTask {

    private Long id;

    /** 白皮书编码（最小治理单元） */
    private String whitePaperCode;

    /** 对象类型：field/dim */
    private String objectType;

    /** 目录（前台目录路径） */
    private String ctgPath;

    /** 白皮书名称 */
    private String wpName;

    /** 白皮书责任人 */
    private String wpOwner;

    /** 白皮书等级 */
    private String wpLevel;

    /** 距今未引用天数 */
    private Integer daysNoVisit;

    /** 最近一次被引用日期 */
    private String lastVisitTime;

    /** 命中策略类型 */
    private String policyType;

    /** 命中策略名称 */
    private String policyName;

    /** 命中原因 */
    private String hitReason;

    /** 状态：pending/noticed/exempted */
    private String status;

    /** 首次通知时间 */
    private Date noticeTime;

    /** 最近一次通知时间 */
    private Date lastNoticeTime;

    /** 累计通知次数 */
    private Integer noticeCount;

    /** 豁免时间 */
    private Date exemptTime;

    /** 豁免到期日 */
    private Date exemptExpireDate;

    /** 扫描批次（数据日期 dt） */
    private String batchNo;

    private Date createdTime;

    private Date updatedTime;

    // 使用该字段的产品
    private String usedProducts;

}
