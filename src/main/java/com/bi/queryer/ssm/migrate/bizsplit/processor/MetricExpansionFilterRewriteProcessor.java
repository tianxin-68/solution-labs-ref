package com.bi.queryer.ssm.migrate.bizsplit.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.enums.MetricExpansionFilterExpandRule;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tplConfig.filter 业务线筛选值改写处理器。
 *
 * 在 MetricExpansionService.executeOne 中，所有视图都会执行 filter 改写（与是否指标膨胀无关）；
 * 具体走哪种模式由 datasetId 是否在膨胀白名单内决定，由 Service 层选择调用入口。
 *
 * 目标字段：filter 中 code 为 businessline、CGW、BQO 的条目（大小写不敏感）。
 * 仅处理 values 数组；values[].id 为空的条目跳过。不区分 filterValueType（include / exclude 同样处理）。
 *
 * 模式一 — 非指标膨胀数据集（rewriteFilters）：
 * 按 MetricExpansionFilterExpandRule 固定规则追加 values。例如「保养」追加「保养油液」「保养配件」，
 * 「改装超市」追加「电瓶车」等；原 value 保留，追加项去重，顺序为「先原值、后追加」。
 *
 * 模式二 — 指标膨胀数据集（rewriteFiltersByTargets）：
 * 仅当 values[].id 等于入参 sourceBusinessline 时，用本视图映射收窄后的 targetBusinessline 列表
 * 替换或追加该条 value。preserveSourceBusinessline=true 时保留 sourceBusinessline 原值并追加 target；
 * 否则整段替换为 target（不保留 sourceBusinessline 原值）。例如 owner 映射仅「保养油液」时，
 * filter 中 id=保养 会被替换为 id=保养油液，而非「保养,保养油液」并存。
 * 其他 id 的 value 原样保留。改写后同步重建 valuesTitle（逗号拼接 title）。
 */
@Component
public class MetricExpansionFilterRewriteProcessor {

    /**
     * 需要做筛选值改写的 filter 字段 code（统一转小写后比较）。
     * 对应前端 tplConfig.filter 中业务线、CGW、BQO 三类维度筛选。
     */
    private static final Set<String> FILTER_FIELD_CODES = new HashSet<>();

    /** 映射 target / 内部业务线名 → filter 实际写入值（id/title） */
    private static final Map<String, String> FILTER_VALUE_WRITE_ALIAS;

    static {
        FILTER_FIELD_CODES.add("businessline");
        FILTER_FIELD_CODES.add("cgw");
        FILTER_FIELD_CODES.add("bqo");

        Map<String, String> aliasMap = new LinkedHashMap<>();
        aliasMap.put("超市改装", "改装升级与车品超市");
        FILTER_VALUE_WRITE_ALIAS = Collections.unmodifiableMap(aliasMap);
    }

    /**
     * 非指标膨胀数据集入口：按固定规则扩展 filter.values。
     *
     * 调用方：MetricExpansionService.executeOne 中 datasetId 不在膨胀白名单时。
     *
     * @param tplConfigJson tplConfig 根 JSON，就地修改 filter 数组
     * @return true 表示至少一条 filter 的 values 被扩展或替换
     */
    public boolean rewriteFilters(JSONObject tplConfigJson) {
        return rewriteFiltersInternal(tplConfigJson, null, null, false);
    }

    /**
     * 指标膨胀数据集入口：将 filter 中等于 sourceBusinessline 的 value 替换为映射 target。
     *
     * 调用方：MetricExpansionService.executeOne 中 datasetId 在膨胀白名单且已按 owner 收窄映射后。
     * sourceBusinessline 为空时不做改写。
     *
     * @param tplConfigJson       tplConfig 根 JSON，就地修改 filter 数组
     * @param sourceBusinessline  入参源业务线（如「保养」），与 values[].id 精确匹配
     * @param targetBusinesslines 本视图映射收窄后的目标业务线列表（去重、保序，不含源业务线）
     * @return true 表示至少一条 filter 的 values 被扩展或替换
     */
    public boolean rewriteFiltersByTargets(JSONObject tplConfigJson, String sourceBusinessline,
                                         List<String> targetBusinesslines) {
        return rewriteFiltersByTargets(tplConfigJson, sourceBusinessline, targetBusinesslines, false);
    }

    /**
     * 指标膨胀数据集入口：将 filter 中等于 sourceBusinessline 的 value 替换或追加为映射 target。
     *
     * @param tplConfigJson            tplConfig 根 JSON，就地修改 filter 数组
     * @param sourceBusinessline       入参源业务线（如「保养」），与 values[].id 精确匹配
     * @param targetBusinesslines      本视图映射收窄后的目标业务线列表（去重、保序，不含源业务线）
     * @param preserveSourceBusinessline true 时保留 sourceBusinessline 原值并追加 target；false 时替换不保留
     * @return true 表示至少一条 filter 的 values 被扩展或替换
     */
    public boolean rewriteFiltersByTargets(JSONObject tplConfigJson, String sourceBusinessline,
                                         List<String> targetBusinesslines,
                                         boolean preserveSourceBusinessline) {
        if (StrUtil.isEmpty(sourceBusinessline)) {
            return false;
        }
        return rewriteFiltersInternal(tplConfigJson, sourceBusinessline, targetBusinesslines,
                preserveSourceBusinessline);
    }

    /**
     * 遍历 tplConfig.filter，对每条业务线类字段尝试改写 values。
     *
     * @param tplConfigJson       tplConfig 根对象
     * @param sourceBusinessline  膨胀模式源业务线；固定规则模式为 null
     * @param targetBusinesslines 膨胀模式目标业务线；固定规则模式为 null
     * @param preserveSourceBusinessline 膨胀模式是否保留 sourceBusinessline 原值
     * @return 是否存在至少一条 filter 被修改
     */
    private boolean rewriteFiltersInternal(JSONObject tplConfigJson, String sourceBusinessline,
                                           List<String> targetBusinesslines,
                                           boolean preserveSourceBusinessline) {
        if (tplConfigJson == null) {
            return false;
        }
        JSONArray filter = tplConfigJson.getJSONArray("filter");
        if (CollUtil.isEmpty(filter)) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < filter.size(); i++) {
            JSONObject field = filter.getJSONObject(i);
            if (field != null && rewriteFilterField(field, sourceBusinessline, targetBusinesslines,
                    preserveSourceBusinessline)) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 改写单条 filter 字段的 values 与 valuesTitle。
     *
     * 前置条件：code 属于 FILTER_FIELD_CODES，且 values 非空。
     * 无实际扩展时返回 false，不修改 field。
     *
     * @param field               单条 filter JSON
     * @param sourceBusinessline  膨胀模式源业务线；固定规则模式为 null
     * @param targetBusinesslines 膨胀模式目标列表；固定规则模式为 null
     * @param preserveSourceBusinessline 膨胀模式是否保留 sourceBusinessline 原值
     * @return true 表示本条 filter 已被改写
     */
    private boolean rewriteFilterField(JSONObject field, String sourceBusinessline,
                                       List<String> targetBusinesslines,
                                       boolean preserveSourceBusinessline) {
        if (!isTargetFilterField(field)) {
            return false;
        }
        JSONArray values = field.getJSONArray("values");
        if (CollUtil.isEmpty(values)) {
            return false;
        }

        JSONArray expandedValues = expandFilterValues(values, sourceBusinessline, targetBusinesslines,
                preserveSourceBusinessline);
        if (expandedValues == null) {
            return false;
        }

        field.put("values", expandedValues);
        field.put("valuesTitle", buildValuesTitle(expandedValues));
        return true;
    }

    /**
     * 判断 filter 字段是否属于业务线类筛选（businessline / CGW / BQO）。
     *
     * @param field filter 字段 JSON
     * @return code 命中 FILTER_FIELD_CODES 时返回 true
     */
    private boolean isTargetFilterField(JSONObject field) {
        String code = field.getString("code");
        if (StrUtil.isEmpty(code)) {
            return false;
        }
        return FILTER_FIELD_CODES.contains(code.toLowerCase(Locale.ROOT));
    }

    /**
     * 扩展或替换 filter.values 数组的核心逻辑。
     *
     * 通过 targetBusinesslines 是否为 null 区分两种模式：
     * 膨胀模式（非 null）：value.id 等于 sourceBusinessline 时，用 target 列表替换或追加（由 preserveSourceBusinessline 决定）；
     * 其余 value 原样保留（按 id 去重）。
     * 固定规则模式（null）：每条 value 先原样保留，再按 MetricExpansionFilterExpandRule 追加缺失 target。
     *
     * id 为空的 value 跳过。新建 value 时克隆 values 中首个 id 非空条目作为模板，
     * 写入 id/title 及 realId、levelFieldName、levelFieldId 等前端所需空字段。
     *
     * @param values              原 filter.values
     * @param sourceBusinessline  膨胀模式源业务线；固定规则模式为 null
     * @param targetBusinesslines 膨胀模式目标业务线；固定规则模式为 null
     * @param preserveSourceBusinessline 膨胀模式是否保留 sourceBusinessline 原值
     * @return 扩展后的新数组；无任何变更时返回 null（调用方据此判断是否写回 field）
     */
    private JSONArray expandFilterValues(JSONArray values, String sourceBusinessline,
                                       List<String> targetBusinesslines,
                                       boolean preserveSourceBusinessline) {
        JSONArray expanded = new JSONArray();
        Set<String> seenIds = new LinkedHashSet<>();
        JSONObject valueTemplate = resolveValueTemplate(values);
        boolean changed = false;
        boolean expansionMode = targetBusinesslines != null;

        for (int i = 0; i < values.size(); i++) {
            JSONObject value = values.getJSONObject(i);
            if (value == null) {
                continue;
            }
            String valueId = value.getString("id");
            if (StrUtil.isEmpty(valueId)) {
                continue;
            }

            if (expansionMode && sourceBusinessline.equals(valueId)) {
                List<String> replacementValues = resolveExpansionReplacementValues(targetBusinesslines);
                if (preserveSourceBusinessline) {
                    appendValueIfAbsent(expanded, seenIds, value);
                }
                if (CollUtil.isEmpty(replacementValues)) {
                    changed = true;
                    continue;
                }
                for (String targetValue : replacementValues) {
                    if (seenIds.contains(targetValue)) {
                        continue;
                    }
                    expanded.add(createFilterValue(valueTemplate, targetValue));
                    seenIds.add(targetValue);
                }
                changed = true;
                continue;
            }

            // 非源业务线 value：原样保留
            appendValueIfAbsent(expanded, seenIds, value);

            if (expansionMode) {
                continue;
            }

            // 固定规则模式：在保留原 value 基础上追加规则定义的 target
            List<String> valuesToAppend = resolveLegacyExpandTargetValues(valueId);
            if (CollUtil.isEmpty(valuesToAppend)) {
                continue;
            }

            for (String targetValue : valuesToAppend) {
                String writeValue = resolveFilterWriteValue(targetValue);
                if (seenIds.contains(writeValue)) {
                    continue;
                }
                expanded.add(createFilterValue(valueTemplate, writeValue));
                seenIds.add(writeValue);
                changed = true;
            }
        }

        return changed ? expanded : null;
    }

    /**
     * 膨胀模式：整理 targetBusinessline 列表，去重保序。
     *
     * 返回列表直接作为替换 sourceBusinessline 后的新 value id，不含源业务线本身。
     *
     * @param targetBusinesslines 映射表收窄后的目标业务线
     * @return 非 null 列表，可能为空（表示映射无 target 时删除源 value）
     */
    private List<String> resolveExpansionReplacementValues(List<String> targetBusinesslines) {
        if (CollUtil.isEmpty(targetBusinesslines)) {
            return CollUtil.newArrayList();
        }
        List<String> result = new ArrayList<>();
        for (String target : targetBusinesslines) {
            String writeValue = resolveFilterWriteValue(target);
            if (StrUtil.isNotEmpty(writeValue) && !result.contains(writeValue)) {
                result.add(writeValue);
            }
        }
        return result;
    }

    /**
     * 将映射/规则中的业务线值转为 filter 实际写入的 id/title。
     * 例如 owner 收窄 target「超市改装」→ filter 写入「改装升级与车品超市」。
     *
     * @param valueId 原始业务线值
     * @return 写入 filter 的值；无别名映射时原样返回
     */
    private String resolveFilterWriteValue(String valueId) {
        if (StrUtil.isEmpty(valueId)) {
            return valueId;
        }
        return FILTER_VALUE_WRITE_ALIAS.getOrDefault(valueId, valueId);
    }

    /**
     * 固定规则模式：按 value.id 查找 MetricExpansionFilterExpandRule，返回需追加的 target 列表。
     *
     * 未命中任何 trigger 时返回空列表，表示该 value 无需追加。
     *
     * @param valueId filter.values[].id
     * @return 规则定义的 target 值列表；无规则时为空列表
     */
    private List<String> resolveLegacyExpandTargetValues(String valueId) {
        MetricExpansionFilterExpandRule rule = MetricExpansionFilterExpandRule.findByTriggerValue(valueId);
        if (rule == null) {
            return CollUtil.newArrayList();
        }
        return rule.getTargetValues();
    }

    /**
     * 从 values 中取首个 id 非空条目，作为 createFilterValue 的 JSON 克隆模板。
     *
     * 保证新建 value 与现有 value 结构一致（filterShowType 等字段随模板继承）。
     *
     * @param values 原 filter.values
     * @return 模板对象；全部 id 为空时返回 null，createFilterValue 将构造最小 JSON
     */
    private JSONObject resolveValueTemplate(JSONArray values) {
        for (int i = 0; i < values.size(); i++) {
            JSONObject value = values.getJSONObject(i);
            if (value != null && StrUtil.isNotEmpty(value.getString("id"))) {
                return value;
            }
        }
        return null;
    }

    /**
     * 将已有 value 追加到结果数组，按 id 去重。
     *
     * @param expanded 输出数组
     * @param seenIds  已出现的 id 集合
     * @param value    待追加的 value 对象
     */
    private void appendValueIfAbsent(JSONArray expanded, Set<String> seenIds, JSONObject value) {
        JSONObject valueToAppend = normalizeExistingFilterValue(value);
        String valueId = valueToAppend.getString("id");
        if (StrUtil.isNotEmpty(valueId) && seenIds.contains(valueId)) {
            return;
        }
        expanded.add(valueToAppend);
        if (StrUtil.isNotEmpty(valueId)) {
            seenIds.add(valueId);
        }
    }

    /**
     * 保留原 filter value 时，同步将需别名的 id/title 转为实际写入值。
     *
     * @param value 原 filter value
     * @return 无需改写时返回原对象；需改写时返回克隆并更新 id/title 的新对象
     */
    private JSONObject normalizeExistingFilterValue(JSONObject value) {
        if (value == null) {
            return new JSONObject();
        }
        String valueId = value.getString("id");
        String writeValue = resolveFilterWriteValue(valueId);
        if (StrUtil.isEmpty(valueId) || writeValue.equals(valueId)) {
            return value;
        }
        JSONObject copy = JSONObject.parseObject(value.toJSONString());
        copy.put("id", writeValue);
        copy.put("title", writeValue);
        return copy;
    }

    /**
     * 基于模板克隆一条新的 filter value，设置 id 与 title。
     *
     * title 与 id 相同（业务线展示名即 id 文本）。入参 valueId 会先经 {@link #resolveFilterWriteValue} 转换。
     * 补全 realId、levelFieldName、levelFieldId 空字段，与前端 FilterField 结构保持一致。
     *
     * @param template 克隆模板，可为 null
     * @param valueId  新业务线 value 的 id（可为映射别名，如「超市改装」）
     * @return 新 value JSON
     */
    private JSONObject createFilterValue(JSONObject template, String valueId) {
        String writeValue = resolveFilterWriteValue(valueId);
        JSONObject copy = template == null
                ? new JSONObject()
                : JSONObject.parseObject(template.toJSONString());
        copy.put("id", writeValue);
        copy.put("title", writeValue);
        if (!copy.containsKey("realId")) {
            copy.put("realId", "");
        }
        if (!copy.containsKey("levelFieldName")) {
            copy.put("levelFieldName", "");
        }
        if (!copy.containsKey("levelFieldId")) {
            copy.put("levelFieldId", "");
        }
        return copy;
    }

    /**
     * 根据扩展后的 values 重建 filter.valuesTitle。
     *
     * 取每条 value 的 title，为空则回退为 id，再用英文逗号拼接。与前端筛选器展示文案一致。
     *
     * @param values 扩展后的 filter.values
     * @return valuesTitle 字符串
     */
    private String buildValuesTitle(JSONArray values) {
        List<String> titles = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            JSONObject value = values.getJSONObject(i);
            if (value == null) {
                continue;
            }
            String title = value.getString("title");
            if (StrUtil.isEmpty(title)) {
                title = value.getString("id");
            }
            if (StrUtil.isNotEmpty(title)) {
                titles.add(title);
            }
        }
        return titles.stream().collect(Collectors.joining(","));
    }
}
