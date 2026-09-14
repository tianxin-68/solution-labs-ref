package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 看板复制接口（执行计划 3.2 节）的返回结果。
 * 该接口由承接看板复制能力的一方实现，本模块只消费，不实现克隆逻辑本身。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardCopyResult {

    /** 新看板 id（ssm_analysis_tpl_base.analysis_tpl_id） */
    private String newAnalysisTplId;

    /** 看板 widget 引用的查询模板/视图旧→新映射；若接口内部已自行完成回填，可为空 */
    private List<QueryTplRefMapping> queryTplRefMappings;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QueryTplRefMapping {
        private String oldTplId;
        private String newTplId;
        private String oldViewId;
        private String newViewId;
    }
}
