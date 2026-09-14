package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 迁移新旧 id 映射记录（表 ssm_migrate_bizsplit_mapping）：
 * 既是审计追溯依据，也是增量执行的幂等判断依据（执行计划第5、6节）。
 * objectType 取值见 {@link com.bi.queryer.ssm.migrate.bizsplit.enums.MetricMigrateObjectType}。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricMigrateMappingEntity {

    private Long id;

    private String objectType;

    private String oldId;

    private String oldName;

    private String newId;

    private String newName;

    private String oldBizLine;

    private String newBizLine;

    private String portalId;

    /** 该 newId 是否为"同名复用"得到的已存在对象：1=是 0=本次新创建 */
    private Integer isReused;

    private String createdBy;

    private String createdTime;
}
