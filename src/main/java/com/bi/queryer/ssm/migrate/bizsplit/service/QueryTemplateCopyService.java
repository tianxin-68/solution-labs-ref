package com.bi.queryer.ssm.migrate.bizsplit.service;

import com.bi.queryer.ssm.migrate.bizsplit.model.QueryTemplateCopyResult;

/**
 * 查询模板（含视图）复制能力契约（执行计划 3.3 节），不得复用
 * {@code TemplateViewService#copyView} / {@code TemplateConfigService#saveAsTemplateAndView} 等老接口。
 * 本模块不实现该接口，由承接查询模板复制能力的一方提供实现 Bean；
 * 迁移工具（{@link com.bi.queryer.ssm.migrate.bizsplit.service.MetricMigrateService}）
 * 只负责在复制后把新模板挂到正确的新目录（ctgId）下。
 */
public interface QueryTemplateCopyService {

    /**
     * 复制单个查询模板（含其全部视图）到新业务线。
     *
     * @param tplId      老查询模板 id
     * @param oldBizLine 老业务线名称
     * @param newBizLine 新业务线名称
     * @return 新模板 id，以及模板下所有视图的旧→新 viewId 映射
     */
    QueryTemplateCopyResult copyQueryTemplate(String tplId, String oldBizLine, String newBizLine);
}
