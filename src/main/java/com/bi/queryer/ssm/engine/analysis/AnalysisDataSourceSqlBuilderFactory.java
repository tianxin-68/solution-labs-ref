package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilder;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisDataSourceCfg;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.promotion.LunarYearWeekPromotionAnalysisDataSourceSqlBuilder;
import com.bi.queryer.ssm.engine.promotion.PromotionAnalysisDataSourceSqlBuilder;
import com.bi.queryer.ssm.enums.CalendarType;

public class AnalysisDataSourceSqlBuilderFactory {

    public static DefaultDataSourceSqlBuilder createAnalysisDataSourceSqlBuilder(StarModel model, QueryConfigure config, QueryContext cxt, AnalysisDataSourceCfg cfg) {

        //业务日历，走大促活动的数据源sql构建器
        CalendarType calendarType = CalendarType.get(config.getSettings().getCalendarType());
        if (CalendarType.BUSINESS == calendarType) {

            //农历年周
            if (cfg.getAnalysisCalcMode() != null && cfg.getAnalysisCalcMode().isTblnyw()) {
                return new LunarYearWeekPromotionAnalysisDataSourceSqlBuilder(model, config, cxt, cfg);
            }
            return new PromotionAnalysisDataSourceSqlBuilder(model, config, cxt, cfg);
        }

        //农历年周
        if (cfg.getAnalysisCalcMode() != null && cfg.getAnalysisCalcMode().isTblnyw()) {
            return new LunarYearWeekAnalysisDataSourceSqlBuilder(model, config, cxt, cfg);
        }

        return new AnalysisDataSourceSqlBuilder(model, config, cxt, cfg);
    }

}
