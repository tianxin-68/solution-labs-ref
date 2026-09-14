package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

/**
 * 模块拆分目录映射（mgp_ctg_biz_line_split_mapping 查询结果）。
 */
@Data
public class BizSplitCtgSplitMapping {

    /** 源目录 id */
    private String oldCtgId;

    /** 源目录名称 */
    private String oldCtgName;

    /** 目标目录 id */
    private String newCtgId;

    /** 目标目录名称 */
    private String newCtgName;
}
