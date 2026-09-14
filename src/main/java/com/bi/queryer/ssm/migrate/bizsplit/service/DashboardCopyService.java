package com.bi.queryer.ssm.migrate.bizsplit.service;

import com.bi.queryer.ssm.migrate.bizsplit.model.DashboardCopyResult;

/**
 * 看板复制能力契约（执行计划 3.2 节）。
 * 本模块不实现该接口，由承接看板复制能力的一方提供实现 Bean；
 * 迁移工具（{@link com.bi.queryer.ssm.migrate.bizsplit.processor.TplConfigMigrateProcessor}）只负责调用与挂载。
 */
public interface DashboardCopyService {

    /**
     * 复制看板到新业务线，复制完成后直接调用真实发布接口把新看板发布上线。
     *
     * @param analysisTplId  老看板 id（必须已发布，暂不支持迁移只有草稿的看板）
     * @param oldBizLine     名称以外文本（描述、widget 标题/内容、筛选器取值等）替换用的老子串
     * @param newBizLine     名称以外文本替换用的新子串，同时也是看板名称替换的目标业务线名
     * @param oldBizLineForName 看板名称专用的老业务线名——名称替换用 {@code oldBizLineForName}→{@code newBizLine}，
     *                          跟 {@code oldBizLine}（名称以外文本用的替换串）是两回事，见调用方 {@code MetricMigrateTarget}
     *                          的 {@code oldBizLine}/{@code oldToken} 字段注释
     * @param oldPortalId    老看板所属的门户 id（{@code AnalysisTemplateService#getAnalysisTemplateConfig} 取线上配置的权限校验需要）
     * @param newPortalId    新看板所属的门户 id（发布接口的权限校验需要一个真实存在的门户 id）
     * @return 新看板 id，以及看板内部 widget 对 queryTplId/viewId 的引用重映射结果
     */
    DashboardCopyResult copyDashboard(String analysisTplId, String oldBizLine, String newBizLine, String oldBizLineForName,
                                       String oldPortalId, String newPortalId);
}
