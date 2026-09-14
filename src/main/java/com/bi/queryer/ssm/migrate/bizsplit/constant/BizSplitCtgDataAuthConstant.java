package com.bi.queryer.ssm.migrate.bizsplit.constant;

/**
 * 多维目录权限初始化常量。
 */
public final class BizSplitCtgDataAuthConstant {

    /** 目录权限模块编码 */
    public static final String MODULE_CODE = "ssd_ctg";

    /** 目录权限维度编码 */
    public static final String DIM_CODE = "ssm_ctg";

    /** Portal 权限主体：用户 */
    public static final String OWNER_TYPE_USER = "User";

    /** Portal 权限主体：组织 */
    public static final String OWNER_TYPE_DEPT = "Dept";

    /** 数据集侧权限主体：用户（小写） */
    public static final String DATASET_OWNER_TYPE_USER = "user";

    /** 数据集侧权限主体：组织（小写） */
    public static final String DATASET_OWNER_TYPE_DEPT = "dept";

    /** 数据集侧目录权限 item_type */
    public static final String DATASET_ITEM_TYPE_CTG = "ctg";

    /** 初始化记录备注 */
    public static final String REMARK_PREFIX = "bizsplit_init:ctg";

    /** 默认权限有效期（天） */
    public static final int ACTIVE_DURATION_DAYS = 9999;

    /** SC.v 键：MGP 数据源 id，供 DataSourceType.getTypeById 解析 */
    public static final String MGP_DATASOURCE_ID_SC_KEY = "ssm.mgp.datasource.id";

    private BizSplitCtgDataAuthConstant() {
    }
}
