package com.bi.queryer.ssm.governance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 豁免模块/目录（ssm_governance_exempt）
 * ctg_id 为空 = 整个模块豁免；非空 = 仅豁免该目录
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GovernanceExempt {

    private Long id;

    /** 豁免范围：view/field */
    private String objectScope;

    /** 模块名称 */
    private String moduleName;

    /** 目录ID（空=整个模块） */
    private String ctgId;

    /** 目录名称 */
    private String ctgName;

    /** 豁免原因 */
    private String reason;

    /** 是否生效：1=生效 0=停用 */
    private Integer isActive;

    private String createdBy;

    /** 创建时间（yyyy-MM-dd HH:mm:ss） */
    private String createdTime;

    /** 更新时间（yyyy-MM-dd HH:mm:ss） */
    private String updatedTime;
}
