package com.bi.queryer.ssm.changenotice.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 变更通知触发范围枚举，与 mgp_change_notice.trigger_scope 取值一致
 */
public enum ChangeNoticeTriggerScope {

    /** 全局触发 */
    GLOBAL("GLOBAL"),
    /** 局部触发（需 local_route_table_names 与当前查询表有交集） */
    LOCAL("LOCAL");

    /** 范围编码，与库表 trigger_scope 一致 */
    private final String code;

    ChangeNoticeTriggerScope(String code) {
        this.code = code;
    }

    /**
     * 获取范围编码
     * @return 编码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 按编码解析触发范围；无法识别时返回 null
     * @param code 触发范围编码
     * @return 枚举实例，未知或空串返回 null
     */
    public static ChangeNoticeTriggerScope fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (ChangeNoticeTriggerScope scope : values()) {
            if (scope.code.equalsIgnoreCase(code.trim())) {
                return scope;
            }
        }
        return null;
    }
}
