package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询模板复制接口（执行计划 3.3 节）的返回结果。
 * 该接口由承接查询模板复制能力的一方实现（必须是新接口，不能复用 TemplateViewService/TemplateConfigService 等老接口），
 * 本模块只消费，不实现克隆逻辑本身。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryTemplateCopyResult {

    /** 新查询模板 id（ssd_query_template.tpl_id） */
    private String newTplId;

    /** 该模板下所有视图的旧→新 viewId 映射 */
    private List<ViewIdMapping> viewIdMappings;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ViewIdMapping {
        private String oldViewId;
        private String newViewId;
    }
}
