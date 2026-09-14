package com.bi.queryer.ssm.governance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 治理审计日志（ssm_governance_audit_log）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GovernanceAuditLog {

    private Long id;

    /** 对象类型：view/field/dim */
    private String objectType;

    /** 对象ID */
    private String objectId;

    /** 对象名称 */
    private String objectName;

    /** 操作 */
    private String action;

    /** 前状态 */
    private String fromStatus;

    /** 后状态 */
    private String toStatus;

    /** 操作人：system 或 用户名 */
    private String operator;

    /** 备注 */
    private String remark;

    /** 创建时间（yyyy-MM-dd HH:mm:ss） */
    private String createdTime;
}
