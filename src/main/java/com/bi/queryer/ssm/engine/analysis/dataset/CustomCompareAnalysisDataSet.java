package com.bi.queryer.ssm.engine.analysis.dataset;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.QueryConfigureType;
import com.bi.queryer.util.BIConsts;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-23  19:38
 * @Description: 自定义对比分析数据集
 */
public class CustomCompareAnalysisDataSet extends AnalysisDataSet {

    private AnalysisItemConfig cfg = null;

    public CustomCompareAnalysisDataSet(String dateField, AnalysisCalcMode calcMode, AnalysisItemConfig cfg) {
        super(dateField, calcMode);
        this.cfg = cfg;
    }

    @Override
    public String getDateFieldExpression(String defaultValue) {
        DateGranularity offsetGranularity = null;
        String formatString = null;
        Integer interval = cfg.getCompareDatesInterval();
        String fieldExpression = null;

        //实时趋势图
        if(config.getSettings().isRtDataset() && QueryConfigureType.Chart == config.getType()){
             fieldExpression = this.getDateFieldFullName();
            return fieldExpression;
        }

        switch (DateGranularity.get(cfg.getDateGranularity())) {
            case DAY:
                offsetGranularity = DateGranularity.DAY;
                formatString = IFunction.Format_Date;
                break;
            case WEEK:
                offsetGranularity = DateGranularity.WEEK;
                fieldExpression = fx.addWeek(this.getDateFieldFullName(), offsetGranularity, interval);
                break;
            case MONTH:
                offsetGranularity = DateGranularity.MONTH;
                fieldExpression = fx.addMonth(this.getDateFieldFullName(), offsetGranularity, interval);
                break;
            case QUARTER:
                offsetGranularity = DateGranularity.QUARTER;
                fieldExpression = fx.addQuarter(this.getDateFieldFullName(), offsetGranularity, interval);
                break;
            case YEAR:
                offsetGranularity = DateGranularity.YEAR;
                formatString = IFunction.Format_Year;
                break;
        }

        if (StrUtil.isEmpty(fieldExpression)) {
            fieldExpression = fx.formatDate(
                    fx.addDate(
                            fx.parseDate(this.getDateFieldFullName(), formatString),
                            offsetGranularity,
                            interval
                    )
                    , formatString);
        }

        if (defaultValue != null) {
            fieldExpression = fx.coalesce(fieldExpression, "'" + defaultValue + "'");
        }

        return fieldExpression;
    }

    @Override
    public String getName() {
        return this.namePrefix + this.calcMode.getCode() + "_" + cfg.getCompareIndex();
    }
}
