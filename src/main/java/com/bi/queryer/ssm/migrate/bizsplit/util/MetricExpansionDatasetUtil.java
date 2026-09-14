package com.bi.queryer.ssm.migrate.bizsplit.util;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.config.SC;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 指标膨胀数据集范围工具。
 *
 * 通过 SC.v {@code ssm.metric.expansion.dataset.ids} 维护「需同时做筛选替换与指标膨胀」的数据集 id 列表
 *（逗号分隔）。execute 时以视图 {@code datasetId} 精确匹配该列表：
 *
 * 在列表内：filter 源业务线按映射 target 替换，并执行指标膨胀、重建 measure 派生字段。
 * 不在列表内：仅按固定规则改写 filter，measureCodes / measureAsset 从正式 cfg_dtl 原样拷贝。
 *
 * 配置为空时，所有 datasetId 均视为不在列表内，即全量视图只做筛选值替换。
 */
public final class MetricExpansionDatasetUtil {

    /** SC.v 键：需执行「筛选替换 + 指标膨胀」的 datasetId 列表，逗号分隔 */
    private static final String EXPANSION_DATASET_IDS_KEY = "ssm.metric.expansion.dataset.ids";

    private MetricExpansionDatasetUtil() {
    }

    /**
     * 判断 datasetId 是否在指标膨胀数据集范围内。
     *
     * @param datasetId 视图绑定的 datasetId（调用方保证非空）
     * @return true 表示需继续执行指标膨胀；false 表示仅做筛选值替换
     */
    public static boolean isExpansionDataset(String datasetId) {
        if (StrUtil.isEmpty(datasetId)) {
            return false;
        }
        Set<String> expansionDatasetIds = loadExpansionDatasetIds();
        if (expansionDatasetIds.isEmpty()) {
            return false;
        }
        return expansionDatasetIds.contains(datasetId.trim());
    }

    /**
     * 从 SC.v 加载并解析指标膨胀数据集 id 集合。
     * 对配置值按逗号切分、trim 后去重；配置缺失或为空时返回空集合。
     *
     * @return 不可变语义上的 id 集合（每次调用重新解析，便于配置热更新）
     */
    private static Set<String> loadExpansionDatasetIds() {
        String raw = SC.v(EXPANSION_DATASET_IDS_KEY, "");
        if (StrUtil.isEmpty(raw)) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotEmpty)
                .forEach(ids::add);
        return ids;
    }
}
