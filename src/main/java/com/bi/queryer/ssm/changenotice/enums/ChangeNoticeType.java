package com.bi.queryer.ssm.changenotice.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 变更通知类型枚举，与 mgp_change_notice.change_type 取值一致
 */
public enum ChangeNoticeType {

    /** 替换/下线 */
    REPLACE_OFFLINE("REPLACE_OFFLINE", "替换/下线"),
    /** 指标口径变更 */
    METRIC_CALIBER("METRIC_CALIBER", "指标口径变更"),
    /** 维度内容变更 */
    DIM_CONTENT("DIM_CONTENT", "维度内容变更"),
    /** 使用约束（需返回必须同查信息） */
    USAGE_CONSTRAINT("USAGE_CONSTRAINT", "使用约束"),
    /** 数据回刷 */
    DATA_BACKFILL("DATA_BACKFILL", "数据回刷");

    /** 类型编码，与库表 change_type 一致 */
    private final String code;
    /** 类型中文描述 */
    private final String desc;

    ChangeNoticeType(String code, String desc) {
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
     * 按编码解析类型；无法识别时返回 null（匹配流程会跳过该通知）
     * @param code 变更类型编码
     * @return 枚举实例，未知或空串返回 null
     */
    public static ChangeNoticeType fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (ChangeNoticeType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return null;
    }
}
