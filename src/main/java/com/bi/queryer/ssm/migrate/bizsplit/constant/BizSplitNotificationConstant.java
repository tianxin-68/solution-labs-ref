package com.bi.queryer.ssm.migrate.bizsplit.constant;

/**
 * 业务线拆分通知相关常量。
 */
public final class BizSplitNotificationConstant {

    /**
     * 视图失效提醒总开关配置项。
     * 配置项 ssm.bizsplit.notification.reminder.enable，关闭时直接返回 affected=false。
     */
    public static final String REMINDER_ENABLE_KEY = "ssm.bizsplit.notification.reminder.enable";

    /** 视图失效提醒开关默认值：开启 */
    public static final String REMINDER_ENABLE_DEFAULT = "false";

    private BizSplitNotificationConstant() {
    }
}
