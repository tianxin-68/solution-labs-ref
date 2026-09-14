package com.bi.queryer.ssm.migrate.bizsplit.constant;

/**
 * 指标膨胀与查询指标数量限制相关常量。
 */
public final class MetricExpansionMeasureConstant {

    /**
     * 单次查询可见普通指标数量上限配置项。
     * 与 {@code SSDQueryController#buildQueryResultGrid} 共用同一配置。
     */
    public static final String MEASURE_COUNT_LIMIT_KEY = "ssm.query.measure.count.limit";

    /** 可见普通指标数量上限默认值 */
    public static final String MEASURE_COUNT_LIMIT_DEFAULT = "50";

    private MetricExpansionMeasureConstant() {
    }
}
