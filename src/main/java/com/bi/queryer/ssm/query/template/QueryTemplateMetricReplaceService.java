package com.bi.queryer.ssm.query.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.portal.template.AnalysisTemplateService;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateExecMode;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVO;
import com.bi.queryer.ssm.portal.template.vo.WidgetNodeVO;
import com.bi.queryer.ssm.query.template.metric.replace.model.TemplateFieldReplaceLogEntity;
import com.bi.queryer.ssm.query.template.metric.replace.model.TemplateMetricReplaceCfgBakEntity;
import com.bi.queryer.ssm.query.template.metric.replace.model.TemplateMetricReplaceEntity;
import com.bi.queryer.ssm.query.template.model.TemplateCfgEntity;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 查询模板指标（字段）替换：按映射表批量替换视图配置 JSON 中的 fieldId、指标编码与名称。
 */
@Service
@Scope("prototype")
public class QueryTemplateMetricReplaceService {
    @Autowired
    private BaseDao dao;

    @Autowired
    private AnalysisTemplateService analysisTemplateService;

    /**
     * 按看板解析出的查询模板批量执行指标替换，并写入 {@code template_field_replace_log}（tpl_id=看板id）。
     *
     * @param analysisTplId 看板 id
     * @param viewId        视图 id，可空
     * @param replaceTaskId 替换任务 id（前端传入）
     */
    public void replaceMetricsForDashboard(String analysisTplId, String viewId, String replaceTaskId) {
        Preconditions.checkArgument(StrUtil.isNotEmpty(analysisTplId), "看板Id为空");
        Preconditions.checkArgument(StrUtil.isNotEmpty(replaceTaskId), "替换任务Id为空");
        AnalysisTemplateEntity templateEntity = analysisTemplateService.getTemplateBase(analysisTplId);
        if (templateEntity == null) {
            throw new SSDException("看板不存在或已删除");
        }
        LinkedHashSet<String> tplIds = new LinkedHashSet<>(32);

        AnalysisTemplateVO prodVo = analysisTemplateService.getOnlineAnalysisTemplateVO(
                templateEntity, AnalysisTemplateExecMode.PROD, viewId);
        if (prodVo != null && CollUtil.isNotEmpty(prodVo.getWidgetConfigs())) {
            collectQueryTplIdsFromWidgetNodes(prodVo.getWidgetConfigs(), tplIds);
        }
        if (CollUtil.isEmpty(tplIds)) {
            return;
        }
        doReplaceMetricsForTemplate(new ArrayList<>(tplIds));
        saveFieldReplaceLog(replaceTaskId, analysisTplId, viewId);
    }

    /**
     * 对指定查询模板下全部视图执行指标替换，并按视图写入 {@code template_field_replace_log}。
     *
     * @param tplIds        查询模板 id 列表
     * @param replaceTaskId 替换任务 id（前端传入）
     */
    public void replaceMetricsForTemplate(List<String> tplIds, String replaceTaskId) {
        Preconditions.checkArgument(StrUtil.isNotEmpty(replaceTaskId), "替换任务Id为空");
        List<TemplateViewEntity> viewWithCfgList = doReplaceMetricsForTemplate(tplIds);
        if (CollUtil.isEmpty(viewWithCfgList)) {
            return;
        }
        String operator = UserManager.get() != null ? UserManager.get().getName() : null;
        List<TemplateFieldReplaceLogEntity> logs = new ArrayList<>(viewWithCfgList.size());
        for (TemplateViewEntity viewEntity : viewWithCfgList) {
            TemplateFieldReplaceLogEntity log = new TemplateFieldReplaceLogEntity();
            log.setReplaceTaskId(replaceTaskId);
            log.setTplId(viewEntity.getTplId());
            log.setViewId(viewEntity.getViewId());
            log.setCreatedBy(operator);
            logs.add(log);
        }
        Lists.partition(logs, 200).forEach(batch ->
                dao.insert("ssm.template.metric.replace.insertFieldReplaceLogBatch", batch));
    }

    /**
     * 执行指标替换（备份 + 更新配置），返回参与处理的视图列表；无规则或无视图时返回空列表。
     */
    private List<TemplateViewEntity> doReplaceMetricsForTemplate(List<String> tplIds) {
        if (CollUtil.isEmpty(tplIds)) {
            throw new IllegalArgumentException("模板ID不能为空");
        }

        List<TemplateMetricReplaceEntity> rules = dao.queryObjectList("ssm.template.metric.replace.listActive", null, TemplateMetricReplaceEntity.class);
        if (CollUtil.isEmpty(rules)) {
            return Collections.emptyList();
        }

        List<TemplateViewEntity> viewWithCfgList = dao.queryObjectList("ssm.template.view.queryViewWithCfgByTplIds", tplIds, TemplateViewEntity.class);
        if (CollUtil.isEmpty(viewWithCfgList)) {
            return Collections.emptyList();
        }

        List<String> cfgIds = new ArrayList<>(viewWithCfgList.size());
        List<TemplateMetricReplaceCfgBakEntity> bakBatch = new ArrayList<>(viewWithCfgList.size());
        String operator = UserManager.get() != null ? UserManager.get().getName() : null;
        for (TemplateViewEntity viewEntity : viewWithCfgList) {
            String cfgId = viewEntity.getCfgId();
            cfgIds.add(cfgId);
            TemplateMetricReplaceCfgBakEntity bak = new TemplateMetricReplaceCfgBakEntity();
            bak.setTplId(viewEntity.getTplId());
            bak.setCfgId(cfgId);
            bak.setViewId(viewEntity.getViewId());
            bak.setTplConfig(viewEntity.getTplConfig());
            bak.setCreatedBy(operator);
            bakBatch.add(bak);
        }

        List<TemplateMetricReplaceCfgBakEntity> cfgBakList = dao.queryObjectList("ssm.template.metric.replace.listCfgBakByCfgIds", cfgIds, TemplateMetricReplaceCfgBakEntity.class);
        Set<String> bakCfgIdSet = cfgBakList.stream().map(TemplateMetricReplaceCfgBakEntity::getCfgId).collect(Collectors.toSet());

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                Map<String, Object> delParams = new HashMap<>(4);
                delParams.put("cfgIds", cfgIds);
                dao.delete("ssm.template.metric.replace.deleteCfgBakByCfgIds", delParams);
                Lists.partition(bakBatch, 1).forEach(batch ->
                        dao.insert("ssm.template.metric.replace.insertCfgBakBatch", batch));

                for (TemplateViewEntity viewEntity : viewWithCfgList) {
                    String cfgId = viewEntity.getCfgId();
                    if (bakCfgIdSet.contains(cfgId)) {
                        continue;
                    }
                    String rawTplConfig = viewEntity.getTplConfig();
                    String replacedTplConfig = rawTplConfig;
                    for (TemplateMetricReplaceEntity rule : rules) {
                        replacedTplConfig = applyReplaceRule(replacedTplConfig, rule);
                    }
                    if (!StrUtil.equals(replacedTplConfig, rawTplConfig)) {
                        TemplateCfgEntity cfgEntity = new TemplateCfgEntity(cfgId, replacedTplConfig);
                        dao.update("ssm.template.updateTemplateCfg", cfgEntity);
                    }
                }
            }
        });
        return viewWithCfgList;
    }

    private void saveFieldReplaceLog(String replaceTaskId, String analysisTplId, String viewId) {
        TemplateFieldReplaceLogEntity log = new TemplateFieldReplaceLogEntity();
        log.setReplaceTaskId(replaceTaskId);
        log.setTplId(analysisTplId);
        log.setViewId(viewId);
        log.setCreatedBy(UserManager.get() != null ? UserManager.get().getName() : null);
        dao.insert("ssm.template.metric.replace.insertFieldReplaceLog", log);
    }

    /**
     * 将 {@code template_metric_replace_cfg_bak} 中备份写回 {@code ssd_query_template_cfg}。
     */
    public void rollbackTemplateConfigFromBak(List<String> tplIds) {
        if (CollUtil.isEmpty(tplIds)) {
            throw new IllegalArgumentException("模板ID不能为空");
        }

        List<TemplateMetricReplaceCfgBakEntity> bakList = dao.queryObjectList(
                "ssm.template.metric.replace.listLatestBakByTplIds", tplIds, TemplateMetricReplaceCfgBakEntity.class);
        if (CollUtil.isEmpty(bakList)) {
            throw new SSDException("该模板没有可回滚的备份记录");
        }

        Map<String, String> cfgIdToBakTplConfig = bakList.stream()
                .filter(b -> b != null && StrUtil.isNotEmpty(b.getCfgId()))
                .collect(Collectors.toMap(TemplateMetricReplaceCfgBakEntity::getCfgId, TemplateMetricReplaceCfgBakEntity::getTplConfig, (a, b) -> a));

        List<TemplateViewEntity> viewWithCfgList = dao.queryObjectList(
                "ssm.template.view.queryViewWithCfgByTplIds", tplIds, TemplateViewEntity.class);
        if (CollUtil.isEmpty(viewWithCfgList)) {
            throw new SSDException("该模板下没有视图配置");
        }

        Map<String, List<TemplateViewEntity>> viewsByCfgId = viewWithCfgList.stream()
                .filter(v -> v != null && StrUtil.isNotEmpty(v.getCfgId()))
                .collect(Collectors.groupingBy(TemplateViewEntity::getCfgId));

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                for (Map.Entry<String, List<TemplateViewEntity>> entry : viewsByCfgId.entrySet()) {
                    String cfgId = entry.getKey();
                    String bakTplConfig = cfgIdToBakTplConfig.get(cfgId);
                    if (bakTplConfig == null) {
                        continue;
                    }
                    TemplateCfgEntity cfgEntity = new TemplateCfgEntity(cfgId, bakTplConfig);
                    dao.update("ssm.template.updateTemplateCfg", cfgEntity);
                }
            }
        });
    }

    private String applyReplaceRule(String tplConfig, TemplateMetricReplaceEntity rule) {
        if (rule == null || StrUtil.isEmpty(rule.getOldWpCode())) {
            return tplConfig;
        }
        List<MetaField> oldFields = SSDMetaCacheManager.getFieldByCode(rule.getOldWpCode());
        List<MetaField> newFields = SSDMetaCacheManager.getFieldByCode(rule.getNewWpCode());

        MetaField newField = null;
        if (!newFields.isEmpty()) {
            Collections.sort(newFields);
            newField = newFields.get(0);
        }

        if (newField == null) {
            return tplConfig;
        }

        String s = tplConfig;
        if (CollUtil.isNotEmpty(oldFields) && StrUtil.isNotEmpty(newField.getId())) {
            for (MetaField oldField : oldFields) {
                if (oldField != null && StrUtil.isNotEmpty(oldField.getId())) {
                    s = replaceLiteral(s, oldField.getId(), newField.getId());
                }
            }
        }
        if (StrUtil.isNotEmpty(rule.getNewWpCode())) {
            s = replaceLiteral(s, rule.getOldWpCode(), rule.getNewWpCode());
        }
        if (CollUtil.isNotEmpty(oldFields) && StrUtil.isNotEmpty(newField.getTitle())) {
            MetaField oldField = oldFields.get(0);
            if (oldField != null && StrUtil.isNotEmpty(oldField.getTitle())) {
                s = replaceLiteral(s, oldField.getTitle(), newField.getTitle());
            }
        }
        return s;
    }

    private static String replaceLiteral(String text, String search, String replacement) {
        if (text == null || StrUtil.isEmpty(search)) {
            return text;
        }
        String rep = replacement != null ? replacement : "";
        return text.replaceAll(Pattern.quote(search), Matcher.quoteReplacement(rep));
    }

    private static void collectQueryTplIdsFromWidgetNodes(List<WidgetNodeVO> nodes, LinkedHashSet<String> out) {
        if (CollUtil.isEmpty(nodes)) {
            return;
        }
        for (WidgetNodeVO node : nodes) {
            if (node == null) {
                continue;
            }
            if (AnalysisTemplateService.QUERY_TPL_WIDGET_TYPE_CODE.equals(node.getWidgetTypeCode())) {
                if (StrUtil.isNotEmpty(node.getQueryTplId())) {
                    out.add(node.getQueryTplId());
                }
                if (StrUtil.isNotEmpty(node.getLocalQueryTplId())) {
                    out.add(node.getLocalQueryTplId());
                }
            }
            collectQueryTplIdsFromWidgetNodes(node.getChildren(), out);
        }
    }
}
