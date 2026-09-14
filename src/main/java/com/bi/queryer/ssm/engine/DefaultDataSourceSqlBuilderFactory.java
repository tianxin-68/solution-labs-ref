package com.bi.queryer.ssm.engine;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.promotion.PromotionDefaultDataSourceSqlBuilder;
import com.bi.queryer.ssm.enums.CalendarType;

public class DefaultDataSourceSqlBuilderFactory {

    public static DefaultDataSourceSqlBuilder createDefaultDataSourceSqlBuilder(StarModel model, QueryConfigure config, QueryContext cxt){

        //业务日历，走大促活动的数据源sql构建器
        CalendarType calendarType = CalendarType.get(config.getSettings().getCalendarType());
        if(CalendarType.BUSINESS == calendarType){
            return new PromotionDefaultDataSourceSqlBuilder(model, config, cxt);
        }

        return new DefaultDataSourceSqlBuilder(model, config, cxt);
    }
}
