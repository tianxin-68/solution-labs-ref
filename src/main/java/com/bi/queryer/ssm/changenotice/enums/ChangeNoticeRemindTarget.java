package com.bi.queryer.ssm.changenotice.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 替换/下线提醒目标枚举，与 mgp_change_notice.remind_targets 中单项取值一致。
 * remind_targets 可为逗号分隔多选，如 REMIND_OLD,REMIND_NEW。
 */
public enum ChangeNoticeRemindTarget {

    /** 提醒旧对象（催迁移） */
    REMIND_OLD("REMIND_OLD", "提醒旧对象"),
    /** 提醒新对象（告知口径变） */
    REMIND_NEW("REMIND_NEW", "提醒新对象");

    /** 目标编码，与库表 remind_targets 单项一致 */
    private final String code;
    /** 目标中文描述 */
    private final String desc;

    ChangeNoticeRemindTarget(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取目标编码
     * @return 编码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取目标中文描述
     * @return 中文描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 按编码解析提醒目标；无法识别时返回 null
     * @param code 提醒目标编码
     * @return 枚举实例，未知或空串返回 null
     */
    public static ChangeNoticeRemindTarget fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (ChangeNoticeRemindTarget target : values()) {
            if (target.code.equalsIgnoreCase(code.trim())) {
                return target;
            }
        }
        return null;
    }
}
