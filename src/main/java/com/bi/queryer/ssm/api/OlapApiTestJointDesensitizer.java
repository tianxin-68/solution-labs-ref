package com.bi.queryer.ssm.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.entity.OlapApiKeyEntity;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.DataSensitiveLevel;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试联调（is_test_joint=1）场景下，对 C4 敏感指标结果做数值脱敏。
 */
@Slf4j
public final class OlapApiTestJointDesensitizer {

    /** 百分号后缀 */
    private static final String SUFFIX_PERCENT = "%";

    /** 百分点后缀（小写，用于匹配） */
    private static final String SUFFIX_PT = "pt";

    private OlapApiTestJointDesensitizer() {
    }

    /**
     * 若 api_key 为测试联调，则对结果集中可追溯到 C4 的指标列按随机系数脱敏。
     *
     * @param dataSet    查询结果集
     * @param engine     查询引擎
     * @param olapApiKey OLAP API Key
     */
    public static void applyIfNeeded(ResultDataSet dataSet, QueryEngine engine, String olapApiKey) {
        try {
            if (dataSet == null || engine == null || StrUtil.isEmpty(olapApiKey)) {
                return;
            }
            OlapApiKeyEntity entity = OlapApiManager.get(olapApiKey);
            if (entity == null || !Enabled.value(entity.getIsTestJoint())) {
                return;
            }
            BigDecimal factor = resolveRandomFactor();
            Set<String> c4Keys = collectC4MeasureKeys(engine);
            if (c4Keys.isEmpty()) {
                return;
            }
            Set<String> columnCodes = collectDesensitizeColumnCodes(dataSet.getColumns(), c4Keys);
            if (columnCodes.isEmpty()) {
                return;
            }
            desensitizeRows(dataSet.getRows(), columnCodes, factor);
            log.info("test joint C4 desensitize applied, factor={}, columnSize={}", factor.toPlainString(), columnCodes.size());
        } catch (Exception e) {
            log.warn("test joint C4 desensitize failed, skip. apiKey={}", BIUtil.desensitizeGuid32(olapApiKey), e);
        }
    }

    /**
     * 生成 [0.000, 0.999] 区间内的 3 位小数随机乘数（单次请求共用）。
     */
    static BigDecimal resolveRandomFactor() {
        int randomInt = ThreadLocalRandom.current().nextInt(1000);
        return BigDecimal.valueOf(randomInt, 3);
    }

    /**
     * 收集 C4 指标及其衍生列可匹配的 id/code/rawCode 键（小写）。
     */
    static Set<String> collectC4MeasureKeys(QueryEngine engine) {
        Set<String> keys = new HashSet<>();
        QueryConfigure config = engine.getConfig();
        if (config == null || config.getResult() == null || CollUtil.isEmpty(config.getResult().getMeasures())) {
            return keys;
        }
        for (QueryField measure : config.getResult().getMeasures()) {
            if (measure == null || BIConsts.ALL_MEASURE_CODE.equalsIgnoreCase(measure.getCode())) {
                continue;
            }
            String originId = resolveOriginMeasureId(measure);
            String originCode = resolveOriginMeasureCode(measure);
            boolean isC4 = isSensitiveC4(originId, originCode)
                    || isSensitiveC4(measure.getId(), measure.getCode())
                    || isSensitiveC4(null, measure.getRawCode());
            if (!isC4) {
                continue;
            }
            addKey(keys, measure.getId());
            addKey(keys, measure.getCode());
            addKey(keys, measure.getRawCode());
            addKey(keys, originId);
            addKey(keys, originCode);
            AnalysisItemConfig analysisConfig = measure.getAnalysisConfig();
            if (analysisConfig != null) {
                addKey(keys, analysisConfig.getMeasureId());
                addKey(keys, analysisConfig.getMeasureCode());
            }
        }
        return keys;
    }

    /**
     * 从结果列中找出需要脱敏的叶子列 code。
     */
    static Set<String> collectDesensitizeColumnCodes(List<ResultDataSetColumn> columns, Set<String> c4Keys) {
        Set<String> columnCodes = new HashSet<>();
        if (CollUtil.isEmpty(columns) || CollUtil.isEmpty(c4Keys)) {
            return columnCodes;
        }
        for (ResultDataSetColumn column : columns) {
            collectDesensitizeColumnCodesRecursive(column, c4Keys, columnCodes);
        }
        return columnCodes;
    }

    private static void collectDesensitizeColumnCodesRecursive(ResultDataSetColumn column,
                                                              Set<String> c4Keys,
                                                              Set<String> columnCodes) {
        if (column == null) {
            return;
        }
        if (CollUtil.isNotEmpty(column.getChildren())) {
            for (ResultDataSetColumn child : column.getChildren()) {
                collectDesensitizeColumnCodesRecursive(child, c4Keys, columnCodes);
            }
            return;
        }
        // 维度区域直接跳过；None 时仅在命中 C4 键时纳入（兼容衍生列 rawQueryArea 缺失）
        QueryArea area = QueryArea.get(column.getRawQueryArea());
        if (area.isDimensionArea()) {
            return;
        }
        if (!matchesC4Keys(column, c4Keys)) {
            return;
        }
        String code = StrUtil.isEmpty(column.getCode()) ? column.getTitle() : column.getCode();
        if (StrUtil.isNotEmpty(code)) {
            columnCodes.add(code);
        }
    }

    private static boolean matchesC4Keys(ResultDataSetColumn column, Set<String> c4Keys) {
        return containsKey(c4Keys, column.getId())
                || containsKey(c4Keys, column.getCode())
                || containsKey(c4Keys, column.getRawCode());
    }

    /**
     * 对结果行中指定列做乘系数脱敏；null/空/非数字跳过。
     */
    static void desensitizeRows(List<Map<String, Object>> rows, Set<String> columnCodes, BigDecimal factor) {
        if (CollUtil.isEmpty(rows) || CollUtil.isEmpty(columnCodes) || factor == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            for (String columnCode : columnCodes) {
                if (!row.containsKey(columnCode)) {
                    continue;
                }
                Object original = row.get(columnCode);
                Object desensitized = multiplyIfNumeric(original, factor);
                if (desensitized != original) {
                    row.put(columnCode, desensitized);
                }
            }
        }
    }

    /**
     * 数值乘系数；支持千分位逗号、% / pt 后缀；无法解析则返回原值。
     * 例：-61.73% → 剥离后缀后乘系数，再拼回 %。
     */
    static Object multiplyIfNumeric(Object value, BigDecimal factor) {
        if (value == null || factor == null) {
            return value;
        }
        if (value instanceof Number) {
            BigDecimal result = new BigDecimal(value.toString()).multiply(factor).stripTrailingZeros();
            return result.toPlainString();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return value;
        }
        String suffix = extractUnitSuffix(text);
        String numberPart = text;
        if (StrUtil.isNotEmpty(suffix)) {
            numberPart = text.substring(0, text.length() - suffix.length()).trim();
        }
        numberPart = numberPart.replace(",", "");
        if (numberPart.isEmpty()) {
            return value;
        }
        try {
            BigDecimal result = new BigDecimal(numberPart).multiply(factor).stripTrailingZeros();
            return result.toPlainString() + suffix;
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * 提取末尾单位后缀（优先 pt，其次 %），保留原始大小写。
     */
    static String extractUnitSuffix(String text) {
        if (StrUtil.isEmpty(text)) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.endsWith(SUFFIX_PT)) {
            return text.substring(text.length() - SUFFIX_PT.length());
        }
        if (text.endsWith(SUFFIX_PERCENT)) {
            return SUFFIX_PERCENT;
        }
        return "";
    }

    private static String resolveOriginMeasureId(QueryField measure) {
        if (Enabled.value(measure.getIsAnalysis()) && measure.getAnalysisConfig() != null
                && StrUtil.isNotEmpty(measure.getAnalysisConfig().getMeasureId())) {
            return measure.getAnalysisConfig().getMeasureId();
        }
        return measure.getId();
    }

    private static String resolveOriginMeasureCode(QueryField measure) {
        if (Enabled.value(measure.getIsAnalysis()) && measure.getAnalysisConfig() != null
                && StrUtil.isNotEmpty(measure.getAnalysisConfig().getMeasureCode())) {
            return measure.getAnalysisConfig().getMeasureCode();
        }
        if (StrUtil.isNotEmpty(measure.getRawCode())) {
            return measure.getRawCode();
        }
        return measure.getCode();
    }

    private static boolean isSensitiveC4(String fieldId, String fieldCode) {
        if (StrUtil.isNotEmpty(fieldId)) {
            MetaField metaField = SSDMetaCacheManager.getField(fieldId);
            if (metaField != null && isC4Level(metaField.getSensitiveLevel())) {
                return true;
            }
        }
        if (StrUtil.isNotEmpty(fieldCode)) {
            List<MetaField> metaFields = SSDMetaCacheManager.getFieldByCode(fieldCode);
            if (CollUtil.isNotEmpty(metaFields)) {
                for (MetaField metaField : metaFields) {
                    if (metaField != null && isC4Level(metaField.getSensitiveLevel())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isC4Level(String sensitiveLevel) {
        return DataSensitiveLevel.C4.getCode().equalsIgnoreCase(sensitiveLevel);
    }

    private static void addKey(Set<String> keys, String key) {
        if (StrUtil.isNotEmpty(key)) {
            keys.add(key.toLowerCase(Locale.ROOT));
        }
    }

    private static boolean containsKey(Set<String> keys, String key) {
        return StrUtil.isNotEmpty(key) && keys.contains(key.toLowerCase(Locale.ROOT));
    }
}
