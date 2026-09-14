package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionFieldMappingEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标膨胀/替换字段 id 映射持久化服务。
 */
@Service
@Scope("prototype")
public class MetricExpansionFieldMappingService {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private BaseDao dao;

    /**
     * 覆盖写入某视图的字段 id 映射（按 view_id + source_businessline 删除后重插）。
     *
     * @param shadowCfgId        影子配置 id；门户替换无影子时传新 cfg id
     * @param context            单视图膨胀上下文
     * @param sourceBusinessline 源业务线
     * @param pendingMappings    待写入映射（仅含 old/new 字段及 metric code）
     */
    public void replaceFieldMappings(String shadowCfgId,
                                     MetricExpansionContext context,
                                     String sourceBusinessline,
                                     List<MetricExpansionFieldMappingEntity> pendingMappings) {
        if (context == null) {
            return;
        }
        replaceFieldMappings(shadowCfgId,
                context.getViewId(),
                context.getTplId(),
                context.getCfgId(),
                context.getViewId(),
                context.getTplId(),
                sourceBusinessline,
                pendingMappings);
    }

    /**
     * 覆盖写入字段 id 映射：按 view_id + source_businessline 删除后重新写入。
     *
     * @param shadowCfgId        影子配置 id；门户替换无影子时传新 cfg id
     * @param viewId             膨胀/替换后的视图 id
     * @param tplId              膨胀/替换后的模板 id
     * @param cfgId              正式 cfg id
     * @param oldViewId          膨胀/替换前的源视图 id
     * @param oldTplId           膨胀/替换前的源模板 id
     * @param sourceBusinessline 源业务线
     * @param pendingMappings    待写入映射
     */
    public void replaceFieldMappings(String shadowCfgId,
                                     String viewId,
                                     String tplId,
                                     String cfgId,
                                     String oldViewId,
                                     String oldTplId,
                                     String sourceBusinessline,
                                     List<MetricExpansionFieldMappingEntity> pendingMappings) {
        if (StrUtil.isEmpty(viewId) || StrUtil.isEmpty(sourceBusinessline)) {
            return;
        }
        Map<String, Object> deleteParam = new HashMap<>();
        deleteParam.put("viewId", viewId);
        deleteParam.put("sourceBusinessline", sourceBusinessline);
        dao.delete("ssm.metric.expansion.field.mapping.deleteByViewIdAndSourceBusinessline", deleteParam);
        if (CollUtil.isEmpty(pendingMappings)) {
            return;
        }
        String operator = resolveOperator();
        List<MetricExpansionFieldMappingEntity> rows = buildPersistRows(
                shadowCfgId, viewId, tplId, cfgId, oldViewId, oldTplId,
                sourceBusinessline, pendingMappings, operator);
        batchInsert(rows);
    }

    /**
     * 组装落库实体，补齐视图/影子/操作人等公共字段。
     */
    private List<MetricExpansionFieldMappingEntity> buildPersistRows(String shadowCfgId,
                                                                     String viewId,
                                                                     String tplId,
                                                                     String cfgId,
                                                                     String oldViewId,
                                                                     String oldTplId,
                                                                     String sourceBusinessline,
                                                                     List<MetricExpansionFieldMappingEntity> pendingMappings,
                                                                     String operator) {
        List<MetricExpansionFieldMappingEntity> rows = new ArrayList<>(pendingMappings.size());
        for (MetricExpansionFieldMappingEntity pending : pendingMappings) {
            if (pending == null
                    || StrUtil.isEmpty(pending.getOldFieldId())
                    || StrUtil.isEmpty(pending.getNewFieldId())) {
                continue;
            }
            MetricExpansionFieldMappingEntity row = new MetricExpansionFieldMappingEntity();
            row.setShadowCfgId(shadowCfgId);
            row.setViewId(viewId);
            row.setTplId(tplId);
            row.setOldViewId(oldViewId);
            row.setOldTplId(oldTplId);
            row.setCfgId(cfgId);
            row.setSourceBusinessline(sourceBusinessline);
            row.setTargetBusinessline(pending.getTargetBusinessline());
            row.setOldFieldId(pending.getOldFieldId());
            row.setOldFieldCode(pending.getOldFieldCode());
            row.setOldFieldTitle(pending.getOldFieldTitle());
            row.setNewFieldId(pending.getNewFieldId());
            row.setNewFieldCode(pending.getNewFieldCode());
            row.setNewFieldTitle(pending.getNewFieldTitle());
            row.setSourceMetricCode(pending.getSourceMetricCode());
            row.setTargetMetricCode(pending.getTargetMetricCode());
            row.setCreatedBy(operator);
            row.setUpdatedBy(operator);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 分批写入映射明细。
     */
    private void batchInsert(List<MetricExpansionFieldMappingEntity> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, rows.size());
            dao.insert("ssm.metric.expansion.field.mapping.batchInsert", rows.subList(from, to));
        }
    }

    private String resolveOperator() {
        return UserManager.get() != null ? UserManager.get().getName() : null;
    }
}
