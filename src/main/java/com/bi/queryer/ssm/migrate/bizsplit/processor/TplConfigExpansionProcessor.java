package com.bi.queryer.ssm.migrate.bizsplit.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.service.MetricExpansionContext;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionFieldMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionTarget;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionMetaUtil;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.sys.enums.Enabled;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * tplConfig 普通指标与引用位置膨胀处理器。
 *
 * 在 MetricExpansionService 中的调用顺序：
 * scanSourceMetricCodes → expandTplConfig →（CalcFieldExpansionProcessor 处理计算字段）
 * → expandTplConfigForNewCalcFields。
 *
 * 列顺序由 result.measures 数组物理顺序与 showOrder 维护，前端 tableStyle 无 columnOrders 字段。
 *
 * 职责划分：
 * 本类负责「映射表 source 指标」在 tplConfig 各区域的 1→N 展开；
 * 计算字段（customFieldConfigure）的公式变体由 CalcFieldExpansionProcessor 生成，
 * 生成后通过 expandTplConfigForNewCalcFields 把变体 id/code 同步到 filter、sort、analysis 等引用处。
 *
 * 扫描范围（与膨胀范围一致，不含 rowDimensions/colDimensions）：
 * filter、result.measures、analysis（thb/target/zb/total）、setting.customCompare、
 * setting.tableStyle.conditionalFormat；
 * 同时下钻计算字段 expressionIdMapping 与 lodConfig.measureId，用于识别 hidden 依赖。
 *
 * 膨胀规则：
 * 命中 activeSourceCodes 的 code 或 fieldId，按映射表顺序展开为 N 个 target；
 * measureIdList 中的字面量 "all" 原样保留；N=1 时等价于单条替换。
 */
@Component
public class TplConfigExpansionProcessor {

    private static final Logger log = LoggerFactory.getLogger(TplConfigExpansionProcessor.class);

    /** thb baseAndZb 等场景：measureIdList 复合引用分隔符，形如 {baseId}___zb_ct */
    private static final String COMPOSITE_MEASURE_REF_SEPARATOR = "___";

    /**
     * 扫描 tplConfig，找出本视图需要膨胀的 source 指标编码。
     *
     * 流程：遍历各区域收集 metric code 与 id→code 索引，再与映射表全集求交集，
     * 写入 context.activeSourceCodes。若结果为空，后续不进行 expandTplConfig。
     *
     * @param context 运行时上下文，会写入 activeSourceCodes 与 fieldIdToCode
     * @return 本视图命中的 source 集合
     */
    public Set<String> scanSourceMetricCodes(MetricExpansionContext context) {
        JSONObject root = context.getTplConfigJson();
        Set<String> foundCodes = new LinkedHashSet<>();

        scanFieldArray(root.getJSONArray("filter"), foundCodes, context);
        JSONObject result = root.getJSONObject("result");
        if (result != null) {
            scanFieldArray(result.getJSONArray("measures"), foundCodes, context);
        }
        scanAnalysisMeasureIds(root.getJSONObject("analysis"), foundCodes, context);
        scanCustomCompare(root.getJSONObject("setting"), foundCodes, context);
        scanConditionalFormat(root.getJSONObject("setting"), foundCodes, context);

        Set<String> activeSources = new LinkedHashSet<>();
        for (String code : foundCodes) {
            if (context.getAllMappingSourceCodes().contains(code)) {
                activeSources.add(code);
            }
        }
        context.setActiveSourceCodes(activeSources);
        return activeSources;
    }

    /**
     * 普通 source 指标在 tplConfig 各引用区域的 1→N 膨胀。
     *
     * 依次处理 measures、filter、sort、analysis、customCompare。
     * 不在此处理计算字段变体。
     * 膨胀 measures 时维护 showOrder 与数组物理顺序。
     *
     * @param context 已完成 target 元数据校验的上下文
     */
    public void expandTplConfig(MetricExpansionContext context) {
        indexExistingMeasureFields(context);
        context.captureInitialMeasureIdsAtExpansionStart();
        JSONObject root = context.getTplConfigJson();
        JSONObject result = root.getJSONObject("result");
        if (result != null) {
            expandMeasures(context, result);
        }
        expandFilterFields(context, root.getJSONArray("filter"));
        expandSortFields(context, root.getJSONObject("setting"));
        expandConditionalFormat(context, root.getJSONObject("setting"), false);
        expandAnalysisSections(context, root.getJSONObject("analysis"));
        expandCustomCompare(context, root.getJSONObject("setting"));
    }

    /**
     * 将「原计算字段 → 业务线变体列表」同步到 filter、sort、analysis、customCompare 等引用处。
     *
     * 前置条件：CalcFieldExpansionProcessor 已写入 calcFieldIdToVariantIds、
     * calcFieldCodeToVariantCodes、calcFieldCodeToVariantIds、calcFieldVariantById。
     * 若上述映射均为空则直接返回。
     *
     * @param context 已包含计算字段变体映射的上下文
     */
    public void expandTplConfigForNewCalcFields(MetricExpansionContext context) {
        if (CollUtil.isEmpty(context.getCalcFieldIdToVariantIds())
                && CollUtil.isEmpty(context.getCalcFieldCodeToVariantCodes())) {
            return;
        }
        JSONObject root = context.getTplConfigJson();
        expandFilterByCalcVariants(context, root.getJSONArray("filter"));
        expandSortByCalcVariants(context, root.getJSONObject("setting"));
        expandConditionalFormat(context, root.getJSONObject("setting"), true);
        expandAnalysisByCalcVariants(context, root.getJSONObject("analysis"));
        expandCustomCompareByCalcVariants(context, root.getJSONObject("setting"));
    }

    /**
     * 扫描字段数组：收集 code，维护 id→code，并下钻计算字段依赖快照。
     *
     * @param fields     字段数组（可为 null）
     * @param foundCodes 收集到的指标 code
     * @param context    上下文
     */
    private void scanFieldArray(JSONArray fields, Set<String> foundCodes, MetricExpansionContext context) {
        if (CollUtil.isEmpty(fields)) {
            return;
        }
        for (int i = 0; i < fields.size(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field == null) {
                continue;
            }
            // LOD 指标 code 常为 _avg_by_d，改由 lodConfig.measureId 索引依赖
            if (MetricExpansionMetaUtil.isPureLodMeasureField(field)) {
                indexLodMeasureDependency(field, foundCodes, context);
                continue;
            }
            String id = field.getString("id");
            String code = field.getString("code");
            if (StrUtil.isNotEmpty(id) && StrUtil.isNotEmpty(code)) {
                context.getFieldIdToCode().put(id, code);
            }
            if (StrUtil.isNotEmpty(code)) {
                foundCodes.add(code);
                String baseCode = MetricExpansionMetaUtil.stripAggExpressionSuffix(code);
                if (StrUtil.isNotEmpty(baseCode) && !baseCode.equals(code)) {
                    foundCodes.add(baseCode);
                }
            }
            // 计算字段依赖快照中的 code 也纳入扫描
            JSONObject customCfg = field.getJSONObject("customFieldConfigure");
            if (customCfg != null) {
                JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
                if (CollUtil.isNotEmpty(mapping)) {
                    for (int j = 0; j < mapping.size(); j++) {
                        JSONObject item = mapping.getJSONObject(j);
                        if (item == null) {
                            continue;
                        }
                        String mapId = item.getString("id");
                        String mapCode = item.getString("code");
                        if (StrUtil.isNotEmpty(mapId) && StrUtil.isNotEmpty(mapCode)) {
                            context.getFieldIdToCode().put(mapId, mapCode);
                        }
                        if (StrUtil.isNotEmpty(mapCode)) {
                            foundCodes.add(mapCode);
                        }
                        if (StrUtil.isNotEmpty(mapId)) {
                            MetricExpansionMetaUtil.indexMeasureIdRef(mapId, foundCodes, context.getFieldIdToCode());
                        }
                        indexNestedLodMappingRef(item, foundCodes, context);
                    }
                }
                indexCalcExpressionRefs(customCfg.getString("expression"), foundCodes, context);
                JSONObject lodConfig = customCfg.getJSONObject("lodConfig");
                if (lodConfig != null) {
                    String measureId = lodConfig.getString("measureId");
                    if (StrUtil.isNotEmpty(measureId)) {
                        MetricExpansionMetaUtil.indexLodMeasureRef(
                                measureId, customCfg, foundCodes, context.getFieldIdToCode());
                    }
                }
            }
        }
    }

    /**
     * 扫描 expressionIdMapping 中嵌套 LOD 项的 lodConfig.measureId。
     */
    private void indexNestedLodMappingRef(JSONObject mappingItem, Set<String> foundCodes,
                                        MetricExpansionContext context) {
        if (mappingItem == null) {
            return;
        }
        JSONObject nestedCfg = mappingItem.getJSONObject("customFieldConfigure");
        if (nestedCfg == null || !CustomFieldType.LOD.getCode().equals(nestedCfg.getString("type"))) {
            return;
        }
        JSONObject nestedLodConfig = nestedCfg.getJSONObject("lodConfig");
        if (nestedLodConfig == null) {
            return;
        }
        String nestedMeasureId = nestedLodConfig.getString("measureId");
        if (StrUtil.isNotEmpty(nestedMeasureId)) {
            MetricExpansionMetaUtil.indexLodMeasureRef(
                    nestedMeasureId, nestedCfg, foundCodes, context.getFieldIdToCode());
        }
    }

    /**
     * 扫描计算字段 formula 中的 [fieldId] 引用，纳入 source 扫描（引用不在 measures 区时也生效）。
     */
    private void indexCalcExpressionRefs(String expression, Set<String> foundCodes, MetricExpansionContext context) {
        if (StrUtil.isEmpty(expression)) {
            return;
        }
        for (String refId : MetricExpansionMetaUtil.extractExpressionFieldIds(expression)) {
            MetricExpansionMetaUtil.indexMeasureIdRef(refId, foundCodes, context.getFieldIdToCode());
        }
    }

    /**
     * 扫描分析区 THB / 目标 / ZB 中的 measureIdList。
     * THB、目标分析按日期粒度分桶（d/w/m/q/y/business），ZB 为 items 数组。
     */
    private void scanAnalysisMeasureIds(JSONObject analysis, Set<String> foundCodes, MetricExpansionContext context) {
        if (analysis == null) {
            return;
        }
        scanMeasureIdContainers(analysis.getJSONObject("thb"), foundCodes, context);
        scanMeasureIdContainers(analysis.getJSONObject("target"), foundCodes, context);
        JSONObject zb = analysis.getJSONObject("zb");
        if (zb != null) {
            expandScanMeasureIdListArray(zb.getJSONArray("items"), foundCodes, context);
        }
        JSONObject compare = analysis.getJSONObject("compare");
        if (compare != null) {
            expandScanMeasureIdListArray(compare.getJSONArray("items"), foundCodes, context);
        }
        scanTotalItemsMeasureIds(analysis.getJSONObject("total"), foundCodes, context);
    }

    /** 扫描 total.items 中单指标引用（占比/汇总等） */
    private void scanTotalItemsMeasureIds(JSONObject total, Set<String> foundCodes, MetricExpansionContext context) {
        if (total == null) {
            return;
        }
        JSONArray items = total.getJSONArray("items");
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item != null) {
                collectMeasureRefFromItem(item, foundCodes, context);
            }
        }
    }

    private void collectMeasureRefFromItem(JSONObject item, Set<String> foundCodes, MetricExpansionContext context) {
        collectIdsFromList(item.getJSONArray("measureIdList"), foundCodes, context);
        String measureId = item.getString("measureId");
        if (StrUtil.isNotEmpty(measureId) && !"all".equalsIgnoreCase(measureId)) {
            registerFieldIdFromMeasures(measureId, context);
            String code = context.getFieldIdToCode().get(measureId);
            if (StrUtil.isNotEmpty(code)) {
                foundCodes.add(code);
            }
        }
        String measureCode = item.getString("measureCode");
        if (StrUtil.isNotEmpty(measureCode)) {
            foundCodes.add(measureCode);
        }
    }

    /**
     * 扫描组合分析（前端 setting.customCompare）中递归出现的 measureIdList。
     */
    private void scanCustomCompare(JSONObject setting, Set<String> foundCodes, MetricExpansionContext context) {
        if (setting == null) {
            return;
        }
        Object customCompare = setting.get("customCompare");
        collectMeasureIdListsRecursive(customCompare, foundCodes, context);
    }

    private void scanMeasureIdContainers(JSONObject container, Set<String> foundCodes, MetricExpansionContext context) {
        if (container == null) {
            return;
        }
        for (String key : container.keySet()) {
            Object value = container.get(key);
            if (value instanceof JSONArray) {
                expandScanMeasureIdListArray((JSONArray) value, foundCodes, context);
            }
        }
    }

    private void expandScanMeasureIdListArray(JSONArray items, Set<String> foundCodes, MetricExpansionContext context) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Object itemObj = items.get(i);
            if (!(itemObj instanceof JSONObject)) {
                continue;
            }
            JSONObject item = (JSONObject) itemObj;
            collectIdsFromList(item.getJSONArray("measureIdList"), foundCodes, context);
            collectIdsFromNestedConfigs(item.getJSONArray("zbThbConfigs"), foundCodes, context);
            collectIdsFromNestedConfigs(item.getJSONArray("targetThbConfigs"), foundCodes, context);
        }
    }

    private void collectIdsFromNestedConfigs(JSONArray configs, Set<String> foundCodes, MetricExpansionContext context) {
        if (CollUtil.isEmpty(configs)) {
            return;
        }
        for (int i = 0; i < configs.size(); i++) {
            JSONObject cfg = configs.getJSONObject(i);
            if (cfg == null) {
                continue;
            }
            String measureId = cfg.getString("measureId");
            if (StrUtil.isNotEmpty(measureId) && context.getFieldIdToCode().containsKey(measureId)) {
                foundCodes.add(context.getFieldIdToCode().get(measureId));
            }
        }
    }

    /**
     * 将 measureIdList 中的 id 通过 fieldIdToCode 反查为 code 并收集。
     * 特殊值 "all" 表示全部指标，扫描阶段跳过。
     */
    private void collectIdsFromList(JSONArray idList, Set<String> foundCodes, MetricExpansionContext context) {
        if (CollUtil.isEmpty(idList)) {
            return;
        }
        for (int i = 0; i < idList.size(); i++) {
            String id = idList.getString(i);
            if (StrUtil.isEmpty(id) || "all".equalsIgnoreCase(id)) {
                continue;
            }
            registerFieldIdFromMeasures(resolveCompositeMeasureBaseRef(id), context);
            registerFieldIdFromMeasures(
                    MetricExpansionMetaUtil.stripAggExpressionSuffix(resolveCompositeMeasureBaseRef(id)), context);
            String lookupRef = resolveCompositeMeasureBaseRef(id);
            lookupRef = MetricExpansionMetaUtil.stripAggExpressionSuffix(lookupRef);
            String code = context.getFieldIdToCode().get(lookupRef);
            if (StrUtil.isNotEmpty(code)) {
                foundCodes.add(code);
                String baseCode = MetricExpansionMetaUtil.stripAggExpressionSuffix(code);
                if (StrUtil.isNotEmpty(baseCode) && !baseCode.equals(code)) {
                    foundCodes.add(baseCode);
                }
            }
        }
    }

    /**
     * 分析区只存 id 时，从 measures 反查 code 补全 fieldIdToCode，避免扫描/膨胀漏掉引用。
     */
    private void registerFieldIdFromMeasures(String fieldId, MetricExpansionContext context) {
        if (StrUtil.isEmpty(fieldId) || context.getFieldIdToCode().containsKey(fieldId)) {
            return;
        }
        JSONObject root = context.getTplConfigJson();
        if (root == null) {
            return;
        }
        JSONObject result = root.getJSONObject("result");
        if (result == null) {
            return;
        }
        JSONArray measures = result.getJSONArray("measures");
        if (CollUtil.isEmpty(measures)) {
            return;
        }
        for (int i = 0; i < measures.size(); i++) {
            JSONObject field = measures.getJSONObject(i);
            if (field == null || !fieldId.equals(field.getString("id"))) {
                continue;
            }
            String code = field.getString("code");
            if (StrUtil.isNotEmpty(code)) {
                context.getFieldIdToCode().put(fieldId, code);
            }
            return;
        }
    }

    private void collectMeasureIdListsRecursive(Object node, Set<String> foundCodes, MetricExpansionContext context) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.containsKey("measureIdList")) {
                collectIdsFromList(obj.getJSONArray("measureIdList"), foundCodes, context);
            }
            for (String key : obj.keySet()) {
                collectMeasureIdListsRecursive(obj.get(key), foundCodes, context);
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.size(); i++) {
                collectMeasureIdListsRecursive(arr.get(i), foundCodes, context);
            }
        }
    }

    /**
     * measures：原 source 位置替换为 N 个 target 的完整 field 对象，依次插入，保持映射表顺序。
     * 非 source 字段仅重写 showOrder，保证顺序连续。
     */
    private void expandMeasures(MetricExpansionContext context, JSONObject result) {
        JSONArray measures = result.getJSONArray("measures");
        if (CollUtil.isEmpty(measures)) {
            return;
        }
        JSONArray expandedMeasures = new JSONArray();
        double showOrder = 1D;
        int measureIndex = 0;
        for (int i = 0; i < measures.size(); i++) {
            JSONObject field = measures.getJSONObject(i);
            if (MetricExpansionMetaUtil.isPureLodMeasureField(field)) {
                field.put("showOrder", showOrder++);
                expandedMeasures.add(field);
                measureIndex++;
                continue;
            }
            String code = field.getString("code");
            String aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(code);
            String expandableCode = aggSuffix != null
                    ? MetricExpansionMetaUtil.stripAggExpressionSuffix(code) : code;
            if (StrUtil.isEmpty(expandableCode)) {
                field.put("showOrder", showOrder++);
                expandedMeasures.add(field);
                measureIndex++;
                continue;
            }
            if (context.isExpandableSourceCode(expandableCode)) {
                context.getMeasureExpandedSourceCodes().add(expandableCode);
                List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(expandableCode);
                if (CollUtil.isEmpty(targets)) {
                    throw new IllegalStateException("缺少膨胀目标: " + expandableCode);
                }
                String oldFieldId = field.getString("id");
                String oldFieldCode = field.getString("code");
                String oldFieldTitle = MetricExpansionMetaUtil.resolveFieldTitle(field);
                if (context.isPreserveSourceData()) {
                    field.put("showOrder", showOrder++);
                    expandedMeasures.add(field);
                    measureIndex++;
                }
                for (MetricExpansionTarget target : targets) {
                    appendExpandedTargetMeasure(context, expandedMeasures, field, target, expandableCode,
                            aggSuffix, oldFieldId, oldFieldCode, oldFieldTitle, showOrder++);
                }
                measureIndex += targets.size();
            } else {
                field.put("showOrder", showOrder++);
                expandedMeasures.add(field);
                measureIndex++;
            }
        }
        expandAnalysisMeasureFields(context, expandedMeasures);
        expandLodMeasureReferences(context, expandedMeasures);
        result.put("measures", expandedMeasures);
    }

    /**
     * 依赖 source 指标的分析项（占比/同环比等 isAnalysis=1）随 base 指标 1→N 同步展开。
     * 同步写入 fieldMappings，供 thb/zb measureIdList 中分析项 id 替换。
     */
    private void expandAnalysisMeasureFields(MetricExpansionContext context, JSONArray measures) {
        if (CollUtil.isEmpty(measures) || CollUtil.isEmpty(context.getActiveSourceCodes())) {
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
            String sourceCode = resolveAnalysisDependencySourceCode(context, analysisConfig);
            if (sourceCode == null) {
                rebuilt.add(field);
                continue;
            }
            List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(sourceCode);
            if (CollUtil.isEmpty(targets)) {
                rebuilt.add(field);
                continue;
            }
            String oldAnalysisId = field.getString("id");
            String oldAnalysisCode = field.getString("code");
            String oldAnalysisTitle = MetricExpansionMetaUtil.resolveFieldTitle(field);
            if (context.isPreserveSourceData()) {
                rebuilt.add(field);
            }
            for (MetricExpansionTarget target : targets) {
                appendExpandedAnalysisMeasure(context, rebuilt, field, analysisConfig, target, sourceCode,
                        oldAnalysisId, oldAnalysisCode, oldAnalysisTitle);
            }
        }
        measures.clear();
        measures.addAll(rebuilt);
    }

    private boolean isAnalysisMeasureField(JSONObject field) {
        return field != null && Enabled.isTrue(field.getInteger("isAnalysis"));
    }

    /** 解析分析项依赖的 source 指标 code */
    private String resolveAnalysisDependencySourceCode(MetricExpansionContext context, JSONObject analysisConfig) {
        String measureCode = analysisConfig.getString("measureCode");
        if (context.isExpandableSourceCode(measureCode)) {
            return measureCode;
        }
        String measureId = analysisConfig.getString("measureId");
        return context.resolveExpandableSourceCodeById(measureId);
    }

    /**
     * 膨胀开始前索引 measures / filter 中已存在的指标 id 与 code，供后续去重。
     */
    private void indexExistingMeasureFields(MetricExpansionContext context) {
        JSONObject root = context.getTplConfigJson();
        JSONObject result = root.getJSONObject("result");
        if (result != null) {
            indexExistingFieldsInArray(context, result.getJSONArray("measures"));
        }
        indexExistingFieldsInArray(context, root.getJSONArray("filter"));
    }

    private void indexExistingFieldsInArray(MetricExpansionContext context, JSONArray fields) {
        if (CollUtil.isEmpty(fields)) {
            return;
        }
        for (int i = 0; i < fields.size(); i++) {
            context.indexExistingMeasureField(fields.getJSONObject(i));
        }
    }

    /**
     * 将 source 膨胀为 target 指标写入容器；配置中已存在同 id/code 时跳过写入，仍记录 fieldMapping。
     */
    private void appendExpandedTargetMeasure(MetricExpansionContext context, JSONArray container,
                                             JSONObject sourceField, MetricExpansionTarget target,
                                             String expandableCode, String aggSuffix,
                                             String oldFieldId, String oldFieldCode, String oldFieldTitle,
                                             double showOrder) {
        MetaField metaField = target.getMetaField();
        String targetId = aggSuffix != null ? metaField.getId() + aggSuffix : metaField.getId();
        String targetCode = aggSuffix != null ? metaField.getCode() + aggSuffix : metaField.getCode();
        if (context.isExpandedMeasureAlreadyInConfig(targetId, targetCode)) {
            recordFieldMappingForExistingTarget(context, oldFieldId, oldFieldCode, oldFieldTitle,
                    targetId, targetCode, expandableCode, metaField.getCode(), target.getTargetBusinessline());
            return;
        }
        JSONObject targetField = aggSuffix != null
                ? MetricExpansionMetaUtil.buildTargetAvgByDayMeasureField(
                sourceField, metaField, aggSuffix, expandableCode, context.getSourceBusinessline(),
                target.getTargetBusinessline(), showOrder)
                : MetricExpansionMetaUtil.buildTargetMeasureField(
                sourceField, metaField, context.getSourceBusinessline(),
                target.getTargetBusinessline(), showOrder);
        container.add(targetField);
        context.registerExpandedMeasureField(targetField);
        context.getFieldIdToCode().put(targetField.getString("id"), targetField.getString("code"));
        context.addFieldMapping(
                oldFieldId,
                oldFieldCode,
                oldFieldTitle,
                targetField.getString("id"),
                targetField.getString("code"),
                MetricExpansionMetaUtil.resolveFieldTitle(targetField),
                expandableCode,
                metaField.getCode(),
                target.getTargetBusinessline());
    }

    /**
     * 将 source 分析项膨胀为 target 分析项；配置中已存在同 id/code 时跳过写入，仍记录 fieldMapping。
     */
    private void appendExpandedAnalysisMeasure(MetricExpansionContext context, JSONArray container,
                                               JSONObject sourceField, JSONObject analysisConfig,
                                               MetricExpansionTarget target, String sourceCode,
                                               String oldAnalysisId, String oldAnalysisCode,
                                               String oldAnalysisTitle) {
        JSONObject expandedAnalysis = buildExpandedAnalysisMeasureField(
                context, sourceField, analysisConfig, target, sourceCode);
        String newId = expandedAnalysis.getString("id");
        String newCode = expandedAnalysis.getString("code");
        if (context.isExpandedMeasureAlreadyInConfig(newId, newCode)) {
            recordFieldMappingForExistingTarget(context, oldAnalysisId, oldAnalysisCode, oldAnalysisTitle,
                    newId, newCode, sourceCode, target.getMetaField().getCode(), target.getTargetBusinessline());
            return;
        }
        container.add(expandedAnalysis);
        context.registerExpandedMeasureField(expandedAnalysis);
        context.addFieldMapping(
                oldAnalysisId,
                oldAnalysisCode,
                oldAnalysisTitle,
                expandedAnalysis.getString("id"),
                expandedAnalysis.getString("code"),
                MetricExpansionMetaUtil.resolveFieldTitle(expandedAnalysis),
                sourceCode,
                target.getMetaField().getCode(),
                target.getTargetBusinessline());
        context.getFieldIdToCode().put(
                expandedAnalysis.getString("id"), expandedAnalysis.getString("code"));
    }

    /**
     * target 指标已在配置中存在时，将 old→existing 写入 fieldMapping，供 analysis / sort 等引用同步。
     */
    private void recordFieldMappingForExistingTarget(MetricExpansionContext context,
                                                     String oldFieldId, String oldFieldCode, String oldFieldTitle,
                                                     String targetId, String targetCode,
                                                     String sourceMetricCode, String targetMetricCode,
                                                     String targetBusinessline) {
        JSONObject existing = context.resolveExistingMeasureField(targetId, targetCode);
        if (existing == null) {
            return;
        }
        context.addFieldMapping(
                oldFieldId,
                oldFieldCode,
                oldFieldTitle,
                existing.getString("id"),
                existing.getString("code"),
                MetricExpansionMetaUtil.resolveFieldTitle(existing),
                sourceMetricCode,
                targetMetricCode,
                targetBusinessline);
        String existingId = existing.getString("id");
        String existingCode = existing.getString("code");
        if (StrUtil.isNotEmpty(existingId) && StrUtil.isNotEmpty(existingCode)) {
            context.getFieldIdToCode().put(existingId, existingCode);
        }
    }

    /**
     * 判断膨胀 target 指标（含可选日均后缀）是否已在 measures / filter 中存在。
     */
    private boolean isTargetMetricAlreadyInConfig(MetricExpansionContext context, MetaField metaField,
                                                  String aggSuffix) {
        if (metaField == null) {
            return false;
        }
        String targetId = aggSuffix != null ? metaField.getId() + aggSuffix : metaField.getId();
        String targetCode = aggSuffix != null ? metaField.getCode() + aggSuffix : metaField.getCode();
        return context.isExpandedMeasureAlreadyInConfig(targetId, targetCode);
    }

    /**
     * 解析 sort / total 等引用上的日均后缀，优先 columnField / fieldCode，其次 orderBy / id。
     */
    private String resolveAggSuffixFromRefs(String... refs) {
        if (refs == null) {
            return null;
        }
        for (String ref : refs) {
            String aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(ref);
            if (aggSuffix != null) {
                return aggSuffix;
            }
        }
        return null;
    }

    private JSONObject buildExpandedAnalysisMeasureField(MetricExpansionContext context, JSONObject sourceField,
                                                         JSONObject analysisConfig, MetricExpansionTarget target,
                                                         String sourceCode) {
        JSONObject copy = JSONObject.parseObject(sourceField.toJSONString());
        MetaField targetMeta = target.getMetaField();
        JSONObject newAnalysisConfig = copy.getJSONObject("analysisConfig");
        newAnalysisConfig.put("measureId", targetMeta.getId());
        newAnalysisConfig.put("measureCode", targetMeta.getCode());

        String newCode = buildAnalysisFieldCode(targetMeta.getCode(), newAnalysisConfig);
        copy.put("id", newCode);
        copy.put("code", newCode);
        copy.put("name", newCode);
        MetricExpansionMetaUtil.applyExpandedOriginRef(copy, targetMeta);
        rewriteAnalysisFieldTitle(copy, sourceCode, targetMeta,
                context.getSourceBusinessline(), target.getTargetBusinessline());
        MetricExpansionMetaUtil.normalizeEmptyDisplayTitle(copy);
        return copy;
    }

    /** 对齐 AnalysisMeasureInitializer.buildAnalysisCode 的常见分支 */
    private String buildAnalysisFieldCode(String measureCode, JSONObject analysisConfig) {
        return MetricExpansionMetaUtil.buildAnalysisFieldCode(measureCode, analysisConfig);
    }

    private void rewriteAnalysisFieldTitle(JSONObject field, String sourceCode, MetaField targetMeta,
                                           String sourceBusinessline, String targetBusinessline) {
        rewriteAnalysisTextProperty(field, "title", sourceCode, targetMeta, sourceBusinessline, targetBusinessline);
        rewriteAnalysisTextProperty(field, "displayTitle", sourceCode, targetMeta, sourceBusinessline, targetBusinessline);
        rewriteAnalysisTextProperty(field, "name", sourceCode, targetMeta, sourceBusinessline, targetBusinessline);
    }

    private void rewriteAnalysisTextProperty(JSONObject field, String key, String sourceCode, MetaField targetMeta,
                                             String sourceBusinessline, String targetBusinessline) {
        if (!field.containsKey(key)) {
            return;
        }
        String value = field.getString(key);
        if (StrUtil.isEmpty(value)) {
            return;
        }
        if (StrUtil.isNotEmpty(sourceCode) && value.contains(sourceCode)) {
            field.put(key, MetricExpansionMetaUtil.replaceLiteral(value, sourceCode, targetMeta.getCode()));
            return;
        }
        field.put(key, MetricExpansionMetaUtil.buildExpandedDisplayName(
                value, sourceBusinessline, targetBusinessline));
    }

    /**
     * filter：若过滤字段本身是待膨胀 source，则展开为 N 条独立条件。
     * 每条继承原操作符、values 等配置，仅替换 id/code/title。
     */
    private void expandFilterFields(MetricExpansionContext context, JSONArray filter) {
        if (CollUtil.isEmpty(filter)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        double showOrder = 1D;
        for (int i = 0; i < filter.size(); i++) {
            JSONObject field = filter.getJSONObject(i);
            if (MetricExpansionMetaUtil.isPureLodMeasureField(field)) {
                field.put("showOrder", showOrder++);
                expanded.add(field);
                continue;
            }
            String code = field.getString("code");
            String aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(code);
            String expandableCode = aggSuffix != null
                    ? MetricExpansionMetaUtil.stripAggExpressionSuffix(code) : code;
            if (StrUtil.isEmpty(expandableCode)) {
                field.put("showOrder", showOrder++);
                expanded.add(field);
                continue;
            }
            if (context.isExpandableSourceCode(expandableCode)) {
                List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(expandableCode);
                if (context.isPreserveSourceData()) {
                    field.put("showOrder", showOrder++);
                    expanded.add(field);
                }
                for (MetricExpansionTarget target : targets) {
                    appendExpandedTargetMeasure(context, expanded, field, target, expandableCode, aggSuffix,
                            field.getString("id"), field.getString("code"),
                            MetricExpansionMetaUtil.resolveFieldTitle(field), showOrder++);
                }
            } else {
                field.put("showOrder", showOrder++);
                expanded.add(field);
            }
        }
        filter.clear();
        filter.addAll(expanded);
        expandLodMeasureReferences(context, filter);
    }

    /**
     * 扫描阶段索引 LOD 依赖：通过 lodConfig.measureId 反查底层指标 code，避免使用 _avg_by_d 等误导 code。
     */
    private void indexLodMeasureDependency(JSONObject field, Set<String> foundCodes, MetricExpansionContext context) {
        JSONObject customCfg = field.getJSONObject("customFieldConfigure");
        if (customCfg == null) {
            return;
        }
        JSONObject lodConfig = customCfg.getJSONObject("lodConfig");
        if (lodConfig == null) {
            return;
        }
        String measureId = lodConfig.getString("measureId");
        if (StrUtil.isEmpty(measureId)) {
            return;
        }
        MetricExpansionMetaUtil.indexLodMeasureRef(measureId, customCfg, foundCodes, context.getFieldIdToCode());
        String underlyingCode = context.getFieldIdToCode().get(measureId);
        String lodFieldId = field.getString("id");
        if (StrUtil.isNotEmpty(lodFieldId) && StrUtil.isNotEmpty(underlyingCode)) {
            context.getFieldIdToCode().put(lodFieldId, underlyingCode);
        }
    }

    /**
     * 更新纯 LOD 指标对底层 source 指标的引用：按 lodConfig.measureId（id）匹配，不按 code。
     */
    private void expandLodMeasureReferences(MetricExpansionContext context, JSONArray fields) {
        if (CollUtil.isEmpty(fields) || CollUtil.isEmpty(context.getActiveSourceCodes())) {
            return;
        }
        for (int i = 0; i < fields.size(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (!MetricExpansionMetaUtil.isPureLodMeasureField(field)) {
                continue;
            }
            updateLodFieldTargetRef(context, field);
        }
    }

    /**
     * 将 LOD 字段 lodConfig.measureId 从 source 替换为 target，同步 id/originId/expression。
     * measureId 对应底层指标不在本视图替换清单（activeSourceCodes）内时，整字段保持不变。
     */
    private void updateLodFieldTargetRef(MetricExpansionContext context, JSONObject field) {
        if (context.isPreserveSourceData()) {
            return;
        }
        JSONObject customCfg = field.getJSONObject("customFieldConfigure");
        if (customCfg == null) {
            return;
        }
        JSONObject lodConfig = customCfg.getJSONObject("lodConfig");
        if (lodConfig == null) {
            return;
        }
        String oldMeasureId = lodConfig.getString("measureId");
        if (StrUtil.isEmpty(oldMeasureId)) {
            return;
        }

        String sourceCode = context.resolveExpandableSourceCodeByMeasureId(oldMeasureId, customCfg);
        if (sourceCode == null) {
            return;
        }

        String originalTitle = MetricExpansionMetaUtil.resolveFieldTitle(field);
        String originalRawTitle = MetricExpansionMetaUtil.resolveRawFieldTitle(field);
        List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(sourceCode);
        if (CollUtil.isEmpty(targets)) {
            return;
        }
        MetricExpansionTarget target = targets.get(0);
        MetaField targetMeta = target.getMetaField();
        if (targetMeta == null) {
            return;
        }

        String oldFieldId = field.getString("id");
        String oldFieldCode = field.getString("code");
        String oldFieldTitle = originalTitle;
        String aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(oldMeasureId);
        if (aggSuffix == null) {
            aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(field.getString("id"));
        }
        if (aggSuffix == null) {
            aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(field.getString("code"));
        }
        String newMeasureId = aggSuffix != null ? targetMeta.getId() + aggSuffix : targetMeta.getId();
        String newMeasureCode = aggSuffix != null ? targetMeta.getCode() + aggSuffix : targetMeta.getCode();
        String lodPrefix = CustomFieldType.LOD.getIdentifier();

        lodConfig.put("measureId", newMeasureId);
        lodConfig.put("measureCode", newMeasureCode);
        field.put("id", lodPrefix + newMeasureId);
        field.put("originId", lodPrefix + targetMeta.getId());
        field.put("originCode", targetMeta.getCode());
        field.put("code", newMeasureCode);
        customCfg.put("expression", "[" + newMeasureId + "]");
        syncLodExpressionIdMapping(customCfg, newMeasureId, newMeasureCode, targetMeta);
        MetricExpansionMetaUtil.applyLodExpectedFieldTitle(
                field, originalRawTitle, resolveLodMappingMeasureTitle(customCfg), targetMeta,
                context.getSourceBusinessline(), target.getTargetBusinessline());

        context.getFieldIdToCode().put(field.getString("id"), newMeasureCode);
        context.getFieldIdToCode().put(newMeasureId, newMeasureCode);
        context.addFieldMapping(
                oldFieldId,
                oldFieldCode,
                oldFieldTitle,
                field.getString("id"),
                field.getString("code"),
                MetricExpansionMetaUtil.resolveFieldTitle(field),
                sourceCode,
                targetMeta.getCode(),
                target.getTargetBusinessline());
    }

    /** 读取 expressionIdMapping 中指定指标的 title（替换前） */
    private String resolveLodMappingMeasureTitle(JSONObject customCfg) {
        if (customCfg == null) {
            return null;
        }
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        if (CollUtil.isEmpty(mapping)) {
            return null;
        }
        JSONObject item = mapping.getJSONObject(0);
        return item != null ? item.getString("title") : null;
    }

    /** 同步 LOD expressionIdMapping 中底层指标 id/code */
    private void syncLodExpressionIdMapping(JSONObject customCfg, String newMeasureId, String newMeasureCode,
                                            MetaField targetMeta) {
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        if (CollUtil.isEmpty(mapping)) {
            return;
        }
        for (int i = 0; i < mapping.size(); i++) {
            JSONObject item = mapping.getJSONObject(i);
            if (item == null) {
                continue;
            }
            item.put("id", newMeasureId);
            item.put("code", newMeasureCode);
            item.put("title", targetMeta.getTitle());
            MetricExpansionMetaUtil.applyExpressionIdMappingKpiNo(item, targetMeta);
            if (StrUtil.isNotEmpty(targetMeta.getModuleCtgId())) {
                item.put("moduleCtgId", targetMeta.getModuleCtgId());
            }
            if (StrUtil.isNotEmpty(targetMeta.getAggExpression())) {
                item.put("aggExpression", targetMeta.getAggExpression());
            }
            if (StrUtil.isNotEmpty(targetMeta.getName())) {
                item.put("name", targetMeta.getName());
            }
        }
    }

    /**
     * 排序配置膨胀：覆盖 tableStyle.upDownSortData 与 leftRightSortData。
     * 兼容旧版 sortFields.fieldCode 与现行 sortItems.orderBy / columnField。
     */
    private void expandSortFields(MetricExpansionContext context, JSONObject setting) {
        if (setting == null) {
            return;
        }
        JSONObject tableStyle = setting.getJSONObject("tableStyle");
        if (tableStyle == null) {
            return;
        }
        expandSortDataBlock(context, tableStyle.getJSONObject("upDownSortData"));
        expandSortDataBlock(context, tableStyle.getJSONObject("leftRightSortData"));
    }

    private void expandSortDataBlock(MetricExpansionContext context, JSONObject sortData) {
        if (sortData == null) {
            return;
        }
        expandSortFieldArray(context, sortData.getJSONArray("sortFields"), "fieldCode");
        expandSortItemArray(context, sortData.getJSONArray("sortItems"));
    }

    private void expandSortFieldArray(MetricExpansionContext context, JSONArray sortFields, String codeKey) {
        if (CollUtil.isEmpty(sortFields)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        for (int i = 0; i < sortFields.size(); i++) {
            JSONObject item = sortFields.getJSONObject(i);
            String code = item.getString(codeKey);
            String aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(code);
            String expandableCode = aggSuffix != null
                    ? MetricExpansionMetaUtil.stripAggExpressionSuffix(code) : code;
            if (context.isExpandableSourceCode(expandableCode)) {
                List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(expandableCode);
                if (context.isPreserveSourceData()) {
                    expanded.add(item);
                }
                for (MetricExpansionTarget target : targets) {
                    MetaField metaField = target.getMetaField();
                    if (isTargetMetricAlreadyInConfig(context, metaField, aggSuffix)) {
                        continue;
                    }
                    JSONObject copy = JSONObject.parseObject(item.toJSONString());
                    copy.put(codeKey, aggSuffix != null ? metaField.getCode() + aggSuffix : metaField.getCode());
                    expanded.add(copy);
                }
            } else {
                expanded.add(item);
            }
        }
        sortFields.clear();
        sortFields.addAll(expanded);
    }

    private void expandSortItemArray(MetricExpansionContext context, JSONArray sortItems) {
        if (CollUtil.isEmpty(sortItems)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        for (int i = 0; i < sortItems.size(); i++) {
            JSONObject item = sortItems.getJSONObject(i);
            expandSingleSortItem(context, item, expanded);
        }
        sortItems.clear();
        sortItems.addAll(expanded);
    }

    /**
     * 单条 sortItem 按 source 1→N 展开；columnField 中同环比等后缀保留，仅替换其中的 source code。
     */
    private void expandSingleSortItem(MetricExpansionContext context, JSONObject item, JSONArray expanded) {
        String sourceCode = resolveSortItemSourceCode(context, item);
        if (sourceCode == null) {
            expanded.add(item);
            return;
        }
        List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(sourceCode);
        if (CollUtil.isEmpty(targets)) {
            expanded.add(item);
            return;
        }
        if (context.isPreserveSourceData()) {
            expanded.add(item);
        }
        String aggSuffix = resolveAggSuffixFromRefs(item.getString("columnField"), item.getString("orderBy"));
        for (MetricExpansionTarget target : targets) {
            MetaField metaField = target.getMetaField();
            if (isTargetMetricAlreadyInConfig(context, metaField, aggSuffix)) {
                continue;
            }
            JSONObject copy = JSONObject.parseObject(item.toJSONString());
            String targetCode = aggSuffix != null ? metaField.getCode() + aggSuffix : metaField.getCode();
            replaceSortItemMetricRef(copy, sourceCode, targetCode);
            expanded.add(copy);
        }
    }

    /**
     * 解析 sortItem 关联的 source code：精确匹配 orderBy/columnField，或在其字符串中嵌入的 source code。
     */
    private String resolveSortItemSourceCode(MetricExpansionContext context, JSONObject item) {
        String orderBy = item.getString("orderBy");
        String columnField = item.getString("columnField");
        String matchKey = firstNonEmpty(orderBy, columnField);
        if (context.isExpandableSourceCode(matchKey)) {
            return matchKey;
        }
        String sourceById = context.resolveExpandableSourceCodeById(matchKey);
        if (sourceById != null) {
            return sourceById;
        }
        String embeddedInColumn = findEmbeddedSourceCode(columnField, context);
        if (embeddedInColumn != null) {
            return embeddedInColumn;
        }
        return findEmbeddedSourceCode(orderBy, context);
    }

    /** 在文本中查找嵌入的 source code，长 code 优先避免短 code 误匹配 */
    private String findEmbeddedSourceCode(String text, MetricExpansionContext context) {
        if (StrUtil.isEmpty(text) || CollUtil.isEmpty(context.getActiveSourceCodes())) {
            return null;
        }
        List<String> sourceCodes = new ArrayList<>(context.getActiveSourceCodes());
        sourceCodes.sort((left, right) -> Integer.compare(right.length(), left.length()));
        for (String sourceCode : sourceCodes) {
            if (text.contains(sourceCode)) {
                return sourceCode;
            }
        }
        return null;
    }

    /** 替换 sortItem 各属性中的指标 code/id 引用，保留同环比等分析后缀 */
    private void replaceSortItemMetricRef(JSONObject item, String oldRef, String newRef) {
        replaceSortItemProperty(item, "orderBy", oldRef, newRef);
        replaceSortItemProperty(item, "columnField", oldRef, newRef);
        replaceSortItemProperty(item, "orderEntity", oldRef, newRef);
    }

    private void replaceSortItemProperty(JSONObject item, String key, String oldRef, String newRef) {
        if (!item.containsKey(key)) {
            return;
        }
        String value = item.getString(key);
        if (StrUtil.isEmpty(value)) {
            return;
        }
        item.put(key, MetricExpansionMetaUtil.replaceLiteral(value, oldRef, newRef));
    }

    /**
     * 分析区膨胀：THB、目标分析、ZB 的 measureIdList，以及 total.aggConfig.configs。
     * zbThbConfigs / targetThbConfigs 中的 measureId 同步按 1→N 展开。
     */
    private void expandAnalysisSections(MetricExpansionContext context, JSONObject analysis) {
        if (analysis == null) {
            return;
        }
        expandMeasureIdContainers(context, analysis.getJSONObject("thb"));
        expandMeasureIdContainers(context, analysis.getJSONObject("target"));
        JSONObject zb = analysis.getJSONObject("zb");
        if (zb != null) {
            expandMeasureIdListInItems(context, zb.getJSONArray("items"));
        }
        JSONObject compare = analysis.getJSONObject("compare");
        if (compare != null) {
            expandMeasureIdListInItems(context, compare.getJSONArray("items"));
        }
        expandTotalItemsSection(context, analysis.getJSONObject("total"));
        expandTotalSection(context, analysis.getJSONObject("total"));
    }

    /**
     * 组合分析膨胀：递归处理 setting.customCompare 内所有 measureIdList。
     */
    private void expandCustomCompare(MetricExpansionContext context, JSONObject setting) {
        if (setting == null) {
            return;
        }
        Object customCompare = setting.get("customCompare");
        if (customCompare != null) {
            expandMeasureIdListsRecursive(context, customCompare);
        }
    }

    private void expandMeasureIdContainers(MetricExpansionContext context, JSONObject container) {
        if (container == null) {
            return;
        }
        for (String key : container.keySet()) {
            Object value = container.get(key);
            if (value instanceof JSONArray) {
                expandMeasureIdListInItems(context, (JSONArray) value);
            }
        }
    }

    private void expandMeasureIdListInItems(MetricExpansionContext context, JSONArray items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Object itemObj = items.get(i);
            if (!(itemObj instanceof JSONObject)) {
                continue;
            }
            JSONObject item = (JSONObject) itemObj;
            syncMeasureIdListOnItem(context, item,
                    expandIdList(context, resolveMeasureRefSourceList(item)));
            expandNestedMeasureIdConfigs(context, item.getJSONArray("zbThbConfigs"));
            expandNestedMeasureIdConfigs(context, item.getJSONArray("targetThbConfigs"));
        }
    }

    /**
     * 解析分析项上的指标引用：优先 measureIdList，缺失时回退 measureId。
     */
    private JSONArray resolveMeasureRefSourceList(JSONObject item) {
        if (item == null) {
            return null;
        }
        JSONArray sourceList = item.getJSONArray("measureIdList");
        if (CollUtil.isNotEmpty(sourceList)) {
            return sourceList;
        }
        String measureId = item.getString("measureId");
        if (StrUtil.isEmpty(measureId)) {
            return null;
        }
        JSONArray fallback = new JSONArray();
        fallback.add(measureId);
        return fallback;
    }

    /**
     * 写回 measureIdList，并同步 measureId / measureCode 供前端单选展示；禁止静默清空。
     */
    private void syncMeasureIdListOnItem(MetricExpansionContext context, JSONObject item, JSONArray expandedList) {
        if (item == null || CollUtil.isEmpty(expandedList)) {
            return;
        }
        JSONArray normalizedList = dedupeJsonArrayPreserveOrder(expandedList);
        item.put("measureIdList", normalizedList);
        item.put("measureId", normalizedList.getString(0));
        String measureCode = context.getFieldIdToCode().get(normalizedList.getString(0));
        if (StrUtil.isNotEmpty(measureCode)) {
            item.put("measureCode", measureCode);
        }
    }

    private void expandNestedMeasureIdConfigs(MetricExpansionContext context, JSONArray configs) {
        if (CollUtil.isEmpty(configs)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        for (int i = 0; i < configs.size(); i++) {
            JSONObject cfg = configs.getJSONObject(i);
            String measureId = cfg.getString("measureId");
            List<String> resolvedIds = resolveExpandedMeasureFieldIds(context, measureId);
            if (CollUtil.isNotEmpty(resolvedIds)) {
                if (context.isPreserveSourceData()) {
                    expanded.add(cfg);
                }
                for (String newMeasureId : resolvedIds) {
                    JSONObject copy = JSONObject.parseObject(cfg.toJSONString());
                    copy.put("measureId", newMeasureId);
                    expanded.add(copy);
                }
            } else {
                expanded.add(cfg);
            }
        }
        configs.clear();
        configs.addAll(expanded);
    }

    /**
     * 总计/小计自定义汇总配置：aggConfig.configs 中按指标列出的项，对 source 展开为 N 条。
     * 全局汇总模式（无逐指标 configs）不受影响。
     */
    private void expandTotalSection(MetricExpansionContext context, JSONObject total) {
        if (total == null) {
            return;
        }
        JSONObject aggConfig = total.getJSONObject("aggConfig");
        if (aggConfig == null) {
            return;
        }
        JSONArray configs = aggConfig.getJSONArray("configs");
        if (CollUtil.isEmpty(configs)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        for (int i = 0; i < configs.size(); i++) {
            JSONObject cfg = configs.getJSONObject(i);
            String code = cfg.getString("code");
            String id = cfg.getString("id");
            String sourceCode = null;
            if (context.isExpandableSourceCode(code)) {
                sourceCode = code;
            } else {
                sourceCode = context.resolveExpandableSourceCodeById(id);
            }
            if (sourceCode != null) {
                List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(sourceCode);
                String aggSuffix = resolveAggSuffixFromRefs(code, id);
                if (context.isPreserveSourceData()) {
                    expanded.add(cfg);
                }
                for (MetricExpansionTarget target : targets) {
                    MetaField meta = target.getMetaField();
                    if (isTargetMetricAlreadyInConfig(context, meta, aggSuffix)) {
                        continue;
                    }
                    JSONObject copy = JSONObject.parseObject(cfg.toJSONString());
                    copy.put("id", aggSuffix != null ? meta.getId() + aggSuffix : meta.getId());
                    copy.put("code", aggSuffix != null ? meta.getCode() + aggSuffix : meta.getCode());
                    expanded.add(copy);
                }
            } else {
                expanded.add(cfg);
            }
        }
        aggConfig.put("configs", expanded);
    }

    private void expandMeasureIdListsRecursive(MetricExpansionContext context, Object node) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.containsKey("measureIdList") || StrUtil.isNotEmpty(obj.getString("measureId"))) {
                syncMeasureIdListOnItem(context, obj,
                        expandIdList(context, resolveMeasureRefSourceList(obj)));
            }
            for (String key : obj.keySet()) {
                expandMeasureIdListsRecursive(context, obj.get(key));
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.size(); i++) {
                expandMeasureIdListsRecursive(context, arr.get(i));
            }
        }
    }

    /**
     * 将 measureIdList 中的 source 字段 id 按映射展开为 N 个 target id。
     *
     * 空 id 与字面量 "all" 原样保留；"all" 表示全部指标，由前端查询时动态解析。
     *
     * @param context 上下文
     * @param idList  原始 measureIdList
     * @return 展开后的 id 列表
     */
    private JSONArray expandIdList(MetricExpansionContext context, JSONArray idList) {
        JSONArray expanded = new JSONArray();
        if (CollUtil.isEmpty(idList)) {
            return expanded;
        }
        for (int i = 0; i < idList.size(); i++) {
            String id = idList.getString(i);
            if (StrUtil.isEmpty(id) || "all".equalsIgnoreCase(id)) {
                expanded.add(id);
                continue;
            }
            expandMeasureRef(context, id, expanded);
        }
        return dedupeJsonArrayPreserveOrder(expanded);
    }

    /** JSONArray 去重并保持首次出现顺序 */
    private JSONArray dedupeJsonArrayPreserveOrder(JSONArray ids) {
        if (CollUtil.isEmpty(ids)) {
            return ids;
        }
        JSONArray deduped = new JSONArray();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.getString(i);
            if (StrUtil.isEmpty(id) || "all".equalsIgnoreCase(id) || seen.add(id)) {
                deduped.add(id);
            }
        }
        return deduped;
    }

    /**
     * 展开 measureIdList 单项：支持 baseAndZb 复合引用 {baseId}___zb_ct。
     */
    private void expandMeasureRef(MetricExpansionContext context, String fieldRef, JSONArray expanded) {
        String suffix = resolveCompositeMeasureSuffix(fieldRef);
        if (suffix == null) {
            expandPlainMeasureRef(context, fieldRef, expanded);
            return;
        }
        String baseRef = resolveCompositeMeasureBaseRef(fieldRef);
        JSONArray resolvedBases = new JSONArray();
        expandPlainMeasureRefCore(context, baseRef, resolvedBases);
        if (CollUtil.isEmpty(resolvedBases)) {
            expanded.add(fieldRef);
            return;
        }
        for (int i = 0; i < resolvedBases.size(); i++) {
            String resolvedBase = resolvedBases.getString(i);
            if (StrUtil.isEmpty(resolvedBase) || "all".equalsIgnoreCase(resolvedBase)) {
                expanded.add(fieldRef);
                continue;
            }
            expanded.add(resolvedBase + COMPOSITE_MEASURE_REF_SEPARATOR + suffix);
        }
    }

    private void expandPlainMeasureRef(MetricExpansionContext context, String fieldRef, JSONArray expanded) {
        String aggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(fieldRef);
        if (aggSuffix != null) {
            expandAggSuffixMeasureRef(context, fieldRef, aggSuffix, expanded);
            return;
        }
        expandPlainMeasureRefCore(context, fieldRef, expanded);
    }

    /** 展开带日均/非空日均后缀的 measure 引用：优先整段 id 映射，再按 base 替换后拼回后缀 */
    private void expandAggSuffixMeasureRef(MetricExpansionContext context, String fieldRef, String aggSuffix,
                                           JSONArray expanded) {
        List<String> directResolved = resolveExpandedMeasureFieldIds(context, fieldRef);
        if (CollUtil.isNotEmpty(directResolved)) {
            if (context.isPreserveSourceData()) {
                expanded.add(fieldRef);
            }
            expanded.addAll(directResolved);
            return;
        }
        String baseRef = MetricExpansionMetaUtil.stripAggExpressionSuffix(fieldRef);
        JSONArray resolvedBases = new JSONArray();
        expandPlainMeasureRefCore(context, baseRef, resolvedBases);
        if (CollUtil.isEmpty(resolvedBases)) {
            expanded.add(fieldRef);
            return;
        }
        for (int i = 0; i < resolvedBases.size(); i++) {
            String resolvedBase = resolvedBases.getString(i);
            if (StrUtil.isEmpty(resolvedBase) || "all".equalsIgnoreCase(resolvedBase)) {
                expanded.add(fieldRef);
                continue;
            }
            expanded.add(resolvedBase + aggSuffix);
        }
    }

    private void expandPlainMeasureRefCore(MetricExpansionContext context, String fieldRef, JSONArray expanded) {
        List<String> resolvedIds = resolveExpandedMeasureFieldIds(context, fieldRef);
        if (CollUtil.isNotEmpty(resolvedIds)) {
            if (context.isPreserveSourceData()) {
                expanded.add(fieldRef);
            }
            expanded.addAll(resolvedIds);
            return;
        }
        expanded.add(fieldRef);
    }

    /**
     * 解析分析区 measure 引用对应的 target field id 列表。
     * 优先 fieldMappings 精确匹配，再按 sourceCode + 后缀规则匹配 measures 膨胀结果。
     */
    private List<String> resolveExpandedMeasureFieldIds(MetricExpansionContext context, String fieldRef) {
        List<String> exactMapped = context.resolveNewFieldIdsByOldFieldRef(fieldRef);
        if (CollUtil.isNotEmpty(exactMapped)) {
            return exactMapped;
        }
        List<String> fuzzyMapped = resolveFuzzyMappedFieldIds(context, fieldRef);
        if (CollUtil.isNotEmpty(fuzzyMapped)) {
            return fuzzyMapped;
        }
        String sourceCode = context.isExpandableSourceCode(fieldRef)
                ? fieldRef : context.resolveExpandableSourceCodeById(fieldRef);
        if (sourceCode == null) {
            return new ArrayList<>();
        }
        List<String> suffixMatched = resolveMappedFieldIdsBySourceAndRef(context, sourceCode, fieldRef);
        if (CollUtil.isNotEmpty(suffixMatched)) {
            return suffixMatched;
        }
        return buildTargetFieldIdsFromMeta(context, sourceCode, fieldRef);
    }

    /**
     * 分析区引用 id 与 measures 字段 id 不完全一致时的模糊匹配（如 base meta id vs id_1_avg_by_d）。
     * 引用无 agg 后缀时仅匹配同样无后缀的 mapping，避免非日均占比误展开为日均指标。
     */
    private List<String> resolveFuzzyMappedFieldIds(MetricExpansionContext context, String fieldRef) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isEmpty(fieldRef) || CollUtil.isEmpty(context.getFieldMappings())) {
            return result;
        }
        String refAggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(fieldRef);
        String refBase = MetricExpansionMetaUtil.stripAggExpressionSuffix(fieldRef);
        for (MetricExpansionFieldMappingEntity mapping : context.getFieldMappings()) {
            if (mapping == null || StrUtil.isEmpty(mapping.getOldFieldId())
                    || StrUtil.isEmpty(mapping.getNewFieldId())) {
                continue;
            }
            if (fieldRef.equals(mapping.getOldFieldId()) || fieldRef.equals(mapping.getOldFieldCode())) {
                result.add(mapping.getNewFieldId());
                continue;
            }
            String oldAggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(mapping.getOldFieldId());
            String oldBase = MetricExpansionMetaUtil.stripAggExpressionSuffix(mapping.getOldFieldId());
            if (refAggSuffix != null) {
                if (refAggSuffix.equals(oldAggSuffix) && refBase.equals(oldBase)) {
                    result.add(mapping.getNewFieldId());
                }
                continue;
            }
            if (oldAggSuffix == null && refBase.equals(oldBase)) {
                result.add(mapping.getNewFieldId());
            }
        }
        return dedupePreserveOrder(result);
    }

    /** 按 sourceCode 与引用后缀规则，从 fieldMappings 匹配 measures 膨胀后的 field id */
    private List<String> resolveMappedFieldIdsBySourceAndRef(MetricExpansionContext context, String sourceCode,
                                                             String fieldRef) {
        List<String> result = new ArrayList<>();
        if (CollUtil.isEmpty(context.getFieldMappings())) {
            return result;
        }
        String refAggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(fieldRef);
        String refWithoutAgg = refAggSuffix != null
                ? MetricExpansionMetaUtil.stripAggExpressionSuffix(fieldRef) : fieldRef;
        for (MetricExpansionFieldMappingEntity mapping : context.getFieldMappings()) {
            if (mapping == null || !sourceCode.equals(mapping.getSourceMetricCode())) {
                continue;
            }
            String oldFieldId = mapping.getOldFieldId();
            String newFieldId = mapping.getNewFieldId();
            if (StrUtil.isEmpty(oldFieldId) || StrUtil.isEmpty(newFieldId)) {
                continue;
            }
            if (fieldRef.equals(oldFieldId)) {
                result.add(newFieldId);
                continue;
            }
            String oldAggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(oldFieldId);
            if (refAggSuffix != null) {
                if (refAggSuffix.equals(oldAggSuffix)
                        && refWithoutAgg.equals(MetricExpansionMetaUtil.stripAggExpressionSuffix(oldFieldId))) {
                    result.add(newFieldId);
                }
                continue;
            }
            if (oldAggSuffix == null && (fieldRef.equals(oldFieldId) || refWithoutAgg.equals(oldFieldId))) {
                result.add(newFieldId);
            }
        }
        return dedupePreserveOrder(result);
    }

    /** 无 fieldMappings 命中时，按 meta id 构建 target field id（保留引用中的 agg 后缀） */
    private List<String> buildTargetFieldIdsFromMeta(MetricExpansionContext context, String sourceCode,
                                                     String fieldRef) {
        List<String> result = new ArrayList<>();
        List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(sourceCode);
        if (CollUtil.isEmpty(targets)) {
            log.warn("指标膨胀 analysis：source[{}] 无 target 映射，跳过 measureIdList 替换", sourceCode);
            return result;
        }
        String refAggSuffix = MetricExpansionMetaUtil.resolveAggExpressionSuffix(fieldRef);
        for (MetricExpansionTarget target : targets) {
            MetaField metaField = target.getMetaField();
            if (metaField == null) {
                continue;
            }
            if (refAggSuffix != null) {
                result.add(metaField.getId() + refAggSuffix);
            } else {
                result.add(metaField.getId());
            }
        }
        return result;
    }

    /** 去重并保持首次出现顺序 */
    private List<String> dedupePreserveOrder(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ids;
        }
        List<String> deduped = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String id : ids) {
            if (StrUtil.isNotEmpty(id) && seen.add(id)) {
                deduped.add(id);
            }
        }
        return deduped;
    }

    /** 解析复合引用中的 base field id/code */
    private String resolveCompositeMeasureBaseRef(String fieldRef) {
        String suffix = resolveCompositeMeasureSuffix(fieldRef);
        if (suffix == null) {
            return fieldRef;
        }
        return fieldRef.substring(0, fieldRef.length() - COMPOSITE_MEASURE_REF_SEPARATOR.length() - suffix.length());
    }

    /** 解析复合引用后缀（如 zb_ct）；非复合引用返回 null */
    private String resolveCompositeMeasureSuffix(String fieldRef) {
        if (StrUtil.isEmpty(fieldRef) || !fieldRef.contains(COMPOSITE_MEASURE_REF_SEPARATOR)) {
            return null;
        }
        int index = fieldRef.indexOf(COMPOSITE_MEASURE_REF_SEPARATOR);
        if (index <= 0) {
            return null;
        }
        String suffix = fieldRef.substring(index + COMPOSITE_MEASURE_REF_SEPARATOR.length());
        return StrUtil.isEmpty(suffix) ? null : suffix;
    }

    /**
     * 计算字段 filter 变体同步：一条原 calc filter 展开为 M 条，每条对应一个业务线变体。
     *
     * 匹配规则：优先用 filter.id 查 calcFieldIdToVariantIds；id 未命中时用 filter.code
     * 查 calcFieldCodeToVariantIds（仅原 calc code 非空的历史数据可能命中；常规四则 calc code 为 ""）。
     *
     * 字段合并：以 measures 中变体完整 JSON 为底（含 customFieldConfigure），
     * 再覆盖 filter 专有字段（values、filterSetting 等）；变体不含 displayTitle。
     *
     * @param context 上下文
     * @param filter  tplConfig.filter 数组，就地替换
     */
    private void expandFilterByCalcVariants(MetricExpansionContext context, JSONArray filter) {
        if (CollUtil.isEmpty(filter)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        double showOrder = 1D;
        for (int i = 0; i < filter.size(); i++) {
            JSONObject field = filter.getJSONObject(i);
            String id = field.getString("id");
            String code = field.getString("code");
            List<String> variantIds = resolveCalcVariantIds(context, id, code);
            if (CollUtil.isNotEmpty(variantIds)) {
                for (String variantId : variantIds) {
                    JSONObject variantField = context.getCalcFieldVariantById().get(variantId);
                    if (variantField != null) {
                        JSONObject merged = mergeCalcFilterField(field, variantField, showOrder++);
                        expanded.add(merged);
                    } else {
                        log.warn("计算字段 filter 变体同步：variantId[{}] 未找到完整字段配置，跳过该 filter 变体",
                                variantId);
                    }
                }
            } else {
                field.put("showOrder", showOrder++);
                expanded.add(field);
            }
        }
        filter.clear();
        filter.addAll(expanded);
    }

    /**
     * 解析原计算字段对应的变体 id 列表。
     *
     * 主路径按 id 匹配 calcFieldIdToVariantIds。code 兜底仅在原 calc code 非空时生效
     * （前端四则/LOD 计算字段 code 恒为 ""，故对常规 calc 该分支不会触发）。
     *
     * @param context 上下文
     * @param id      filter 或引用处的字段 id，可为空
     * @param code    filter 或引用处的字段 code，id 未命中时作为兜底
     * @return 变体 id 列表；未匹配返回 null
     */
    private List<String> resolveCalcVariantIds(MetricExpansionContext context, String id, String code) {
        List<String> variantIds = context.getCalcFieldIdToVariantIds().get(id);
        if (CollUtil.isNotEmpty(variantIds)) {
            return variantIds;
        }
        if (StrUtil.isNotEmpty(code)) {
            return context.getCalcFieldCodeToVariantIds().get(code);
        }
        return null;
    }

    /**
     * 合并计算字段变体 JSON 与 filter 筛选配置。
     *
     * 变体提供 id、code、title、customFieldConfigure 等度量定义；
     * filter 保留用户已填的 values、filterType 等交互状态。
     *
     * @param filterField  原 filter 条目
     * @param variantField measures 中对应变体的完整 JSON
     * @param showOrder    新的 filter 展示顺序
     * @return 合并后的 filter 条目
     */
    private JSONObject mergeCalcFilterField(JSONObject filterField, JSONObject variantField, double showOrder) {
        JSONObject merged = JSONObject.parseObject(variantField.toJSONString());
        merged.put("showOrder", showOrder);
        MetricExpansionMetaUtil.normalizeEmptyDisplayTitle(merged);
        copyFilterOnlyProperties(filterField, merged);
        return merged;
    }

    /**
     * 从原 filter 条目复制筛选区专有属性到目标 JSON，避免覆盖变体的公式与标识字段。
     */
    private void copyFilterOnlyProperties(JSONObject filterField, JSONObject target) {
        copyIfPresent(filterField, target, "values");
        copyIfPresent(filterField, target, "valuesTitle");
        copyIfPresent(filterField, target, "filterSetting");
        copyIfPresent(filterField, target, "filterValueType");
        copyIfPresent(filterField, target, "filterType");
        copyIfPresent(filterField, target, "fieldValues");
        copyIfPresent(filterField, target, "filterValueMode");
        copyIfPresent(filterField, target, "isPlaceholder");
        copyIfPresent(filterField, target, "isNew");
        copyIfPresent(filterField, target, "datasetId");
        copyIfPresent(filterField, target, "fieldType");
        copyIfPresent(filterField, target, "isFilter");
        copyIfPresent(filterField, target, "aggExpressionType");
        copyIfPresent(filterField, target, "originId");
        copyIfPresent(filterField, target, "originCode");
    }

    private void copyIfPresent(JSONObject source, JSONObject target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    /**
     * 计算字段在 sort 区域的变体同步：原 calc code/id 展开为 M 条 sort 规则。
     *
     * 处理 tableStyle.upDownSortData 与 leftRightSortData 下的 sortFields、sortItems。
     */
    private void expandSortByCalcVariants(MetricExpansionContext context, JSONObject setting) {
        if (setting == null) {
            return;
        }
        JSONObject tableStyle = setting.getJSONObject("tableStyle");
        if (tableStyle == null) {
            return;
        }
        expandSortFieldsByCalc(context, tableStyle.getJSONObject("upDownSortData"));
        expandSortFieldsByCalc(context, tableStyle.getJSONObject("leftRightSortData"));
    }

    private void expandSortFieldsByCalc(MetricExpansionContext context, JSONObject sortData) {
        if (sortData == null) {
            return;
        }
        JSONArray sortFields = sortData.getJSONArray("sortFields");
        if (CollUtil.isNotEmpty(sortFields)) {
            JSONArray expanded = new JSONArray();
            for (int i = 0; i < sortFields.size(); i++) {
                JSONObject item = sortFields.getJSONObject(i);
                String code = item.getString("fieldCode");
                List<String> codeVariants = context.getCalcFieldCodeToVariantCodes().get(code);
                List<String> idVariants = context.getCalcFieldCodeToVariantIds().get(code);
                List<String> variants = CollUtil.isNotEmpty(codeVariants) ? codeVariants : idVariants;
                if (CollUtil.isNotEmpty(variants)) {
                    for (String variant : variants) {
                        JSONObject copy = JSONObject.parseObject(item.toJSONString());
                        copy.put("fieldCode", variant);
                        expanded.add(copy);
                    }
                } else {
                    expanded.add(item);
                }
            }
            sortFields.clear();
            sortFields.addAll(expanded);
        }
        JSONArray sortItems = sortData.getJSONArray("sortItems");
        if (CollUtil.isNotEmpty(sortItems)) {
            JSONArray expanded = new JSONArray();
            for (int i = 0; i < sortItems.size(); i++) {
                JSONObject item = sortItems.getJSONObject(i);
                String calcRef = resolveCalcSortRef(context, item);
                List<String> variants = resolveCalcVariantRefs(context, calcRef);
                if (CollUtil.isNotEmpty(variants)) {
                    for (String variant : variants) {
                        JSONObject copy = JSONObject.parseObject(item.toJSONString());
                        replaceSortItemMetricRef(copy, calcRef, variant);
                        expanded.add(copy);
                    }
                } else {
                    expanded.add(item);
                }
            }
            sortItems.clear();
            sortItems.addAll(expanded);
        }
    }

    /**
     * 解析 sortItem 关联的原计算字段 code 或 id（含 columnField 内嵌引用）。
     */
    private String resolveCalcSortRef(MetricExpansionContext context, JSONObject item) {
        String orderBy = item.getString("orderBy");
        String columnField = item.getString("columnField");
        String matchKey = firstNonEmpty(orderBy, columnField);
        if (isCalcSortRef(context, matchKey)) {
            return matchKey;
        }
        String embeddedInColumn = findEmbeddedCalcRef(columnField, context);
        if (embeddedInColumn != null) {
            return embeddedInColumn;
        }
        return findEmbeddedCalcRef(orderBy, context);
    }

    private boolean isCalcSortRef(MetricExpansionContext context, String ref) {
        if (StrUtil.isEmpty(ref)) {
            return false;
        }
        return CollUtil.isNotEmpty(context.getCalcFieldCodeToVariantCodes().get(ref))
                || CollUtil.isNotEmpty(context.getCalcFieldIdToVariantIds().get(ref));
    }

    private List<String> resolveCalcVariantRefs(MetricExpansionContext context, String calcRef) {
        if (StrUtil.isEmpty(calcRef)) {
            return null;
        }
        List<String> codeVariants = context.getCalcFieldCodeToVariantCodes().get(calcRef);
        if (CollUtil.isNotEmpty(codeVariants)) {
            return codeVariants;
        }
        return context.getCalcFieldIdToVariantIds().get(calcRef);
    }

    /** 在 sort 文本中查找嵌入的原 calc code/id，长串优先 */
    private String findEmbeddedCalcRef(String text, MetricExpansionContext context) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        List<String> calcRefs = new ArrayList<>();
        if (CollUtil.isNotEmpty(context.getCalcFieldCodeToVariantCodes())) {
            calcRefs.addAll(context.getCalcFieldCodeToVariantCodes().keySet());
        }
        if (CollUtil.isNotEmpty(context.getCalcFieldIdToVariantIds())) {
            calcRefs.addAll(context.getCalcFieldIdToVariantIds().keySet());
        }
        if (CollUtil.isEmpty(calcRefs)) {
            return null;
        }
        calcRefs.sort((left, right) -> Integer.compare(right.length(), left.length()));
        for (String calcRef : calcRefs) {
            if (StrUtil.isNotEmpty(calcRef) && text.contains(calcRef)) {
                return calcRef;
            }
        }
        return null;
    }

    /**
     * 计算字段在 analysis 区域的变体同步。
     *
     * 将 thb、target、zb.items 中 measureIdList 里的原 calc id 替换为 M 个变体 id；
     * 同步处理 items 内 zbThbConfigs、targetThbConfigs 的 measureId；
     * total.aggConfig.configs 中 calc 条目按 id 展开为多条。
     */
    private void expandAnalysisByCalcVariants(MetricExpansionContext context, JSONObject analysis) {
        if (analysis == null) {
            return;
        }
        replaceCalcIdsInContainers(context, analysis.getJSONObject("thb"));
        replaceCalcIdsInContainers(context, analysis.getJSONObject("target"));
        JSONObject zb = analysis.getJSONObject("zb");
        if (zb != null) {
            replaceCalcIdsInItems(context, zb.getJSONArray("items"));
        }
        JSONObject compare = analysis.getJSONObject("compare");
        if (compare != null) {
            replaceCalcIdsInItems(context, compare.getJSONArray("items"));
        }
        replaceCalcIdsInTotal(context, analysis.getJSONObject("total"));
        expandTotalItemsByCalcVariants(context, analysis.getJSONObject("total"));
    }

    private void expandCustomCompareByCalcVariants(MetricExpansionContext context, JSONObject setting) {
        if (setting == null) {
            return;
        }
        replaceCalcIdsRecursive(context, setting.get("customCompare"));
    }

    private void replaceCalcIdsInContainers(MetricExpansionContext context, JSONObject container) {
        if (container == null) {
            return;
        }
        for (String key : container.keySet()) {
            Object value = container.get(key);
            if (value instanceof JSONArray) {
                replaceCalcIdsInItems(context, (JSONArray) value);
            }
        }
    }

    /**
     * 分析配置 items 中：measureIdList 与嵌套 THB 配置的 calc id 替换。
     *
     * measureIdList 走 replaceCalcIdList（1→M）；
     * zbThbConfigs、targetThbConfigs 内 measureId 走 replaceCalcNestedMeasureIdConfigs。
     */
    private void replaceCalcIdsInItems(MetricExpansionContext context, JSONArray items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Object itemObj = items.get(i);
            if (!(itemObj instanceof JSONObject)) {
                continue;
            }
            JSONObject item = (JSONObject) itemObj;
            syncMeasureIdListOnItem(context, item,
                    replaceCalcIdList(context, resolveMeasureRefSourceList(item)));
            replaceCalcNestedMeasureIdConfigs(context, item.getJSONArray("zbThbConfigs"));
            replaceCalcNestedMeasureIdConfigs(context, item.getJSONArray("targetThbConfigs"));
        }
    }

    /**
     * 分析项 zbThbConfigs / targetThbConfigs 中 calc measureId 按 1→M 展开为变体 id。
     *
     * 与普通指标 expandNestedMeasureIdConfigs 对称：原一条 THB 绑定可变为 M 条，每条对应一个业务线变体。
     */
    private void replaceCalcNestedMeasureIdConfigs(MetricExpansionContext context, JSONArray configs) {
        if (CollUtil.isEmpty(configs)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        for (int i = 0; i < configs.size(); i++) {
            JSONObject cfg = configs.getJSONObject(i);
            if (cfg == null) {
                continue;
            }
            String measureId = cfg.getString("measureId");
            List<String> variants = context.getCalcFieldIdToVariantIds().get(measureId);
            if (CollUtil.isNotEmpty(variants)) {
                if (context.isPreserveSourceData()) {
                    expanded.add(cfg);
                }
                for (String variantId : variants) {
                    JSONObject copy = JSONObject.parseObject(cfg.toJSONString());
                    copy.put("measureId", variantId);
                    expanded.add(copy);
                }
            } else {
                expanded.add(cfg);
            }
        }
        configs.clear();
        configs.addAll(expanded);
    }

    private void replaceCalcIdsInTotal(MetricExpansionContext context, JSONObject total) {
        if (total == null) {
            return;
        }
        JSONObject aggConfig = total.getJSONObject("aggConfig");
        if (aggConfig == null) {
            return;
        }
        JSONArray configs = aggConfig.getJSONArray("configs");
        if (CollUtil.isEmpty(configs)) {
            return;
        }
        JSONArray expanded = new JSONArray();
        for (int i = 0; i < configs.size(); i++) {
            JSONObject cfg = configs.getJSONObject(i);
            String id = cfg.getString("id");
            String code = cfg.getString("code");
            List<String> idVariants = context.getCalcFieldIdToVariantIds().get(id);
            List<String> codeVariants = context.getCalcFieldCodeToVariantCodes().get(code);
            if (CollUtil.isNotEmpty(idVariants)) {
                for (int j = 0; j < idVariants.size(); j++) {
                    JSONObject copy = JSONObject.parseObject(cfg.toJSONString());
                    copy.put("id", idVariants.get(j));
                    if (CollUtil.isNotEmpty(codeVariants) && j < codeVariants.size()) {
                        copy.put("code", codeVariants.get(j));
                    } else {
                        copy.put("code", "");
                    }
                    expanded.add(copy);
                }
            } else {
                expanded.add(cfg);
            }
        }
        aggConfig.put("configs", expanded);
    }

    private void replaceCalcIdsRecursive(MetricExpansionContext context, Object node) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.containsKey("measureIdList") || StrUtil.isNotEmpty(obj.getString("measureId"))) {
                syncMeasureIdListOnItem(context, obj,
                        replaceCalcIdList(context, resolveMeasureRefSourceList(obj)));
            }
            for (String key : obj.keySet()) {
                replaceCalcIdsRecursive(context, obj.get(key));
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.size(); i++) {
                replaceCalcIdsRecursive(context, arr.get(i));
            }
        }
    }

    private JSONArray replaceCalcIdList(MetricExpansionContext context, JSONArray idList) {
        JSONArray expanded = new JSONArray();
        if (CollUtil.isEmpty(idList)) {
            return expanded;
        }
        for (int i = 0; i < idList.size(); i++) {
            String id = idList.getString(i);
            if (StrUtil.isEmpty(id) || "all".equalsIgnoreCase(id)) {
                expanded.add(id);
                continue;
            }
            List<String> variants = context.getCalcFieldIdToVariantIds().get(id);
            if (CollUtil.isNotEmpty(variants)) {
                if (context.isPreserveSourceData()) {
                    expanded.add(id);
                }
                expanded.addAll(variants);
                continue;
            }
            // 非 calc id 已在 expandTplConfig 阶段完成 1→N，此处仅透传，避免二次 expand 产生重复
            expanded.add(id);
        }
        return dedupeJsonArrayPreserveOrder(expanded);
    }

    private String firstNonEmpty(String a, String b) {
        if (StrUtil.isNotEmpty(a)) {
            return a;
        }
        return b;
    }

    /**
     * total.items 指标引用同步为 measureIdList 数组（单条配置内 1→N）。
     */
    private void expandTotalItemsSection(MetricExpansionContext context, JSONObject total) {
        syncTotalItemsMeasureRefs(context, total, false);
    }

    /** 计算字段变体同步 total.items 中的 measureIdList */
    private void expandTotalItemsByCalcVariants(MetricExpansionContext context, JSONObject total) {
        syncTotalItemsMeasureRefs(context, total, true);
    }

    /**
     * 将 total.items 上的 measureId / measureIdList 同步为新 field id 数组，不拆配置项。
     */
    private void syncTotalItemsMeasureRefs(MetricExpansionContext context, JSONObject total, boolean calcVariantPass) {
        if (total == null) {
            return;
        }
        JSONArray items = total.getJSONArray("items");
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item == null) {
                continue;
            }
            JSONArray sourceList = resolveMeasureRefSourceList(item);
            if (CollUtil.isEmpty(sourceList)) {
                continue;
            }
            JSONArray expandedList = calcVariantPass
                    ? replaceCalcIdList(context, sourceList)
                    : expandIdList(context, sourceList);
            syncMeasureIdListOnItem(context, item, expandedList);
        }
    }

    /**
     * 扫描 setting.tableStyle.conditionalFormat 中的 measureColumnCode。
     * 维度规则不参与 source 扫描；指标规则按 base code 或「sourceCode_分析后缀」识别。
     */
    private void scanConditionalFormat(JSONObject setting, Set<String> foundCodes, MetricExpansionContext context) {
        JSONArray rules = resolveConditionalFormatRules(setting);
        if (CollUtil.isEmpty(rules)) {
            return;
        }
        for (int i = 0; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            if (rule == null || isConditionalFormatDimRule(rule)) {
                continue;
            }
            collectExpandableSourceFromMeasureColumnCode(rule.getString("measureColumnCode"), foundCodes, context);
        }
    }

    /**
     * 条件格式规则膨胀：source 指标 1→N 时，每条规则按 target 复制并替换 measureColumnCode。
     * calc 变体阶段将 id 型 measureColumnCode 替换为全部变体 id。
     */
    private void expandConditionalFormat(MetricExpansionContext context, JSONObject setting, boolean calcVariantPass) {
        JSONArray rules = resolveConditionalFormatRules(setting);
        if (CollUtil.isEmpty(rules)) {
            return;
        }
        JSONObject tableStyle = setting.getJSONObject("tableStyle");
        JSONArray expandedRules = new JSONArray();
        for (int i = 0; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            if (rule == null) {
                continue;
            }
            if (isConditionalFormatDimRule(rule)) {
                expandedRules.add(rule);
                continue;
            }
            String measureColumnCode = rule.getString("measureColumnCode");
            List<String> expandedCodes = calcVariantPass
                    ? expandConditionalFormatMeasureColumnCodeByCalc(context, measureColumnCode)
                    : expandConditionalFormatMeasureColumnCode(context, measureColumnCode);
            appendExpandedConditionalFormatRules(expandedRules, rule, measureColumnCode, expandedCodes);
        }
        tableStyle.put("conditionalFormat", expandedRules);
    }

    private JSONArray resolveConditionalFormatRules(JSONObject setting) {
        if (setting == null) {
            return null;
        }
        JSONObject tableStyle = setting.getJSONObject("tableStyle");
        if (tableStyle == null) {
            return null;
        }
        return tableStyle.getJSONArray("conditionalFormat");
    }

    private boolean isConditionalFormatDimRule(JSONObject rule) {
        return rule != null && "dim".equalsIgnoreCase(rule.getString("measureColumnType"));
    }

    private void collectExpandableSourceFromMeasureColumnCode(String measureColumnCode, Set<String> foundCodes,
                                                              MetricExpansionContext context) {
        if (StrUtil.isEmpty(measureColumnCode)) {
            return;
        }
        for (String mappingSource : context.getAllMappingSourceCodes()) {
            if (matchesExpandableMeasureColumnCode(measureColumnCode, mappingSource)) {
                foundCodes.add(mappingSource);
            }
        }
        String codeById = context.getFieldIdToCode().get(measureColumnCode);
        if (StrUtil.isNotEmpty(codeById)) {
            foundCodes.add(codeById);
            String baseCode = MetricExpansionMetaUtil.stripAggExpressionSuffix(codeById);
            if (StrUtil.isNotEmpty(baseCode) && !baseCode.equals(codeById)) {
                foundCodes.add(baseCode);
            }
        }
    }

    private boolean matchesExpandableMeasureColumnCode(String measureColumnCode, String sourceCode) {
        if (StrUtil.isEmpty(measureColumnCode) || StrUtil.isEmpty(sourceCode)) {
            return false;
        }
        if (sourceCode.equals(measureColumnCode)) {
            return true;
        }
        String codeWithoutAgg = MetricExpansionMetaUtil.stripAggExpressionSuffix(measureColumnCode);
        if (sourceCode.equals(codeWithoutAgg)) {
            return true;
        }
        return measureColumnCode.startsWith(sourceCode + "_");
    }

    private void appendExpandedConditionalFormatRules(JSONArray expandedRules, JSONObject rule, String originalCode,
                                                      List<String> expandedCodes) {
        if (CollUtil.isEmpty(expandedCodes)) {
            expandedRules.add(rule);
            return;
        }
        if (expandedCodes.size() == 1 && expandedCodes.get(0).equals(originalCode)) {
            expandedRules.add(rule);
            return;
        }
        for (String expandedCode : expandedCodes) {
            JSONObject copy = JSONObject.parseObject(rule.toJSONString());
            copy.put("measureColumnCode", expandedCode);
            expandedRules.add(copy);
        }
    }

    private List<String> expandConditionalFormatMeasureColumnCode(MetricExpansionContext context, String measureColumnCode) {
        if (StrUtil.isEmpty(measureColumnCode)) {
            return singletonMeasureColumnCode(measureColumnCode);
        }
        for (String sourceCode : context.getActiveSourceCodes()) {
            List<String> expanded = buildTargetMeasureColumnCodes(context, measureColumnCode, sourceCode);
            if (CollUtil.isNotEmpty(expanded)) {
                return prependOriginalMeasureColumnCode(context, measureColumnCode, expanded);
            }
        }
        List<String> mappedRefs = context.resolveNewFieldIdsByOldFieldRef(measureColumnCode);
        if (CollUtil.isNotEmpty(mappedRefs)) {
            return prependOriginalMeasureColumnCode(context, measureColumnCode, mappedRefs);
        }
        return singletonMeasureColumnCode(measureColumnCode);
    }

    private List<String> expandConditionalFormatMeasureColumnCodeByCalc(MetricExpansionContext context,
                                                                        String measureColumnCode) {
        if (StrUtil.isEmpty(measureColumnCode)) {
            return singletonMeasureColumnCode(measureColumnCode);
        }
        List<String> variantIds = context.getCalcFieldIdToVariantIds().get(measureColumnCode);
        if (CollUtil.isNotEmpty(variantIds)) {
            return prependOriginalMeasureColumnCode(context, measureColumnCode, variantIds);
        }
        variantIds = context.getCalcFieldCodeToVariantIds().get(measureColumnCode);
        if (CollUtil.isNotEmpty(variantIds)) {
            return prependOriginalMeasureColumnCode(context, measureColumnCode, variantIds);
        }
        return singletonMeasureColumnCode(measureColumnCode);
    }

    private List<String> prependOriginalMeasureColumnCode(MetricExpansionContext context, String measureColumnCode,
                                                          List<String> expandedCodes) {
        if (!context.isPreserveSourceData() || CollUtil.isEmpty(expandedCodes)) {
            return expandedCodes;
        }
        List<String> result = new ArrayList<>();
        result.add(measureColumnCode);
        result.addAll(expandedCodes);
        return dedupePreserveOrder(result);
    }

    private List<String> buildTargetMeasureColumnCodes(MetricExpansionContext context, String measureColumnCode,
                                                       String sourceCode) {
        if (!matchesExpandableMeasureColumnCode(measureColumnCode, sourceCode)) {
            return null;
        }
        String suffix = "";
        if (!sourceCode.equals(measureColumnCode)
                && !sourceCode.equals(MetricExpansionMetaUtil.stripAggExpressionSuffix(measureColumnCode))) {
            suffix = measureColumnCode.substring(sourceCode.length());
        }
        List<MetricExpansionTarget> targets = context.getTargetsBySourceCode().get(sourceCode);
        if (CollUtil.isEmpty(targets)) {
            return null;
        }
        List<String> expandedCodes = new ArrayList<>();
        for (MetricExpansionTarget target : targets) {
            expandedCodes.add(target.getMetaField().getCode() + suffix);
        }
        return expandedCodes;
    }

    private List<String> singletonMeasureColumnCode(String measureColumnCode) {
        List<String> codes = new ArrayList<>(1);
        codes.add(measureColumnCode);
        return codes;
    }
}
