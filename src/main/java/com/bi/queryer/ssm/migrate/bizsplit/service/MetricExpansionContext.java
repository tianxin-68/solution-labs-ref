package com.bi.queryer.ssm.migrate.bizsplit.service;

import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionFieldMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionTarget;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionMetaUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单视图指标膨胀运行时上下文。
 *
 * 生命周期：MetricExpansionService.executeOne 为每个 view 创建一份，贯穿
 * 筛选改写 → 扫描 → 普通膨胀 → 计算字段变体 → 派生字段重建。
 * 禁止跨视图复用。
 *
 * 字段说明：
 * allMappingSourceCodes — 映射表全部 source，扫描阶段与之求交集得到 activeSourceCodes。
 * activeSourceCodes — 本视图实际要膨胀的 source 集合。
 * targetsBySourceCode — 每个 active source 对应的 target 列表，顺序与映射表 pkid 一致。
 * fieldIdToCode — 字段 id 到 metric code 的索引，供 analysis.measureIdList 等只存 id 的位置反查。
 * measureExpandedSourceCodes — 在 measures 数组中被 1→N 替换的 source（不含仅出现在 calc 依赖中的 source）。
 * calcFieldIdToVariantIds — 原 calc id → 变体 id 列表。
 * calcFieldCodeToVariantCodes — 原 calc code → 变体 code 列表（仅原 code 非空时写入；前端四则 calc code 恒为 ""）。
 * calcFieldCodeToVariantIds — 原 calc code → 变体 id 列表（resolveCalcVariantIds 在 id 未命中且 code 非空时的兜底）。
 * calcFieldVariantById — 变体 id → measures 中完整字段 JSON（filter 合并 customFieldConfigure 时使用）。
 * fieldMappings — 本次膨胀收集的老/新 field id 映射，execute 写影子时落库。
 * sourceBusinessline — execute 入参源业务线，用于普通指标 displayTitle 与 calc title 改写。
 * preserveSourceData — execute 未指定 targetBusinessline 时为 true：膨胀与 filter 改写保留原始数据，仅追加 target。
 */
public class MetricExpansionContext {

    /** 当前处理的视图 id，保存时保持不变 */
    private String viewId;
    /** 视图所属查询模板 id，保存时保持不变 */
    private String tplId;
    /** 视图绑定的配置 id，用于更新 ssd_query_template_cfg / cfg_dtl */
    private String cfgId;
    /** 库内明文 tplConfig 解析后的 JSON 根对象，后续所有膨胀均就地修改此对象 */
    private JSONObject tplConfigJson;
    /** 映射表中全部 source_metric_code，扫描时作为候选全集 */
    private Set<String> allMappingSourceCodes = new LinkedHashSet<>();
    /** 本视图最终要执行膨胀的 source 集合 */
    private Set<String> activeSourceCodes = new LinkedHashSet<>();
    /** sourceCode -> 目标列表（含元数据与业务线），保持映射表顺序 */
    private Map<String, List<MetricExpansionTarget>> targetsBySourceCode = new LinkedHashMap<>();
    /** fieldId -> metricCode，扫描与膨胀过程持续维护 */
    private Map<String, String> fieldIdToCode = new LinkedHashMap<>();
    /** 步骤六新生成的计算字段 code */
    private Set<String> generatedCalcFieldCodes = new LinkedHashSet<>();
    /** 在 result.measures 中被直接膨胀的 source code */
    private Set<String> measureExpandedSourceCodes = new LinkedHashSet<>();
    /** 原计算字段 id -> 生成的变体 id 列表（顺序与业务线交集顺序一致） */
    private Map<String, List<String>> calcFieldIdToVariantIds = new LinkedHashMap<>();
    /** 原计算字段 code -> 生成的变体 code 列表 */
    private Map<String, List<String>> calcFieldCodeToVariantCodes = new LinkedHashMap<>();
    /** 原计算字段 code -> 生成的变体 id 列表 */
    private Map<String, List<String>> calcFieldCodeToVariantIds = new LinkedHashMap<>();
    /** 计算字段变体 id -> 完整字段 JSON（供 filter 等位置同步 customFieldConfigure） */
    private Map<String, JSONObject> calcFieldVariantById = new LinkedHashMap<>();
    /** 本次膨胀收集的 field id 映射明细（落库前仅填 old/new 及 metric code） */
    private List<MetricExpansionFieldMappingEntity> fieldMappings = new ArrayList<>();
    /** 本次 execute 入参源业务线，用于膨胀指标展示名改写 */
    private String sourceBusinessline;
    /** 未指定 targetBusinessline 时为 true：保留 source 指标与 filter 源业务线，仅追加 target */
    private boolean preserveSourceData;
    /** 膨胀开始前 result.measures 中已存在的指标 id，供私域可见性兜底判定「新膨胀指标」 */
    private final Set<String> initialMeasureIdsAtExpansionStart = new LinkedHashSet<>();
    /** 膨胀开始前 tplConfig 中已存在的指标 id（measures / filter） */
    private final Set<String> existingMeasureIds = new LinkedHashSet<>();
    /** 膨胀开始前 tplConfig 中已存在的指标 code（measures / filter） */
    private final Set<String> existingMeasureCodes = new LinkedHashSet<>();
    /** id -> 已存在指标字段，供重复跳过时写入 fieldMapping */
    private final Map<String, JSONObject> existingMeasureFieldById = new LinkedHashMap<>();
    /** code -> 已存在指标字段，供重复跳过时写入 fieldMapping */
    private final Map<String, JSONObject> existingMeasureFieldByCode = new LinkedHashMap<>();

    /**
     * 记录一条老/新 field id 映射。
     *
     * @param oldFieldId         老指标 field id
     * @param oldFieldCode       老指标 code
     * @param oldFieldTitle      老指标 title
     * @param newFieldId         新指标 field id
     * @param newFieldCode       新指标 code
     * @param newFieldTitle      新指标 title
     * @param sourceMetricCode   映射表 source_metric_code，计算指标可空
     * @param targetMetricCode   映射表 target_metric_code，计算指标可空
     * @param targetBusinessline 本条新指标对应的目标业务线
     */
    public void addFieldMapping(String oldFieldId, String oldFieldCode,
                                String oldFieldTitle,
                                String newFieldId, String newFieldCode,
                                String newFieldTitle,
                                String sourceMetricCode, String targetMetricCode,
                                String targetBusinessline) {
        if (StrUtil.isEmpty(oldFieldId) || StrUtil.isEmpty(newFieldId)) {
            return;
        }
        MetricExpansionFieldMappingEntity mapping = new MetricExpansionFieldMappingEntity();
        mapping.setOldFieldId(oldFieldId);
        mapping.setOldFieldCode(oldFieldCode);
        mapping.setOldFieldTitle(oldFieldTitle);
        mapping.setNewFieldId(newFieldId);
        mapping.setNewFieldCode(newFieldCode);
        mapping.setNewFieldTitle(newFieldTitle);
        mapping.setSourceMetricCode(sourceMetricCode);
        mapping.setTargetMetricCode(targetMetricCode);
        mapping.setTargetBusinessline(targetBusinessline);
        fieldMappings.add(mapping);
    }

    /**
     * 通过 old field id 查找本次膨胀写入的全部 new field id（1→N 时多行）。
     *
     * @param oldFieldId 膨胀前 field id
     * @return 新 field id 列表；无映射时返回空列表
     */
    public List<String> resolveNewFieldIdsByOldFieldId(String oldFieldId) {
        return resolveNewFieldIdsByOldFieldRef(oldFieldId);
    }

    /**
     * 通过 old field id 或 old field code 查找本次膨胀写入的全部 new field id（1→N 时多行）。
     *
     * @param oldFieldRef 膨胀前 field id 或 code
     * @return 新 field id 列表；无映射时返回空列表
     */
    public List<String> resolveNewFieldIdsByOldFieldRef(String oldFieldRef) {
        List<String> newFieldIds = new ArrayList<>();
        if (StrUtil.isEmpty(oldFieldRef) || CollUtil.isEmpty(fieldMappings)) {
            return newFieldIds;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (MetricExpansionFieldMappingEntity mapping : fieldMappings) {
            if (mapping == null) {
                continue;
            }
            if (!oldFieldRef.equals(mapping.getOldFieldId())
                    && !oldFieldRef.equals(mapping.getOldFieldCode())) {
                continue;
            }
            if (StrUtil.isNotEmpty(mapping.getNewFieldId()) && seen.add(mapping.getNewFieldId())) {
                newFieldIds.add(mapping.getNewFieldId());
            }
        }
        return newFieldIds;
    }

    /**
     * 判断指标编码是否属于本次待膨胀的 source。
     *
     * @param code 指标编码，允许为空
     * @return true 表示需要按映射表展开为 N 个 target
     */
    public boolean isExpandableSourceCode(String code) {
        return code != null && activeSourceCodes.contains(code);
    }

    /**
     * 通过 fieldId 反查是否为可膨胀 source。
     * analysis / sort 等位置通常只存 id，需要先映射到 code 再判断。
     *
     * @param fieldId 字段 id
     * @return 可膨胀时返回 source code，否则返回 null
     */
    public String resolveExpandableSourceCodeById(String fieldId) {
        if (fieldId == null) {
            return null;
        }
        String normalizedFieldId = fieldId.startsWith("lod:")
                ? fieldId.substring("lod:".length()) : fieldId;
        String code = fieldIdToCode.get(fieldId);
        if (!isExpandableSourceCode(code)) {
            code = fieldIdToCode.get(normalizedFieldId);
        }
        if (isExpandableSourceCode(code)) {
            return code;
        }
        String baseFieldId = MetricExpansionMetaUtil.stripAggExpressionSuffix(normalizedFieldId);
        if (!normalizedFieldId.equals(baseFieldId)) {
            code = fieldIdToCode.get(baseFieldId);
            if (isExpandableSourceCode(code)) {
                return code;
            }
        }
        code = MetricExpansionMetaUtil.resolveMeasureCodeByMeasureId(normalizedFieldId);
        if (isExpandableSourceCode(code)) {
            return code;
        }
        if (!normalizedFieldId.equals(baseFieldId)) {
            code = MetricExpansionMetaUtil.resolveMeasureCodeByMeasureId(baseFieldId);
            if (isExpandableSourceCode(code)) {
                return code;
            }
        }
        return null;
    }

    /**
     * 通过 measureId 反查可膨胀 source code。
     * customCfg 非空时，元数据失效则回退 expressionIdMapping（LOD / lod_calc 场景）。
     *
     * @param measureId 字段 id 或 lodConfig.measureId
     * @param customCfg 含 expressionIdMapping 的配置，可为 null
     * @return 可膨胀的 source code；否则 null
     */
    public String resolveExpandableSourceCodeByMeasureId(String measureId, JSONObject customCfg) {
        if (StrUtil.isEmpty(measureId)) {
            return null;
        }
        String sourceCode = resolveExpandableSourceCodeById(measureId);
        if (StrUtil.isNotEmpty(sourceCode)) {
            return sourceCode;
        }
        String baseMeasureId = MetricExpansionMetaUtil.stripAggExpressionSuffix(measureId);
        if (!measureId.equals(baseMeasureId)) {
            sourceCode = resolveExpandableSourceCodeById(baseMeasureId);
            if (StrUtil.isNotEmpty(sourceCode)) {
                return sourceCode;
            }
        }
        if (customCfg == null) {
            return null;
        }
        String underlyingCode = MetricExpansionMetaUtil.resolveLodUnderlyingSourceCode(customCfg, measureId);
        if (StrUtil.isEmpty(underlyingCode)) {
            return null;
        }
        if (isExpandableSourceCode(underlyingCode)) {
            return underlyingCode;
        }
        String baseCode = MetricExpansionMetaUtil.stripAggExpressionSuffix(underlyingCode);
        if (isExpandableSourceCode(baseCode)) {
            return baseCode;
        }
        return null;
    }

    /**
     * 返回本视图实际膨胀的 source 编码副本，用于接口响应。
     *
     * @return source 编码列表
     */
    public List<String> listExpandedSourceCodes() {
        return new ArrayList<>(activeSourceCodes);
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getTplId() {
        return tplId;
    }

    public void setTplId(String tplId) {
        this.tplId = tplId;
    }

    public String getCfgId() {
        return cfgId;
    }

    public void setCfgId(String cfgId) {
        this.cfgId = cfgId;
    }

    public JSONObject getTplConfigJson() {
        return tplConfigJson;
    }

    public void setTplConfigJson(JSONObject tplConfigJson) {
        this.tplConfigJson = tplConfigJson;
    }

    public Set<String> getAllMappingSourceCodes() {
        return allMappingSourceCodes;
    }

    public void setAllMappingSourceCodes(Set<String> allMappingSourceCodes) {
        this.allMappingSourceCodes = allMappingSourceCodes;
    }

    public Set<String> getActiveSourceCodes() {
        return activeSourceCodes;
    }

    public void setActiveSourceCodes(Set<String> activeSourceCodes) {
        this.activeSourceCodes = activeSourceCodes;
    }

    public Map<String, List<MetricExpansionTarget>> getTargetsBySourceCode() {
        return targetsBySourceCode;
    }

    public void setTargetsBySourceCode(Map<String, List<MetricExpansionTarget>> targetsBySourceCode) {
        this.targetsBySourceCode = targetsBySourceCode;
    }

    public Map<String, String> getFieldIdToCode() {
        return fieldIdToCode;
    }

    public void setFieldIdToCode(Map<String, String> fieldIdToCode) {
        this.fieldIdToCode = fieldIdToCode;
    }

    public Set<String> getGeneratedCalcFieldCodes() {
        return generatedCalcFieldCodes;
    }

    public void setGeneratedCalcFieldCodes(Set<String> generatedCalcFieldCodes) {
        this.generatedCalcFieldCodes = generatedCalcFieldCodes;
    }

    public Set<String> getMeasureExpandedSourceCodes() {
        return measureExpandedSourceCodes;
    }

    public void setMeasureExpandedSourceCodes(Set<String> measureExpandedSourceCodes) {
        this.measureExpandedSourceCodes = measureExpandedSourceCodes;
    }

    public Map<String, List<String>> getCalcFieldIdToVariantIds() {
        return calcFieldIdToVariantIds;
    }

    public void setCalcFieldIdToVariantIds(Map<String, List<String>> calcFieldIdToVariantIds) {
        this.calcFieldIdToVariantIds = calcFieldIdToVariantIds;
    }

    public Map<String, List<String>> getCalcFieldCodeToVariantCodes() {
        return calcFieldCodeToVariantCodes;
    }

    public void setCalcFieldCodeToVariantCodes(Map<String, List<String>> calcFieldCodeToVariantCodes) {
        this.calcFieldCodeToVariantCodes = calcFieldCodeToVariantCodes;
    }

    public Map<String, List<String>> getCalcFieldCodeToVariantIds() {
        return calcFieldCodeToVariantIds;
    }

    public void setCalcFieldCodeToVariantIds(Map<String, List<String>> calcFieldCodeToVariantIds) {
        this.calcFieldCodeToVariantIds = calcFieldCodeToVariantIds;
    }

    public Map<String, JSONObject> getCalcFieldVariantById() {
        return calcFieldVariantById;
    }

    public void setCalcFieldVariantById(Map<String, JSONObject> calcFieldVariantById) {
        this.calcFieldVariantById = calcFieldVariantById;
    }

    public List<MetricExpansionFieldMappingEntity> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<MetricExpansionFieldMappingEntity> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public String getSourceBusinessline() {
        return sourceBusinessline;
    }

    public void setSourceBusinessline(String sourceBusinessline) {
        this.sourceBusinessline = sourceBusinessline;
    }

    /**
     * 是否保留原始 source 数据（指标、filter 源业务线等），仅追加膨胀结果。
     *
     * @return execute 未指定 targetBusinessline 时为 true
     */
    public boolean isPreserveSourceData() {
        return preserveSourceData;
    }

    public void setPreserveSourceData(boolean preserveSourceData) {
        this.preserveSourceData = preserveSourceData;
    }

    /**
     * 索引膨胀开始前 tplConfig 中已存在的指标字段（按 id / code）。
     *
     * @param field measures 或 filter 中的指标字段
     */
    public void indexExistingMeasureField(JSONObject field) {
        if (field == null) {
            return;
        }
        String id = field.getString("id");
        String code = field.getString("code");
        if (StrUtil.isNotEmpty(id)) {
            existingMeasureIds.add(id);
            existingMeasureFieldById.putIfAbsent(id, field);
        }
        if (StrUtil.isNotEmpty(code)) {
            existingMeasureCodes.add(code);
            existingMeasureFieldByCode.putIfAbsent(code, field);
        }
    }

    /**
     * 判断膨胀 target 的 id 或 code 是否已在配置中存在。
     *
     * @param fieldId   目标指标 id
     * @param fieldCode 目标指标 code
     * @return 已存在时返回 true，不再追加新字段
     */
    public boolean isExpandedMeasureAlreadyInConfig(String fieldId, String fieldCode) {
        return (StrUtil.isNotEmpty(fieldId) && existingMeasureIds.contains(fieldId))
                || (StrUtil.isNotEmpty(fieldCode) && existingMeasureCodes.contains(fieldCode));
    }

    /**
     * 解析配置中已存在的指标字段，优先按 id 匹配，其次按 code。
     *
     * @param fieldId   目标指标 id
     * @param fieldCode 目标指标 code
     * @return 已存在字段；未命中时返回 null
     */
    public JSONObject resolveExistingMeasureField(String fieldId, String fieldCode) {
        if (StrUtil.isNotEmpty(fieldId) && existingMeasureFieldById.containsKey(fieldId)) {
            return existingMeasureFieldById.get(fieldId);
        }
        if (StrUtil.isNotEmpty(fieldCode) && existingMeasureFieldByCode.containsKey(fieldCode)) {
            return existingMeasureFieldByCode.get(fieldCode);
        }
        return null;
    }

    /**
     * 将本次新写入的膨胀指标登记到索引，避免同批后续 target 重复追加。
     *
     * @param field 新追加的指标字段
     */
    public void registerExpandedMeasureField(JSONObject field) {
        indexExistingMeasureField(field);
    }

    /**
     * 在 expandTplConfig 写入新指标前，快照 result.measures 中已有 field id。
     * 仅用于私域 execute 可见性兜底，不包含 filter 区域字段。
     */
    public void captureInitialMeasureIdsAtExpansionStart() {
        initialMeasureIdsAtExpansionStart.clear();
        if (tplConfigJson == null) {
            return;
        }
        JSONObject result = tplConfigJson.getJSONObject("result");
        if (result == null) {
            return;
        }
        JSONArray measures = result.getJSONArray("measures");
        if (CollUtil.isEmpty(measures)) {
            return;
        }
        for (int i = 0; i < measures.size(); i++) {
            JSONObject field = measures.getJSONObject(i);
            if (field == null) {
                continue;
            }
            String fieldId = field.getString("id");
            if (StrUtil.isNotEmpty(fieldId)) {
                initialMeasureIdsAtExpansionStart.add(fieldId);
            }
        }
    }

    /**
     * 判断 field id 是否为膨胀开始前 result.measures 中已存在的指标。
     *
     * @param fieldId 指标 field id
     * @return 膨胀前已存在时返回 true
     */
    public boolean isInitialMeasureAtExpansionStart(String fieldId) {
        return StrUtil.isNotEmpty(fieldId) && initialMeasureIdsAtExpansionStart.contains(fieldId);
    }
}
