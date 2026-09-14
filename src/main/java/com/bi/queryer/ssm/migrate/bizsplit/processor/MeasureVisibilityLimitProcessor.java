package com.bi.queryer.ssm.migrate.bizsplit.processor;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.migrate.bizsplit.constant.MetricExpansionMeasureConstant;
import com.bi.queryer.ssm.migrate.bizsplit.service.MetricExpansionContext;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 私域 execute 膨胀完成后，对可见普通指标数量做兜底处理。
 *
 * 规则：targetBusinessline 为空（preserveSourceData）且可见普通指标超过配置上限时，
 * 将本次新产生的膨胀指标设为不可见，不改动膨胀前已存在的 source 指标。
 */
@Component
public class MeasureVisibilityLimitProcessor {

    private static final Logger log = LoggerFactory.getLogger(MeasureVisibilityLimitProcessor.class);

    /**
     * 私域膨胀完成后执行可见性兜底：超限时隐藏新膨胀的普通指标。
     *
     * @param context 单视图膨胀上下文，需已调用 {@link MetricExpansionContext#captureInitialMeasureIdsAtExpansionStart()}
     */
    public void applyExpandedMeasureVisibilityLimit(MetricExpansionContext context) {
        if (context == null || !context.isPreserveSourceData()) {
            return;
        }
        JSONObject tplConfigJson = context.getTplConfigJson();
        if (tplConfigJson == null) {
            return;
        }
        JSONObject result = tplConfigJson.getJSONObject("result");
        if (result == null) {
            return;
        }
        JSONArray measures = result.getJSONArray("measures");
        if (CollUtil.isEmpty(measures)) {
            return;
        }

        int measureCountLimit = Integer.parseInt(SC.v(
                MetricExpansionMeasureConstant.MEASURE_COUNT_LIMIT_KEY,
                MetricExpansionMeasureConstant.MEASURE_COUNT_LIMIT_DEFAULT));
        int visibleBefore = countVisibleNonAnalysisMeasures(measures);
        if (visibleBefore <= measureCountLimit) {
            return;
        }

        int hiddenExpandedCount = 0;
        for (int i = 0; i < measures.size(); i++) {
            JSONObject field = measures.getJSONObject(i);
            if (field == null) {
                continue;
            }
            String fieldId = field.getString("id");
            if (context.isInitialMeasureAtExpansionStart(fieldId)) {
                continue;
            }
            if (isAnalysisMeasure(field)) {
                continue;
            }
            if (!Enabled.isTrue(field.getInteger("isShow"))) {
                continue;
            }
            field.put("isShow", Enabled.NO.getId());
            hiddenExpandedCount++;
        }

        int visibleAfter = countVisibleNonAnalysisMeasures(measures);
        log.info("[MetricExpansion] measure visibility limit applied, viewId={}, visibleBefore={}, limit={}, "
                        + "hiddenExpandedCount={}, visibleAfter={}",
                context.getViewId(), visibleBefore, measureCountLimit, hiddenExpandedCount, visibleAfter);
        if (visibleAfter > measureCountLimit) {
            log.warn("[MetricExpansion] visible measures still exceed limit after hiding expanded metrics, "
                            + "viewId={}, visibleAfter={}, limit={}",
                    context.getViewId(), visibleAfter, measureCountLimit);
        }
    }

    /**
     * 统计 result.measures 中可见且非分析派生的普通指标数量。
     */
    private int countVisibleNonAnalysisMeasures(JSONArray measures) {
        int count = 0;
        for (int i = 0; i < measures.size(); i++) {
            JSONObject field = measures.getJSONObject(i);
            if (field == null) {
                continue;
            }
            if (isAnalysisMeasure(field)) {
                continue;
            }
            if (Enabled.isTrue(field.getInteger("isShow"))) {
                count++;
            }
        }
        return count;
    }

    /** 是否为分析派生指标（不计入查询 50 限制） */
    private boolean isAnalysisMeasure(JSONObject field) {
        return field != null && Enabled.isTrue(field.getInteger("isAnalysis"));
    }
}
