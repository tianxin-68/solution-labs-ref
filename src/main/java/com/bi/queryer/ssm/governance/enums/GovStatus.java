package com.bi.queryer.ssm.governance.enums;

/**
 * 治理任务状态机
 *
 * 视图：pending → noticed → offline → purged；noticed → exempted；offline → rolled_back
 * 指标：pending → noticed → exempted（SSM 不执行下线）
 */
public enum GovStatus {

    PENDING("pending", "-"),
    NOTICED("noticed", "已通知"),
    EXEMPTED("exempted", "延迟30天"),
    OFFLINE("offline", "已下线"),
    ROLLED_BACK("rolled_back", "已回滚"),
    PURGED("purged", "已删除");

    private final String code;

    private final String desc;

    GovStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static GovStatus get(String code) {
        for (GovStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
