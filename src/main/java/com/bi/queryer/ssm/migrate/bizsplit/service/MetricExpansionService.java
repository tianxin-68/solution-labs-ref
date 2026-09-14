package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.migrate.bizsplit.builder.DerivedFieldBuilder;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionCfgDtlShadowEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionCfgShadowEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionExecuteReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionExecuteRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionFieldMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionPromoteLogEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionPromoteRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionTarget;
import com.bi.queryer.ssm.migrate.bizsplit.processor.CalcFieldExpansionProcessor;
import com.bi.queryer.ssm.migrate.bizsplit.processor.MeasureVisibilityLimitProcessor;
import com.bi.queryer.ssm.migrate.bizsplit.processor.MetricExpansionFilterRewriteProcessor;
import com.bi.queryer.ssm.migrate.bizsplit.processor.TplConfigExpansionProcessor;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionDatasetUtil;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionMetaUtil;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionOwnerUtil;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionReplaceTplReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionReplaceTplRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionUpdateTplReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionUpdateTplRsp;
import com.bi.queryer.ssm.query.template.TemplateLinkService;
import com.bi.queryer.ssm.query.template.model.TemplateAddReq;
import com.bi.queryer.ssm.query.template.model.TemplateCfgDtlEntity;
import com.bi.queryer.ssm.query.template.model.TemplateCfgEntity;
import com.bi.queryer.ssm.query.template.model.TemplateEntity;
import com.bi.queryer.ssm.query.template.model.TemplateLinkEntity;
import com.bi.queryer.ssm.query.template.model.TemplateLinkRsp;
import com.bi.queryer.ssm.query.template.enums.TemplateViewType;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.model.HrEmployee;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 指标膨胀/替换主服务。
 *
 * 整体流程（单视图 executeOne）：
 * 1. 解析 tplConfig 明文 JSON，构建 {@link MetricExpansionContext}。
 * 2. 非指标膨胀数据集：filter 中 businessline/CGW/BQO 按固定规则扩展（保养 / 改装超市）。
 * 3. 指标膨胀数据集：filter 中 id 等于 sourceBusinessline 时，用本视图映射 targetBusinessline 替换或追加原值
 *    （未指定 targetBusinessline 时保留源业务线并追加 target；指定 target 时替换不保留源值），
 *    并继续扫描膨胀指标；否则 measureCodes / measureAsset 保持正式 cfg_dtl 原值。
 * 4. filter 或指标任一发生变更则写入影子库（已有影子则覆盖更新）；两者均未变更则 skipped。
 *
 * 指标膨胀子流程（dataset 在配置白名单内且 activeSourceCodes 非空时）：
 * scanSourceMetricCodes → loadAndValidateTargets → expandTplConfig（普通 source 1→N）
 * → processCalcFields（计算字段按业务线变体）。
 * → rebuildDerivedFields（measureCodes / measureAsset）。
 *
 * 映射加载约定：sourceBusinessline 必填；targetBusinessline 可选（门户指定时 SQL 直接过滤）；
 * 未指定 target 时按各视图 owner 的 DEPT_ID 收窄，规则见 {@link MetricExpansionOwnerUtil}。
 *
 * 影子配置约定：execute 写入影子，同一 view_id 重复执行则覆盖更新；UT 读/存有影子走影子，Product 走正式库；
 * promote 将影子写入新正式 cfg 并切换视图 cfg_id；rollback 删除上线新 cfg/dtl 并恢复 promote 前 cfg_id。
 *
 * 批量语义：viewIds 按每批 {@link #VIEW_BATCH_SIZE} 切分；批内单视图异常写入 errorMessage，不中断后续视图。
 * promote 批内按 {@link #PROMOTE_THREAD_POOL_SIZE} 线程并行处理。
 *
 * 门户替换：replaceTemplateViews 复制整模板并直接写正式库，不写影子表，见该方法 Javadoc。
 * 门户重跑：updateReplacedTemplateViews 对已生成模板原地更新正式 cfg，见该方法 Javadoc。
 */
@Service
@Scope("prototype")
public class MetricExpansionService {

    private static final Logger log = LoggerFactory.getLogger(MetricExpansionService.class);

    /** 单批加载/处理的 viewId 数量上限，避免 IN 参数过多 */
    private static final int VIEW_BATCH_SIZE = 100;

    /** promote 批内并行线程数 */
    private static final int PROMOTE_THREAD_POOL_SIZE = 5;

    @Autowired
    private BaseDao dao;

    @Autowired
    private TplConfigExpansionProcessor tplConfigExpansionProcessor;

    /** 业务线 filter 筛选值替换，所有视图 execute 时必执行 */
    @Autowired
    private MetricExpansionFilterRewriteProcessor metricExpansionFilterRewriteProcessor;

    @Autowired
    private CalcFieldExpansionProcessor calcFieldExpansionProcessor;

    @Autowired
    private MeasureVisibilityLimitProcessor measureVisibilityLimitProcessor;

    @Autowired
    private DerivedFieldBuilder derivedFieldBuilder;

    @Autowired
    private MetricExpansionShadowService metricExpansionShadowService;

    @Autowired
    private MetricExpansionFieldMappingService metricExpansionFieldMappingService;

    @Autowired
    private TemplateLinkService templateLinkService;

    /**
     * 按批量 viewIds 执行指标膨胀/替换。
     * 结果写入影子库（按 cfg_id 覆盖），不修改正式 cfg；UT 页面经正式读路径可读到影子配置。
     *
     * @param req 请求体，viewIds、sourceBusinessline 必填；targetBusinessline 可选
     * @return 与入参 viewIds 顺序对应的逐视图结果
     */
    public List<MetricExpansionExecuteRsp> execute(MetricExpansionExecuteReq req) {
        Preconditions.checkArgument(req != null && CollUtil.isNotEmpty(req.getViewIds()), "viewIds 不能为空");

        // 映射与视图数量无关，整次请求只加载一次，供各批次扫描与 target 校验复用
        List<MetricExpansionMappingEntity> allMappings = loadMappings(req);
        Set<String> allSourceCodes = collectSourceMetricCodes(allMappings);
        System.out.println("[MetricExpansion] 映射加载完成, mappings=" + allMappings.size()
                + ", sourceCodes=" + allSourceCodes.size());

        // 按固定批次切分，避免一次 IN 过多 viewId
        List<String> viewIds = req.getViewIds();
        List<MetricExpansionExecuteRsp> results = new ArrayList<>(viewIds.size());
        int totalBatches = (viewIds.size() + VIEW_BATCH_SIZE - 1) / VIEW_BATCH_SIZE;
        for (int from = 0; from < viewIds.size(); from += VIEW_BATCH_SIZE) {
            int to = Math.min(from + VIEW_BATCH_SIZE, viewIds.size());
            int batchIndex = from / VIEW_BATCH_SIZE + 1;
            List<String> batchViewIds = viewIds.subList(from, to);
            System.out.println("[MetricExpansion] 批次 " + batchIndex + "/" + totalBatches
                    + " 开始, 视图 " + (from + 1) + "-" + to + "/" + viewIds.size());
            results.addAll(executeBatch(batchViewIds, allSourceCodes, allMappings, req));
            System.out.println("[MetricExpansion] 批次 " + batchIndex + "/" + totalBatches + " 完成");
        }
        return results;
    }

    /**
     * 按业务线替换模板下全部视图（门户替换场景，不写影子表）。
     *
     * 入参：源模板 tplId、sourceBusinessline、targetBusinessline（均必填）。
     *
     * 与 execute + promote 的区别：
     *   - 不读写影子表，膨胀结果直接写入正式 ssd_query_template_cfg / cfg_dtl
     *   - 复制整模板为新 tplId，并为每个视图生成新 viewId、新 cfgId
     *   - 固定 targetBusinessline，不按 owner 组织收窄映射
     *   - 映射表未配置时仅做 filter 替换，不执行指标膨胀
     *
     * 处理步骤：
     *   1. 校验入参，加载源模板及下属全部视图（含 tplConfig）
     *   2. 按源/目标业务线加载指标映射
     *   3. 逐视图膨胀配置，组装新 view/cfg/dtl 及 old→new viewId 映射
     *   4. 复制模板跳转链接（源/同模板内目标 viewId 映射到新 id）
     *   5. 同一事务写入 ssd_query_template、ssd_query_template_cfg、cfg_dtl、ssd_query_template_view、跳转链接
     *
     * @param req 模板 id 及源/目标业务线
     * @return 新模板 id 与全部视图的 oldViewId → newViewId 映射
     */
    public MetricExpansionReplaceTplRsp replaceTemplateViews(MetricExpansionReplaceTplReq req) {
        long methodStart = System.currentTimeMillis();
        // 1. 校验入参
        Preconditions.checkArgument(req != null, "请求不能为空");
        Preconditions.checkArgument(StrUtil.isNotEmpty(req.getTplId()), "tplId 不能为空");
        Preconditions.checkArgument(StrUtil.isNotEmpty(req.getSourceBusinessline()), "sourceBusinessline 不能为空");
        Preconditions.checkArgument(StrUtil.isNotEmpty(req.getTargetBusinessline()), "targetBusinessline 不能为空");

        TemplateAddReq tplQuery = new TemplateAddReq();
        tplQuery.setTplId(req.getTplId());
        TemplateEntity sourceTpl = (TemplateEntity) dao.queryObject(
                "ssm.template.queryTemplateEntityById", tplQuery);
        if (sourceTpl == null) {
            throw new SSDException("模板不存在: " + req.getTplId());
        }

        List<TemplateViewEntity> sourceViews = dao.queryObjectList(
                "ssm.template.view.queryViewWithCfgByTplIds",
                java.util.Collections.singletonList(req.getTplId()), TemplateViewEntity.class);
        if (CollUtil.isEmpty(sourceViews)) {
            throw new SSDException("模板下无视图: " + req.getTplId());
        }

        // 2. 加载指标映射及 source 编码全集
        List<MetricExpansionMappingEntity> mappings = loadMappings(
                req.getSourceBusinessline(), req.getTargetBusinessline());
        Set<String> sourceCodes = collectSourceMetricCodes(mappings);

        String newTplId = Guid.id();
        String operator = resolveOperator();
        List<TemplateViewEntity> newViews = new ArrayList<>();
        List<TemplateCfgEntity> newCfgs = new ArrayList<>();
        List<TemplateCfgDtlEntity> newDtls = new ArrayList<>();
        List<MetricExpansionReplaceTplRsp.ViewIdMapping> viewIdMappings = new ArrayList<>();
        Map<String, String> viewIdMap = new LinkedHashMap<>();
        Map<String, String> cfgIdMap = new LinkedHashMap<>();
        List<PortalReplaceFieldMappingWrite> portalFieldMappingWrites = new ArrayList<>();

        // 3. 逐视图膨胀并组装待写入实体
        long expandStart = System.currentTimeMillis();
        for (TemplateViewEntity sourceView : sourceViews) {
            ExpandedViewConfig expanded = expandViewConfigForPortalReplace(
                    sourceView, sourceCodes, mappings, req.getSourceBusinessline(), req.getTargetBusinessline());

            String newViewId = Guid.id();
            String newCfgId = Guid.id();
            viewIdMap.put(sourceView.getViewId(), newViewId);
            cfgIdMap.put(sourceView.getCfgId(), newCfgId);
            newCfgs.add(new TemplateCfgEntity(newCfgId, expanded.getTplConfigJson()));
            TemplateCfgDtlEntity newDtl = toFormalDtl(newCfgId, expanded);
            if (newDtl != null) {
                newDtls.add(newDtl);
            }

            if (CollUtil.isNotEmpty(expanded.getContext().getFieldMappings())) {
                PortalReplaceFieldMappingWrite mappingWrite = new PortalReplaceFieldMappingWrite();
                mappingWrite.setOldViewId(sourceView.getViewId());
                mappingWrite.setOldTplId(req.getTplId());
                mappingWrite.setNewViewId(newViewId);
                mappingWrite.setNewTplId(newTplId);
                mappingWrite.setNewCfgId(newCfgId);
                mappingWrite.setFieldMappings(expanded.getContext().getFieldMappings());
                portalFieldMappingWrites.add(mappingWrite);
            }

            TemplateViewEntity newView = new TemplateViewEntity();
            newView.setViewId(newViewId);
            newView.setViewName(sourceView.getViewName());
            newView.setViewType(sourceView.getViewType());
            newView.setViewStatus(sourceView.getViewStatus());
            newView.setTplId(newTplId);
            newView.setCfgId(newCfgId);
            newView.setSortId(sourceView.getSortId());
            newView.setDatasetId(StrUtil.isNotEmpty(sourceView.getDatasetId())
                    ? sourceView.getDatasetId() : sourceTpl.getDatasetId());
            newView.setIsActive(sourceView.getIsActive());
            newView.setAssetExpiresTime(sourceView.getAssetExpiresTime());
            newView.setCreatedBy(operator);
            newViews.add(newView);

            viewIdMappings.add(MetricExpansionReplaceTplRsp.ViewIdMapping.builder()
                    .oldViewId(sourceView.getViewId())
                    .newViewId(newViewId)
                    .build());
        }

        long expandCost = System.currentTimeMillis() - expandStart;

        TemplateEntity newTpl = new TemplateEntity();
        newTpl.setTplId(newTplId);
        newTpl.setTplName(sourceTpl.getTplName());
        newTpl.setTplDesc(sourceTpl.getTplDesc());
        newTpl.setTplType(sourceTpl.getTplType());
        newTpl.setCtgId(sourceTpl.getCtgId());
        newTpl.setTplOwner(sourceTpl.getTplOwner());
        newTpl.setDatasetId(sourceTpl.getDatasetId());
        newTpl.setCreatedBy(operator);
        String newTplCfgId = cfgIdMap.get(sourceTpl.getCfgId());
        newTpl.setCfgId(StrUtil.isNotEmpty(newTplCfgId) ? newTplCfgId : newViews.get(0).getCfgId());

        // 4. 复制跳转链接：源 view/tpl 映射到新 id；同模板内目标 view 一并映射
        List<TemplateLinkEntity> templateLinkEntityList = buildRemappedTemplateLinks(
                req.getTplId(), newTplId, viewIdMap);

        // 5. 同一事务写入新模板、cfg、cfg_dtl、视图、跳转链接、字段 id 映射
        long writeStart = System.currentTimeMillis();
        final List<TemplateLinkEntity> linkEntitiesToInsert = templateLinkEntityList;
        final List<PortalReplaceFieldMappingWrite> fieldMappingWrites = portalFieldMappingWrites;
        final String sourceBusinessline = req.getSourceBusinessline();
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.insert("ssm.template.saveTemplate", newTpl);
                for (TemplateCfgEntity cfgEntity : newCfgs) {
                    dao.insert("ssm.template.saveTemplateCfg", cfgEntity);
                }
                for (TemplateCfgDtlEntity dtlEntity : newDtls) {
                    dao.insert("ssm.query.template.cfg.dtl.add", dtlEntity);
                }
                Map<String, Object> viewParam = new HashMap<>();
                viewParam.put("viewList", newViews);
                dao.insert("ssm.template.view.bacthAdd", viewParam);

                if (CollUtil.isNotEmpty(linkEntitiesToInsert)) {
                    Map<String, Object> linkParam = new HashMap<>();
                    linkParam.put("templateLinkEntityList", linkEntitiesToInsert);
                    dao.insert("ssm.template.link.batchInsertTemplateLink", linkParam);
                    templateLinkService.saveTemplateLinkFiled(linkEntitiesToInsert);
                }

                for (PortalReplaceFieldMappingWrite mappingWrite : fieldMappingWrites) {
                    // 门户替换无影子表，shadow_cfg_id 写入新 cfg id
                    metricExpansionFieldMappingService.replaceFieldMappings(
                            mappingWrite.getNewCfgId(),
                            mappingWrite.getNewViewId(),
                            mappingWrite.getNewTplId(),
                            mappingWrite.getNewCfgId(),
                            mappingWrite.getOldViewId(),
                            mappingWrite.getOldTplId(),
                            sourceBusinessline,
                            mappingWrite.getFieldMappings());
                }
            }
        });

        log.info("metric expansion replace template {} -> {}, views={}, 膨胀耗时{}ms, 落库耗时{}ms, 总耗时{}ms",
                req.getTplId(), newTplId, viewIdMappings.size(),
                expandCost, System.currentTimeMillis() - writeStart, System.currentTimeMillis() - methodStart);
        try {
            Thread.sleep(300);
        }catch (Exception e){
            e.printStackTrace();
        }

        log.info("metric expansion replace template {} -> {}, views={}",
                req.getTplId(), newTplId, viewIdMappings.size());
        return MetricExpansionReplaceTplRsp.builder()
                .newTplId(newTplId)
                .viewIdMappings(viewIdMappings)
                .build();
    }

    /**
     * 已生成门户模板原地重跑：按 field_mapping 血缘从源视图重新膨胀，覆盖写入目标视图正式 cfg。
     *
     * 与 replaceTemplateViews（COPY）的区别：
     * - tplId / viewId / cfgId 不变，不复制模板、不重写跳转链接
     * - UPDATE ssd_query_template_cfg、ssd_query_template_cfg_dtl，不写影子表
     * - 输入始终来自 mapping.old_view_id 对应源视图（避免对已膨胀配置二次膨胀）
     *
     * @param req tplIds、sourceBusinessline、targetBusinessline 必填；viewIds 可选
     * @return 逐视图更新结果
     */
    public MetricExpansionUpdateTplRsp updateReplacedTemplateViews(MetricExpansionUpdateTplReq req) {
        Preconditions.checkArgument(req != null, "请求不能为空");
        Preconditions.checkArgument(CollUtil.isNotEmpty(req.getTplIds()), "tplIds 不能为空");
        Preconditions.checkArgument(StrUtil.isNotEmpty(req.getSourceBusinessline()), "sourceBusinessline 不能为空");
        Preconditions.checkArgument(StrUtil.isNotEmpty(req.getTargetBusinessline()), "targetBusinessline 不能为空");

        List<String> tplIds = req.getTplIds().stream()
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (CollUtil.isEmpty(tplIds)) {
            throw new SSDException("tplIds 不能为空");
        }

        List<MetricExpansionMappingEntity> mappings = loadMappings(
                req.getSourceBusinessline(), req.getTargetBusinessline());
        Set<String> sourceCodes = collectSourceMetricCodes(mappings);

        // 仅枚举目标模板下视图元数据（viewId/cfgId/tplId）；膨胀输入来自源视图，不读目标 tplConfig
        Map<String, Object> viewQueryParam = new HashMap<>();
        viewQueryParam.put("tplIdList", tplIds);
        List<TemplateViewEntity> targetViews = dao.queryObjectList(
                "ssm.template.view.batchGetByTplId", viewQueryParam, TemplateViewEntity.class);
        if (CollUtil.isEmpty(targetViews)) {
            throw new SSDException("模板下无视图: " + tplIds);
        }

        Set<String> viewIdFilter = CollUtil.isEmpty(req.getViewIds())
                ? null
                : req.getViewIds().stream().filter(StrUtil::isNotEmpty).collect(java.util.stream.Collectors.toSet());

        List<MetricExpansionUpdateTplRsp.ViewResult> viewResults = new ArrayList<>();
        for (TemplateViewEntity targetView : targetViews) {
            if (viewIdFilter != null && !viewIdFilter.contains(targetView.getViewId())) {
                continue;
            }
            viewResults.add(updateOneReplacedView(
                    targetView, mappings, sourceCodes, req.getSourceBusinessline(), req.getTargetBusinessline()));

            try {
                Thread.sleep(300);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        if (viewIdFilter != null && viewResults.isEmpty()) {
            throw new SSDException("viewIds 与 tplIds 下视图无交集");
        }

        log.info("metric expansion update replaced templates tplIds={}, views={}", tplIds, viewResults.size());
        return MetricExpansionUpdateTplRsp.builder().viewResults(viewResults).build();
    }

    /**
     * 单视图原地更新：源视图重跑膨胀后覆盖目标 cfg / dtl / field_mapping。
     */
    private MetricExpansionUpdateTplRsp.ViewResult updateOneReplacedView(TemplateViewEntity targetView,
                                                                         List<MetricExpansionMappingEntity> mappings,
                                                                         Set<String> sourceCodes,
                                                                         String sourceBusinessline,
                                                                         String targetBusinessline) {
        MetricExpansionUpdateTplRsp.ViewResult result = MetricExpansionUpdateTplRsp.ViewResult.builder()
                .tplId(targetView.getTplId())
                .viewId(targetView.getViewId())
                .cfgId(targetView.getCfgId())
                .updated(false)
                .skipped(false)
                .build();
        try {
            TemplateViewEntity sourceView = loadSourceViewByFieldMapping(targetView.getViewId(), sourceBusinessline);
            ExpandedViewConfig expanded = expandViewConfigForPortalReplace(
                    sourceView, sourceCodes, mappings, sourceBusinessline, targetBusinessline);

            final String cfgId = targetView.getCfgId();
            final String tplConfig = expanded.getContext().getTplConfigJson().toJSONString();
            final TemplateCfgDtlEntity dtlEntity = toFormalDtl(cfgId, expanded);
            final List<MetricExpansionFieldMappingEntity> fieldMappings = expanded.getContext().getFieldMappings();
            final String oldViewId = sourceView.getViewId();
            final String oldTplId = sourceView.getTplId();

            dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                @Override
                public void execute() {
                    dao.update("ssm.template.updateTemplateCfg", new TemplateCfgEntity(cfgId, tplConfig));
                    if (dtlEntity != null) {
                        dao.update("ssm.query.template.cfg.dtl.update", dtlEntity);
                    }
                    if (CollUtil.isNotEmpty(fieldMappings)) {
                        metricExpansionFieldMappingService.replaceFieldMappings(
                                cfgId,
                                targetView.getViewId(),
                                targetView.getTplId(),
                                cfgId,
                                oldViewId,
                                oldTplId,
                                sourceBusinessline,
                                fieldMappings);
                    }
                }
            });
            result.setUpdated(true);
        } catch (Exception e) {
            log.error("update replaced view failed, viewId={}", targetView.getViewId(), e);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    /**
     * 通过 field_mapping 血缘加载源视图 tplConfig（膨胀输入必须为源视图，不能为目标视图）。
     *
     * @param targetViewId       已生成目标视图 id
     * @param sourceBusinessline 源业务线（与 mapping 一致）
     * @return 源视图（含 tplConfig）
     */
    private TemplateViewEntity loadSourceViewByFieldMapping(String targetViewId, String sourceBusinessline) {
        List<MetricExpansionFieldMappingEntity> mappings = dao.queryObjectList(
                "ssm.metric.expansion.field.mapping.listByViewId",
                targetViewId,
                MetricExpansionFieldMappingEntity.class);
        if (CollUtil.isEmpty(mappings)) {
            throw new SSDException("视图无字段映射记录，无法原地重跑，请先执行 replace/template: " + targetViewId);
        }
        MetricExpansionFieldMappingEntity lineage = mappings.stream()
                .filter(item -> sourceBusinessline.equals(item.getSourceBusinessline()))
                .findFirst()
                .orElse(null);
        if (lineage == null || StrUtil.isEmpty(lineage.getOldViewId())) {
            throw new SSDException("视图无源业务线映射血缘: viewId=" + targetViewId
                    + ", sourceBusinessline=" + sourceBusinessline);
        }

        List<TemplateViewEntity> sourceViews = dao.queryObjectList(
                "ssm.template.view.queryViewWithCfgByIds",
                java.util.Collections.singletonList(lineage.getOldViewId()),
                TemplateViewEntity.class);
        if (CollUtil.isEmpty(sourceViews) || sourceViews.get(0) == null) {
            throw new SSDException("源视图不存在或 tplConfig 为空: oldViewId=" + lineage.getOldViewId());
        }
        TemplateViewEntity sourceView = sourceViews.get(0);
        if (StrUtil.isEmpty(sourceView.getTplConfig())) {
            throw new SSDException("源视图 tplConfig 为空: oldViewId=" + lineage.getOldViewId());
        }
        return sourceView;
    }

    /**
     * 门户替换专用：对单视图执行筛选替换与指标膨胀（内存态，不写库）。
     *
     * 与 expandViewConfig 的区别：固定 targetBusinessline，不做 owner 组织过滤；
     * filter 无论数据集是否在膨胀白名单，均按 source → 唯一 target 替换（映射为空时不做指标膨胀）。
     * 维度侧字段沿用正式 cfg_dtl，指标侧在白名单数据集且映射非空且命中 source 时膨胀。
     *
     * @param view               含 tplConfig 的源视图
     * @param sourceCodes        映射表全部 source 编码
     * @param mappings           已按 source/target 业务线收窄的映射
     * @param sourceBusinessline 源业务线
     * @param targetBusinessline 目标业务线
     * @return 膨胀后的 tplConfig 与 measure 派生字段，含原 cfg_dtl 供组装新 dtl
     */
    private ExpandedViewConfig expandViewConfigForPortalReplace(TemplateViewEntity view,
                                                                Set<String> sourceCodes,
                                                                List<MetricExpansionMappingEntity> mappings,
                                                                String sourceBusinessline,
                                                                String targetBusinessline) {
        MetricExpansionContext context = buildContext(view, sourceCodes, sourceBusinessline);
        TemplateCfgDtlEntity oldDtl = loadFormalCfgDtl(view.getViewId());
        String measureCodes = oldDtl != null ? oldDtl.getTplConfigFieldMeasureCodes() : null;
        String measureAsset = oldDtl != null ? oldDtl.getTplConfigFieldMeasureAsset() : null;

        // 门户替换：无论是否指标膨胀数据集，filter 均将 sourceBusinessline 替换为唯一 target（不追加多业务线）
        metricExpansionFilterRewriteProcessor.rewriteFiltersByTargets(
                context.getTplConfigJson(), sourceBusinessline,
                java.util.Collections.singletonList(targetBusinessline));

        if (MetricExpansionDatasetUtil.isExpansionDataset(view.getDatasetId())
                && CollUtil.isNotEmpty(mappings)) {
            tplConfigExpansionProcessor.scanSourceMetricCodes(context);
            if (CollUtil.isNotEmpty(context.getActiveSourceCodes())) {
                loadAndValidateTargets(context, mappings);
                tplConfigExpansionProcessor.expandTplConfig(context);
                calcFieldExpansionProcessor.processCalcFields(context);
                String[] derivedFields = derivedFieldBuilder.rebuildDerivedFields(context.getTplConfigJson());
                measureCodes = derivedFields[0];
                measureAsset = derivedFields[1];
            }
        }

        ExpandedViewConfig result = new ExpandedViewConfig();
        result.setContext(context);
        result.setOldDtl(oldDtl);
        result.setMeasureCodes(measureCodes);
        result.setMeasureAsset(measureAsset);
        return result;
    }

    /**
     * 由膨胀结果与原 cfg_dtl 组装新正式 cfg_dtl。
     * 维度侧拷贝原值，指标侧使用膨胀后的 measureCodes / measureAsset。
     *
     * @param cfgId    新 cfg id
     * @param expanded 单视图膨胀结果
     * @return 待 insert 的 dtl；原 dtl 与 measure 均为空时返回 null
     */
    private TemplateCfgDtlEntity toFormalDtl(String cfgId, ExpandedViewConfig expanded) {
        TemplateCfgDtlEntity oldDtl = expanded.getOldDtl();
        if (oldDtl == null && StrUtil.isEmpty(expanded.getMeasureCodes())
                && StrUtil.isEmpty(expanded.getMeasureAsset())) {
            return null;
        }
        TemplateCfgDtlEntity dtl = new TemplateCfgDtlEntity(
                cfgId,
                oldDtl != null ? oldDtl.getTplConfigFieldDimCodes() : null,
                expanded.getMeasureCodes());
        dtl.setTplConfigFieldDimAsset(oldDtl != null ? oldDtl.getTplConfigFieldDimAsset() : null);
        dtl.setTplConfigFieldMeasureAsset(expanded.getMeasureAsset());
        return dtl;
    }

    /**
     * 复制源模板跳转链接到新模板。
     * 源 tpl/view 映射为新 id；若跳转目标也在同一源模板内，目标 view/tpl 一并映射。
     *
     * @param oldTplId  源模板 id
     * @param newTplId  新模板 id
     * @param viewIdMap 旧 viewId → 新 viewId
     * @return 待 insert 的跳转实体列表
     */
    private List<TemplateLinkEntity> buildRemappedTemplateLinks(String oldTplId,
                                                                String newTplId,
                                                                Map<String, String> viewIdMap) {
        Map<String, Object> linkQueryParam = new HashMap<>();
        linkQueryParam.put("tplIds", java.util.Collections.singletonList(oldTplId));
        List<TemplateLinkRsp> linkList = dao.queryObjectList(
                "ssm.template.link.queryLinkToOtherTemplateList", linkQueryParam, TemplateLinkRsp.class);
        if (CollUtil.isEmpty(linkList)) {
            return new ArrayList<>();
        }

        templateLinkService.buildTemplateLinkField(linkList);

        List<TemplateLinkRsp> remappedLinks = new ArrayList<>();
        for (TemplateLinkRsp linkItem : linkList) {
            String newSourceViewId = viewIdMap.get(linkItem.getQueryViewId());
            if (StrUtil.isEmpty(newSourceViewId)) {
                continue;
            }
            linkItem.setQueryTplId(newTplId);
            linkItem.setQueryViewId(newSourceViewId);
            if (oldTplId.equals(linkItem.getTplId())) {
                String newTargetViewId = viewIdMap.get(linkItem.getViewId());
                if (StrUtil.isEmpty(newTargetViewId)) {
                    log.warn("skip template link, internal target view not mapped: tplId={}, viewId={}",
                            oldTplId, linkItem.getViewId());
                    continue;
                }
                linkItem.setTplId(newTplId);
                linkItem.setViewId(newTargetViewId);
            }
            remappedLinks.add(linkItem);
        }
        return templateLinkService.buildLinkEntityList(remappedLinks);
    }

    /**
     * 按源/目标业务线加载指标映射（门户 replaceTemplateViews 与 execute 共用）。
     *
     * @param sourceBusinessline 源业务线
     * @param targetBusinessline 目标业务线
     * @return 映射列表；无配置时返回空列表，仅做 filter 替换
     */
    private List<MetricExpansionMappingEntity> loadMappings(String sourceBusinessline,
                                                            String targetBusinessline) {
        MetricExpansionExecuteReq expandReq = new MetricExpansionExecuteReq();
        expandReq.setSourceBusinessline(sourceBusinessline);
        expandReq.setTargetBusinessline(targetBusinessline);
        return loadMappings(expandReq, false);
    }

    /**
     * 处理一批 viewIds：加载本批视图与 owner 信息后逐个膨胀。
     * 本批查不到的 viewId 记入 errorMessage，不中断同批及其他批次。
     *
     * @param batchViewIds   本批 viewId（最多 {@link #VIEW_BATCH_SIZE}）
     * @param allSourceCodes 源业务线映射下全部 source 编码（扫描候选全集）
     * @param allMappings    源业务线映射（指定 target 时已收窄；否则为全量，待按视图再过滤）
     * @param req            原始请求
     * @return 与 batchViewIds 顺序对应的结果
     */
    private List<MetricExpansionExecuteRsp> executeBatch(List<String> batchViewIds,
                                                         Set<String> allSourceCodes,
                                                         List<MetricExpansionMappingEntity> allMappings,
                                                         MetricExpansionExecuteReq req) {
        // 本批视图 + tplConfig；含 createdBy，供个人视图取 owner
        List<TemplateViewEntity> viewList = dao.queryObjectList(
                "ssm.template.view.queryViewWithCfgByIds", batchViewIds, TemplateViewEntity.class);

        Map<String, TemplateViewEntity> viewById = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(viewList)) {
            for (TemplateViewEntity view : viewList) {
                if (view != null && StrUtil.isNotEmpty(view.getViewId())) {
                    viewById.put(view.getViewId(), view);
                }
            }
        }

        // 仅未指定 targetBusinessline 时需要按 owner 组织过滤，预加载本批 tplOwner 与 DEPT_ID
        Map<String, String> tplOwnerByTplId = new HashMap<>();
        Map<String, HrEmployee> employeeByName = new HashMap<>();
        if (StrUtil.isEmpty(req.getTargetBusinessline()) && CollUtil.isNotEmpty(viewList)) {
            tplOwnerByTplId = loadTplOwnerByTplIds(viewList);
            employeeByName = loadEmployeesForOwnerFilter(viewList, tplOwnerByTplId);
        }

        List<MetricExpansionExecuteRsp> results = new ArrayList<>(batchViewIds.size());
        for (int i = 0; i < batchViewIds.size(); i++) {
            String viewId = batchViewIds.get(i);
            System.out.println("[MetricExpansion] 处理视图 [" + (i + 1) + "/" + batchViewIds.size()
                    + "] viewId=" + viewId);
            TemplateViewEntity view = viewById.get(viewId);
            MetricExpansionExecuteRsp rsp = new MetricExpansionExecuteRsp();
            rsp.setViewId(viewId);
            if (view == null) {
                rsp.setErrorMessage("视图不存在或无配置: " + viewId);
                System.out.println("[MetricExpansion] 视图失败 viewId=" + viewId + ", error=" + rsp.getErrorMessage());
                results.add(rsp);
                continue;
            }
            try {
                rsp = executeOne(view, allSourceCodes, allMappings, req, tplOwnerByTplId, employeeByName);
                if (StrUtil.isNotEmpty(rsp.getErrorMessage())) {
                    System.out.println("[MetricExpansion] 视图失败 viewId=" + viewId
                            + ", error=" + rsp.getErrorMessage());
                } else {
                    System.out.println("[MetricExpansion] 视图完成 viewId=" + viewId
                            + ", skipped=" + rsp.getSkipped()
                            + ", filterRewritten=" + rsp.getFilterRewritten()
                            + ", metricExpanded=" + rsp.getMetricExpanded());
                }
            } catch (Exception e) {
                // 单视图失败隔离：记录错误后继续处理后续 viewId
                log.error("指标膨胀失败 viewId={}", viewId, e);
                rsp.setViewId(view.getViewId());
                rsp.setTplId(view.getTplId());
                rsp.setCfgId(view.getCfgId());
                rsp.setErrorMessage(e.getMessage());
                System.out.println("[MetricExpansion] 视图异常 viewId=" + viewId + ", error=" + e.getMessage());
            }
            results.add(rsp);
        }
        return results;
    }

    /**
     * 按入参加载映射：每次只处理一个 sourceBusinessline。
     * 门户场景指定 targetBusinessline 时 SQL 一并收窄；
     * 未指定则查该源业务线下全部，后续在各视图上按 owner 的 DEPT_ID 再过滤。
     *
     * @param req 执行请求（sourceBusinessline 必填）
     * @return 映射列表；无配置时返回空列表，支持纯 filter 替换
     */
    private List<MetricExpansionMappingEntity> loadMappings(MetricExpansionExecuteReq req) {
        return loadMappings(req, false);
    }

    /**
     * 按入参加载映射：每次只处理一个 sourceBusinessline。
     * 门户场景指定 targetBusinessline 时 SQL 一并收窄；
     * 未指定则查该源业务线下全部，后续在各视图上按 owner 的 DEPT_ID 再过滤。
     *
     * @param req              执行请求（sourceBusinessline 必填）
     * @param requireNonEmpty  true 时映射为空抛异常；false 时返回空列表（execute / replaceTemplateViews 纯 filter 替换）
     * @return 映射列表（按 pkid 升序）
     */
    private List<MetricExpansionMappingEntity> loadMappings(MetricExpansionExecuteReq req,
                                                            boolean requireNonEmpty) {
        Preconditions.checkArgument(StrUtil.isNotEmpty(req.getSourceBusinessline()),
                "sourceBusinessline 不能为空");

        Map<String, Object> params = new HashMap<>();
        params.put("sourceBusinessline", req.getSourceBusinessline());
        // 门户替换模板场景会带 targetBusinessline，直接在 SQL 侧过滤
        if (StrUtil.isNotEmpty(req.getTargetBusinessline())) {
            params.put("targetBusinessline", req.getTargetBusinessline());
        }

        List<MetricExpansionMappingEntity> mappings = dao.queryObjectList(
                "ssm.metric.expansion.mapping.listByBusinessline",
                params, MetricExpansionMappingEntity.class);
        if (CollUtil.isEmpty(mappings)) {
            if (requireNonEmpty) {
                String msg = "指标膨胀映射为空: sourceBusinessline=" + req.getSourceBusinessline();
                if (StrUtil.isNotEmpty(req.getTargetBusinessline())) {
                    msg = msg + ", targetBusinessline=" + req.getTargetBusinessline();
                }
                throw new SSDException(msg);
            }
            return new ArrayList<>();
        }
        return mappings;
    }

    /**
     * 未指定 targetBusinessline 时，按视图 owner 的 DEPT_ID 收窄映射。
     * 公共视图：模板 tplOwner（先剔除 BI 部门 200704）；个人视图：视图 createdBy。
     * 收窄规则见 {@link MetricExpansionOwnerUtil}。
     *
     * @param view             当前视图
     * @param allMappings      源业务线下全量映射
     * @param tplOwnerByTplId  tplId → tplOwner 缓存
     * @param employeeByName   用户名 → HR 员工（含 DEPT_ID）
     * @param sourceBusinessline 入参源业务线
     * @return 本视图可用映射
     */
    private List<MetricExpansionMappingEntity> filterMappingsByOwner(
            TemplateViewEntity view,
            List<MetricExpansionMappingEntity> allMappings,
            Map<String, String> tplOwnerByTplId,
            Map<String, HrEmployee> employeeByName,
            String sourceBusinessline) {
        if (CollUtil.isEmpty(allMappings)) {
            return new ArrayList<>();
        }
        List<String> ownerNames = resolveViewOwnerNames(view, tplOwnerByTplId);
        // 仅公共视图剔除 BI owner；个人视图创建人直接参与组织判断
        if (TemplateViewType.PUBLIC.getCode().equalsIgnoreCase(view.getViewType())) {
            ownerNames = MetricExpansionOwnerUtil.removeBiOwners(ownerNames, employeeByName);
        }
        Set<String> matched = MetricExpansionOwnerUtil.resolveMatchedBusinesslines(
                ownerNames, employeeByName, sourceBusinessline);
        List<MetricExpansionMappingEntity> filtered =
                MetricExpansionOwnerUtil.filterByBusinesslines(allMappings, matched);
        if (CollUtil.isEmpty(filtered)) {
            throw new SSDException("按 owner 组织过滤后映射为空, viewId=" + view.getViewId()
                    + ", matched=" + matched);
        }
        return filtered;
    }

    /**
     * 解析视图用于组织判断的 owner 用户名。
     * 公共视图取模板 tplOwner（可多人）；个人视图取视图 createdBy。
     *
     * @param view            视图
     * @param tplOwnerByTplId 模板 owner 缓存（仅公共视图使用）
     * @return owner 用户名列表
     */
    private List<String> resolveViewOwnerNames(TemplateViewEntity view,
                                               Map<String, String> tplOwnerByTplId) {
        if (TemplateViewType.PERSONAL.getCode().equalsIgnoreCase(view.getViewType())) {
            List<String> names = new ArrayList<>();
            if (StrUtil.isNotEmpty(view.getCreatedBy())) {
                names.add(view.getCreatedBy());
            }
            return names;
        }
        String tplOwner = tplOwnerByTplId == null ? null : tplOwnerByTplId.get(view.getTplId());
        return MetricExpansionOwnerUtil.splitOwnerNames(tplOwner);
    }

    /**
     * 批量加载本批公共视图对应模板的 tplOwner。
     * 个人视图不查模板 owner，避免多余 IO。
     *
     * @param viewList 本批视图列表
     * @return tplId → tplOwner（逗号分隔多用户）
     */
    private Map<String, String> loadTplOwnerByTplIds(List<TemplateViewEntity> viewList) {
        Map<String, String> result = new HashMap<>();
        Set<String> tplIds = new LinkedHashSet<>();
        for (TemplateViewEntity view : viewList) {
            if (view != null && StrUtil.isNotEmpty(view.getTplId())
                    && TemplateViewType.PUBLIC.getCode().equalsIgnoreCase(view.getViewType())) {
                tplIds.add(view.getTplId());
            }
        }
        if (CollUtil.isEmpty(tplIds)) {
            return result;
        }
        List<TemplateEntity> templates = dao.queryObjectList(
                "ssm.template.listTplOwnerByTplIds", new ArrayList<>(tplIds), TemplateEntity.class);
        if (CollUtil.isEmpty(templates)) {
            return result;
        }
        for (TemplateEntity template : templates) {
            if (template != null && StrUtil.isNotEmpty(template.getTplId())) {
                result.put(template.getTplId(), template.getTplOwner());
            }
        }
        return result;
    }

    /**
     * 收集本批公共/个人视图 owner 用户名，并批量查询 HR 的 DEPT_ID。
     * 用于后续 BI 剔除与油液/配件业务线匹配。
     *
     * @param viewList        本批视图列表
     * @param tplOwnerByTplId 模板 owner 缓存
     * @return 用户名 → HR 员工（主要使用 DEPT_ID）
     */
    private Map<String, HrEmployee> loadEmployeesForOwnerFilter(List<TemplateViewEntity> viewList,
                                                                Map<String, String> tplOwnerByTplId) {
        Set<String> userNames = new LinkedHashSet<>();
        for (TemplateViewEntity view : viewList) {
            if (view == null) {
                continue;
            }
            // 个人视图：创建人即为 owner
            if (TemplateViewType.PERSONAL.getCode().equalsIgnoreCase(view.getViewType())) {
                if (StrUtil.isNotEmpty(view.getCreatedBy())) {
                    userNames.add(view.getCreatedBy());
                }
                continue;
            }
            // 公共视图：解析模板 tplOwner 多人列表
            userNames.addAll(MetricExpansionOwnerUtil.splitOwnerNames(
                    tplOwnerByTplId.get(view.getTplId())));
        }
        Map<String, HrEmployee> result = new HashMap<>();
        if (CollUtil.isEmpty(userNames)) {
            return result;
        }
        List<HrEmployee> employees = dao.queryObjectList(
                "user.listEmployeeDeptByUserNames", new ArrayList<>(userNames), HrEmployee.class);
        if (CollUtil.isEmpty(employees)) {
            return result;
        }
        for (HrEmployee employee : employees) {
            if (employee != null && StrUtil.isNotEmpty(employee.getUserName())) {
                result.put(employee.getUserName(), employee);
            }
        }
        return result;
    }

    /**
     * 从映射列表收集去重后的 source_metric_code。
     *
     * @param mappings 已过滤的映射
     * @return 保持首次出现顺序的 source 编码集合
     */
    private Set<String> collectSourceMetricCodes(List<MetricExpansionMappingEntity> mappings) {
        Set<String> sourceCodes = new LinkedHashSet<>();
        for (MetricExpansionMappingEntity mapping : mappings) {
            if (mapping != null && StrUtil.isNotEmpty(mapping.getSourceMetricCode())) {
                sourceCodes.add(mapping.getSourceMetricCode());
            }
        }
        return sourceCodes;
    }

    /**
     * 对单个视图执行筛选替换，并按 datasetId 决定是否继续指标膨胀。
     *
     * 执行顺序：解析 tplConfig →
     * 膨胀数据集下按映射 targetBusinessline 替换或追加 filter 源业务线值，否则按固定规则改写 filter →
     * 膨胀数据集且命中可膨胀 source 时走指标膨胀并重建 measure 派生字段；
     * 否则 measureCodes / measureAsset 沿用正式 cfg_dtl → 有变更则写入或更新影子。
     *
     * @param view             含 tplConfig、datasetId 的视图实体
     * @param allSourceCodes   源业务线映射下全部 source 编码（扫描候选全集）
     * @param allMappings      源业务线映射（指定 target 时已收窄；否则为全量）
     * @param req              原始请求（用于判断是否按 owner 过滤）
     * @param tplOwnerByTplId  公共视图模板 owner 缓存
     * @param employeeByName   owner 的 DEPT_ID 缓存
     * @return 单视图执行结果，含 filterRewritten / metricExpanded 标记
     */
    private MetricExpansionExecuteRsp executeOne(TemplateViewEntity view,
                                                 Set<String> allSourceCodes,
                                                 List<MetricExpansionMappingEntity> allMappings,
                                                 MetricExpansionExecuteReq req,
                                                 Map<String, String> tplOwnerByTplId,
                                                 Map<String, HrEmployee> employeeByName) {
        MetricExpansionExecuteRsp rsp = new MetricExpansionExecuteRsp();
        rsp.setViewId(view.getViewId());
        rsp.setTplId(view.getTplId());
        rsp.setCfgId(view.getCfgId());

        ExpandedViewConfig expandedViewConfig = expandViewConfig(
                view, allSourceCodes, allMappings, req, tplOwnerByTplId, employeeByName);
        if (!expandedViewConfig.isChanged()) {
            rsp.setSkipped(true);
            return rsp;
        }

        String shadowCfgId = saveExpandedConfig(expandedViewConfig, req);

        rsp.setShadowCfgId(shadowCfgId);
        rsp.setFilterRewritten(expandedViewConfig.isFilterRewritten());
        rsp.setMetricExpanded(expandedViewConfig.isMetricExpanded());
        rsp.setExpandedSourceCodes(expandedViewConfig.isMetricExpanded()
                ? expandedViewConfig.getExpandedSourceCodes() : null);
        rsp.setMeasureCodes(expandedViewConfig.getMeasureCodes());
        rsp.setSkipped(false);
        return rsp;
    }

    /**
     * 对单个视图执行筛选替换与指标膨胀，返回最终 tplConfig 与 measure 派生字段。
     * 不写库；门户指定 targetBusinessline 时不按 owner 过滤映射。
     */
    private ExpandedViewConfig expandViewConfig(TemplateViewEntity view,
                                                Set<String> allSourceCodes,
                                                List<MetricExpansionMappingEntity> allMappings,
                                                MetricExpansionExecuteReq req,
                                                Map<String, String> tplOwnerByTplId,
                                                Map<String, HrEmployee> employeeByName) {
        MetricExpansionContext context = buildContext(view, allSourceCodes, req.getSourceBusinessline());
        context.setPreserveSourceData(StrUtil.isEmpty(req.getTargetBusinessline()));
        TemplateCfgDtlEntity oldDtl = loadFormalCfgDtl(view.getViewId());
        String measureCodes = oldDtl != null ? oldDtl.getTplConfigFieldMeasureCodes() : null;
        String measureAsset = oldDtl != null ? oldDtl.getTplConfigFieldMeasureAsset() : null;

        boolean filterRewritten = false;
        boolean metricExpanded = false;

        if (MetricExpansionDatasetUtil.isExpansionDataset(view.getDatasetId())) {
            List<MetricExpansionMappingEntity> viewMappings = allMappings;
            if (StrUtil.isEmpty(req.getTargetBusinessline()) && CollUtil.isNotEmpty(allMappings)) {
                viewMappings = filterMappingsByOwner(
                        view, allMappings, tplOwnerByTplId, employeeByName, req.getSourceBusinessline());
            }
            if (CollUtil.isEmpty(viewMappings)) {
                // 映射未配置或 owner 收窄后为空：仅做固定规则 filter 改写，不执行指标膨胀
                filterRewritten = metricExpansionFilterRewriteProcessor.rewriteFilters(context.getTplConfigJson());
            } else {
                List<String> targetBusinesslines = collectTargetBusinesslines(viewMappings);
                filterRewritten = metricExpansionFilterRewriteProcessor.rewriteFiltersByTargets(
                        context.getTplConfigJson(), req.getSourceBusinessline(), targetBusinesslines,
                        context.isPreserveSourceData());

                tplConfigExpansionProcessor.scanSourceMetricCodes(context);
                if (CollUtil.isNotEmpty(context.getActiveSourceCodes())) {
                    loadAndValidateTargets(context, viewMappings);
                    tplConfigExpansionProcessor.expandTplConfig(context);
                    calcFieldExpansionProcessor.processCalcFields(context);
                    measureVisibilityLimitProcessor.applyExpandedMeasureVisibilityLimit(context);

                    String[] derivedFields = derivedFieldBuilder.rebuildDerivedFields(context.getTplConfigJson());
                    measureCodes = derivedFields[0];
                    measureAsset = derivedFields[1];
                    metricExpanded = true;
                }
            }
        } else {
            filterRewritten = metricExpansionFilterRewriteProcessor.rewriteFilters(context.getTplConfigJson());
        }

        ExpandedViewConfig expandedViewConfig = new ExpandedViewConfig();
        expandedViewConfig.setContext(context);
        expandedViewConfig.setOldDtl(oldDtl);
        expandedViewConfig.setMeasureCodes(measureCodes);
        expandedViewConfig.setMeasureAsset(measureAsset);
        expandedViewConfig.setFilterRewritten(filterRewritten);
        expandedViewConfig.setMetricExpanded(metricExpanded);
        return expandedViewConfig;
    }

    /**
     * 从本视图可用映射中收集去重后的 targetBusinessline（保持映射表首次出现顺序）。
     * 供指标膨胀数据集下 filter 筛选值按目标业务线扩展。
     *
     * @param mappings 本视图映射（已按 owner / 门户 target 收窄）
     * @return 目标业务线列表
     */
    private List<String> collectTargetBusinesslines(List<MetricExpansionMappingEntity> mappings) {
        List<String> result = new ArrayList<>();
        if (CollUtil.isEmpty(mappings)) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (MetricExpansionMappingEntity mapping : mappings) {
            if (mapping == null || StrUtil.isEmpty(mapping.getTargetBusinessline())) {
                continue;
            }
            if (seen.add(mapping.getTargetBusinessline())) {
                result.add(mapping.getTargetBusinessline());
            }
        }
        return result;
    }

    /**
     * 加载视图正式 cfg_dtl。
     * 仅做筛选替换、未执行指标膨胀时，measureCodes / measureAsset 原样取自该记录。
     *
     * @param viewId 视图 id
     * @return 正式 dtl；不存在时返回 null
     */
    private TemplateCfgDtlEntity loadFormalCfgDtl(String viewId) {
        return (TemplateCfgDtlEntity) dao.queryObject(
                "ssm.query.template.cfg.dtl.queryByViewId", viewId);
    }

    /**
     * 步骤一：解析库内明文 tplConfig，并初始化上下文基础字段。
     * 数据库中的 tplConfig 非加密，直接 JSON 解析即可。
     *
     * @param view           视图实体（含明文 tplConfig）
     * @param allSourceCodes 映射表全部 source
     * @param sourceBusinessline 入参源业务线
     * @return 已填充基础信息的上下文
     */
    private MetricExpansionContext buildContext(TemplateViewEntity view, Set<String> allSourceCodes,
                                                String sourceBusinessline) {
        if (StrUtil.isEmpty(view.getTplConfig())) {
            throw new SSDException("视图 tplConfig 为空: " + view.getViewId());
        }
        JSONObject tplConfigJson = JSON.parseObject(view.getTplConfig());
        if (tplConfigJson == null) {
            throw new SSDException("视图 tplConfig 解析失败: " + view.getViewId());
        }
        MetricExpansionContext context = new MetricExpansionContext();
        context.setViewId(view.getViewId());
        context.setTplId(view.getTplId());
        context.setCfgId(view.getCfgId());
        context.setTplConfigJson(tplConfigJson);
        context.setAllMappingSourceCodes(allSourceCodes);
        context.setSourceBusinessline(sourceBusinessline);
        return context;
    }

    /**
     * 步骤三：为每个 active source 从预加载映射取 target 并校验元数据。
     * 复用本视图已过滤映射，保证与 source/target 业务线及 owner 组织过滤一致。
     * target 不存在或已停用时抛出 SSDException，中止当前视图。
     *
     * @param context     运行时上下文
     * @param allMappings 本视图可用映射
     */
    private void loadAndValidateTargets(MetricExpansionContext context,
                                        List<MetricExpansionMappingEntity> allMappings) {
        Map<String, List<MetricExpansionMappingEntity>> mappingsBySource = new LinkedHashMap<>();
        for (MetricExpansionMappingEntity mapping : allMappings) {
            if (mapping == null || StrUtil.isEmpty(mapping.getSourceMetricCode())) {
                continue;
            }
            mappingsBySource
                    .computeIfAbsent(mapping.getSourceMetricCode(), key -> new ArrayList<>())
                    .add(mapping);
        }

        Map<String, List<MetricExpansionTarget>> targetsBySource = new LinkedHashMap<>();
        for (String sourceCode : context.getActiveSourceCodes()) {
            List<MetricExpansionMappingEntity> mappings = mappingsBySource.get(sourceCode);
            if (CollUtil.isEmpty(mappings)) {
                throw new SSDException("映射表无源指标配置: " + sourceCode);
            }
            List<MetricExpansionTarget> targets = new ArrayList<>();
            for (MetricExpansionMappingEntity mapping : mappings) {
                MetaField metaField = MetricExpansionMetaUtil.validateTargetMetric(mapping.getTargetMetricCode());
                MetricExpansionTarget target = new MetricExpansionTarget();
                target.setTargetMetricCode(mapping.getTargetMetricCode());
                target.setTargetBusinessline(mapping.getTargetBusinessline());
                target.setMetaField(metaField);
                targets.add(target);
            }
            targetsBySource.put(sourceCode, targets);
        }
        context.setTargetsBySourceCode(targetsBySource);
    }

    /**
     * 将 execute 结果写入影子库（已有影子则覆盖更新，不更新正式表）。
     * 维度资产始终取自正式 dtl；measure 字段由 expandedViewConfig 提供（膨胀后 rebuild 或原样拷贝）。
     *
     * @param expandedViewConfig 单视图膨胀结果，含 context、oldDtl、最终 tplConfigJson、measureCodes、measureAsset
     * @param req                原始请求（记录源/目标业务线到影子元数据）
     * @return 新生成的 shadowCfgId
     */
    private String saveExpandedConfig(ExpandedViewConfig expandedViewConfig,
                                      MetricExpansionExecuteReq req) {
        MetricExpansionContext context = expandedViewConfig.getContext();
        TemplateCfgDtlEntity oldDtl = expandedViewConfig.getOldDtl();
        String cfgId = context.getCfgId();
        String viewId = context.getViewId();
        String tplId = context.getTplId();
        String tplConfigJson = expandedViewConfig.getTplConfigJson();
        String measureCodes = expandedViewConfig.getMeasureCodes();
        String measureAsset = expandedViewConfig.getMeasureAsset();

        TemplateCfgDtlEntity newDtl = new TemplateCfgDtlEntity(
                cfgId,
                oldDtl != null ? oldDtl.getTplConfigFieldDimCodes() : null,
                measureCodes);
        newDtl.setTplConfigFieldDimAsset(oldDtl != null ? oldDtl.getTplConfigFieldDimAsset() : null);
        newDtl.setTplConfigFieldMeasureAsset(measureAsset);

        final String[] shadowCfgIdHolder = new String[1];
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                shadowCfgIdHolder[0] = metricExpansionShadowService.saveShadowForExecute(
                        cfgId, viewId, tplId, tplConfigJson, newDtl,
                        req.getSourceBusinessline(), req.getTargetBusinessline());
                // 无论是否膨胀都同步映射：未膨胀时 fieldMappings 为空，会先删旧映射再返回，避免降级场景脏数据
                metricExpansionFieldMappingService.replaceFieldMappings(
                        shadowCfgIdHolder[0],
                        context,
                        req.getSourceBusinessline(),
                        context.getFieldMappings());
            }
        });
        return shadowCfgIdHolder[0];
    }

    /**
     * 应用上线：将影子配置写入新正式 cfg/dtl，并将视图 cfg_id 切换到新 cfg。
     * viewIds 可空：未传时上线影子表中全部视图。
     *
     * @param viewIds 视图 id 列表，可空
     * @return 逐视图结果；无待上线视图时返回空列表
     */
    public List<MetricExpansionPromoteRsp> promote(List<String> viewIds) {
        List<String> targetViewIds = resolvePromoteViewIds(viewIds);
        if (CollUtil.isEmpty(targetViewIds)) {
            System.out.println("[MetricExpansion] promote 无待上线视图");
            return new ArrayList<>();
        }
        System.out.println("[MetricExpansion] promote 待上线视图数=" + targetViewIds.size());
        List<MetricExpansionPromoteRsp> results = new ArrayList<>(targetViewIds.size());
        int totalBatches = (targetViewIds.size() + VIEW_BATCH_SIZE - 1) / VIEW_BATCH_SIZE;
        for (int from = 0; from < targetViewIds.size(); from += VIEW_BATCH_SIZE) {
            int to = Math.min(from + VIEW_BATCH_SIZE, targetViewIds.size());
            int batchIndex = from / VIEW_BATCH_SIZE + 1;
            System.out.println("[MetricExpansion] promote 批次 " + batchIndex + "/" + totalBatches
                    + " 开始, 视图 " + (from + 1) + "-" + to + "/" + targetViewIds.size());
            results.addAll(promoteBatch(targetViewIds.subList(from, to)));
            System.out.println("[MetricExpansion] promote 批次 " + batchIndex + "/" + totalBatches + " 完成");
        }
        return results;
    }

    /**
     * 解析 promote 目标 viewIds：入参为空则取影子表全部 view_id。
     *
     * @param viewIds 入参 viewIds，可空
     * @return 待上线 viewIds
     */
    private List<String> resolvePromoteViewIds(List<String> viewIds) {
        if (CollUtil.isNotEmpty(viewIds)) {
            return viewIds;
        }
        return metricExpansionShadowService.listAllShadowViewIds();
    }

    /**
     * 回滚上线：删除 promote 写入的新正式 cfg/dtl，并将视图 cfg_id 恢复为 old_cfg_id。
     *
     * @param viewIds 视图 id 列表
     * @return 逐视图结果
     */
    public List<MetricExpansionPromoteRsp> rollbackPromote(List<String> viewIds) {
        Preconditions.checkArgument(CollUtil.isNotEmpty(viewIds), "viewIds 不能为空");
        System.out.println("[MetricExpansion] rollback 待回滚视图数=" + viewIds.size());
        List<MetricExpansionPromoteRsp> results = new ArrayList<>(viewIds.size());
        int totalBatches = (viewIds.size() + VIEW_BATCH_SIZE - 1) / VIEW_BATCH_SIZE;
        for (int from = 0; from < viewIds.size(); from += VIEW_BATCH_SIZE) {
            int to = Math.min(from + VIEW_BATCH_SIZE, viewIds.size());
            int batchIndex = from / VIEW_BATCH_SIZE + 1;
            System.out.println("[MetricExpansion] rollback 批次 " + batchIndex + "/" + totalBatches
                    + " 开始, 视图 " + (from + 1) + "-" + to + "/" + viewIds.size());
            results.addAll(rollbackPromoteBatch(viewIds.subList(from, to)));
            System.out.println("[MetricExpansion] rollback 批次 " + batchIndex + "/" + totalBatches + " 完成");
        }
        return results;
    }

    private List<MetricExpansionPromoteRsp> promoteBatch(List<String> batchViewIds) {
        List<TemplateViewEntity> viewList = dao.queryObjectList(
                "ssm.template.view.queryViewByIds", batchViewIds, TemplateViewEntity.class);
        Map<String, TemplateViewEntity> viewById = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(viewList)) {
            for (TemplateViewEntity view : viewList) {
                if (view != null && StrUtil.isNotEmpty(view.getViewId())) {
                    viewById.put(view.getViewId(), view);
                }
            }
        }
        MetricExpansionPromoteRsp[] resultHolder = new MetricExpansionPromoteRsp[batchViewIds.size()];
        ExecutorService executor = Executors.newFixedThreadPool(PROMOTE_THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>(batchViewIds.size());
        try {
            for (int i = 0; i < batchViewIds.size(); i++) {
                final int index = i;
                final String viewId = batchViewIds.get(i);
                final TemplateViewEntity view = viewById.get(viewId);
                futures.add(executor.submit(() -> {
                    resultHolder[index] = executePromoteView(viewId, view, index + 1, batchViewIds.size());
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SSDException("promote 批处理被中断", e);
        } catch (Exception e) {
            log.error("promote 批处理任务异常", e);
            throw new SSDException("promote 批处理失败: " + e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
        List<MetricExpansionPromoteRsp> results = new ArrayList<>(batchViewIds.size());
        for (MetricExpansionPromoteRsp rsp : resultHolder) {
            results.add(rsp);
        }
        return results;
    }

    /**
     * 单视图 promote 任务（供批内多线程调用；异常写入 errorMessage，不向外抛出）。
     */
    private MetricExpansionPromoteRsp executePromoteView(String viewId, TemplateViewEntity view,
                                                         int sequenceNo, int totalCount) {
        System.out.println("[MetricExpansion] promote 处理视图 [" + sequenceNo + "/" + totalCount
                + "] viewId=" + viewId);
        MetricExpansionPromoteRsp rsp = new MetricExpansionPromoteRsp();
        rsp.setViewId(viewId);
        if (view == null) {
            rsp.setErrorMessage("视图不存在或无配置: " + viewId);
            System.out.println("[MetricExpansion] promote 视图失败 viewId=" + viewId
                    + ", error=" + rsp.getErrorMessage());
            return rsp;
        }
        try {
            rsp = promoteOne(view);
            if (StrUtil.isNotEmpty(rsp.getErrorMessage())) {
                System.out.println("[MetricExpansion] promote 视图失败 viewId=" + viewId
                        + ", error=" + rsp.getErrorMessage());
            } else {
                System.out.println("[MetricExpansion] promote 视图完成 viewId=" + viewId
                        + ", skipped=" + rsp.getSkipped()
                        + ", oldCfgId=" + rsp.getOldCfgId()
                        + ", newCfgId=" + rsp.getNewCfgId());
            }
        } catch (Exception e) {
            log.error("指标膨胀上线失败 viewId={}", viewId, e);
            rsp.setViewId(view.getViewId());
            rsp.setTplId(view.getTplId());
            rsp.setOldCfgId(view.getCfgId());
            rsp.setErrorMessage(e.getMessage());
            System.out.println("[MetricExpansion] promote 视图异常 viewId=" + viewId
                    + ", error=" + e.getMessage());
        }
        return rsp;
    }

    private List<MetricExpansionPromoteRsp> rollbackPromoteBatch(List<String> batchViewIds) {
        List<TemplateViewEntity> viewList = dao.queryObjectList(
                "ssm.template.view.queryViewByIds", batchViewIds, TemplateViewEntity.class);
        Map<String, TemplateViewEntity> viewById = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(viewList)) {
            for (TemplateViewEntity view : viewList) {
                if (view != null && StrUtil.isNotEmpty(view.getViewId())) {
                    viewById.put(view.getViewId(), view);
                }
            }
        }
        List<MetricExpansionPromoteRsp> results = new ArrayList<>(batchViewIds.size());
        for (int i = 0; i < batchViewIds.size(); i++) {
            String viewId = batchViewIds.get(i);
            System.out.println("[MetricExpansion] rollback 处理视图 [" + (i + 1) + "/" + batchViewIds.size()
                    + "] viewId=" + viewId);
            TemplateViewEntity view = viewById.get(viewId);
            MetricExpansionPromoteRsp rsp = new MetricExpansionPromoteRsp();
            rsp.setViewId(viewId);
            if (view == null) {
                rsp.setErrorMessage("视图不存在或无配置: " + viewId);
                System.out.println("[MetricExpansion] rollback 视图失败 viewId=" + viewId
                        + ", error=" + rsp.getErrorMessage());
                results.add(rsp);
                continue;
            }
            try {
                rsp = rollbackPromoteOne(view);
                if (StrUtil.isNotEmpty(rsp.getErrorMessage())) {
                    System.out.println("[MetricExpansion] rollback 视图失败 viewId=" + viewId
                            + ", error=" + rsp.getErrorMessage());
                } else {
                    System.out.println("[MetricExpansion] rollback 视图完成 viewId=" + viewId
                            + ", skipped=" + rsp.getSkipped()
                            + ", oldCfgId=" + rsp.getOldCfgId()
                            + ", newCfgId=" + rsp.getNewCfgId());
                }
            } catch (Exception e) {
                log.error("指标膨胀回滚失败 viewId={}", viewId, e);
                rsp.setViewId(view.getViewId());
                rsp.setTplId(view.getTplId());
                rsp.setErrorMessage(e.getMessage());
                System.out.println("[MetricExpansion] rollback 视图异常 viewId=" + viewId
                        + ", error=" + e.getMessage());
            }
            results.add(rsp);
        }
        return results;
    }

    /**
     * 单视图上线：影子 → 新正式 cfg/dtl，视图 cfg_id 指向新 cfg。
     *
     * @param view 视图实体
     * @return 上线结果
     */
    private MetricExpansionPromoteRsp promoteOne(TemplateViewEntity view) {
        MetricExpansionPromoteRsp rsp = new MetricExpansionPromoteRsp();
        rsp.setViewId(view.getViewId());
        rsp.setTplId(view.getTplId());
        rsp.setOldCfgId(view.getCfgId());

        MetricExpansionCfgShadowEntity shadow = metricExpansionShadowService.getShadowByViewId(view.getViewId());
        if (shadow == null || StrUtil.isEmpty(shadow.getTplConfig())) {
            rsp.setSkipped(true);
            rsp.setErrorMessage("视图无影子配置，无法上线: " + view.getViewId());
            return rsp;
        }

        MetricExpansionPromoteLogEntity existingLog = (MetricExpansionPromoteLogEntity) dao.queryObject(
                "ssm.query.template.cfg.promote.getByViewId", view.getViewId());
        if (existingLog != null
                && MetricExpansionPromoteLogEntity.STATUS_PROMOTED.equals(existingLog.getStatus())
                && StrUtil.equals(view.getCfgId(), existingLog.getNewCfgId())) {
            rsp.setSkipped(true);
            rsp.setOldCfgId(existingLog.getOldCfgId());
            rsp.setNewCfgId(existingLog.getNewCfgId());
            rsp.setShadowCfgId(existingLog.getShadowCfgId());
            return rsp;
        }

        String oldCfgId = view.getCfgId();
        String newCfgId = Guid.id();
        String operator = resolveOperator();
        MetricExpansionCfgDtlShadowEntity shadowDtl = metricExpansionShadowService.getShadowDtlEntityByCfgId(
                shadow.getCfgId());

        final TemplateCfgEntity cfgEntity = new TemplateCfgEntity(newCfgId, shadow.getTplConfig());
        final TemplateCfgDtlEntity cfgDtlEntity = buildFormalDtlFromShadow(newCfgId, shadowDtl);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.insert("ssm.template.saveTemplateCfg", cfgEntity);
                if (cfgDtlEntity != null) {
                    dao.insert("ssm.query.template.cfg.dtl.add", cfgDtlEntity);
                }

                TemplateViewEntity updateView = new TemplateViewEntity();
                updateView.setViewId(view.getViewId());
                updateView.setCfgId(newCfgId);
                dao.update("ssm.template.view.updateViewCfgId", updateView);

                MetricExpansionPromoteLogEntity logEntity = new MetricExpansionPromoteLogEntity();
                logEntity.setViewId(view.getViewId());
                logEntity.setTplId(view.getTplId());
                logEntity.setShadowCfgId(shadow.getShadowCfgId());
                logEntity.setOldCfgId(oldCfgId);
                logEntity.setNewCfgId(newCfgId);
                logEntity.setStatus(MetricExpansionPromoteLogEntity.STATUS_PROMOTED);
                logEntity.setCreatedBy(operator);
                logEntity.setUpdatedBy(operator);
                dao.delete("ssm.query.template.cfg.promote.deleteByViewId", view.getViewId());
                dao.insert("ssm.query.template.cfg.promote.insertLog", logEntity);
            }
        });

        rsp.setNewCfgId(newCfgId);
        rsp.setShadowCfgId(shadow.getShadowCfgId());
        rsp.setSkipped(false);
        return rsp;
    }

    /**
     * 单视图回滚：先删上线新 cfg/dtl，再将视图 cfg_id 恢复为 old_cfg_id。
     *
     * @param view 视图实体
     * @return 回滚结果
     */
    private MetricExpansionPromoteRsp rollbackPromoteOne(TemplateViewEntity view) {
        MetricExpansionPromoteRsp rsp = new MetricExpansionPromoteRsp();
        rsp.setViewId(view.getViewId());
        rsp.setTplId(view.getTplId());
        rsp.setNewCfgId(view.getCfgId());

        MetricExpansionPromoteLogEntity promoteLog = (MetricExpansionPromoteLogEntity) dao.queryObject(
                "ssm.query.template.cfg.promote.getByViewId", view.getViewId());
        if (promoteLog == null) {
            throw new SSDException("视图无上线记录，无法回滚: " + view.getViewId());
        }
        if (MetricExpansionPromoteLogEntity.STATUS_ROLLED_BACK.equals(promoteLog.getStatus())) {
            rsp.setSkipped(true);
            rsp.setOldCfgId(promoteLog.getOldCfgId());
            rsp.setNewCfgId(promoteLog.getNewCfgId());
            rsp.setShadowCfgId(promoteLog.getShadowCfgId());
            return rsp;
        }
        if (!MetricExpansionPromoteLogEntity.STATUS_PROMOTED.equals(promoteLog.getStatus())) {
            throw new SSDException("视图上线状态异常，无法回滚: " + view.getViewId());
        }

        String operator = resolveOperator();
        final String oldCfgId = promoteLog.getOldCfgId();
        final String newCfgId = promoteLog.getNewCfgId();
        if (StrUtil.isEmpty(newCfgId)) {
            throw new SSDException("上线记录缺少 newCfgId，无法回滚: " + view.getViewId());
        }
        if (StrUtil.equals(oldCfgId, newCfgId)) {
            throw new SSDException("上线记录 oldCfgId 与 newCfgId 相同，无法回滚: " + view.getViewId());
        }

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.delete("ssm.query.template.cfg.dtl.delete", newCfgId);
                dao.delete("ssm.template.cfg.delete", newCfgId);

                TemplateViewEntity updateView = new TemplateViewEntity();
                updateView.setViewId(view.getViewId());
                updateView.setCfgId(oldCfgId);
                dao.update("ssm.template.view.updateViewCfgId", updateView);

                MetricExpansionPromoteLogEntity updateLog = new MetricExpansionPromoteLogEntity();
                updateLog.setViewId(view.getViewId());
                updateLog.setStatus(MetricExpansionPromoteLogEntity.STATUS_ROLLED_BACK);
                updateLog.setUpdatedBy(operator);
                dao.update("ssm.query.template.cfg.promote.updateStatus", updateLog);
            }
        });

        rsp.setOldCfgId(oldCfgId);
        rsp.setNewCfgId(newCfgId);
        rsp.setShadowCfgId(promoteLog.getShadowCfgId());
        rsp.setSkipped(false);
        return rsp;
    }

    /**
     * 将影子 dtl 转为正式 dtl 写入对象。
     *
     * @param newCfgId  新正式 cfg id
     * @param shadowDtl 影子 dtl
     * @return 正式 dtl；影子无 dtl 时返回 null
     */
    private TemplateCfgDtlEntity buildFormalDtlFromShadow(String newCfgId,
                                                          MetricExpansionCfgDtlShadowEntity shadowDtl) {
        if (shadowDtl == null) {
            return null;
        }
        TemplateCfgDtlEntity dtl = new TemplateCfgDtlEntity(
                newCfgId,
                shadowDtl.getTplConfigFieldDimCodes(),
                shadowDtl.getTplConfigFieldMeasureCodes());
        dtl.setTplConfigFieldDimAsset(shadowDtl.getTplConfigFieldDimAsset());
        dtl.setTplConfigFieldMeasureAsset(shadowDtl.getTplConfigFieldMeasureAsset());
        return dtl;
    }

    /**
     * @return 当前操作人用户名，未登录时返回 null
     */
    private String resolveOperator() {
        return UserManager.get() != null ? UserManager.get().getName() : null;
    }

    /** 单视图膨胀结果（内存态，供影子写入或正式库写入复用） */
    private static class ExpandedViewConfig {

        private MetricExpansionContext context;
        private TemplateCfgDtlEntity oldDtl;
        private String measureCodes;
        private String measureAsset;
        private boolean filterRewritten;
        private boolean metricExpanded;

        boolean isChanged() {
            return filterRewritten || metricExpanded;
        }

        String getTplConfigJson() {
            return context.getTplConfigJson().toJSONString();
        }

        List<String> getExpandedSourceCodes() {
            return context.listExpandedSourceCodes();
        }

        MetricExpansionContext getContext() {
            return context;
        }

        void setContext(MetricExpansionContext context) {
            this.context = context;
        }

        TemplateCfgDtlEntity getOldDtl() {
            return oldDtl;
        }

        void setOldDtl(TemplateCfgDtlEntity oldDtl) {
            this.oldDtl = oldDtl;
        }

        String getMeasureCodes() {
            return measureCodes;
        }

        void setMeasureCodes(String measureCodes) {
            this.measureCodes = measureCodes;
        }

        String getMeasureAsset() {
            return measureAsset;
        }

        void setMeasureAsset(String measureAsset) {
            this.measureAsset = measureAsset;
        }

        boolean isFilterRewritten() {
            return filterRewritten;
        }

        void setFilterRewritten(boolean filterRewritten) {
            this.filterRewritten = filterRewritten;
        }

        boolean isMetricExpanded() {
            return metricExpanded;
        }

        void setMetricExpanded(boolean metricExpanded) {
            this.metricExpanded = metricExpanded;
        }
    }

    /** 门户 replaceTemplateViews 待写入的字段 id 映射 */
    private static class PortalReplaceFieldMappingWrite {

        private String oldViewId;
        private String oldTplId;
        private String newViewId;
        private String newTplId;
        private String newCfgId;
        private List<MetricExpansionFieldMappingEntity> fieldMappings;

        public String getOldViewId() {
            return oldViewId;
        }

        public void setOldViewId(String oldViewId) {
            this.oldViewId = oldViewId;
        }

        public String getOldTplId() {
            return oldTplId;
        }

        public void setOldTplId(String oldTplId) {
            this.oldTplId = oldTplId;
        }

        public String getNewViewId() {
            return newViewId;
        }

        public void setNewViewId(String newViewId) {
            this.newViewId = newViewId;
        }

        public String getNewTplId() {
            return newTplId;
        }

        public void setNewTplId(String newTplId) {
            this.newTplId = newTplId;
        }

        public String getNewCfgId() {
            return newCfgId;
        }

        public void setNewCfgId(String newCfgId) {
            this.newCfgId = newCfgId;
        }

        public List<MetricExpansionFieldMappingEntity> getFieldMappings() {
            return fieldMappings;
        }

        public void setFieldMappings(List<MetricExpansionFieldMappingEntity> fieldMappings) {
            this.fieldMappings = fieldMappings;
        }
    }
}
