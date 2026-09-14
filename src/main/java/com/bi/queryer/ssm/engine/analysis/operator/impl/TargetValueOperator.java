package com.bi.queryer.ssm.engine.analysis.operator.impl;

import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.promotion.PromotionConsts;
import com.bi.queryer.ssm.engine.result.ResultDataSetTargetConfig;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.bi.queryer.ssm.meta.targetValue.TargetValueAdaptiveInfo;
import com.bi.queryer.ssm.meta.targetValue.TargetValueCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 占比同环比操作符
 */
public class TargetValueOperator extends BaseOperator {

    @Override
    public String preCalc(QueryConfigure config, QueryField measureField, Map<String, String> rawMeasureExpressions, List<StarModel> models) {
        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        ResultDataSetTargetConfig targetConfig = cfg.getTargetConfig();
        if (targetConfig.isActive() && !AnalysisCalcMode.CONTRIBUTION_RATE.getCode().equals(cfg.getCalcMode()) && StringUtils.isNotEmpty(cfg.getCalcMode())) {
            return "null";
        }
        IFunction fx = FunctionManager.getFunction();
        boolean isBusinessCalendar = config.getSettings().isBusinessCalendar();
        String dateGranularity = config.getSettings().getDateGranularity();
        AnalysisCalcMode calcMode = AnalysisCalcMode.get(targetConfig.getTargetCalcMode());
        String measureCode = cfg.getMeasureCode();
        String rawMeasureValue = rawMeasureExpressions.get(measureCode);
        if (calcMode == AnalysisCalcMode.TIME_PROGRESS) {
            return getDateProgress(isBusinessCalendar, models, fx, dateGranularity);
        } else if (calcMode == AnalysisCalcMode.PREDICT_VALUE) {
            String dateProgress = getDateProgress(isBusinessCalendar, models, fx, dateGranularity);
            return String.format("%s / %s", rawMeasureValue, dateProgress);
        }

        Map<String, List<TargetValueAdaptiveInfo>> measureAdaptiveMap = TargetValueCacheManager.getMeasureAdaptiveMap();
        AnalysisCalcType calcType = AnalysisCalcType.get(targetConfig.getTargetCalcType());
        String calcExpression = "";
        String targetValue = String.format("%s.%s_%s_%s", BIConsts.TARGET_VALUE_TABLE_ALIAS, cfg.getMeasureCode(), targetConfig.getTargetCalcMode(), "value");
        List<TargetValueAdaptiveInfo> adaptiveInfo = measureAdaptiveMap.get(measureCode);
        boolean isPositive;
        if (BIUtil.isEmpty(adaptiveInfo)) {
            isPositive = true;
        } else {
            isPositive = Enabled.YES.getId().equals(adaptiveInfo.get(0).getMeasureField().getIsPositive());
        }

        switch (calcType) {
            case REAL_VALUE:
                calcExpression = targetValue;
                break;
            case VALUE:
            case CONTRIBUTION_RATE:
                if (isPositive) {
                    calcExpression = String.format("%s - %s", targetValue, rawMeasureValue);
                } else {
                    calcExpression = String.format("%s - %s", rawMeasureValue, targetValue);
                }
                break;
            case RATIO:
                String isPositiveValue = isPositive ? "1" : "0";
                calcExpression = fx.completionRate(rawMeasureValue, targetValue, isPositiveValue);
                break;
            case P_RATIO:
                String dateProgress = getDateProgress(isBusinessCalendar, models, fx, dateGranularity);
                String expr = String.format("(%s / %s)", rawMeasureValue, dateProgress);
                calcExpression = fx.completionRate(expr, targetValue, isPositive ? "1" : "0");
                break;
        }

        return calcExpression;
    }

    private String getDateProgress(boolean isBusinessCalendar, List<StarModel> models, IFunction fx, String dateGranularity) {
        if (isBusinessCalendar) {
            List<String> coalesceDims = new ArrayList<>();
            for (StarModel model : models) {
                coalesceDims.add(model.getAlias() + "." + PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE);
            }
            return fx.coalesce(coalesceDims);
        } else {
            String dt = FieldUtil.getModelFiledExpression(models, fx, BIConsts.DATE_CODE);
            return fx.getDateProgress(dateGranularity, dt);
        }
    }
}
