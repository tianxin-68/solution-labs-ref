package com.bi.queryer.ssm.governance.enums;

/**
 * 审计日志动作（写入 ssm_governance_audit_log.action）
 */
public class GovAction {

    /** 扫描命中，创建任务 */
    public static final String SCAN_HIT = "扫描命中";

    /** 发送通知 */
    public static final String NOTICE = "发送通知";

    /** 宽限期满自动下线 */
    public static final String AUTO_OFFLINE = "自动下线";

    /** 管理员立即下线（跳过宽限期） */
    public static final String OFFLINE_NOW = "立即下线";

    /** 责任人申请延迟30天 */
    public static final String EXEMPT = "申请延迟30天";

    /** 保护期内回滚恢复 */
    public static final String ROLLBACK = "回滚恢复";

    /** 保护期满物理删除 */
    public static final String PURGE = "物理删除";

    /** 豁免到期重新评估，重入 pending */
    public static final String RECHECK = "重新评估";

    /** 操作人：系统 */
    public static final String OPERATOR_SYSTEM = "system";

    private GovAction() {
    }
}
