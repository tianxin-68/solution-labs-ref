package com.bi.queryer.ssm.migrate.bizsplit.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务线筛选值扩展规则枚举。
 *
 * 用于 {@code tplConfig.filter[]} 中字段 code 为 businessline / CGW / BQO 时的 values 扩展。
 * 当某条 value 的 id 等于 {@link #triggerValue} 时，按 {@link #targetValues} 追加缺失项；
 * 已存在的 id 不重复添加，整体保持「先原值、后追加」的顺序。
 *
 * 不区分 filterValueType（include / exclude 均同样处理）。
 */
public enum MetricExpansionFilterExpandRule {

    /**
     * 保养业务线扩展。
     * 触发值「保养」→ 保养、保养油液、保养配件。
     */
    MAINTENANCE("保养", "保养", "保养油液", "保养配件"),

    /**
     * 改装超市业务线扩展。
     * 触发值「改装超市」→ 改装超市、电瓶车、电子改装、改装升级与车品超市。
     */
    MODIFY_MARKET("改装超市", "改装超市", "电瓶车", "电子改装", "改装升级与车品超市");

    /** triggerValue → 规则，供 O(1) 查找 */
    private static final Map<String, MetricExpansionFilterExpandRule> TRIGGER_VALUE_MAP;

    static {
        Map<String, MetricExpansionFilterExpandRule> map = new LinkedHashMap<>();
        for (MetricExpansionFilterExpandRule rule : values()) {
            map.put(rule.triggerValue, rule);
        }
        TRIGGER_VALUE_MAP = Collections.unmodifiableMap(map);
    }

    /** 触发扩展的筛选值 id，须与 filter.values[].id 精确匹配 */
    private final String triggerValue;

    /** 扩展后的完整目标值列表（含 trigger 本身，顺序即追加顺序） */
    private final List<String> targetValues;

    /**
     * @param triggerValue 触发扩展的筛选值 id
     * @param targetValues 扩展后的全部目标值（第一个元素通常与 triggerValue 相同）
     */
    MetricExpansionFilterExpandRule(String triggerValue, String... targetValues) {
        this.triggerValue = triggerValue;
        this.targetValues = Collections.unmodifiableList(Arrays.asList(targetValues));
    }

    public String getTriggerValue() {
        return triggerValue;
    }

    public List<String> getTargetValues() {
        return targetValues;
    }

    /**
     * 按 filter.values[].id 查找对应的扩展规则。
     *
     * @param valueId 筛选值 id
     * @return 命中规则；未命中任意 trigger 时返回 null
     */
    public static MetricExpansionFilterExpandRule findByTriggerValue(String valueId) {
        if (valueId == null) {
            return null;
        }
        return TRIGGER_VALUE_MAP.get(valueId);
    }
}
