package com.bi.queryer.ssm.migrate.bizsplit.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 非映射组织用户的业务线权限扩展规则。
 *
 * 当用户已有源业务线权限、且不属于 BizSplitDeptBusinessLineAuth 预置组织时，
 * 按本规则补充拆分后的目标业务线。
 */
public enum BizSplitBusinessLineAuthExpandRule {

    /** 已有「保养」→ 补充保养油液、保养配件 */
    MAINTENANCE("保养", "保养油液", "保养配件"),

    /** 已有「改装超市」→ 补充改装升级与车品超市、电子改装、电瓶车 */
    MODIFY_MARKET("改装超市", "改装升级与车品超市", "电子改装", "电瓶车");

    private static final Map<String, BizSplitBusinessLineAuthExpandRule> TRIGGER_VALUE_MAP;

    static {
        Map<String, BizSplitBusinessLineAuthExpandRule> map = new LinkedHashMap<>();
        for (BizSplitBusinessLineAuthExpandRule rule : values()) {
            map.put(rule.sourceBusinessLine, rule);
        }
        TRIGGER_VALUE_MAP = Collections.unmodifiableMap(map);
    }

    /** 触发的源业务线（用户已有权限） */
    private final String sourceBusinessLine;

    /** 需补充的目标业务线（不含源业务线本身） */
    private final List<String> targetBusinessLines;

    BizSplitBusinessLineAuthExpandRule(String sourceBusinessLine, String... targetBusinessLines) {
        this.sourceBusinessLine = sourceBusinessLine;
        this.targetBusinessLines = Collections.unmodifiableList(Arrays.asList(targetBusinessLines));
    }

    public String getSourceBusinessLine() {
        return sourceBusinessLine;
    }

    public List<String> getTargetBusinessLines() {
        return targetBusinessLines;
    }

    /**
     * 按已有业务线查找扩展规则。
     *
     * @param businessLine 用户已有业务线
     * @return 命中规则；未命中返回 null
     */
    public static BizSplitBusinessLineAuthExpandRule findBySourceBusinessLine(String businessLine) {
        if (businessLine == null) {
            return null;
        }
        return TRIGGER_VALUE_MAP.get(businessLine);
    }
}
