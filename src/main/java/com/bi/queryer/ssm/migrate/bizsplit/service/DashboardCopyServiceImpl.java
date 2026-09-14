package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.enums.MetricMigrateObjectType;
import com.bi.queryer.ssm.migrate.bizsplit.model.DashboardCopyResult;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.QueryTemplateCopyResult;
import com.bi.queryer.ssm.migrate.bizsplit.processor.MetricMigrateFilterRewriteProcessor;
import com.bi.queryer.ssm.migrate.bizsplit.util.MigrateMappingHelper;
import com.bi.queryer.ssm.portal.template.AnalysisTemplateService;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateDraftStatus;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateExecMode;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateOnlineStatus;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVO;
import com.bi.queryer.ssm.portal.template.vo.WidgetNodeVO;
import com.bi.queryer.ssm.query.template.model.TemplateAddReq;
import com.bi.queryer.ssm.query.template.model.TemplateEntity;
import com.bi.queryer.ssm.query.template.model.TemplateViewMapping;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板复制的真实实现（执行计划 3.2 节：本方案自定义接口，入参固定为看板id/老业务线/新业务线）。
 * <p>
 * 只支持迁移<b>已发布</b>的看板，且<b>不复制看板页签(视图)</b>——只处理看板顶层草稿/线上配置本身
 * （{@code ssm_analysis_tpl_view} 不迁移，需要页签的看板迁移后手动在新看板里重建）。整体按
 * "先查询配置、替换配置、保存草稿、发布"四步来，且每一步都直接调用 {@link AnalysisTemplateService}
 * 已有的真实接口，本类不再手工拼装 {@code ssm_analysis_tpl_widget}/{@code _cfg_local}/{@code _cfg_prod} 行：
 * <ol>
 *     <li><b>查询配置</b>：{@link AnalysisTemplateService#getAnalysisTemplateConfig}（{@code PROD} 模式，
 *     不传 viewId，取看板顶层内容）。该方法在看板从未发布过时会静默返回一份通用默认组件（不是"没有内容"），
 *     单靠它没法区分"已发布"和"只有草稿"，所以先用 {@link AnalysisTemplateService#getOnlineConfig}
 *     做一次门禁：查不到线上配置直接报错，不读取/复制任何默认占位内容。返回的是前端用的嵌套
 *     {@code WidgetNodeVO} 树。</li>
 *     <li><b>替换配置</b>：{@code rewriteWidgetTree} 直接在这棵 {@code WidgetNodeVO} 树上递归改写——
 *     标题/描述按业务线子串替换、组件配置里的指标 code 按 {@code ssm_metric_expansion_field_mapping} 映射替换、
 *     {@code queryTplId} 按迁移映射表回填（见下方"widget 引用的查询模板"）、每个 widget 换一个新 widgetId
 *     （树结构本身表达父子关系，不需要像扁平行那样单独处理 {@code parentWidgetId}，也就不会有根节点
 *     {@code parentWidgetId} 该填什么的问题——那本该是所属看板自己的 {@code analysisTplId}，flatten 成扁平
 *     行反而容易漏这一点）。</li>
 *     <li><b>保存草稿</b>：把改写后的顶层树整棵作为 {@link AnalysisTemplateService#update} 的入参保存——
 *     和一个真实用户"编辑保存"完全一样的调用。</li>
 *     <li><b>发布</b>：紧接着调用 {@link AnalysisTemplateService#publish}——查询模板快照、{@code isLatest}
 *     维护、草稿清理、默认视图重建都由发布接口自己完成。</li>
 * </ol>
 * <p>
 * <b>widget 引用的查询模板</b>：先查第5节的迁移映射表（{@link MigrateMappingHelper}）看是否已经有该查询模板
 * 在本业务线下的新 id；查不到（增量执行时、或这是这个查询模板第一次在本次迁移里被引用到）就直接调用
 * {@link QueryTemplateCopyService} 现场复制一份，复制完立即记录 QUERY_TPL/QUERY_TPL_VIEW 映射——
 * 同一次 {@code execute} 里后续别的看板/widget 再引用到同一个老查询模板时，映射已经在，不会重复复制。
 * 这里改写的是 {@code WidgetNodeVO.getLocalQueryTplId()}（线上模式下解析出的"真实/可编辑"查询模板 id），
 * 不是 {@code getQueryTplId()}（发布时打的那份不可变快照 id）——业务线迁移要迁移的是真实模板本身，
 * 快照只是某一次发布的历史留痕，拿去继续迁移没有意义。
 * 查询模板复制能力仍然由承接方提供实现 Bean（{@code @Autowired(required = false)}），本类不实现克隆逻辑本身，
 * 没有实现 Bean 时直接报错，不会静默留着老 queryTplId。
 */
@Service
public class DashboardCopyServiceImpl implements DashboardCopyService {

    private static final Logger log = LoggerFactory.getLogger(DashboardCopyServiceImpl.class);

    @Autowired
    private BaseDao dao;

    @Autowired
    private MetricMigrateFilterRewriteProcessor rewriteProcessor;

    @Autowired
    private MigrateMappingHelper mappingHelper;

    @Autowired(required = false)
    private QueryTemplateCopyService queryTemplateCopyService;

    @Autowired
    private AnalysisTemplateService analysisTemplateService;

    @Override
    public DashboardCopyResult copyDashboard(String analysisTplId, String oldBizLine, String newBizLine, String oldBizLineForName,
                                              String oldPortalId, String newPortalId) {
        AnalysisTemplateEntity oldTpl = analysisTemplateService.getTemplateBase(analysisTplId);
        if (oldTpl == null || AnalysisTemplateOnlineStatus.OFFLINE.getCode().equals(oldTpl.getOnlineStatus())) {
            return null;
        }

        // getAnalysisTemplateConfig 在从未发布过时会静默返回一份通用默认组件，不能单靠它判断"是否已发布"，
        // 这里先用 getOnlineConfig 做门禁：查不到线上配置就直接报错，不读取/复制任何默认占位内容
        if (analysisTemplateService.getOnlineConfig(analysisTplId) == null) {
            log.info("[看板复制] {} 未发布，暂不支持迁移", analysisTplId);
            return null;
        }

        // 兜底：取不到 tplId/viewId 精确范围时使用（非查询模板类 widget，或该范围下这张表还没有明细数据）
        MetricMigrateFilterRewriteProcessor.MetricFieldMapping fallbackMapping =
                rewriteProcessor.loadMetricCodeMapping(oldBizLine, newBizLine);
        String operator = operatorName();

        // ------- 1. 查询配置：看板顶层线上内容 -------
        long stepStart = System.currentTimeMillis();
        AnalysisTemplateVO oldProdConfig = analysisTemplateService.getAnalysisTemplateConfig(
                analysisTplId, oldPortalId, AnalysisTemplateExecMode.PROD, 0, null);
        log.info("[看板复制] {} 查询线上配置耗时{}ms", analysisTplId, System.currentTimeMillis() - stepStart);

        // ------- 2. 替换配置：在 WidgetNodeVO 树上原地改写业务线/指标/查询模板引用 -------
        stepStart = System.currentTimeMillis();
        String newAnalysisTplId = Guid.id();
        List<WidgetNodeVO> newTopTree = rewriteWidgetTree(
                oldProdConfig.getWidgetConfigs(), oldBizLine, newBizLine, fallbackMapping, operator);
        log.info("[看板复制] {} 替换配置（含按需复制查询模板）耗时{}ms", analysisTplId, System.currentTimeMillis() - stepStart);

        AnalysisTemplateEntity newTpl = AnalysisTemplateEntity.builder()
                .analysisTplId(newAnalysisTplId)
                .analysisTplName(rewriteProcessor.rewriteText(oldTpl.getAnalysisTplName(), oldBizLineForName, newBizLine))
                .analysisTplOwner(oldTpl.getAnalysisTplOwner())
                .analysisTplDesc(rewriteProcessor.rewriteText(oldTpl.getAnalysisTplDesc(), oldBizLine, newBizLine))
                .draftStatus(AnalysisTemplateDraftStatus.INIT.getCode())
                .onlineStatus(AnalysisTemplateOnlineStatus.INIT.getCode())
                .isActive(1)
                .createdBy(operator)
                .sourceDataType(oldTpl.getSourceDataType())
                .hasAiSummary(oldTpl.getHasAiSummary())
                .analysisTplType(oldTpl.getAnalysisTplType())
                .build();
        dao.insert("ssm.analysisTemplate.create", newTpl);

        // ------- 3. 保存草稿：和真实用户"编辑保存"完全一样的调用 -------
        stepStart = System.currentTimeMillis();
        AnalysisTemplateVO updateVo = new AnalysisTemplateVO();
        updateVo.setAnalysisTplId(newAnalysisTplId);
        updateVo.setPortalId(newPortalId);
        updateVo.setAnalysisTplName(newTpl.getAnalysisTplName());
        updateVo.setAnalysisTplDesc(newTpl.getAnalysisTplDesc());
        updateVo.setSourceDataType(newTpl.getSourceDataType());
        updateVo.setAnalysisTplType(newTpl.getAnalysisTplType());
        updateVo.setWidgetConfigs(newTopTree);
        analysisTemplateService.update(updateVo);
        log.info("[看板复制] {} 保存草稿耗时{}ms", newAnalysisTplId, System.currentTimeMillis() - stepStart);

        // ------- 4. 发布：查询模板快照、isLatest 维护、草稿清理、默认视图重建全部交给发布接口自己完成 -------
        stepStart = System.currentTimeMillis();
        AnalysisTemplateVO publishVo = new AnalysisTemplateVO();
        publishVo.setAnalysisTplId(newAnalysisTplId);
        publishVo.setPortalId(newPortalId);
        publishVo.setReason("业务线拆分迁移自动发布");
        analysisTemplateService.publish(publishVo, false);
        log.info("[看板复制] {} 发布耗时{}ms", newAnalysisTplId, System.currentTimeMillis() - stepStart);

        // 看板复制不考虑"看板页签(视图)"——只复制顶层草稿/线上配置本身，老看板的页签(ssm_analysis_tpl_view)
        // 不迁移。AnalysisTplViewService#create 落库时按 widgetId 匹配线上组件回填 queryTplId/
        // queryTplViewIdMapping，迁移出来的页签 widget 全是新生成的 widgetId 永远匹配不上，之前折腾的
        // 补写逻辑效果也不稳定，索性不做，需要页签的看板迁移后手动在新看板里重建。

        return DashboardCopyResult.builder().newAnalysisTplId(newAnalysisTplId).build();
    }

    /**
     * 在老 {@code WidgetNodeVO} 树上递归改写出一棵新树：每个节点换一个新 widgetId（父子关系由树结构本身
     * 表达，不需要像扁平行那样额外处理 parentWidgetId），标题/描述/组件配置按业务线+指标映射改写，
     * {@code queryTplId} 按迁移映射表回填（查不到映射就保留原值，见类注释；这里读的是
     * {@code getLocalQueryTplId()}，不是发布快照 id {@code getQueryTplId()}，理由见类注释）。
     */
    private List<WidgetNodeVO> rewriteWidgetTree(List<WidgetNodeVO> oldNodes, String oldBizLine, String newBizLine,
                                                  MetricMigrateFilterRewriteProcessor.MetricFieldMapping fallbackMapping,
                                                  String operator) {
        if (CollUtil.isEmpty(oldNodes)) {
            return new ArrayList<>();
        }
        List<WidgetNodeVO> flatNodes = flattenTree(oldNodes);
        Map<String, String> widgetIdMapping = new HashMap<>();
        for (WidgetNodeVO node : flatNodes) {
            widgetIdMapping.put(node.getWidgetId(), Guid.id());
        }

        // 先把这棵树引用到的查询模板都迁移好，再去查 ssm_metric_expansion_field_mapping——那张表是
        // MetricExpansionService#replaceTemplateViews（remapQueryTplId 触发的按需复制）执行时才写入的
        // 明细数据，如果先查表再触发复制，查的时候这张表对这个 tplId 还是空的，指标 code/id 会因为查不到
        // 精确映射而静默退化成 fallbackMapping（甚至 fallbackMapping 也可能还没有，直接原样保留）。
        // remapQueryTplId 内部按迁移映射表做了幂等检查，这里重复调用不会重复触发复制。
        flatNodes.stream()
                .map(WidgetNodeVO::getLocalQueryTplId)
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .forEach(oldTplId -> remapQueryTplId(oldTplId, oldBizLine, newBizLine, operator));

        Map<String, MetricMigrateFilterRewriteProcessor.MetricFieldMapping> widgetMappings =
                loadWidgetMappings(flatNodes, oldBizLine, newBizLine, fallbackMapping);

        // 本次（这一棵树自己的）结构性引用重映射：storyline 的 targetWidgetId、AI 解读依赖的 widgetId+tplId、
        // 查询模板 widget 的 viewId——都不是指标 code/id，不走 MetricFieldMapping。tplId 查不到映射保留旧值
        // （跟 queryTplId 本身的兜底策略一致）；viewId 查不到映射置空，不留旧视图 id（旧视图很可能不属于新
        // queryTplId，留着不如清空提示用户重新选择，见 MetricMigrateFilterRewriteProcessor 类注释第4条）。
        MetricMigrateFilterRewriteProcessor.WidgetRefMapping widgetRefMapping =
                new MetricMigrateFilterRewriteProcessor.WidgetRefMapping(widgetIdMapping,
                        tplId -> remapQueryTplId(tplId, oldBizLine, newBizLine, operator),
                        viewId -> {
                            String remapped = remapViewId(viewId, newBizLine);
                            return remapped.equals(viewId) ? null : remapped;
                        });

        return rewriteWidgetNodes(oldNodes, oldBizLine, newBizLine, widgetIdMapping, widgetMappings, widgetRefMapping, operator);
    }

    private List<WidgetNodeVO> rewriteWidgetNodes(List<WidgetNodeVO> oldNodes, String oldBizLine, String newBizLine,
                                                   Map<String, String> widgetIdMapping,
                                                   Map<String, MetricMigrateFilterRewriteProcessor.MetricFieldMapping> widgetMappings,
                                                   MetricMigrateFilterRewriteProcessor.WidgetRefMapping widgetRefMapping,
                                                   String operator) {
        List<WidgetNodeVO> result = new ArrayList<>();
        if (CollUtil.isEmpty(oldNodes)) {
            return result;
        }
        for (WidgetNodeVO oldNode : oldNodes) {
            MetricMigrateFilterRewriteProcessor.MetricFieldMapping fieldMapping = widgetMappings.get(oldNode.getWidgetId());

            WidgetNodeVO newNode = new WidgetNodeVO();
            newNode.setWidgetId(widgetIdMapping.get(oldNode.getWidgetId()));
            newNode.setWidgetTypeCode(oldNode.getWidgetTypeCode());
            newNode.setWidgetTitle(rewriteProcessor.rewriteText(oldNode.getWidgetTitle(), oldBizLine, newBizLine));
            newNode.setWidgetDesc(rewriteProcessor.rewriteText(oldNode.getWidgetDesc(), oldBizLine, newBizLine));
            newNode.setWidgetSettings(rewriteProcessor.rewriteConfigJson(
                    oldNode.getWidgetSettings(), oldBizLine, newBizLine, fieldMapping, widgetRefMapping));
            newNode.setWidgetOptions(rewriteProcessor.rewriteConfigJson(
                    oldNode.getWidgetOptions(), oldBizLine, newBizLine, fieldMapping, widgetRefMapping));
            newNode.setQueryTplViewIdMappingList(remapViewIdMappingList(oldNode.getQueryTplViewIdMappingList(), newBizLine));
            newNode.setQueryTplId(remapQueryTplId(oldNode.getLocalQueryTplId(), oldBizLine, newBizLine, operator));
            if ("queryTemplate".equals(oldNode.getWidgetTypeCode())) {
                // 临时诊断日志：queryTplViewIdMappingList 时有时无的问题排查到 publish() 内部为止，
                // 再往下是"老代码"不方便改；这里把我们自己这一侧真正落进草稿的 queryTplId/viewId 打出来，
                // 对照 publish() 快照时是否用的是同一份，帮助判断问题到底出在这一侧还是 publish() 那一侧
                log.info("[看板复制-诊断] oldWidgetId={} oldLocalQueryTplId={} oldViewId={} -> newQueryTplId={} newViewId={}",
                        oldNode.getWidgetId(), oldNode.getLocalQueryTplId(), extractViewId(oldNode.getWidgetOptions()),
                        newNode.getQueryTplId(), extractViewId(newNode.getWidgetOptions()));
            }
            newNode.setChildren(rewriteWidgetNodes(oldNode.getChildren(), oldBizLine, newBizLine,
                    widgetIdMapping, widgetMappings, widgetRefMapping, operator));
            result.add(newNode);
        }
        return result;
    }

    /** 递归摊平成一份只读的扁平列表，纯粹用来枚举 widgetId/queryTplId、算指标映射缓存，不用于落库 */
    private List<WidgetNodeVO> flattenTree(List<WidgetNodeVO> nodes) {
        List<WidgetNodeVO> result = new ArrayList<>();
        if (CollUtil.isEmpty(nodes)) {
            return result;
        }
        for (WidgetNodeVO node : nodes) {
            result.add(node);
            result.addAll(flattenTree(node.getChildren()));
        }
        return result;
    }

    /**
     * 每个 widget 一份指标 code/id 映射：能从 widget 上解析出 queryTplId + widgetOptions.viewId 时，
     * 优先按 (tplId, viewId) 精确查 {@code ssm_metric_expansion_field_mapping}（同一次迁移实例产出的
     * 明细，code/id 都是精确对应，不用再靠元数据反查猜）；解析不出，或精确范围查出来是空的（比如这张表
     * 还没来得及为这个范围写明细数据），退化用只按业务线对过滤的 {@code fallbackMapping}。
     * 同一个 (tplId, viewId) 只查一次，多个 widget 共用查询结果。
     */
    private Map<String, MetricMigrateFilterRewriteProcessor.MetricFieldMapping> loadWidgetMappings(
            List<WidgetNodeVO> oldWidgets, String oldBizLine, String newBizLine,
            MetricMigrateFilterRewriteProcessor.MetricFieldMapping fallbackMapping) {
        Map<String, MetricMigrateFilterRewriteProcessor.MetricFieldMapping> scopedCache = new HashMap<>();
        Map<String, MetricMigrateFilterRewriteProcessor.MetricFieldMapping> result = new HashMap<>();
        if (CollUtil.isEmpty(oldWidgets)) {
            return result;
        }
        for (WidgetNodeVO oldWidget : oldWidgets) {
            String tplId = oldWidget.getLocalQueryTplId();
            String viewId = extractViewId(oldWidget.getWidgetOptions());
            if (StrUtil.isEmpty(tplId) || StrUtil.isEmpty(viewId)) {
                result.put(oldWidget.getWidgetId(), fallbackMapping);
                continue;
            }
            String cacheKey = tplId + "|" + viewId;
            MetricMigrateFilterRewriteProcessor.MetricFieldMapping scoped = scopedCache.computeIfAbsent(cacheKey,
                    k -> rewriteProcessor.loadMetricCodeMapping(oldBizLine, newBizLine, tplId, viewId));
            result.put(oldWidget.getWidgetId(), scoped.isEmpty() ? fallbackMapping : scoped);
        }
        return result;
    }

    /** widgetOptions 里的 {@code viewId}——该 widget 引用的查询模板具体展示哪一个视图；解析不出返回 null */
    private String extractViewId(String widgetOptions) {
        if (StrUtil.isEmpty(widgetOptions)) {
            return null;
        }
        try {
            JSONObject options = JSON.parseObject(widgetOptions);
            return options.getString("viewId");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查迁移映射表看该查询模板在目标业务线下是否已有新 id；没有就直接调用 {@link QueryTemplateCopyService}
     * 现场复制一份（含其全部视图），复制完立即记录 QUERY_TPL/QUERY_TPL_VIEW 映射（见类注释）。
     * 没有接入实现 Bean 时直接报错——查询模板是看板正常展示数据的必要依赖，静默留着老业务线的 queryTplId
     * 没有意义，不能像 viewId 那样"查不到就置空"降级处理。
     */
    private String remapQueryTplId(String oldQueryTplId, String oldBizLine, String newBizLine, String operator) {
        if (StrUtil.isEmpty(oldQueryTplId)) {
            return oldQueryTplId;
        }
        MetricMigrateMappingEntity mapping = mappingHelper.getMapping(
                MetricMigrateObjectType.QUERY_TPL.getCode(), oldQueryTplId, newBizLine);
        if (mapping != null) {
            return mapping.getNewId();
        }

        if (queryTemplateCopyService == null) {
            throw new BIException("查询模板复制能力（QueryTemplateCopyService）尚未接入实现，无法复制看板依赖的查询模板：" + oldQueryTplId);
        }
        long copyStart = System.currentTimeMillis();
        QueryTemplateCopyResult copyResult = queryTemplateCopyService.copyQueryTemplate(oldQueryTplId, oldBizLine, newBizLine);
        if (copyResult == null) {
            return null;
        }
        log.info("[看板复制] 按需复制查询模板{}耗时{}ms", oldQueryTplId, System.currentTimeMillis() - copyStart);
        // 新建出来的模板要挂到一个明确的目录下——复制接口只管内容克隆，不管目录归属。这里没有一个像
        // MetricMigrateService#copyTemplatesUnderCtg 那样现成的"这次的目标目录"，默认沿用老模板自己当前
        // 所在的目录；如果那个目录本身也已经作为共享空间被迁移过（有 SPACE_CTG 映射），改挂到迁移后的新目录，
        // 跟共享空间迁移的结果保持一致，而不是留在老目录或者查询模板复制接口默认给的任意目录下。
        String targetCtgId = resolveOnDemandCopyTargetCtgId(oldQueryTplId, newBizLine);
        if (StrUtil.isNotEmpty(targetCtgId)) {
            updateTemplateCtg(copyResult.getNewTplId(), targetCtgId);
        }
        mappingHelper.recordMapping(MetricMigrateObjectType.QUERY_TPL.getCode(),
                oldQueryTplId, null, copyResult.getNewTplId(), null, oldBizLine, newBizLine, null, false, operator);
        if (CollUtil.isNotEmpty(copyResult.getViewIdMappings())) {
            for (QueryTemplateCopyResult.ViewIdMapping viewMapping : copyResult.getViewIdMappings()) {
                mappingHelper.recordMapping(MetricMigrateObjectType.QUERY_TPL_VIEW.getCode(),
                        viewMapping.getOldViewId(), null, viewMapping.getNewViewId(), null,
                        oldBizLine, newBizLine, null, false, operator);
            }
        }
        return copyResult.getNewTplId();
    }

    /**
     * 现场复制的新模板应该挂到哪个目录：老模板自己当前的 ctgId，除非那个目录本身也已经作为共享空间
     * 被迁移过（{@code SPACE_CTG} 映射存在），这种情况下改用迁移后的新目录 id。
     */
    private String resolveOnDemandCopyTargetCtgId(String oldQueryTplId, String newBizLine) {
        TemplateAddReq req = new TemplateAddReq();
        req.setTplId(oldQueryTplId);
        TemplateEntity oldTpl = (TemplateEntity) dao.queryObject("ssm.template.queryTemplateEntityById", req);
        if (oldTpl == null || StrUtil.isEmpty(oldTpl.getCtgId())) {
            return null;
        }
        MetricMigrateMappingEntity ctgMapping = mappingHelper.getMapping(
                MetricMigrateObjectType.SPACE_CTG.getCode(), oldTpl.getCtgId(), newBizLine);
        return ctgMapping != null ? ctgMapping.getNewId() : oldTpl.getCtgId();
    }

    private void updateTemplateCtg(String tplId, String ctgId) {
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("ctgId", ctgId);
        updateParams.put("tplIdList", Collections.singletonList(tplId));
        dao.update("ssm.template.ctg.updateTemplateCtg", updateParams);
    }

    /** 同上，对 queryTplViewIdMappingList 里出现的每个 viewId 做尽力回填，查不到映射的 viewId 保留原值 */
    private List<TemplateViewMapping> remapViewIdMappingList(List<TemplateViewMapping> oldMappings, String newBizLine) {
        if (CollUtil.isEmpty(oldMappings)) {
            return new ArrayList<>();
        }
        List<TemplateViewMapping> result = new ArrayList<>();
        for (TemplateViewMapping vm : oldMappings) {
            result.add(new TemplateViewMapping(
                    remapViewId(vm.getOldViewId(), newBizLine), remapViewId(vm.getNewViewId(), newBizLine)));
        }
        return result;
    }

    private String remapViewId(String oldViewId, String newBizLine) {
        if (StrUtil.isEmpty(oldViewId)) {
            return oldViewId;
        }
        MetricMigrateMappingEntity mapping = mappingHelper.getMapping(
                MetricMigrateObjectType.QUERY_TPL_VIEW.getCode(), oldViewId, newBizLine);
        return mapping != null ? mapping.getNewId() : oldViewId;
    }

    private String operatorName() {
        User user = UserManager.get();
        return user == null ? "system" : user.getName();
    }
}
