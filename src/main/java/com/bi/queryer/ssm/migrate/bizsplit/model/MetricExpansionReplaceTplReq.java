package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

/**
 * 按业务线替换模板下全部视图的请求。
 *
 * 门户替换场景：指定源/目标业务线，复制模板并对每个视图做指标膨胀后写入正式库（不写影子表）。
 */
@Data
public class MetricExpansionReplaceTplReq {

    /** 源模板 id */
    private String tplId;

    /** 源业务线 */
    private String sourceBusinessline;

    /** 目标业务线 */
    private String targetBusinessline;
}
