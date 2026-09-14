package com.bi.queryer.ssm.migrate.bizsplit.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionFieldMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.service.MetricExpansionContext;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionTarget;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionMetaUtil;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 计算指标（四则 / LOD）公式变体处理器。
 *
 * 调用时机：TplConfigExpansionProcessor.expandTplConfig 之后。
 * 此时 measures 里普通 source 已替换为 target，但计算字段的 expressionIdMapping 仍指向旧 source id/code。
 *
 * 核心约束：一个计算字段若依赖多个待膨胀 source，必须按 target_businessline 取交集，
 * 每个交集中的业务线生成一个变体，保证同一变体内所有依赖都来自同一目标业务线，避免公式混用。
 *
 * 处理步骤：
 * 1. 扫描 measures 中含 customFieldConfigure.expression 的用户四则/LOD 计算字段（排除跨模型指标），
 *    找出引用 active source 的计算字段；
 * 2. 对 expandableSources 的 target_businessline 求交集；
     * 3. 每个业务线调用 buildVariant 生成变体（新 id、code 置空、改写 expression）；
 * 4. 删除原计算字段，measures 中替换为变体列表，并写入变体映射供 filter/sort/analysis 同步；
 * 5. 委托 expandTplConfigForNewCalcFields 同步其他引用位置。
 *
 * 命名约定：变体 title 按业务线规则改写；不保留 displayTitle（前端展示走 title）。
 * 异常策略：交集为空跳过该计算字段；单个 source 在业务线下缺 target 仅 warn，不阻断其他变体。
 */
@Component
public class CalcFieldExpansionProcessor {

    private static final Logger log = LoggerFactory.getLogger(CalcFieldExpansionProcessor.class);
    private static final String ANALYSIS_ID_PREFIX = CustomFieldType.ANALYSIS.getIdentifier();

    @Autowired
    private TplConfigExpansionProcessor tplConfigExpansionProcessor;

    /**
     * 执行计算字段膨胀主流程。
     *
     * 采用「先 buildExpansionPlans 再统一改写 measures」的两阶段方式，避免边遍历边修改数组下标错乱。
     * 改写 measures 时同步维护：showOrder、fieldIdToCode、
     * calcFieldIdToVariantIds、calcFieldCodeToVariantCodes、calcFieldCodeToVariantIds、calcFieldVariantById。
     * 最后调用 expandTplConfigForNewCalcFields 同步 filter/sort/analysis 等引用。
     *
     * @param context 运行时上下文
     */
    public void processCalcFields(MetricExpansionContext context) {
        JSONObject result = context.getTplConfigJson().getJSONObject("result");
        if (result == null) {
            return;
        }
        JSONArray measures = result.getJSONArray("measures");
        if (CollUtil.isEmpty(measures)) {
            return;
        }

        // 先规划再改写，避免边遍历边修改导致下标错乱
        List<CalcFieldExpansionPlan> plans = buildExpansionPlans(context, measures);
        if (CollUtil.isEmpty(plans)) {
            return;
        }

        JSONArray newMeasures = new JSONArray();
        double showOrder = 1D;
        int measureIndex = 0;
        for (int i = 0; i < measures.size(); i++) {
            JSONObject measure = measures.getJSONObject(i);
            CalcFieldExpansionPlan matchedPlan = findPlan(plans, measure);
            if (matchedPlan == null) {
                measure.put("showOrder", showOrder++);
                newMeasures.add(measure);
                measureIndex++;
                continue;
            }
            // preserveSourceData 时保留原计算字段，并追加业务线变体
            if (context.isPreserveSourceData()) {
                measure.put("showOrder", showOrder++);
                newMeasures.add(measure);
                measureIndex++;
            }
            String oldId = matchedPlan.getOriginalId();
            String oldCode = matchedPlan.getOriginalCode();
            String oldFieldTitle = MetricExpansionMetaUtil.resolveFieldTitle(measure);
            List<String> variantIds = new ArrayList<>();
            List<String> variantCodes = new ArrayList<>();
            List<JSONObject> variants = matchedPlan.getVariants();
            List<String> variantBusinessLines = matchedPlan.getVariantBusinessLines();
            for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
                JSONObject variant = variants.get(variantIndex);
                variant.put("showOrder", showOrder++);
                newMeasures.add(variant);
                String code = variant.getString("code");
                String id = variant.getString("id");
                if (StrUtil.isNotEmpty(code)) {
                    context.getGeneratedCalcFieldCodes().add(code);
                    variantCodes.add(code);
                }
                if (StrUtil.isNotEmpty(id)) {
                    variantIds.add(id);
                    context.getFieldIdToCode().put(id, code);
                    context.getCalcFieldVariantById().put(id, JSONObject.parseObject(variant.toJSONString()));
                }
                String targetBusinessline = CollUtil.isNotEmpty(variantBusinessLines)
                        && variantIndex < variantBusinessLines.size()
                        ? variantBusinessLines.get(variantIndex) : null;
                context.addFieldMapping(
                        oldId,
                        null,
                        oldFieldTitle,
                        id,
                        null,
                        MetricExpansionMetaUtil.resolveFieldTitle(variant),
                        null,
                        null,
                        targetBusinessline);
            }
            measureIndex += variants.size();
            // 记录旧→新映射，供 filter/sort/analysis 同步替换（主路径按 id 匹配）
            if (StrUtil.isNotEmpty(oldId)) {
                context.getCalcFieldIdToVariantIds().put(oldId, variantIds);
            }
            // 前端四则/LOD 计算字段 code 恒为 ""，以下 Map 通常为空；仅当原字段 code 非空时写入（历史脏数据兜底）
            if (StrUtil.isNotEmpty(oldCode)) {
                context.getCalcFieldCodeToVariantCodes().put(oldCode, variantCodes);
                context.getCalcFieldCodeToVariantIds().put(oldCode, variantIds);
            }
        }
        result.put("measures", newMeasures);
        expandCalcDependentAnalysisMeasureFields(context, newMeasures);

        // 将新计算字段接入排序、过滤、分析、总计等位置
        tplConfigExpansionProcessor.expandTplConfigForNewCalcFields(context);
    }

    /**
     * 为每个受影响的计算字段构建膨胀计划（含全部业务线变体 JSON）。
     *
     * @param context  上下文
     * @param measures 当前 measures（步骤四之后）
     * @return 计划列表；无受影响字段时为空
     */
    private List<CalcFieldExpansionPlan> buildExpansionPlans(MetricExpansionContext context, JSONArray measures) {
        List<CalcFieldExpansionPlan> plans = new ArrayList<>();
        for (int i = 0; i < measures.size(); i++) {
            JSONObject measure = measures.getJSONObject(i);
            JSONObject customCfg = measure.getJSONObject("customFieldConfigure");
            // 无 expression 视为非四则/LOD 计算字段
            if (customCfg == null || StrUtil.isEmpty(customCfg.getString("expression"))) {
                continue;
            }
            // 跨模型指标虽有 expression，但 code 来自元数据且不应走计算字段变体逻辑（对齐 UICalcFieldNormalizer / TemplateViewService）
            if (isCrossModelMeasureField(measure)) {
                continue;
            }
            // 纯 LOD 指标 code 常为 _avg_by_d，不能走计算字段变体；依赖项由 expandLodMeasureReferences 按 measureId 更新
            if (MetricExpansionMetaUtil.isPureLodMeasureField(measure)) {
                continue;
            }
            Set<String> referencedCodes = extractReferencedMetricCodes(context, measure);
            Set<String> expandableSources = new LinkedHashSet<>();
            for (String code : referencedCodes) {
                if (context.isExpandableSourceCode(code)) {
                    expandableSources.add(code);
                }
            }
            // 公式未引用任何本次 source，则计算字段不受影响
            if (CollUtil.isEmpty(expandableSources)) {
                continue;
            }
            Set<String> commonBusinessLines = intersectBusinessLines(context, expandableSources);
            if (CollUtil.isEmpty(commonBusinessLines)) {
                // 业务已确认交集必然非空；此处仍做防御，避免脏数据导致空变体
                log.warn("计算字段[{}] 依赖源指标的 target_businessline 交集为空，跳过变体生成",
                        measure.getString("code"));
                continue;
            }
            List<JSONObject> variants = new ArrayList<>();
            List<String> variantBusinessLines = new ArrayList<>();
            for (String businessLine : commonBusinessLines) {
                JSONObject variant = buildVariant(context, measure, expandableSources, businessLine);
                if (variant != null) {
                    variants.add(variant);
                    variantBusinessLines.add(businessLine);
                }
            }
            if (CollUtil.isEmpty(variants)) {
                continue;
            }
            CalcFieldExpansionPlan plan = new CalcFieldExpansionPlan();
            plan.setOriginalId(measure.getString("id"));
            plan.setOriginalCode(measure.getString("code"));
            plan.setVariants(variants);
            plan.setVariantBusinessLines(variantBusinessLines);
            plans.add(plan);
        }
        return plans;
    }

    /**
     * 从计算字段配置中提取公式引用的全部指标 code。
     * 来源：expressionIdMapping[].code/id，以及 lodConfig.measureId。
     *
     * @param context 用于 id→code 反查
     * @param measure 计算字段 JSON
     * @return 引用到的指标 code 集合
     */
    private Set<String> extractReferencedMetricCodes(MetricExpansionContext context, JSONObject measure) {
        Set<String> codes = new LinkedHashSet<>();
        JSONObject customCfg = measure.getJSONObject("customFieldConfigure");
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        if (CollUtil.isNotEmpty(mapping)) {
            for (int i = 0; i < mapping.size(); i++) {
                JSONObject item = mapping.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String code = item.getString("code");
                String id = item.getString("id");
                JSONObject analysisItemConfig = item.getJSONObject("analysisItemConfig");
                if (analysisItemConfig != null) {
                    String dependencySourceCode = resolveAnalysisDependencySourceCode(context, analysisItemConfig);
                    if (StrUtil.isNotEmpty(dependencySourceCode)) {
                        codes.add(dependencySourceCode);
                    }
                    continue;
                }
                if (StrUtil.isNotEmpty(code)) {
                    codes.add(code);
                } else if (StrUtil.isNotEmpty(id)) {
                    String codeById = context.getFieldIdToCode().get(id);
                    if (StrUtil.isEmpty(codeById)) {
                        codeById = MetricExpansionMetaUtil.resolveMeasureCodeByMeasureId(id);
                    }
                    if (StrUtil.isNotEmpty(codeById)) {
                        codes.add(codeById);
                    }
                }
                String atomicSourceCode = resolveAtomicMappingSourceCode(context, item);
                if (StrUtil.isNotEmpty(atomicSourceCode)) {
                    codes.add(atomicSourceCode);
                }
                String nestedLodSourceCode = resolveNestedLodUnderlyingSourceCode(item);
                if (StrUtil.isNotEmpty(nestedLodSourceCode)) {
                    codes.add(nestedLodSourceCode);
                }
            }
        }
        JSONObject lodConfig = customCfg.getJSONObject("lodConfig");
        if (lodConfig != null) {
            addReferencedMetricCode(codes,
                    MetricExpansionMetaUtil.resolveLodUnderlyingSourceCode(
                            customCfg, lodConfig.getString("measureId")));
        }
        String expression = customCfg.getString("expression");
        if (StrUtil.isNotEmpty(expression)) {
            for (String refId : MetricExpansionMetaUtil.extractExpressionFieldIds(expression)) {
                String refCode = MetricExpansionMetaUtil.resolveMeasureCodeByMeasureId(refId);
                if (StrUtil.isEmpty(refCode)) {
                    refCode = MetricExpansionMetaUtil.resolveMeasureCodeByMeasureId(
                            stripLodPrefix(refId));
                }
                if (StrUtil.isNotEmpty(refCode)) {
                    codes.add(refCode);
                }
            }
        }
        return codes;
    }

    /**
     * 解析 LOD 日均/非空日均后缀（如 _avg_by_d），无后缀返回 null。
     */
    private String resolveLodMeasureAggSuffix(JSONObject measure, JSONObject customCfg) {
        if (measure != null) {
            String suffixFromId = MetricExpansionMetaUtil.resolveAggExpressionSuffix(
                    stripLodPrefix(measure.getString("id")));
            if (suffixFromId != null) {
                return suffixFromId;
            }
            String aggExpressionType = measure.getString("aggExpressionType");
            if (AggExpressionType.Avg_By_Day.getCode().equals(aggExpressionType)) {
                return "_" + AggExpressionType.Avg_By_Day.getCode();
            }
            if (AggExpressionType.Avg_By_Day_Real.getCode().equals(aggExpressionType)) {
                return "_" + AggExpressionType.Avg_By_Day_Real.getCode();
            }
        }
        if (customCfg != null) {
            JSONObject cfgLod = customCfg.getJSONObject("lodConfig");
            if (cfgLod != null) {
                String suffixFromMeasureId = MetricExpansionMetaUtil.resolveAggExpressionSuffix(
                        cfgLod.getString("measureId"));
                if (suffixFromMeasureId != null) {
                    return suffixFromMeasureId;
                }
            }
        }
        return null;
    }

    private String stripLodPrefix(String fieldId) {
        if (StrUtil.isEmpty(fieldId)) {
            return fieldId;
        }
        String lodPrefix = CustomFieldType.LOD.getIdentifier();
        return fieldId.startsWith(lodPrefix) ? fieldId.substring(lodPrefix.length()) : fieldId;
    }

    private String appendAggSuffixIfNeeded(String measureId, String aggSuffix) {
        if (StrUtil.isEmpty(measureId) || StrUtil.isEmpty(aggSuffix)) {
            return measureId;
        }
        if (measureId.endsWith(aggSuffix)) {
            return measureId;
        }
        return measureId + aggSuffix;
    }

    /** 将底层指标 code 及其日均后缀归一化形式加入引用集合 */
    private void addReferencedMetricCode(Set<String> codes, String metricCode) {
        if (codes == null || StrUtil.isEmpty(metricCode)) {
            return;
        }
        codes.add(metricCode);
        String baseCode = MetricExpansionMetaUtil.stripAggExpressionSuffix(metricCode);
        if (StrUtil.isNotEmpty(baseCode) && !baseCode.equals(metricCode)) {
            codes.add(baseCode);
        }
    }

    /** 通过 measureId（含日均后缀）反查可膨胀 source code */
    private String resolveSourceCodeByMeasureId(MetricExpansionContext context, String measureId,
                                                JSONObject customCfg) {
        if (context == null || StrUtil.isEmpty(measureId)) {
            return null;
        }
        return context.resolveExpandableSourceCodeByMeasureId(measureId, customCfg);
    }

    /**
     * 计算多个 expandableSources 的 target_businessline 交集。
     * 交集中的每个业务线对应生成一个计算字段变体。
     *
     * @param context           含 targetsBySourceCode
     * @param expandableSources 需要对齐的 source 集合
     * @return 业务线交集（保持首次出现顺序）
     */
    private Set<String> intersectBusinessLines(MetricExpansionContext context, Set<String> expandableSources) {
        Set<String> intersection = null;
        for (String sourceCode : expandableSources) {
            List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(sourceCode);
            Set<String> lines = new LinkedHashSet<>();
            if (CollUtil.isNotEmpty(targets)) {
                for (MetricExpansionTarget target : targets) {
                    if (StrUtil.isNotEmpty(target.getTargetBusinessline())) {
                        lines.add(target.getTargetBusinessline());
                    }
                }
            }
            if (intersection == null) {
                intersection = new LinkedHashSet<>(lines);
            } else {
                intersection.retainAll(lines);
            }
        }
        return intersection == null ? new LinkedHashSet<>() : intersection;
    }

    /**
     * 生成单个业务线下的计算字段变体 JSON。
     *
     * id：按字段类型生成（LOD/lod_calc 保留 lod: 前缀，与前端保存一致）；code 置空字符串。
     * title/name：按 MetricExpansionMetaUtil.buildExpandedDisplayName 改写；移除 displayTitle。
     * expression：将 expandableSources 对应的旧 id（含 [id] 形式）与 code 替换为
     * 该业务线下的 target id/code；expressionIdMapping 与 lodConfig.measureId 同步更新。
     *
     * @param context           上下文
     * @param original          原计算字段（深拷贝来源）
     * @param expandableSources 本次公式中需要替换的 source code 集合
     * @param businessLine      当前变体对应的目标业务线
     * @return 变体字段 JSON
     */
    private JSONObject buildVariant(MetricExpansionContext context, JSONObject original,
                                    Set<String> expandableSources, String businessLine) {
        JSONObject variant = JSONObject.parseObject(original.toJSONString());
        String newId = buildVariantFieldId(original);
        variant.put("id", newId);
        variant.put("code", "");

        String sourceBusinessline = context.getSourceBusinessline();
        String title = MetricExpansionMetaUtil.resolveFieldTitle(original);
        if (StrUtil.isNotEmpty(title)) {
            String expandedTitle = MetricExpansionMetaUtil.buildExpandedDisplayName(
                    title, sourceBusinessline, businessLine);
            variant.put("title", expandedTitle);
            variant.put("name", expandedTitle);
        } else {
            variant.put("name", "");
        }
        variant.put("displayTitle", "");

        JSONObject customCfg = variant.getJSONObject("customFieldConfigure");
        String aggSuffix = resolveLodMeasureAggSuffix(original, customCfg);
        String expression = customCfg.getString("expression");
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        JSONArray newMapping = new JSONArray();
        MetaField syncedLodTargetMeta = null;

        // 分析项须先于 sourceCode 字面量替换，避免 analysis:D_ORD_xxx 被截断替换
        expression = replaceAnalysisRefsInExpression(context, mapping, expandableSources, businessLine, expression);

        for (String sourceCode : expandableSources) {
            MetricExpansionTarget matched = findTargetByBusinessLine(
                    context.getTargetsBySourceCode().get(sourceCode), businessLine);
            if (matched == null || matched.getMetaField() == null) {
                // 不在交集但个别 source 缺失该业务线时：告警并跳过该依赖替换，不阻断其他可用变体
                log.warn("计算字段变体生成：source[{}] 在业务线[{}] 下无对应 target，跳过该依赖替换",
                        sourceCode, businessLine);
                continue;
            }
            MetaField targetMeta = matched.getMetaField();
            // 前端表达式使用 [fieldId]；同时兼容裸 id / code / title 字面量
            List<String> oldIds = findOldIdsForSource(context, mapping, sourceCode, expression);
            // 长 id 优先，且裸 id 替换带标识符边界，避免 uuid 误伤源字段 uuid_1
            oldIds.sort(Comparator.comparingInt(String::length).reversed());
            Set<String> replacedMappingKeys = new LinkedHashSet<>();
            for (String oldId : oldIds) {
                String depAggSuffix = resolveDependencyAggSuffix(oldId);
                if (depAggSuffix == null && aggSuffix != null) {
                    depAggSuffix = aggSuffix;
                }
                String newTargetMeasureId = appendAggSuffixIfNeeded(targetMeta.getId(), depAggSuffix);
                expression = replaceDependencyMeasureRefInExpression(expression, oldId, newTargetMeasureId);
                JSONObject mappingItem = findMappingItemByOldId(mapping, oldId, sourceCode, context);
                if (mappingItem != null) {
                    String mappingKey = StrUtil.emptyToDefault(mappingItem.getString("id"), oldId)
                            + "|" + StrUtil.emptyToDefault(mappingItem.getString("code"), sourceCode);
                    if (replacedMappingKeys.add(mappingKey)) {
                        expression = replaceDependencyDisplayRefInExpression(
                                expression, mappingItem, targetMeta, depAggSuffix,
                                sourceBusinessline, businessLine, context);
                    }
                } else if (depAggSuffix == null) {
                    expression = replaceSourceMetricCodeAvoidingAnalysis(
                            expression, sourceCode, targetMeta.getCode());
                }
            }
            if (CustomFieldType.LOD == CustomFieldType.get(customCfg.getString("type"))) {
                syncedLodTargetMeta = targetMeta;
            }
        }
        customCfg.put("expression", expression);

        if (CollUtil.isNotEmpty(mapping)) {
            for (int i = 0; i < mapping.size(); i++) {
                JSONObject item = mapping.getJSONObject(i);
                if (isAnalysisMappingItem(item)) {
                    JSONObject analysisItemConfig = item.getJSONObject("analysisItemConfig");
                    String dependencySourceCode = resolveAnalysisDependencySourceCode(context, analysisItemConfig);
                    if (expandableSources.contains(dependencySourceCode)) {
                        ExpandedAnalysisRef expandedRef = resolveExpandedAnalysisRef(
                                context, item, dependencySourceCode, businessLine);
                        if (expandedRef != null) {
                            newMapping.add(expandedRef.getMappingItem());
                        } else {
                            log.warn("计算字段变体生成：分析项[{}] 在业务线[{}] 下无法解析膨胀结果，保留原 mapping",
                                    resolveAnalysisMappingLookupCode(item), businessLine);
                            newMapping.add(JSONObject.parseObject(item.toJSONString()));
                        }
                    } else {
                        newMapping.add(JSONObject.parseObject(item.toJSONString()));
                    }
                    continue;
                }
                String nestedLodSourceCode = resolveNestedLodMappingSourceCode(context, item);
                if (expandableSources.contains(nestedLodSourceCode)) {
                    MetricExpansionTarget matched = findTargetByBusinessLine(
                            context.getTargetsBySourceCode().get(nestedLodSourceCode), businessLine);
                    if (matched != null && matched.getMetaField() != null) {
                        String nestedAggSuffix = resolveMappingItemAggSuffix(item);
                        if (nestedAggSuffix == null) {
                            nestedAggSuffix = resolveLodMeasureAggSuffix(null, item.getJSONObject("customFieldConfigure"));
                        }
                        newMapping.add(buildNestedLodExpressionIdMappingItem(item, matched.getMetaField(), nestedAggSuffix));
                        continue;
                    }
                }
                String resolvedSourceCode = resolveAtomicMappingSourceCode(context, item);
                if (expandableSources.contains(resolvedSourceCode)) {
                    MetricExpansionTarget matched = findTargetByBusinessLine(
                            context.getTargetsBySourceCode().get(resolvedSourceCode), businessLine);
                    if (matched != null && matched.getMetaField() != null) {
                        MetaField meta = matched.getMetaField();
                        String depAggSuffix = resolveMappingItemAggSuffix(item);
                        JSONObject measureField = findExpandedMeasureField(context, meta, depAggSuffix);
                        if (measureField != null) {
                            newMapping.add(MetricExpansionMetaUtil.buildExpressionIdMappingItem(measureField));
                        } else {
                            newMapping.add(buildExpressionIdMappingItemWithAggSuffix(meta, depAggSuffix));
                        }
                        continue;
                    }
                }
                JSONObject lodRefMapping = buildLodRefExpressionIdMappingFromMeasures(context, item);
                if (lodRefMapping != null) {
                    newMapping.add(lodRefMapping);
                    continue;
                }
                newMapping.add(JSONObject.parseObject(item.toJSONString()));
            }
            customCfg.put("expressionIdMapping", newMapping);
        }

        // LOD 计算字段还需同步 lodConfig.measureId
        JSONObject lodConfig = customCfg.getJSONObject("lodConfig");
        if (lodConfig != null) {
            String measureId = lodConfig.getString("measureId");
            String sourceCode = resolveSourceCodeByMeasureId(context, measureId, customCfg);
            if (expandableSources.contains(sourceCode)) {
                MetricExpansionTarget matched = findTargetByBusinessLine(
                        context.getTargetsBySourceCode().get(sourceCode), businessLine);
                if (matched != null && matched.getMetaField() != null) {
                    syncedLodTargetMeta = matched.getMetaField();
                    lodConfig.put("measureId", appendAggSuffixIfNeeded(syncedLodTargetMeta.getId(), aggSuffix));
                }
            }
        }
        // 纯 LOD 字段 id 须与 lodConfig.measureId 一致：lod:measureId（日均时 measureId 含 _avg_by_d 后缀）
        if (CustomFieldType.LOD == CustomFieldType.get(customCfg.getString("type"))) {
            JSONObject syncedLodConfig = customCfg.getJSONObject("lodConfig");
            if (syncedLodConfig != null && StrUtil.isNotEmpty(syncedLodConfig.getString("measureId"))) {
                String measureId = syncedLodConfig.getString("measureId");
                variant.put("id", CustomFieldType.LOD.getIdentifier() + measureId);
                customCfg.put("expression", "[" + measureId + "]");
                if (aggSuffix != null && syncedLodTargetMeta != null && StrUtil.isNotEmpty(syncedLodTargetMeta.getCode())) {
                    variant.put("code", syncedLodTargetMeta.getCode() + aggSuffix);
                }
            }
        }
        return variant;
    }

    /**
     * 替换公式中对某依赖指标的 id 引用。
     * mapping id 或 expression 引用若带 lod: 前缀，须保留前缀，不能走 replaceFieldIdInExpression 整段替换。
     *
     * @param expression       公式字符串
     * @param oldId            旧 fieldId（可含 lod: 前缀与日均后缀）
     * @param newTargetMeasureId 新底层 measureId（不含 lod: 前缀）
     * @return 替换后的公式
     */
    private String replaceDependencyMeasureRefInExpression(String expression, String oldId, String newTargetMeasureId) {
        if (StrUtil.isEmpty(expression) || StrUtil.isEmpty(oldId) || StrUtil.isEmpty(newTargetMeasureId)) {
            return expression;
        }
        String lodPrefix = CustomFieldType.LOD.getIdentifier();
        String measureOldId = stripLodPrefix(oldId);
        if (oldId.startsWith(lodPrefix)) {
            // 带 lod: 的引用仅替换完整 measureId（含日均后缀），不再对 base id 二次替换，避免 lod:uuid 前缀误伤已替换的长 id
            return replaceLodMeasureRefInExpression(expression, measureOldId, newTargetMeasureId);
        }
        expression = MetricExpansionMetaUtil.replaceFieldIdInExpression(
                expression, oldId, newTargetMeasureId);
        expression = replaceLodMeasureRefInExpression(expression, measureOldId, newTargetMeasureId);
        String baseMeasureOldId = MetricExpansionMetaUtil.stripAggExpressionSuffix(measureOldId);
        if (baseMeasureOldId.equals(measureOldId)) {
            return expression;
        }
        expression = MetricExpansionMetaUtil.replaceFieldIdInExpression(
                expression, baseMeasureOldId, newTargetMeasureId);
        return replaceLodMeasureRefInExpression(expression, baseMeasureOldId, newTargetMeasureId);
    }

    /**
     * 替换公式中 [lod:measureId] 形式的 LOD 引用（oldMeasureId / newMeasureId 均不含 lod: 前缀）。
     */
    private String replaceLodMeasureRefInExpression(String expression, String oldMeasureId, String newMeasureId) {
        if (StrUtil.isEmpty(expression) || StrUtil.isEmpty(oldMeasureId)) {
            return expression;
        }
        oldMeasureId = stripLodPrefix(oldMeasureId);
        newMeasureId = stripLodPrefix(newMeasureId);
        if (oldMeasureId.equals(newMeasureId)) {
            return expression;
        }
        String lodPrefix = CustomFieldType.LOD.getIdentifier();
        String oldLodRef = "[" + lodPrefix + oldMeasureId + "]";
        if (!expression.contains(oldLodRef)) {
            return expression;
        }
        return MetricExpansionMetaUtil.replaceLiteral(expression, oldLodRef,
                "[" + lodPrefix + newMeasureId + "]");
    }

    /**
     * 更新 lod 四则 expressionIdMapping 中嵌套的 LOD 计算字段配置。
     */
    private JSONObject buildNestedLodExpressionIdMappingItem(JSONObject originalItem, MetaField targetMeta,
                                                             String aggSuffix) {
        JSONObject item = JSONObject.parseObject(originalItem.toJSONString());
        String newMeasureId = appendAggSuffixIfNeeded(targetMeta.getId(), aggSuffix);
        String newLodId = CustomFieldType.LOD.getIdentifier() + newMeasureId;
        // 嵌套 LOD 引用在 common 四则公式中以 [lod:measureId] 出现，mapping id 须与 expression 一致
        item.put("id", newLodId);
        if (item.containsKey("value")) {
            item.put("value", newLodId);
        }
        item.put("code", appendAggSuffixIfNeeded(targetMeta.getCode(), aggSuffix));
        item.put("title", targetMeta.getTitle());
        item.put("originId", newLodId);
        if (StrUtil.isNotEmpty(targetMeta.getModuleCtgId())) {
            item.put("moduleCtgId", targetMeta.getModuleCtgId());
        }
        JSONObject nestedCfg = item.getJSONObject("customFieldConfigure");
        if (nestedCfg != null) {
            nestedCfg.put("expression", "[" + newMeasureId + "]");
            JSONObject nestedLodConfig = nestedCfg.getJSONObject("lodConfig");
            if (nestedLodConfig != null) {
                nestedLodConfig.put("measureId", newMeasureId);
            }
            JSONArray nestedMapping = nestedCfg.getJSONArray("expressionIdMapping");
            if (CollUtil.isNotEmpty(nestedMapping)) {
                for (int i = 0; i < nestedMapping.size(); i++) {
                    JSONObject mapItem = nestedMapping.getJSONObject(i);
                    if (mapItem == null) {
                        continue;
                    }
                    mapItem.put("id", newMeasureId);
                    mapItem.put("code", appendAggSuffixIfNeeded(targetMeta.getCode(), aggSuffix));
                    mapItem.put("title", targetMeta.getTitle());
                    MetricExpansionMetaUtil.applyExpressionIdMappingKpiNo(mapItem, targetMeta);
                    if (StrUtil.isNotEmpty(targetMeta.getModuleCtgId())) {
                        mapItem.put("moduleCtgId", targetMeta.getModuleCtgId());
                    }
                    if (StrUtil.isNotEmpty(targetMeta.getAggExpression())) {
                        mapItem.put("aggExpression", targetMeta.getAggExpression());
                    }
                    if (StrUtil.isNotEmpty(targetMeta.getName())) {
                        mapItem.put("name", targetMeta.getName());
                    }
                }
            }
        }
        return item;
    }

    /** 从 expressionIdMapping 嵌套 LOD 配置解析底层 source 指标 code */
    private String resolveNestedLodUnderlyingSourceCode(JSONObject mappingItem) {
        if (mappingItem == null) {
            return null;
        }
        JSONObject nestedCfg = mappingItem.getJSONObject("customFieldConfigure");
        if (nestedCfg == null || !CustomFieldType.LOD.getCode().equals(nestedCfg.getString("type"))) {
            return null;
        }
        JSONObject nestedLodConfig = nestedCfg.getJSONObject("lodConfig");
        if (nestedLodConfig == null) {
            return null;
        }
        return MetricExpansionMetaUtil.resolveLodUnderlyingSourceCode(
                nestedCfg, nestedLodConfig.getString("measureId"));
    }

    /** 从 expressionIdMapping 嵌套 LOD 配置解析可膨胀 source 指标 code */
    private String resolveNestedLodMappingSourceCode(MetricExpansionContext context, JSONObject mappingItem) {
        if (mappingItem == null) {
            return null;
        }
        JSONObject nestedCfg = mappingItem.getJSONObject("customFieldConfigure");
        if (nestedCfg == null || !CustomFieldType.LOD.getCode().equals(nestedCfg.getString("type"))) {
            return null;
        }
        JSONObject nestedLodConfig = nestedCfg.getJSONObject("lodConfig");
        if (nestedLodConfig == null) {
            return null;
        }
        return context.resolveExpandableSourceCodeByMeasureId(
                nestedLodConfig.getString("measureId"), nestedCfg);
    }

    /**
     * 生成计算字段变体 id。
     * 纯 LOD 字段在 buildVariant 末尾按 lodConfig.measureId 写回 id；lod_calc / 分析项仍保留 lod:/analysis: 前缀 + Guid。
     */
    private String buildVariantFieldId(JSONObject original) {
        JSONObject customCfg = original == null ? null : original.getJSONObject("customFieldConfigure");
        if (customCfg != null && CustomFieldType.LOD.getCode().equals(customCfg.getString("type"))) {
            JSONObject lodConfig = customCfg.getJSONObject("lodConfig");
            if (lodConfig != null && StrUtil.isNotEmpty(lodConfig.getString("measureId"))) {
                return CustomFieldType.LOD.getIdentifier() + lodConfig.getString("measureId");
            }
        }
        if (customCfg != null && StrUtil.isNotEmpty(customCfg.getString("type"))) {
            CustomFieldType fieldType = CustomFieldType.get(customCfg.getString("type"));
            if (fieldType == CustomFieldType.LOD_CALC && StrUtil.isNotEmpty(fieldType.getIdentifier())) {
                return fieldType.getIdentifier() + Guid.id();
            }
        }
        String originalId = original == null ? null : original.getString("id");
        if (StrUtil.isNotEmpty(originalId)) {
            if (originalId.startsWith(CustomFieldType.LOD.getIdentifier())) {
                return CustomFieldType.LOD.getIdentifier() + Guid.id();
            }
            if (originalId.startsWith(CustomFieldType.ANALYSIS.getIdentifier())) {
                return CustomFieldType.ANALYSIS.getIdentifier() + Guid.id();
            }
        }
        return Guid.id();
    }

    /**
     * 依赖计算字段的分析项（如 calc_占列总计）随 calc 变体 1→M 同步展开。
     * expandAnalysisMeasureFields 仅处理 source 普通指标，calc 变体生成后需补跑本方法。
     */
    private void expandCalcDependentAnalysisMeasureFields(MetricExpansionContext context, JSONArray measures) {
        if (CollUtil.isEmpty(measures) || CollUtil.isEmpty(context.getCalcFieldIdToVariantIds())) {
            return;
        }
        JSONArray rebuilt = new JSONArray();
        for (int i = 0; i < measures.size(); i++) {
            JSONObject field = measures.getJSONObject(i);
            if (!isAnalysisMeasureField(field)) {
                rebuilt.add(field);
                continue;
            }
            JSONObject analysisConfig = field.getJSONObject("analysisConfig");
            if (analysisConfig == null) {
                rebuilt.add(field);
                continue;
            }
            String dependencyCalcId = analysisConfig.getString("measureId");
            List<String> calcVariantIds = context.getCalcFieldIdToVariantIds().get(dependencyCalcId);
            if (CollUtil.isEmpty(calcVariantIds)) {
                rebuilt.add(field);
                continue;
            }
            String oldAnalysisId = field.getString("id");
            String oldAnalysisCode = field.getString("code");
            String oldAnalysisTitle = MetricExpansionMetaUtil.resolveFieldTitle(field);
            String oldCalcTitle = resolveOldCalcTitle(context, dependencyCalcId);
            if (context.isPreserveSourceData()) {
                rebuilt.add(field);
            }
            for (String calcVariantId : calcVariantIds) {
                JSONObject calcVariant = context.getCalcFieldVariantById().get(calcVariantId);
                if (calcVariant == null) {
                    continue;
                }
                String targetBusinessline = resolveCalcVariantBusinessLine(context, dependencyCalcId, calcVariantId);
                JSONObject expandedAnalysis = buildExpandedCalcAnalysisMeasureField(
                        field, analysisConfig, calcVariant, calcVariantId, oldCalcTitle, targetBusinessline);
                rebuilt.add(expandedAnalysis);
                context.addFieldMapping(
                        oldAnalysisId,
                        oldAnalysisCode,
                        oldAnalysisTitle,
                        expandedAnalysis.getString("id"),
                        expandedAnalysis.getString("code"),
                        MetricExpansionMetaUtil.resolveFieldTitle(expandedAnalysis),
                        null,
                        null,
                        targetBusinessline);
                context.getFieldIdToCode().put(
                        expandedAnalysis.getString("id"), expandedAnalysis.getString("code"));
            }
        }
        measures.clear();
        measures.addAll(rebuilt);
    }

    private JSONObject buildExpandedCalcAnalysisMeasureField(JSONObject sourceField, JSONObject analysisConfig,
                                                             JSONObject calcVariant, String calcVariantId,
                                                             String oldCalcTitle, String targetBusinessline) {
        JSONObject copy = JSONObject.parseObject(sourceField.toJSONString());
        JSONObject newAnalysisConfig = copy.getJSONObject("analysisConfig");
        newAnalysisConfig.put("measureId", calcVariantId);
        newAnalysisConfig.put("measureCode", StrUtil.nullToEmpty(calcVariant.getString("code")));

        String calcVariantTitle = MetricExpansionMetaUtil.resolveFieldTitle(calcVariant);
        rewriteCalcDependentAnalysisText(copy, "title", sourceField, oldCalcTitle, calcVariantTitle);
        rewriteCalcDependentAnalysisText(copy, "displayTitle", sourceField, oldCalcTitle, calcVariantTitle);
        rewriteCalcDependentAnalysisText(copy, "name", sourceField, oldCalcTitle, calcVariantTitle);

        String newCode = rewriteCalcDependentAnalysisCode(
                sourceField.getString("code"), oldCalcTitle, calcVariantTitle, newAnalysisConfig);
        copy.put("id", newCode);
        copy.put("code", newCode);
        if (!copy.containsKey("name") || StrUtil.isEmpty(copy.getString("name"))) {
            copy.put("name", newCode);
        }
        MetricExpansionMetaUtil.normalizeEmptyDisplayTitle(copy);
        return copy;
    }

    private String rewriteCalcDependentAnalysisCode(String oldCode, String oldCalcTitle, String calcVariantTitle,
                                                    JSONObject analysisConfig) {
        String rewritten = rewriteCalcDependentAnalysisTextValue(oldCode, oldCalcTitle, calcVariantTitle);
        if (StrUtil.isNotEmpty(rewritten)) {
            return rewritten;
        }
        String measureKey = resolveCalcMeasureKey(calcVariantTitle);
        return MetricExpansionMetaUtil.buildAnalysisFieldCode(measureKey, analysisConfig);
    }

    private void rewriteCalcDependentAnalysisText(JSONObject field, String key, JSONObject sourceField,
                                                  String oldCalcTitle, String calcVariantTitle) {
        if (!sourceField.containsKey(key)) {
            return;
        }
        String value = sourceField.getString(key);
        if (StrUtil.isEmpty(value)) {
            field.put(key, value);
            return;
        }
        field.put(key, rewriteCalcDependentAnalysisTextValue(value, oldCalcTitle, calcVariantTitle));
    }

    private String rewriteCalcDependentAnalysisTextValue(String value, String oldCalcTitle, String calcVariantTitle) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }
        if (StrUtil.isNotEmpty(oldCalcTitle) && value.contains(oldCalcTitle)) {
            return MetricExpansionMetaUtil.replaceLiteral(value, oldCalcTitle, calcVariantTitle);
        }
        String oldCalcKey = resolveCalcMeasureKey(oldCalcTitle);
        String newCalcKey = resolveCalcMeasureKey(calcVariantTitle);
        if (StrUtil.isNotEmpty(oldCalcKey) && StrUtil.isNotEmpty(newCalcKey)
                && !oldCalcKey.equals(newCalcKey) && value.contains(oldCalcKey)) {
            return MetricExpansionMetaUtil.replaceLiteral(value, oldCalcKey, newCalcKey);
        }
        return value;
    }

    /** 从 calc 展示名提取 code 键（如 轮胎-calc → calc） */
    private String resolveCalcMeasureKey(String calcTitle) {
        if (StrUtil.isEmpty(calcTitle)) {
            return calcTitle;
        }
        int index = calcTitle.lastIndexOf('-');
        if (index >= 0 && index < calcTitle.length() - 1) {
            return calcTitle.substring(index + 1);
        }
        return calcTitle;
    }

    private String resolveOldCalcTitle(MetricExpansionContext context, String oldCalcId) {
        if (context == null || StrUtil.isEmpty(oldCalcId)) {
            return null;
        }
        for (MetricExpansionFieldMappingEntity mapping : context.getFieldMappings()) {
            if (mapping != null && oldCalcId.equals(mapping.getOldFieldId())) {
                return mapping.getOldFieldTitle();
            }
        }
        return null;
    }

    private String resolveCalcVariantBusinessLine(MetricExpansionContext context, String oldCalcId,
                                                   String calcVariantId) {
        if (context == null || StrUtil.isEmpty(oldCalcId) || StrUtil.isEmpty(calcVariantId)) {
            return null;
        }
        for (MetricExpansionFieldMappingEntity mapping : context.getFieldMappings()) {
            if (mapping == null || !oldCalcId.equals(mapping.getOldFieldId())) {
                continue;
            }
            if (calcVariantId.equals(mapping.getNewFieldId())) {
                return mapping.getTargetBusinessline();
            }
        }
        return null;
    }

    /**
     * 判断是否为跨模型指标：虽有 customFieldConfigure.expression，但 code 来自元数据，不走 calc 变体逻辑。
     */
    private boolean isCrossModelMeasureField(JSONObject measure) {
        if (measure == null) {
            return false;
        }
        if (FieldType.CROSS_MODEL_MEASURE == FieldType.get(measure.getString("fieldType"))) {
            return true;
        }
        String fieldId = measure.getString("id");
        if (StrUtil.isEmpty(fieldId)) {
            return false;
        }
        MetaField metaField = SSDMetaCacheManager.getField(fieldId);
        return metaField != null
                && FieldType.CROSS_MODEL_MEASURE == FieldType.get(metaField.getFieldType());
    }

    private boolean isAnalysisMeasureField(JSONObject field) {
        return field != null && Enabled.isTrue(field.getInteger("isAnalysis"));
    }

    private JSONObject findExpandedMeasureField(MetricExpansionContext context, MetaField meta) {
        return findExpandedMeasureField(context, meta, null);
    }

    /**
     * 从已膨胀 measures 中查找 target 指标；日均依赖须带 aggSuffix 匹配 id/code 后缀。
     */
    private JSONObject findExpandedMeasureField(MetricExpansionContext context, MetaField meta, String aggSuffix) {
        if (context == null || meta == null || context.getTplConfigJson() == null) {
            return null;
        }
        JSONObject result = context.getTplConfigJson().getJSONObject("result");
        if (result == null) {
            return null;
        }
        JSONArray measures = result.getJSONArray("measures");
        if (CollUtil.isEmpty(measures)) {
            return null;
        }
        String expectedId = appendAggSuffixIfNeeded(meta.getId(), aggSuffix);
        String expectedCode = appendAggSuffixIfNeeded(meta.getCode(), aggSuffix);
        JSONObject matchedByCode = null;
        for (int i = 0; i < measures.size(); i++) {
            JSONObject measure = measures.getJSONObject(i);
            if (measure == null) {
                continue;
            }
            if (StrUtil.isNotEmpty(expectedId) && expectedId.equals(measure.getString("id"))) {
                return measure;
            }
            if (matchedByCode == null
                    && StrUtil.isNotEmpty(expectedCode)
                    && expectedCode.equals(measure.getString("code"))) {
                matchedByCode = measure;
            }
        }
        return matchedByCode;
    }

    /**
     * 从依赖 fieldId/code 解析日均/非空日均后缀。
     */
    private String resolveDependencyAggSuffix(String fieldIdOrCode) {
        if (StrUtil.isEmpty(fieldIdOrCode)) {
            return null;
        }
        return MetricExpansionMetaUtil.resolveAggExpressionSuffix(stripLodPrefix(fieldIdOrCode));
    }

    /**
     * 从 expressionIdMapping 项解析日均/非空日均后缀。
     */
    private String resolveMappingItemAggSuffix(JSONObject mappingItem) {
        if (mappingItem == null) {
            return null;
        }
        String suffix = resolveDependencyAggSuffix(mappingItem.getString("id"));
        if (suffix != null) {
            return suffix;
        }
        return resolveDependencyAggSuffix(mappingItem.getString("code"));
    }

    /**
     * 按 oldId 在 expressionIdMapping 中查找对应项。
     */
    private JSONObject findMappingItemByOldId(JSONArray mapping, String oldId, String sourceCode,
                                              MetricExpansionContext context) {
        if (CollUtil.isEmpty(mapping) || StrUtil.isEmpty(oldId)) {
            return null;
        }
        for (int i = 0; i < mapping.size(); i++) {
            JSONObject item = mapping.getJSONObject(i);
            if (item == null || isAnalysisMappingItem(item)) {
                continue;
            }
            String itemId = item.getString("id");
            if (oldId.equals(itemId)) {
                return item;
            }
            String strippedOldId = MetricExpansionMetaUtil.stripAggExpressionSuffix(oldId);
            if (StrUtil.isNotEmpty(itemId) && itemId.equals(strippedOldId)) {
                return item;
            }
            String itemSourceCode = resolveAtomicMappingSourceCode(context, item);
            if (sourceCode.equals(itemSourceCode) && StrUtil.isNotEmpty(itemId)
                    && (oldId.equals(itemId) || strippedOldId.equals(itemId))) {
                return item;
            }
        }
        return null;
    }

    /**
     * 替换公式展示文案：title / code 字面量，保留日均派生指标后缀。
     */
    private String replaceDependencyDisplayRefInExpression(String expression, JSONObject mappingItem,
                                                           MetaField targetMeta, String aggSuffix,
                                                           String sourceBusinessline, String targetBusinessline,
                                                           MetricExpansionContext context) {
        JSONObject expandedField = findExpandedMeasureField(context, targetMeta, aggSuffix);
        String oldTitle = mappingItem.getString("title");
        if (StrUtil.isNotEmpty(oldTitle)) {
            String newTitle;
            if (expandedField != null) {
                newTitle = MetricExpansionMetaUtil.resolveFieldTitle(expandedField);
            } else {
                newTitle = MetricExpansionMetaUtil.buildExpandedDisplayName(
                        oldTitle, sourceBusinessline, targetBusinessline);
            }
            expression = MetricExpansionMetaUtil.replaceLiteral(expression, oldTitle, newTitle);
        }
        String oldCode = mappingItem.getString("code");
        String newCode = appendAggSuffixIfNeeded(targetMeta.getCode(), aggSuffix);
        // LOD 日均字段 code 常为 _avg_by_d，全局替换会误伤 [uuid_avg_by_d] 中的后缀片段
        if (StrUtil.isNotEmpty(oldCode) && StrUtil.isNotEmpty(newCode)
                && !oldCode.equals(newCode)
                && !MetricExpansionMetaUtil.isAggExpressionSuffixOnly(oldCode)) {
            expression = replaceSourceMetricCodeAvoidingAnalysis(expression, oldCode, newCode);
        }
        return expression;
    }

    /**
     * 从 measures 同步 LOD 依赖项 mapping（id 形如 lod:measureId）。
     */
    private JSONObject buildLodRefExpressionIdMappingFromMeasures(MetricExpansionContext context,
                                                                  JSONObject mappingItem) {
        if (context == null || mappingItem == null) {
            return null;
        }
        String mapId = mappingItem.getString("id");
        if (StrUtil.isEmpty(mapId) || !mapId.startsWith(CustomFieldType.LOD.getIdentifier())) {
            return null;
        }
        JSONObject lodMeasure = findMeasureFieldByIdOrCode(context, mapId, null);
        if (lodMeasure == null) {
            return null;
        }
        JSONObject synced = MetricExpansionMetaUtil.buildExpressionIdMappingItem(lodMeasure);
        if (mappingItem.containsKey("level")) {
            synced.put("level", mappingItem.get("level"));
        }
        if (mappingItem.containsKey("isRatio")) {
            synced.put("isRatio", mappingItem.get("isRatio"));
        }
        return synced;
    }

    /**
     * measures 未命中时，按 aggSuffix 构造带日均后缀的 mapping 兜底项。
     */
    private JSONObject buildExpressionIdMappingItemWithAggSuffix(MetaField meta, String aggSuffix) {
        JSONObject item = MetricExpansionMetaUtil.buildExpressionIdMappingItem(meta);
        if (StrUtil.isEmpty(aggSuffix) || meta == null) {
            return item;
        }
        item.put("id", appendAggSuffixIfNeeded(meta.getId(), aggSuffix));
        item.put("code", appendAggSuffixIfNeeded(meta.getCode(), aggSuffix));
        String title = item.getString("title");
        if (StrUtil.isNotEmpty(title) && StrUtil.isNotEmpty(meta.getCode())
                && title.contains(meta.getCode()) && !title.contains(aggSuffix)) {
            item.put("title", title + aggSuffix);
        }
        MetricExpansionMetaUtil.applyExpressionIdMappingKpiNo(item, meta);
        return item;
    }

    /**
     * 替换公式中分析项引用（analysis: 前缀 id 及分析 code）。
     */
    private String replaceAnalysisRefsInExpression(MetricExpansionContext context, JSONArray mapping,
                                                   Set<String> expandableSources, String businessLine,
                                                   String expression) {
        if (CollUtil.isEmpty(mapping) || StrUtil.isEmpty(expression)) {
            return expression;
        }
        String updatedExpression = expression;
        for (int i = 0; i < mapping.size(); i++) {
            JSONObject item = mapping.getJSONObject(i);
            if (!isAnalysisMappingItem(item)) {
                continue;
            }
            JSONObject analysisItemConfig = item.getJSONObject("analysisItemConfig");
            String dependencySourceCode = resolveAnalysisDependencySourceCode(context, analysisItemConfig);
            if (!expandableSources.contains(dependencySourceCode)) {
                continue;
            }
            ExpandedAnalysisRef expandedRef = resolveExpandedAnalysisRef(
                    context, item, dependencySourceCode, businessLine);
            if (expandedRef == null) {
                log.warn("计算字段变体生成：分析项[{}] 在业务线[{}] 下无法解析膨胀结果，跳过 expression 替换",
                        resolveAnalysisMappingLookupCode(item), businessLine);
                continue;
            }
            String newAnalysisId = expandedRef.getNewAnalysisId();
            String newAnalysisCode = expandedRef.getNewAnalysisCode();
            String oldId = item.getString("id");
            String oldCode = item.getString("code");
            if (StrUtil.isNotEmpty(oldId)) {
                updatedExpression = MetricExpansionMetaUtil.replaceFieldIdInExpression(
                        updatedExpression, oldId, newAnalysisId);
            }
            if (StrUtil.isNotEmpty(oldCode)) {
                updatedExpression = MetricExpansionMetaUtil.replaceLiteral(
                        updatedExpression, oldCode, newAnalysisCode);
            }
        }
        return updatedExpression;
    }

    /**
     * 解析分析项在目标业务线下的新 id/code 及完整 mapping 项。
     * 优先 measures + fieldMappings；无独立分析 measure 时按 analysisItemConfig 现场推导。
     */
    private ExpandedAnalysisRef resolveExpandedAnalysisRef(MetricExpansionContext context, JSONObject originalItem,
                                                           String dependencySourceCode, String businessLine) {
        if (context == null || originalItem == null || StrUtil.isEmpty(dependencySourceCode)
                || StrUtil.isEmpty(businessLine)) {
            return null;
        }
        JSONObject analysisItemConfig = originalItem.getJSONObject("analysisItemConfig");
        if (analysisItemConfig == null) {
            return null;
        }
        MetricExpansionTarget matched = findTargetByBusinessLine(
                context.getTargetsBySourceCode().get(dependencySourceCode), businessLine);
        if (matched == null || matched.getMetaField() == null) {
            return null;
        }
        MetaField targetMeta = matched.getMetaField();
        String newAnalysisCode = resolveExpandedAnalysisCode(context, originalItem, businessLine, targetMeta,
                analysisItemConfig);
        if (StrUtil.isEmpty(newAnalysisCode)) {
            return null;
        }
        String newAnalysisId = ANALYSIS_ID_PREFIX + newAnalysisCode;

        JSONObject expandedMeasure = findExpandedAnalysisMeasureField(context, originalItem, businessLine);
        JSONObject mappingItem;
        if (expandedMeasure != null) {
            mappingItem = MetricExpansionMetaUtil.buildAnalysisExpressionIdMappingItem(expandedMeasure);
        } else {
            mappingItem = buildAnalysisMappingItemFallback(context, originalItem, analysisItemConfig, targetMeta,
                    dependencySourceCode, businessLine, newAnalysisId, newAnalysisCode);
        }
        syncAnalysisMappingUiFields(originalItem, mappingItem, newAnalysisId);

        ExpandedAnalysisRef ref = new ExpandedAnalysisRef();
        ref.setNewAnalysisId(newAnalysisId);
        ref.setNewAnalysisCode(newAnalysisCode);
        ref.setMappingItem(mappingItem);
        return ref;
    }

    private String resolveExpandedAnalysisCode(MetricExpansionContext context, JSONObject originalItem,
                                             String businessLine, MetaField targetMeta,
                                             JSONObject analysisItemConfig) {
        MetricExpansionFieldMappingEntity fieldMapping = resolveAnalysisFieldMapping(context, originalItem, businessLine);
        if (fieldMapping != null && StrUtil.isNotEmpty(fieldMapping.getNewFieldCode())) {
            return fieldMapping.getNewFieldCode();
        }
        JSONObject updatedConfig = JSONObject.parseObject(analysisItemConfig.toJSONString());
        updatedConfig.put("measureId", targetMeta.getId());
        updatedConfig.put("measureCode", targetMeta.getCode());
        return MetricExpansionMetaUtil.buildAnalysisFieldCode(targetMeta.getCode(), updatedConfig);
    }

    private JSONObject buildAnalysisMappingItemFallback(MetricExpansionContext context, JSONObject originalItem,
                                                        JSONObject analysisItemConfig, MetaField targetMeta,
                                                        String dependencySourceCode, String businessLine,
                                                        String newAnalysisId, String newAnalysisCode) {
        JSONObject updatedConfig = JSONObject.parseObject(analysisItemConfig.toJSONString());
        updatedConfig.put("measureId", targetMeta.getId());
        updatedConfig.put("measureCode", targetMeta.getCode());

        JSONObject item = new JSONObject();
        item.put("id", newAnalysisId);
        item.put("code", newAnalysisCode);
        item.put("analysisItemConfig", updatedConfig);
        String title = MetricExpansionMetaUtil.resolveFieldTitle(originalItem);
        if (StrUtil.isNotEmpty(title)) {
            String expandedTitle = rewriteAnalysisMappingTitle(title, dependencySourceCode, targetMeta,
                    context.getSourceBusinessline(), businessLine);
            item.put("title", expandedTitle);
        }
        if (StrUtil.isNotEmpty(targetMeta.getModuleCtgId())) {
            item.put("moduleCtgId", targetMeta.getModuleCtgId());
        }
        if (StrUtil.isNotEmpty(targetMeta.getDataType())) {
            item.put("dataType", targetMeta.getDataType());
        }
        if (originalItem.containsKey("isRatio")) {
            item.put("isRatio", originalItem.get("isRatio"));
        }
        MetricExpansionMetaUtil.normalizeEmptyDisplayTitle(item);
        return item;
    }

    private String rewriteAnalysisMappingTitle(String title, String sourceCode, MetaField targetMeta,
                                               String sourceBusinessline, String targetBusinessline) {
        if (StrUtil.isNotEmpty(sourceCode) && title.contains(sourceCode)) {
            return MetricExpansionMetaUtil.replaceLiteral(title, sourceCode, targetMeta.getCode());
        }
        return MetricExpansionMetaUtil.buildExpandedDisplayName(title, sourceBusinessline, targetBusinessline);
    }

    /** 同步前端 expressionIdMapping 扩展字段（level/label/value 等） */
    private void syncAnalysisMappingUiFields(JSONObject originalItem, JSONObject mappingItem, String newAnalysisId) {
        if (originalItem == null || mappingItem == null) {
            return;
        }
        if (originalItem.containsKey("level")) {
            mappingItem.put("level", originalItem.get("level"));
        }
        if (originalItem.containsKey("isRatio")) {
            mappingItem.put("isRatio", originalItem.get("isRatio"));
        }
        if (originalItem.containsKey("canSetDecimal")) {
            mappingItem.put("canSetDecimal", originalItem.get("canSetDecimal"));
        }
        String title = mappingItem.getString("title");
        if (StrUtil.isNotEmpty(title)) {
            mappingItem.put("label", title);
        } else if (originalItem.containsKey("label")) {
            mappingItem.put("label", originalItem.get("label"));
        }
        mappingItem.put("value", newAnalysisId);
    }

    /**
     * 替换 source 指标 code，但跳过 analysis: 前缀 token 内部，避免半截替换。
     */
    private String replaceSourceMetricCodeAvoidingAnalysis(String expression, String sourceCode, String targetCode) {
        if (StrUtil.isEmpty(expression) || StrUtil.isEmpty(sourceCode) || sourceCode.equals(targetCode)) {
            return expression;
        }
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (cursor < expression.length()) {
            int analysisStart = expression.indexOf(ANALYSIS_ID_PREFIX, cursor);
            if (analysisStart < 0) {
                result.append(expression.substring(cursor).replace(sourceCode, targetCode));
                break;
            }
            String segment = expression.substring(cursor, analysisStart);
            result.append(segment.replace(sourceCode, targetCode));
            int tokenEnd = analysisStart + ANALYSIS_ID_PREFIX.length();
            while (tokenEnd < expression.length()) {
                char current = expression.charAt(tokenEnd);
                if (current == ']' || current == ' ' || current == '+'
                        || current == '-' || current == '*' || current == '/'
                        || current == '(' || current == ')') {
                    break;
                }
                tokenEnd++;
            }
            result.append(expression, analysisStart, tokenEnd);
            cursor = tokenEnd;
        }
        return result.toString();
    }

    /**
     * 从 fieldMappings 查找分析项在指定业务线下的新 id/code。
     */
    private MetricExpansionFieldMappingEntity resolveAnalysisFieldMapping(MetricExpansionContext context,
                                                                          JSONObject mappingItem,
                                                                          String businessLine) {
        if (context == null || mappingItem == null || StrUtil.isEmpty(businessLine)) {
            return null;
        }
        String lookupCode = resolveAnalysisMappingLookupCode(mappingItem);
        String lookupId = resolveAnalysisMappingLookupId(mappingItem);
        for (MetricExpansionFieldMappingEntity mapping : context.getFieldMappings()) {
            if (mapping == null || !businessLine.equals(mapping.getTargetBusinessline())) {
                continue;
            }
            if (StrUtil.isNotEmpty(lookupId) && lookupId.equals(mapping.getOldFieldId())) {
                return mapping;
            }
            if (StrUtil.isNotEmpty(lookupCode) && lookupCode.equals(mapping.getOldFieldCode())) {
                return mapping;
            }
        }
        return null;
    }

    /**
     * 从已膨胀 measures 中查找分析项字段 JSON。
     */
    private JSONObject findExpandedAnalysisMeasureField(MetricExpansionContext context, JSONObject mappingItem,
                                                      String businessLine) {
        MetricExpansionFieldMappingEntity fieldMapping = resolveAnalysisFieldMapping(context, mappingItem, businessLine);
        if (fieldMapping == null) {
            return null;
        }
        return findMeasureFieldByIdOrCode(context, fieldMapping.getNewFieldId(), fieldMapping.getNewFieldCode());
    }

    private JSONObject findMeasureFieldByIdOrCode(MetricExpansionContext context, String fieldId, String fieldCode) {
        if (context == null || context.getTplConfigJson() == null) {
            return null;
        }
        JSONObject result = context.getTplConfigJson().getJSONObject("result");
        if (result == null) {
            return null;
        }
        JSONArray measures = result.getJSONArray("measures");
        if (CollUtil.isEmpty(measures)) {
            return null;
        }
        JSONObject matchedById = null;
        JSONObject matchedByCode = null;
        for (int i = 0; i < measures.size(); i++) {
            JSONObject measure = measures.getJSONObject(i);
            if (measure == null) {
                continue;
            }
            if (StrUtil.isNotEmpty(fieldId) && fieldId.equals(measure.getString("id"))) {
                matchedById = measure;
                break;
            }
            if (matchedByCode == null
                    && StrUtil.isNotEmpty(fieldCode)
                    && fieldCode.equals(measure.getString("code"))) {
                matchedByCode = measure;
            }
        }
        return matchedById != null ? matchedById : matchedByCode;
    }

    private boolean isAnalysisMappingItem(JSONObject mappingItem) {
        if (mappingItem == null) {
            return false;
        }
        if (mappingItem.getJSONObject("analysisItemConfig") != null) {
            return true;
        }
        String id = mappingItem.getString("id");
        return StrUtil.isNotEmpty(id) && id.contains(ANALYSIS_ID_PREFIX);
    }

    /** 解析分析项 mapping 用于 fieldMappings 查找的 code（measures 中 id/code 不含 analysis: 前缀） */
    private String resolveAnalysisMappingLookupCode(JSONObject mappingItem) {
        String code = mappingItem.getString("code");
        if (StrUtil.isNotEmpty(code)) {
            return code;
        }
        return resolveAnalysisMappingLookupId(mappingItem);
    }

    private String resolveAnalysisMappingLookupId(JSONObject mappingItem) {
        String id = mappingItem.getString("id");
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        if (id.startsWith(ANALYSIS_ID_PREFIX)) {
            return id.substring(ANALYSIS_ID_PREFIX.length());
        }
        return id;
    }

    /** 解析分析项依赖的 source 指标 code */
    private String resolveAnalysisDependencySourceCode(MetricExpansionContext context, JSONObject analysisItemConfig) {
        if (analysisItemConfig == null) {
            return null;
        }
        String measureCode = analysisItemConfig.getString("measureCode");
        if (context.isExpandableSourceCode(measureCode)) {
            return measureCode;
        }
        String measureId = analysisItemConfig.getString("measureId");
        return context.resolveExpandableSourceCodeById(measureId);
    }

    /**
     * 查找公式中某 source 指标对应的旧 fieldId，用于 expression 内 id 替换。
     *
     * 优先从 expressionIdMapping 解析（支持 code 为空、仅 id 的项）；
     * mapping 未命中时，仅在 expression 字符串中实际出现的 id 且 fieldIdToCode 映射为 sourceCode 时才兜底，
     * 避免误用 filter 等区域的 stale id。
     *
     * @param context      上下文
     * @param mapping      原计算字段的 expressionIdMapping
     * @param sourceCode   待替换的 source 指标 code
     * @param expression   原公式字符串，用于兜底 id 校验
     * @return 需要替换的旧 id 列表
     */
    private List<String> findOldIdsForSource(MetricExpansionContext context, JSONArray mapping,
                                             String sourceCode, String expression) {
        Set<String> idSet = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(mapping)) {
            for (int i = 0; i < mapping.size(); i++) {
                JSONObject item = mapping.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String itemSourceCode = resolveAtomicMappingSourceCode(context, item);
                if (StrUtil.isEmpty(itemSourceCode)) {
                    itemSourceCode = resolveNestedLodMappingSourceCode(context, item);
                }
                if (StrUtil.isEmpty(itemSourceCode)) {
                    itemSourceCode = resolveMappingSourceCode(context, item);
                }
                if (!sourceCode.equals(itemSourceCode)) {
                    continue;
                }
                String itemId = item.getString("id");
                if (StrUtil.isNotEmpty(itemId)) {
                    idSet.add(itemId);
                }
            }
        }
        appendOldIdsFromExpression(context, sourceCode, expression, idSet);
        return new ArrayList<>(idSet);
    }

    /**
     * 从 formula 中补充 source 对应的旧 fieldId（引用不在 measures / mapping 时通过元数据反查）。
     */
    private void appendOldIdsFromExpression(MetricExpansionContext context, String sourceCode,
                                            String expression, Set<String> idSet) {
        if (StrUtil.isEmpty(expression) || StrUtil.isEmpty(sourceCode)) {
            return;
        }
        for (String refId : MetricExpansionMetaUtil.extractExpressionFieldIds(expression)) {
            if (idSet.contains(refId)) {
                continue;
            }
            if (MetricExpansionMetaUtil.matchesSourceCode(refId, sourceCode)) {
                idSet.add(refId);
                continue;
            }
            String codeById = context.getFieldIdToCode().get(refId);
            if (sourceCode.equals(codeById)) {
                idSet.add(refId);
            }
        }
        for (Map.Entry<String, String> entry : context.getFieldIdToCode().entrySet()) {
            if (!sourceCode.equals(entry.getValue())) {
                continue;
            }
            String fieldId = entry.getKey();
            if (expression.contains(fieldId)) {
                idSet.add(fieldId);
            }
        }
        String lodPrefix = CustomFieldType.LOD.getIdentifier();
        for (Map.Entry<String, String> entry : context.getFieldIdToCode().entrySet()) {
            if (!sourceCode.equals(entry.getValue())) {
                continue;
            }
            String fieldId = entry.getKey();
            String lodFieldId = lodPrefix + fieldId;
            if (expression.contains(lodFieldId)) {
                idSet.add(fieldId);
            }
        }
    }

    /**
     * 从 expressionIdMapping 单项解析其引用的 atomic source code（不含分析项）。
     * 优先通过 id→fieldIdToCode 反查，避免 code 字段误存分析项 code 导致漏替换。
     */
    private String resolveAtomicMappingSourceCode(MetricExpansionContext context, JSONObject mappingItem) {
        if (mappingItem == null || isAnalysisMappingItem(mappingItem)) {
            return null;
        }
        String mapId = mappingItem.getString("id");
        if (StrUtil.isNotEmpty(mapId)) {
            String codeById = context.getFieldIdToCode().get(mapId);
            if (!context.isExpandableSourceCode(codeById)) {
                codeById = MetricExpansionMetaUtil.resolveMeasureCodeByMeasureId(mapId);
            }
            if (context.isExpandableSourceCode(codeById)) {
                return codeById;
            }
            String baseMapId = MetricExpansionMetaUtil.stripAggExpressionSuffix(mapId);
            if (!baseMapId.equals(mapId)) {
                codeById = context.getFieldIdToCode().get(baseMapId);
                if (!context.isExpandableSourceCode(codeById)) {
                    codeById = MetricExpansionMetaUtil.resolveMeasureCodeByMeasureId(baseMapId);
                }
                if (context.isExpandableSourceCode(codeById)) {
                    return codeById;
                }
            }
        }
        String mapCode = mappingItem.getString("code");
        if (context.isExpandableSourceCode(mapCode)) {
            return mapCode;
        }
        return null;
    }

    /**
     * 从 expressionIdMapping 单项解析其引用的 metric source code。
     *
     * 优先读 code 字段；code 为空时通过 id 查 fieldIdToCode 反查。
     *
     * @param context     上下文
     * @param mappingItem mapping 数组中的单项
     * @return source code；无法解析返回 null
     */
    private String resolveMappingSourceCode(MetricExpansionContext context, JSONObject mappingItem) {
        if (mappingItem == null) {
            return null;
        }
        String mapCode = mappingItem.getString("code");
        if (StrUtil.isNotEmpty(mapCode)) {
            return mapCode;
        }
        String mapId = mappingItem.getString("id");
        if (StrUtil.isEmpty(mapId)) {
            return null;
        }
        return context.getFieldIdToCode().get(mapId);
    }

    /**
     * 在目标列表中按业务线精确匹配一条 target。
     *
     * @param targets      某 source 的全部 target
     * @param businessLine 目标业务线
     * @return 匹配项；不存在返回 null
     */
    private MetricExpansionTarget findTargetByBusinessLine(List<MetricExpansionTarget> targets, String businessLine) {
        if (CollUtil.isEmpty(targets)) {
            return null;
        }
        for (MetricExpansionTarget target : targets) {
            if (businessLine.equals(target.getTargetBusinessline())) {
                return target;
            }
        }
        return null;
    }

    /**
     * 按原计算字段 id/code 在计划列表中定位对应计划。
     */
    private CalcFieldExpansionPlan findPlan(List<CalcFieldExpansionPlan> plans, JSONObject measure) {
        String id = measure.getString("id");
        String code = measure.getString("code");
        for (CalcFieldExpansionPlan plan : plans) {
            if ((StrUtil.isNotEmpty(id) && id.equals(plan.getOriginalId()))
                    || (StrUtil.isNotEmpty(code) && code.equals(plan.getOriginalCode()))) {
                return plan;
            }
        }
        return null;
    }

    /**
     * 单个计算字段的膨胀计划：记录原字段标识与全部业务线变体。
     */
    private static class CalcFieldExpansionPlan {
        /** 原计算字段 id */
        private String originalId;
        /** 原计算字段 code */
        private String originalCode;
        /** 按业务线生成的变体字段列表 */
        private List<JSONObject> variants;
        /** 与 variants 一一对应的目标业务线 */
        private List<String> variantBusinessLines;

        public String getOriginalId() {
            return originalId;
        }

        public void setOriginalId(String originalId) {
            this.originalId = originalId;
        }

        public String getOriginalCode() {
            return originalCode;
        }

        public void setOriginalCode(String originalCode) {
            this.originalCode = originalCode;
        }

        public List<JSONObject> getVariants() {
            return variants;
        }

        public void setVariants(List<JSONObject> variants) {
            this.variants = variants;
        }

        public List<String> getVariantBusinessLines() {
            return variantBusinessLines;
        }

        public void setVariantBusinessLines(List<String> variantBusinessLines) {
            this.variantBusinessLines = variantBusinessLines;
        }
    }

    /** 分析项膨胀解析结果：新 id/code 及完整 mapping 项 */
    private static class ExpandedAnalysisRef {
        private String newAnalysisId;
        private String newAnalysisCode;
        private JSONObject mappingItem;

        public String getNewAnalysisId() {
            return newAnalysisId;
        }

        public void setNewAnalysisId(String newAnalysisId) {
            this.newAnalysisId = newAnalysisId;
        }

        public String getNewAnalysisCode() {
            return newAnalysisCode;
        }

        public void setNewAnalysisCode(String newAnalysisCode) {
            this.newAnalysisCode = newAnalysisCode;
        }

        public JSONObject getMappingItem() {
            return mappingItem;
        }

        public void setMappingItem(JSONObject mappingItem) {
            this.mappingItem = mappingItem;
        }
    }
}
