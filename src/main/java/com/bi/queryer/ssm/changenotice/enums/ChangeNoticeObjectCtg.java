package com.bi.queryer.ssm.changenotice.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 变更通知对象角色枚举，与 mgp_change_notice_object.object_ctg 取值一致。
 * 表示对象在通知配置中的角色，用于匹配命中与出参组装。
 */
public enum ChangeNoticeObjectCtg {

    /** 变更对象（多数类型的核心命中对象） */
    CHANGE("CHANGE", "变更对象"),
    /** 替换映射（仅 REPLACE_OFFLINE：旧对象 → 新对象） */
    MAPPING("MAPPING", "替换映射"),
    /** 限定受影响指标（仅维度内容变更可选配置） */
    AFFECTED_METRIC("AFFECTED_METRIC", "限定受影响指标"),
    /** 回刷提醒范围字段（DATA_BACKFILL 命中判定用） */
    REMIND_FIELD("REMIND_FIELD", "回刷提醒范围字段"),
    /** 必须同查字段（USAGE_CONSTRAINT 约束与出参） */
    MUST_QUERY("MUST_QUERY", "必须同查字段");

    /** 角色编码，与库表 object_ctg 一致 */
    private final String code;
    /** 角色中文描述 */
    private final String desc;

    ChangeNoticeObjectCtg(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取角色编码
     * @return 编码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取角色中文描述
     * @return 中文描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 按编码解析对象角色；无法识别时返回 null
     * @param code 对象角色编码
     * @return 枚举实例，未知或空串返回 null
     */
    public static ChangeNoticeObjectCtg fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (ChangeNoticeObjectCtg ctg : values()) {
            if (ctg.code.equalsIgnoreCase(code.trim())) {
                return ctg;
            }
        }
        return null;
    }
}
