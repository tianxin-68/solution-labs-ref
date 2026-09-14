package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 手动触发一次迁移（首次或增量通用）的入参。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricMigrateExecuteReq {

    /**
     * 本次要处理的新业务线目标列表；重复调用只会增量同步尚未迁移的部分。
     * 每个 target 自带 portalId（复制来源所在门户），一次请求内允许混合来自不同门户的 target。
     */
    private List<MetricMigrateTarget> targets;
}
