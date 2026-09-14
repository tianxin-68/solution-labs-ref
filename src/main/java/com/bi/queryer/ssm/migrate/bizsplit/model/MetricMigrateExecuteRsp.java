package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 手动触发一次迁移（首次或增量通用）的出参。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricMigrateExecuteRsp {

    /** 本次运行实际处理的对象数（新建 + 复用） */
    private int totalProcessed;

    /** 本次新创建的对象数 */
    private int createdCount;

    /** 本次命中"同名复用"的对象数 */
    private int reusedCount;

    /** 本次处理的映射明细，供人工核对 */
    private List<MetricMigrateMappingEntity> mappings;
}
