package com.bi.queryer.ssm.migrate.bizsplit.constant;

/**
 * 业务线拆分权限初始化常量。
 *
 * 写入目标表：bi_portal.sys_data_auth
 */
public final class BizSplitDataAuthConstant {

    /** 行级权限模块编码 */
    public static final String MODULE_CODE = "ssm_row";

    /** 业务线维度编码，对应 sys_data_auth.dim_code */
    public static final String DIM_CODE = "ssm_dim_bizline";

    /** 权限主体类型：按用户写入 */
    public static final String OWNER_TYPE_USER = "User";

    /** 权限有效期（天），与工单审批行级权限一致 */
    public static final int ACTIVE_DURATION_DAYS = 9999;

    /** 初始化记录备注前缀，便于与手工配置区分 */
    public static final String REMARK_PREFIX = "bizsplit_init:user_businessline";

    /** REPLACE INTO 单批条数上限 */
    public static final int REPLACE_BATCH_SIZE = 500;

    private BizSplitDataAuthConstant() {
    }
}
