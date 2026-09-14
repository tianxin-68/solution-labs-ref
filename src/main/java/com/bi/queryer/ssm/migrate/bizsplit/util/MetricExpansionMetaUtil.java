package com.bi.queryer.ssm.migrate.bizsplit.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.enums.Enabled;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 指标膨胀元数据与字段命名工具。
 *
 * 职责：
 * selectMetaFieldByCode — 同 code 多条元数据时优先 is_show=1 且权重最大；
 * validateTargetMetric — 校验映射 target 在元数据缓存中存在且未停用；
 * buildTargetMeasureField — 普通 source 指标 1→N 时克隆字段 JSON 并改写为 target；
 * buildExpressionIdMappingItem — 计算字段 expressionIdMapping 项，对齐前端 Object.assign 引用字段元数据；
 * buildExpandedDisplayName — 按业务线规则改写展示名（用于 displayTitle 与 calc title）；
 * replaceLiteral — 计算字段 formula 中的 id/code 字面量替换；
 * replaceFieldIdInExpression — 公式中字段 id 替换，避免短 id 误伤含后缀的长 id（如 uuid 误替换 uuid_1）。
 *
 * 普通指标命名规则：title/name 始终取 target 元数据 title；仅当源字段有别名（displayTitle 非空）
 * 时对 displayTitle 做业务线改写（含源业务线则替换，否则前缀「目标业务线-」）；无别名时置空字符串，与前端保存一致。
 * 计算字段命名由 CalcFieldExpansionProcessor 处理，不使用 displayTitle。
 */
public final class MetricExpansionMetaUtil {

    /** 与 DerivedFieldBuilder / 前端 getOriginalExpByAggregateExp 一致，长后缀优先 */
    private static final String SUFFIX_AVG_BY_DAY_REAL = "_" + AggExpressionType.Avg_By_Day_Real.getCode();
    private static final String SUFFIX_AVG_BY_DAY = "_" + AggExpressionType.Avg_By_Day.getCode();
    private static final String[] AGG_EXPRESSION_SUFFIXES = {SUFFIX_AVG_BY_DAY_REAL, SUFFIX_AVG_BY_DAY};

    /** 与 CustomFieldParser.expressionPattern 一致，提取 [fieldId] 中的 id */
    private static final Pattern EXPRESSION_FIELD_ID_PATTERN = Pattern.compile("(?<=\\[)(.+?)(?=\\])");

    private MetricExpansionMetaUtil() {
    }

    /**
     * 解析日均/非空日均后缀（含前导下划线），非派生字段返回 null。
     */
    public static String resolveAggExpressionSuffix(String codeOrId) {
        if (StrUtil.isEmpty(codeOrId)) {
            return null;
        }
        for (String suffix : AGG_EXPRESSION_SUFFIXES) {
            if (codeOrId.endsWith(suffix)) {
                return suffix;
            }
        }
        return null;
    }

    /**
     * 判断 code/id 是否仅为日均/非空日均后缀（如 _avg_by_d），不能作为公式 code 字面量替换。
     */
    public static boolean isAggExpressionSuffixOnly(String codeOrId) {
        if (StrUtil.isEmpty(codeOrId)) {
            return false;
        }
        for (String suffix : AGG_EXPRESSION_SUFFIXES) {
            if (suffix.equals(codeOrId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 去除日均/非空日均后缀，还原 base id/code。
     */
    public static String stripAggExpressionSuffix(String codeOrId) {
        String suffix = resolveAggExpressionSuffix(codeOrId);
        if (suffix == null) {
            return codeOrId;
        }
        return codeOrId.substring(0, codeOrId.length() - suffix.length());
    }

    /**
     * 判断 measures/filter 字段是否为日均或非空日均派生指标。
     */
    public static boolean isAvgByDayMeasureField(JSONObject field) {
        if (field == null) {
            return false;
        }
        String aggExpressionType = field.getString("aggExpressionType");
        if (AggExpressionType.Avg_By_Day.getCode().equals(aggExpressionType)
                || AggExpressionType.Avg_By_Day_Real.getCode().equals(aggExpressionType)) {
            return true;
        }
        return resolveAggExpressionSuffix(field.getString("code")) != null;
    }

    /**
     * 判断是否为纯 LOD 指标（customFieldConfigure.type=lod，非 lod 四则运算）。
     * LOD 指标 code 常为 _avg_by_d 等后缀，不能用于 source 匹配。
     *
     * @param field tplConfig 字段 JSON
     * @return true 表示纯 LOD 指标
     */
    public static boolean isPureLodMeasureField(JSONObject field) {
        if (field == null) {
            return false;
        }
        if ("lod".equals(field.getString("cptType"))) {
            return true;
        }
        JSONObject customCfg = field.getJSONObject("customFieldConfigure");
        if (customCfg == null) {
            return false;
        }
        return CustomFieldType.LOD.getCode().equals(customCfg.getString("type"));
    }

    /**
     * 通过 measureId 反查元数据指标 code（支持日均后缀；不含 lod: 前缀）。
     *
     * @param measureId LOD 依赖指标 id
     * @return 元数据 code；未找到返回 null
     */
    public static String resolveMeasureCodeByMeasureId(String measureId) {
        if (StrUtil.isEmpty(measureId)) {
            return null;
        }
        String normalizedId = measureId.startsWith(CustomFieldType.LOD.getIdentifier())
                ? measureId.substring(CustomFieldType.LOD.getIdentifier().length()) : measureId;
        MetaField metaField = SSDMetaCacheManager.getField(normalizedId);
        if (metaField != null && StrUtil.isNotEmpty(metaField.getCode())) {
            return metaField.getCode();
        }
        String baseId = stripAggExpressionSuffix(normalizedId);
        if (!normalizedId.equals(baseId)) {
            metaField = SSDMetaCacheManager.getField(baseId);
            if (metaField != null && StrUtil.isNotEmpty(metaField.getCode())) {
                return metaField.getCode();
            }
        }
        return null;
    }

    /**
     * 解析 LOD 底层 source 指标 code。
     * 优先 expressionIdMapping：measureId 在元数据仍可能命中，但 code 常为物理字段名而非 KPI 编码（如 D_TFC_xxx）。
     * 与 {@link com.bi.queryer.ssm.migrate.bizsplit.builder.DerivedFieldBuilder} 解析顺序一致。
     *
     * @param customCfg LOD 字段 customFieldConfigure
     * @param measureId   lodConfig.measureId
     * @return 底层指标 code；未找到返回 null
     */
    public static String resolveLodUnderlyingSourceCode(JSONObject customCfg, String measureId) {
        if (StrUtil.isEmpty(measureId)) {
            return null;
        }
        String mappingCode = resolveLodSourceCodeFromExpressionMapping(customCfg, measureId);
        if (StrUtil.isNotEmpty(mappingCode)) {
            return mappingCode;
        }
        return resolveMeasureCodeByMeasureId(measureId);
    }

    /**
     * 从 LOD expressionIdMapping 解析底层指标 code。
     * 优先匹配 measureId 对应项，否则取首条有效 code/kpiNo。
     *
     * @param customCfg LOD 字段 customFieldConfigure
     * @param measureId   lodConfig.measureId，可为空
     * @return 底层指标 code；未找到返回 null
     */
    public static String resolveLodSourceCodeFromExpressionMapping(JSONObject customCfg, String measureId) {
        if (customCfg == null) {
            return null;
        }
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        if (CollUtil.isEmpty(mapping)) {
            return null;
        }
        String fallbackCode = null;
        for (int i = 0; i < mapping.size(); i++) {
            JSONObject item = mapping.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String mapCode = item.getString("code");
            if (StrUtil.isEmpty(mapCode)) {
                mapCode = item.getString("kpiNo");
            }
            if (StrUtil.isEmpty(mapCode)) {
                continue;
            }
            if (StrUtil.isNotEmpty(measureId) && measureId.equals(item.getString("id"))) {
                return mapCode;
            }
            if (fallbackCode == null) {
                fallbackCode = mapCode;
            }
        }
        return fallbackCode;
    }

    /**
     * 索引 LOD measureId 与 expressionIdMapping，维护 id→code 并纳入扫描集合。
     *
     * @param measureId     lodConfig.measureId
     * @param customCfg     LOD 字段 customFieldConfigure
     * @param foundCodes    扫描收集的 code 集合
     * @param fieldIdToCode id → code 索引
     */
    public static void indexLodMeasureRef(String measureId, JSONObject customCfg,
                                          Set<String> foundCodes, Map<String, String> fieldIdToCode) {
        if (StrUtil.isEmpty(measureId) || foundCodes == null || fieldIdToCode == null) {
            return;
        }
        indexLodExpressionMappingRefs(customCfg, foundCodes, fieldIdToCode);
        String underlyingCode = resolveLodUnderlyingSourceCode(customCfg, measureId);
        if (StrUtil.isEmpty(underlyingCode)) {
            return;
        }
        fieldIdToCode.put(measureId, underlyingCode);
        String baseMeasureId = stripAggExpressionSuffix(measureId);
        if (!measureId.equals(baseMeasureId)) {
            fieldIdToCode.put(baseMeasureId, stripAggExpressionSuffix(underlyingCode));
        }
        foundCodes.add(underlyingCode);
        String baseCode = stripAggExpressionSuffix(underlyingCode);
        if (StrUtil.isNotEmpty(baseCode) && !baseCode.equals(underlyingCode)) {
            foundCodes.add(baseCode);
        }
    }

    /**
     * 扫描 LOD expressionIdMapping，维护 id→code 并纳入 source 候选。
     */
    public static void indexLodExpressionMappingRefs(JSONObject customCfg, Set<String> foundCodes,
                                                     Map<String, String> fieldIdToCode) {
        if (customCfg == null || foundCodes == null || fieldIdToCode == null) {
            return;
        }
        JSONArray mapping = customCfg.getJSONArray("expressionIdMapping");
        if (CollUtil.isEmpty(mapping)) {
            return;
        }
        for (int i = 0; i < mapping.size(); i++) {
            JSONObject item = mapping.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String mapId = item.getString("id");
            String mapCode = item.getString("code");
            if (StrUtil.isEmpty(mapCode)) {
                mapCode = item.getString("kpiNo");
            }
            if (StrUtil.isNotEmpty(mapId) && StrUtil.isNotEmpty(mapCode)) {
                fieldIdToCode.put(mapId, mapCode);
            }
            if (StrUtil.isNotEmpty(mapCode)) {
                foundCodes.add(mapCode);
                String baseCode = stripAggExpressionSuffix(mapCode);
                if (StrUtil.isNotEmpty(baseCode) && !baseCode.equals(mapCode)) {
                    foundCodes.add(baseCode);
                }
            }
        }
    }

    /**
     * 从四则/LOD 公式中提取 [fieldId] 引用列表。
     *
     * @param expression 公式字符串
     * @return 引用的 fieldId 列表
     */
    public static List<String> extractExpressionFieldIds(String expression) {
        if (StrUtil.isEmpty(expression)) {
            return Collections.emptyList();
        }
        List<String> fieldIds = new ArrayList<>();
        Matcher matcher = EXPRESSION_FIELD_ID_PATTERN.matcher(expression);
        while (matcher.find()) {
            String fieldId = matcher.group(1);
            if (StrUtil.isNotEmpty(fieldId)) {
                fieldIds.add(fieldId);
            }
        }
        return fieldIds;
    }

    /**
     * 将 measureId 索引到 fieldIdToCode，并把底层 code 纳入扫描集合（支持不在 measures 区的隐藏引用）。
     *
     * @param measureId     公式或 mapping 中的字段 id
     * @param foundCodes    扫描收集的 code 集合
     * @param fieldIdToCode id → code 索引
     */
    public static void indexMeasureIdRef(String measureId, Set<String> foundCodes, Map<String, String> fieldIdToCode) {
        if (StrUtil.isEmpty(measureId) || foundCodes == null || fieldIdToCode == null) {
            return;
        }
        if (measureId.startsWith(CustomFieldType.ANALYSIS.getIdentifier())) {
            return;
        }
        String normalizedId = measureId.startsWith(CustomFieldType.LOD.getIdentifier())
                ? measureId.substring(CustomFieldType.LOD.getIdentifier().length()) : measureId;
        String code = resolveMeasureCodeByMeasureId(normalizedId);
        if (StrUtil.isEmpty(code)) {
            return;
        }
        fieldIdToCode.put(normalizedId, code);
        if (!measureId.equals(normalizedId)) {
            fieldIdToCode.put(measureId, code);
        }
        foundCodes.add(code);
        String baseCode = stripAggExpressionSuffix(code);
        if (StrUtil.isNotEmpty(baseCode) && !baseCode.equals(code)) {
            foundCodes.add(baseCode);
        }
        String baseId = stripAggExpressionSuffix(normalizedId);
        if (!normalizedId.equals(baseId)) {
            fieldIdToCode.put(baseId, baseCode);
        }
    }

    /**
     * 判断 measureId 解析出的 code 是否等于指定 source code（含日均后缀归一化）。
     */
    public static boolean matchesSourceCode(String measureId, String sourceCode) {
        if (StrUtil.isEmpty(measureId) || StrUtil.isEmpty(sourceCode)) {
            return false;
        }
        String refCode = resolveMeasureCodeByMeasureId(measureId);
        if (sourceCode.equals(refCode)) {
            return true;
        }
        return sourceCode.equals(stripAggExpressionSuffix(refCode));
    }

    /**
     * 按 code 选取元数据字段：优先 is_show=1，再取权重最大（事实表字段权重 ×10000，与 FieldUtil 一致）。
     * 若无可见字段，回退为全部候选中权重最大且未停用的字段。
     *
     * @param fieldCode 字段编码
     * @return 匹配的元数据；不存在或未启用时返回 null
     */
    public static MetaField selectMetaFieldByCode(String fieldCode) {
        if (StrUtil.isEmpty(fieldCode)) {
            return null;
        }
        List<MetaField> fields = SSDMetaCacheManager.getFieldByCode(fieldCode);
        if (CollUtil.isEmpty(fields)) {
            return null;
        }
        MetaField selected = selectBestMetaFieldByWeight(fields, true);
        if (selected == null) {
            selected = selectBestMetaFieldByWeight(fields, false);
        }
        return selected;
    }

    /**
     * 校验 target 指标元数据存在且可用，返回优先使用的 MetaField。
     * 同一 code 可能对应多条元数据，优先 is_show=1 且权重最大。
     * isActive=0 视为停用并报错，中止当前视图膨胀。
     *
     * @param targetMetricCode 目标指标编码
     * @return 可用的元数据
     */
    public static MetaField validateTargetMetric(String targetMetricCode) {
        if (StrUtil.isEmpty(targetMetricCode)) {
            throw new SSDException("目标指标编码为空");
        }
        MetaField field = selectMetaFieldByCode(targetMetricCode);
        if (field == null) {
            throw new SSDException("目标指标不存在或不可用: " + targetMetricCode);
        }
        if (field.getIsActive() != null && field.getIsActive() == 0) {
            throw new SSDException("目标指标已停用: " + targetMetricCode);
        }
        return field;
    }

    /**
     * 从候选元数据中按权重选取最优字段。
     *
     * @param fields      同 code 候选列表
     * @param requireShow true 时仅考虑 is_show=1
     * @return 最优字段；无匹配时返回 null
     */
    private static MetaField selectBestMetaFieldByWeight(List<MetaField> fields, boolean requireShow) {
        MetaField best = null;
        for (MetaField field : fields) {
            if (field == null || isInactiveMetaField(field)) {
                continue;
            }
            if (requireShow && !Enabled.isTrue(field.getIsShow())) {
                continue;
            }
            if (best == null || compareMetaFieldWeight(field, best) > 0) {
                best = field;
            }
        }
        return best;
    }

    /** 元数据是否已停用 */
    private static boolean isInactiveMetaField(MetaField field) {
        return field.getIsActive() != null && field.getIsActive() == 0;
    }

    /**
     * 比较两字段有效权重，事实表字段权重加权；相等时按 id 稳定排序。
     *
     * @return 大于 0 表示 left 更优
     */
    private static int compareMetaFieldWeight(MetaField left, MetaField right) {
        int weightDiff = Integer.compare(resolveEffectiveWeight(left), resolveEffectiveWeight(right));
        if (weightDiff != 0) {
            return weightDiff;
        }
        return StrUtil.compare(left.getId(), right.getId(), true);
    }

    /** 有效权重：事实表字段 ×10000，与 FieldUtil.getMaxWeightFields 一致 */
    private static int resolveEffectiveWeight(MetaField field) {
        int weight = field.getWeight();
        if (StrUtil.isNotEmpty(field.getFactTableId())) {
            return weight * 10000;
        }
        return weight;
    }

    /**
     * 以 source field JSON 为模板，深拷贝后替换为 target 的 id/code/title/moduleCtgId，并写入 showOrder。
     * title/name 始终使用 target 元数据 title；源字段有别名时仅 displayTitle 按业务线规则改写。
     *
     * @param sourceField        原字段 JSON
     * @param targetMeta         目标指标元数据
     * @param sourceBusinessline 入参源业务线
     * @param targetBusinessline 当前 target 对应的目标业务线
     * @param showOrder          新的展示顺序
     * @return 目标字段 JSON
     */
    public static JSONObject buildTargetMeasureField(JSONObject sourceField, MetaField targetMeta,
                                                     String sourceBusinessline, String targetBusinessline,
                                                     double showOrder) {
        JSONObject copy = JSON.parseObject(sourceField.toJSONString());
        copy.put("id", targetMeta.getId());
        copy.put("code", targetMeta.getCode());
        applyExpandedFieldNames(copy, sourceField, targetMeta, sourceBusinessline, targetBusinessline);
        if (StrUtil.isNotEmpty(targetMeta.getModuleCtgId())) {
            copy.put("moduleCtgId", targetMeta.getModuleCtgId());
        } else {
            copy.remove("moduleCtgId");
        }
        copy.put("showOrder", showOrder);
        // isShow 保留源视图用户配置的显隐状态，不使用 target 元数据默认值
        applyExpandedOriginRef(copy, targetMeta);
        applyExpandedMeasureKpiNo(copy, targetMeta);
        // 普通指标不应携带原计算配置
        copy.remove("customFieldConfigure");
        normalizeEmptyDisplayTitle(copy);
        return copy;
    }

    /**
     * 日均/非空日均派生指标 1→N：保留 aggExpressionType 等聚合配置，id/code 追加后缀。
     * 展示名在源字段 title 基础上替换 sourceCode 与业务线，保留「(日均)」「(非空日均)」等后缀文案。
     */
    public static JSONObject buildTargetAvgByDayMeasureField(JSONObject sourceField, MetaField targetMeta,
                                                             String aggSuffix, String sourceCode,
                                                             String sourceBusinessline, String targetBusinessline,
                                                             double showOrder) {
        JSONObject copy = JSON.parseObject(sourceField.toJSONString());
        copy.put("id", targetMeta.getId() + aggSuffix);
        copy.put("code", targetMeta.getCode() + aggSuffix);
        applyExpandedAvgFieldNames(copy, sourceField, targetMeta, sourceCode, sourceBusinessline, targetBusinessline);
        if (StrUtil.isNotEmpty(targetMeta.getModuleCtgId())) {
            copy.put("moduleCtgId", targetMeta.getModuleCtgId());
        } else {
            copy.remove("moduleCtgId");
        }
        copy.put("showOrder", showOrder);
        // isShow 保留源视图用户配置的显隐状态，不使用 target 元数据默认值
        applyExpandedOriginRef(copy, targetMeta);
        applyExpandedMeasureKpiNo(copy, targetMeta);
        copy.remove("customFieldConfigure");
        normalizeEmptyDisplayTitle(copy);
        return copy;
    }

    /**
     * 由已膨胀 measures 中的 target 字段构造 expressionIdMapping 项。
     * 对齐前端 CptField：引用字段元数据整体展开，剔除 measures 行专有属性。
     *
     * @param measureField result.measures 中的指标字段 JSON
     * @return expressionIdMapping 单项
     */
    public static JSONObject buildExpressionIdMappingItem(JSONObject measureField) {
        if (measureField == null) {
            return new JSONObject();
        }
        JSONObject item = JSON.parseObject(measureField.toJSONString());
        item.remove("showOrder");
        item.remove("customFieldConfigure");
        item.remove("sortType");
        normalizeEmptyDisplayTitle(item);
        applyExpressionIdMappingKpiNo(item, measureField);
        return item;
    }

    /**
     * 由元数据构造 expressionIdMapping 项（measures 中未找到 target 字段时的兜底）。
     * 字段集合对齐 CustomFieldExpressionIdMapping：id、code、title、moduleCtgId、kpiNo。
     *
     * @param meta 目标指标元数据
     * @return expressionIdMapping 单项
     */
    public static JSONObject buildExpressionIdMappingItem(MetaField meta) {
        JSONObject item = new JSONObject();
        if (meta == null) {
            return item;
        }
        item.put("id", meta.getId());
        item.put("code", meta.getCode());
        item.put("title", meta.getTitle());
        if (StrUtil.isNotEmpty(meta.getModuleCtgId())) {
            item.put("moduleCtgId", meta.getModuleCtgId());
        }
        applyExpressionIdMappingKpiNo(item, meta);
        return item;
    }

    /**
     * 解析 expressionIdMapping / measures 中应写入的 kpiNo。
     * 日均/非空日均派生指标的 id、code 可带 _avg_by_d 后缀，kpiNo 须还原为 base KPI 编码。
     *
     * @param meta 目标指标元数据
     * @return base kpiNo；无法解析时返回 null
     */
    public static String resolveExpressionIdMappingKpiNo(MetaField meta) {
        if (meta == null) {
            return null;
        }
        String kpiNo = meta.getKpiNo();
        if (StrUtil.isEmpty(kpiNo)) {
            kpiNo = meta.getCode();
        }
        return stripAggExpressionSuffix(kpiNo);
    }

    /**
     * 从 tplConfig 字段 JSON 解析 expressionIdMapping 应写入的 kpiNo。
     *
     * @param field tplConfig 字段 JSON
     * @return base kpiNo；无法解析时返回 null
     */
    public static String resolveExpressionIdMappingKpiNo(JSONObject field) {
        if (field == null) {
            return null;
        }
        String kpiNo = field.getString("kpiNo");
        if (StrUtil.isEmpty(kpiNo)) {
            kpiNo = field.getString("originCode");
        }
        if (StrUtil.isEmpty(kpiNo)) {
            kpiNo = field.getString("code");
        }
        if (StrUtil.isEmpty(kpiNo)) {
            return null;
        }
        return stripAggExpressionSuffix(kpiNo);
    }

    /**
     * 将 base kpiNo 写入 expressionIdMapping 项。
     */
    public static void applyExpressionIdMappingKpiNo(JSONObject mappingItem, MetaField meta) {
        if (mappingItem == null) {
            return;
        }
        String kpiNo = resolveExpressionIdMappingKpiNo(meta);
        if (StrUtil.isNotEmpty(kpiNo)) {
            mappingItem.put("kpiNo", kpiNo);
        } else {
            mappingItem.remove("kpiNo");
        }
    }

    /**
     * 从 measures 字段解析 kpiNo 并写入 expressionIdMapping 项。
     */
    public static void applyExpressionIdMappingKpiNo(JSONObject mappingItem, JSONObject measureField) {
        if (mappingItem == null) {
            return;
        }
        String kpiNo = resolveExpressionIdMappingKpiNo(measureField);
        if (StrUtil.isNotEmpty(kpiNo)) {
            mappingItem.put("kpiNo", kpiNo);
        } else {
            mappingItem.remove("kpiNo");
        }
    }

    /**
     * 膨胀后的 measures 字段同步 base kpiNo，避免日均后缀写入 kpiNo。
     */
    public static void applyExpandedMeasureKpiNo(JSONObject targetField, MetaField targetMeta) {
        if (targetField == null) {
            return;
        }
        String kpiNo = resolveExpressionIdMappingKpiNo(targetMeta);
        if (StrUtil.isNotEmpty(kpiNo)) {
            targetField.put("kpiNo", kpiNo);
        } else {
            targetField.remove("kpiNo");
        }
    }

    /**
     * 由已膨胀 measures 中的分析项字段构造 expressionIdMapping 项。
     * id 使用 analysis: 前缀；analysisConfig 写入 analysisItemConfig。
     *
     * @param expandedAnalysisMeasure result.measures 中 isAnalysis=1 的字段 JSON
     * @return 计算字段 expressionIdMapping 分析项
     */
    public static JSONObject buildAnalysisExpressionIdMappingItem(JSONObject expandedAnalysisMeasure) {
        if (expandedAnalysisMeasure == null) {
            return new JSONObject();
        }
        JSONObject item = buildExpressionIdMappingItem(expandedAnalysisMeasure);
        item.remove("analysisConfig");
        String code = expandedAnalysisMeasure.getString("code");
        if (StrUtil.isNotEmpty(code)) {
            item.put("id", CustomFieldType.ANALYSIS.getIdentifier() + code);
            item.put("code", code);
        }
        JSONObject analysisConfig = expandedAnalysisMeasure.getJSONObject("analysisConfig");
        if (analysisConfig != null) {
            item.put("analysisItemConfig", JSON.parseObject(analysisConfig.toJSONString()));
        }
        return item;
    }

    /**
     * 对齐 AnalysisMeasureInitializer.buildAnalysisCode / TplConfigExpansionProcessor 的常见分支。
     *
     * @param measureCode    依赖指标 code
     * @param analysisConfig 分析配置 JSON（analysisConfig / analysisItemConfig）
     * @return 分析项 code
     */
    public static String buildAnalysisFieldCode(String measureCode, JSONObject analysisConfig) {
        if (analysisConfig == null || StrUtil.isEmpty(measureCode)) {
            return measureCode;
        }
        String calcMode = analysisConfig.getString("calcMode");
        String calcType = analysisConfig.getString("calcType");
        JSONObject targetConfig = analysisConfig.getJSONObject("targetConfig");
        if (targetConfig != null && Enabled.isTrue(targetConfig.getInteger("isActive"))) {
            String targetCode = targetConfig.getString("targetCode");
            String baseCode = measureCode + (StrUtil.isNotEmpty(targetCode) ? targetCode : "");
            if (StrUtil.isNotEmpty(calcMode) && StrUtil.isNotEmpty(calcType)) {
                return baseCode + "_" + calcMode + "_" + calcType;
            }
            if (StrUtil.isNotEmpty(calcMode)) {
                return baseCode + "_" + calcMode;
            }
            return baseCode;
        }
        String ctrCalcMode = analysisConfig.getString("ctrCalcMode");
        if (StrUtil.isNotEmpty(ctrCalcMode) && StrUtil.isNotEmpty(calcMode) && StrUtil.isNotEmpty(calcType)) {
            return String.format("%s_%s_%s_%s", measureCode, ctrCalcMode, calcMode, calcType);
        }
        JSONObject zbThbConfig = analysisConfig.getJSONObject("zbThbConfig");
        if (zbThbConfig != null && StrUtil.isNotEmpty(zbThbConfig.getString("zbCalcMode"))) {
            String zbCalcMode = zbThbConfig.getString("zbCalcMode");
            String thbCalcMode = zbThbConfig.getString("thbCalcMode");
            Integer compareIndex = analysisConfig.getInteger("compareIndex");
            if (compareIndex != null) {
                return String.format("%s_%s_%s_%s_%s", measureCode, zbCalcMode, thbCalcMode, compareIndex, calcType);
            }
            return String.format("%s_%s_%s_%s", measureCode, zbCalcMode, thbCalcMode, calcType);
        }
        return String.format("%s_%s_%s", measureCode, calcMode, calcType);
    }

    /**
     * 无别名时 displayTitle 置为 ''，与前端保存归一化一致。
     */
    public static void normalizeEmptyDisplayTitle(JSONObject field) {
        if (field == null) {
            return;
        }
        String displayTitle = field.getString("displayTitle");
        if (StrUtil.isEmpty(displayTitle)) {
            field.put("displayTitle", "");
        }
    }

    /**
     * 按业务线规则改写展示名，用于 displayTitle 与计算字段 title。
     *
     * 规则一：originalName 含 sourceBusinessline 时，将其替换为 targetBusinessline；
     * 规则二：不含 sourceBusinessline 时，返回「targetBusinessline + "-" + originalName」。
     *
     * @param originalName       原展示名
     * @param sourceBusinessline 入参源业务线，可为空
     * @param targetBusinessline 当前 target 或变体对应的目标业务线
     * @return 改写后的展示名
     */
    public static String buildExpandedDisplayName(String originalName, String sourceBusinessline,
                                                  String targetBusinessline) {
        if (StrUtil.isEmpty(originalName)) {
            return originalName;
        }
        if (StrUtil.isEmpty(targetBusinessline)) {
            return originalName;
        }
        if (StrUtil.isNotEmpty(sourceBusinessline) && originalName.contains(sourceBusinessline)) {
            return originalName.replace(sourceBusinessline, targetBusinessline);
        }
        return targetBusinessline + "-" + originalName;
    }

    /**
     * 写入 target 字段的 title、name、displayTitle。
     *
     * title/name 统一使用 target 元数据 title；displayTitle 仅在源字段有别名时按业务线规则改写，
     * 否则置空字符串，避免深拷贝残留旧别名。
     */
    private static void applyExpandedFieldNames(JSONObject targetField, JSONObject sourceField,
                                                MetaField targetMeta, String sourceBusinessline,
                                                String targetBusinessline) {
        putTitleAndName(targetField, targetMeta != null ? targetMeta.getTitle() : null);

        String displayTitle = sourceField.getString("displayTitle");
        if (StrUtil.isNotEmpty(displayTitle)) {
            targetField.put("displayTitle",
                    buildExpandedDisplayName(displayTitle, sourceBusinessline, targetBusinessline));
        } else {
            targetField.put("displayTitle", "");
        }
    }

    /** 日均派生指标：保留源展示名结构，替换其中的 sourceCode 与业务线前缀 */
    private static void applyExpandedAvgFieldNames(JSONObject targetField, JSONObject sourceField,
                                                   MetaField targetMeta, String sourceCode,
                                                   String sourceBusinessline, String targetBusinessline) {
        applyExpandedDerivedFieldNames(targetField, sourceField, targetMeta, sourceCode,
                sourceBusinessline, targetBusinessline);
    }

    /**
     * LOD 指标名称改写：优先按指定指标（mapping）title 对齐，其次 source 元数据 title / 业务线规则。
     *
     * @param originalTitle         替换前 LOD 字段 title/name
     * @param originalMappingTitle  替换前 expressionIdMapping 中指定指标的 title
     * @param originalDisplayTitle  替换前 displayTitle
     * @param sourceMeasureId       替换前 lodConfig.measureId；已替换场景可空
     */
    public static void applyExpandedLodFieldNames(JSONObject targetField, String originalTitle,
                                                  String originalMappingTitle, String originalDisplayTitle,
                                                  MetaField targetMeta, String sourceCode,
                                                  String sourceBusinessline, String targetBusinessline,
                                                  String sourceMeasureId) {
        if (targetField == null || targetMeta == null) {
            return;
        }
        String title = deriveLodFieldTitle(originalTitle, originalMappingTitle, targetMeta, sourceCode,
                sourceBusinessline, targetBusinessline, sourceMeasureId);
        putTitleAndName(targetField, title);

        if (StrUtil.isEmpty(originalDisplayTitle)) {
            targetField.put("displayTitle", "");
            return;
        }
        String displayTitle = deriveLodFieldTitle(originalDisplayTitle, originalMappingTitle, targetMeta,
                sourceCode, sourceBusinessline, targetBusinessline, sourceMeasureId);
        targetField.put("displayTitle", displayTitle);
    }

    /**
     * 推导 LOD 指标展示名：与指定指标 title 对齐优先，避免仅改 lodConfig 不改指标名称。
     */
    private static String deriveLodFieldTitle(String originalTitle, String originalMappingTitle,
                                              MetaField targetMeta, String sourceCode,
                                              String sourceBusinessline, String targetBusinessline,
                                              String sourceMeasureId) {
        if (StrUtil.isEmpty(originalTitle)) {
            return targetMeta.getTitle();
        }
        String title = originalTitle;
        if (StrUtil.isNotEmpty(originalMappingTitle)) {
            if (title.equals(originalMappingTitle)) {
                return targetMeta.getTitle();
            }
            if (title.contains(originalMappingTitle)) {
                title = replaceLiteral(title, originalMappingTitle, targetMeta.getTitle());
            }
        }
        MetaField sourceMeta = resolveSourceMetaByMeasureId(sourceMeasureId);
        if (sourceMeta != null && StrUtil.isNotEmpty(sourceMeta.getTitle())
                && !sourceMeta.getTitle().equals(targetMeta.getTitle())
                && title.contains(sourceMeta.getTitle())) {
            title = replaceLiteral(title, sourceMeta.getTitle(), targetMeta.getTitle());
        }
        if (StrUtil.isNotEmpty(sourceCode) && StrUtil.isNotEmpty(targetMeta.getCode())
                && title.contains(sourceCode)) {
            title = replaceLiteral(title, sourceCode, targetMeta.getCode());
        }
        if (title.equals(originalTitle)) {
            title = buildExpandedDisplayName(title, sourceBusinessline, targetBusinessline);
        }
        if (title.equals(originalTitle) && StrUtil.isNotEmpty(targetMeta.getTitle())) {
            title = targetMeta.getTitle();
        }
        return title;
    }

    /**
     * 推导 LOD 指标替换后的 title/name。
     * 优先业务线展示名改写（LOD 名称常含源业务线前缀），其次 mapping 指定指标 title，最后 target 元数据 title。
     *
     * @param rawTitle            字段原始 title/name（不含 displayTitle）
     * @param mappingTitle        expressionIdMapping 指定指标 title；可为空
     * @param targetMeta          目标指标元数据
     * @param sourceBusinessline  源业务线
     * @param targetBusinessline  目标业务线
     * @return 期望 title；无法推导时返回 null
     */
    public static String resolveLodExpectedTitle(String rawTitle, String mappingTitle, MetaField targetMeta,
                                                 String sourceBusinessline, String targetBusinessline) {
        if (StrUtil.isNotEmpty(mappingTitle) && !mappingTitle.equals(rawTitle)) {
            return mappingTitle;
        }
        if (StrUtil.isNotEmpty(rawTitle)) {
            String expanded = buildExpandedDisplayName(rawTitle, sourceBusinessline, targetBusinessline);
            if (StrUtil.isNotEmpty(expanded) && !expanded.equals(rawTitle)) {
                return expanded;
            }
        }
        if (targetMeta != null && StrUtil.isNotEmpty(targetMeta.getTitle())
                && !targetMeta.getTitle().equals(rawTitle)) {
            return targetMeta.getTitle();
        }
        return null;
    }

    /**
     * 将 LOD 字段 title/name 同步为期望展示名，并清理残留的旧 displayTitle。
     */
    public static void applyLodExpectedFieldTitle(JSONObject field, String rawTitle, String mappingTitle,
                                                  MetaField targetMeta, String sourceBusinessline,
                                                  String targetBusinessline) {
        if (field == null) {
            return;
        }
        String expectedTitle = resolveLodExpectedTitle(rawTitle, mappingTitle, targetMeta,
                sourceBusinessline, targetBusinessline);
        if (StrUtil.isEmpty(expectedTitle)) {
            return;
        }
        String currentRawTitle = resolveRawFieldTitle(field);
        if (!expectedTitle.equals(currentRawTitle)) {
            putLodFieldTitle(field, expectedTitle);
        }
        String displayTitle = field.getString("displayTitle");
        if (StrUtil.isNotEmpty(displayTitle) && !expectedTitle.equals(displayTitle)) {
            field.put("displayTitle", "");
        }
    }

    /** 通过 lodConfig.measureId 反查 source 元数据 */
    private static MetaField resolveSourceMetaByMeasureId(String measureId) {
        if (StrUtil.isEmpty(measureId)) {
            return null;
        }
        String normalizedId = measureId.startsWith(CustomFieldType.LOD.getIdentifier())
                ? measureId.substring(CustomFieldType.LOD.getIdentifier().length()) : measureId;
        MetaField metaField = SSDMetaCacheManager.getField(normalizedId);
        if (metaField != null) {
            return metaField;
        }
        return SSDMetaCacheManager.getField(stripAggExpressionSuffix(normalizedId));
    }

    /** 派生/LOD 指标：保留源展示名结构，替换其中的 sourceCode 与业务线前缀 */
    private static void applyExpandedDerivedFieldNames(JSONObject targetField, JSONObject sourceField,
                                                       MetaField targetMeta, String sourceCode,
                                                       String sourceBusinessline, String targetBusinessline) {
        String title = resolveFieldTitle(sourceField);
        if (StrUtil.isNotEmpty(sourceCode) && StrUtil.isNotEmpty(title) && title.contains(sourceCode)) {
            title = replaceLiteral(title, sourceCode, targetMeta.getCode());
        }
        title = buildExpandedDisplayName(title, sourceBusinessline, targetBusinessline);
        putTitleAndName(targetField, title);

        String displayTitle = sourceField.getString("displayTitle");
        if (StrUtil.isNotEmpty(displayTitle)) {
            if (StrUtil.isNotEmpty(sourceCode) && displayTitle.contains(sourceCode)) {
                displayTitle = replaceLiteral(displayTitle, sourceCode, targetMeta.getCode());
            }
            targetField.put("displayTitle",
                    buildExpandedDisplayName(displayTitle, sourceBusinessline, targetBusinessline));
        } else {
            targetField.put("displayTitle", "");
        }
    }

    /**
     * 同步写入 title 与 name。
     */
    private static void putTitleAndName(JSONObject targetField, String title) {
        putLodFieldTitle(targetField, title);
    }

    /**
     * 写入 LOD/普通字段的 title、name（不含 displayTitle）。
     *
     * @param field 字段 JSON
     * @param title 展示名
     */
    public static void putLodFieldTitle(JSONObject field, String title) {
        if (field == null || StrUtil.isEmpty(title)) {
            return;
        }
        field.put("title", title);
        field.put("name", title);
    }

    /**
     * 读取字段原始 title/name，不含 displayTitle（LOD 指标名称与指定指标对齐时使用）。
     *
     * @param field tplConfig 字段 JSON
     * @return title 或 name；均空时返回 null
     */
    public static String resolveRawFieldTitle(JSONObject field) {
        if (field == null) {
            return null;
        }
        String title = field.getString("title");
        if (StrUtil.isNotEmpty(title)) {
            return title;
        }
        return field.getString("name");
    }

    /**
     * 膨胀后同步 originId/originCode 指向 target 元数据，供前端反查字段树。
     *
     * @param targetField 膨胀后的字段 JSON
     * @param targetMeta  目标指标元数据
     */
    public static void applyExpandedOriginRef(JSONObject targetField, MetaField targetMeta) {
        if (targetField == null || targetMeta == null) {
            return;
        }
        targetField.put("originId", targetMeta.getId());
        targetField.put("originCode", targetMeta.getCode());
    }

    /**
     * 解析 tplConfig 字段展示名：优先 displayTitle，其次 title、name、code。
     *
     * @param field tplConfig 字段 JSON
     * @return 展示名；无法解析时返回 null
     */
    public static String resolveFieldTitle(JSONObject field) {
        if (field == null) {
            return null;
        }
        String displayTitle = field.getString("displayTitle");
        if (StrUtil.isNotEmpty(displayTitle)) {
            return displayTitle;
        }
        String title = field.getString("title");
        if (StrUtil.isNotEmpty(title)) {
            return title;
        }
        String name = field.getString("name");
        if (StrUtil.isNotEmpty(name)) {
            return name;
        }
        return field.getString("code");
    }

    /**
     * 由元数据解析字段展示名。
     *
     * @param meta 指标元数据
     * @return 展示名；无法解析时返回 null
     */
    public static String resolveFieldTitle(MetaField meta) {
        if (meta == null) {
            return null;
        }
        if (StrUtil.isNotEmpty(meta.getTitle())) {
            return meta.getTitle();
        }
        if (StrUtil.isNotEmpty(meta.getName())) {
            return meta.getName();
        }
        return meta.getCode();
    }

    /**
     * 字面量全量替换。
     * 用于公式中旧 id/code 到新 id/code 的一致替换；from 与 to 相同或入参为空时原样返回。
     *
     * @param text 原文
     * @param from 被替换片段
     * @param to   替换目标
     * @return 替换后文本
     */
    public static String replaceLiteral(String text, String from, String to) {
        if (StrUtil.isEmpty(text) || StrUtil.isEmpty(from) || from.equals(to)) {
            return text;
        }
        return text.replace(from, to);
    }

    /**
     * 在公式中替换字段 id，支持 [id] 与裸 id 两种引用形式。
     * 裸 id 替换要求前后不为标识符字符，避免 uuid 误替换 uuid_1 的前缀。
     *
     * @param expression 公式字符串
     * @param oldId      原字段 id
     * @param newId      新字段 id
     * @return 替换后的公式
     */
    public static String replaceFieldIdInExpression(String expression, String oldId, String newId) {
        if (StrUtil.isEmpty(expression) || StrUtil.isEmpty(oldId) || oldId.equals(newId)) {
            return expression;
        }
        String updated = replaceLiteral(expression, "[" + oldId + "]", "[" + newId + "]");
        return replaceBareFieldIdToken(updated, oldId, newId);
    }

    /**
     * 裸 id 替换：前后不能紧邻标识符字符（字母/数字/_），保证完整 token 匹配。
     */
    private static String replaceBareFieldIdToken(String text, String oldId, String newId) {
        Pattern pattern = Pattern.compile("(?<![\\w])" + Pattern.quote(oldId) + "(?![\\w])");
        return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(newId));
    }
}
