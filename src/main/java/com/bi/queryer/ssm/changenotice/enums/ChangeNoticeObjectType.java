package com.bi.queryer.ssm.changenotice.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 变更通知对象类型枚举，与 mgp_change_notice_object.object_type / rel_object_type 取值一致。
 */
public enum ChangeNoticeObjectType {

    /**
     * 指标组。
     * 替换下线后指标组仍存在时，按查询字段所属目录命中。
     */
    METRIC_GROUP("METRIC_GROUP", "指标组"),
    /**
     * 维度组。
     * 替换下线后维度组仍存在时，按查询字段所属目录命中。
     */
    DIM_GROUP("DIM_GROUP", "维度组"),
    /**
     * 多维模块。
     * 替换下线后模块仍存在时，按查询字段所属目录命中。
     */
    MODULE("MODULE", "多维模块");

    /** 类型编码，与库表 object_type 一致 */
    private final String code;
    /** 类型中文描述 */
    private final String desc;

    ChangeNoticeObjectType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取类型编码
     * @return 编码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取类型中文描述
     * @return 中文描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 按编码解析对象类型；无法识别时返回 null
     * @param code 对象类型编码
     * @return 枚举实例，未知或空串返回 null
     */
    public static ChangeNoticeObjectType fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (ChangeNoticeObjectType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return null;
    }

    /**
     * 是否为目录类对象类型（指标组 / 维度组 / 多维模块）
     * @param objectType 对象类型编码
     * @return true 表示 METRIC_GROUP、DIM_GROUP 或 MODULE
     */
    public static boolean isCtg(String objectType) {
        ChangeNoticeObjectType type = fromCode(objectType);
        return METRIC_GROUP == type || DIM_GROUP == type || MODULE == type;
    }
}
