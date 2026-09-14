package com.bi.queryer.ssm.engine.promotion;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisDataSourceCfg;
import com.bi.queryer.ssm.engine.analysis.lunaryearweek.LunarYearWeekManager;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.view.DataSourceViewBuilderFactory;
import com.bi.queryer.ssm.engine.view.IDataSourceViewBuilder;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIConsts;

public class LunarYearWeekPromotionAnalysisDataSourceSqlBuilder extends PromotionAnalysisDataSourceSqlBuilder{
    public LunarYearWeekPromotionAnalysisDataSourceSqlBuilder(StarModel model, QueryConfigure config, QueryContext cxt, AnalysisDataSourceCfg cfg) {
        super(model, config, cxt, cfg);
    }

    /**
     * 构建comd的select字段
     * @param selectFragment
     * @return
     */
    public String buildComdSelectFragment(String selectFragment) {
        String result = String.format("ln_yw_cfg.current_dt as %s", BIConsts.MIN_DATE_GRANULARITY_CODE);
        return result;
    }

    public String buildJoinSql() {

        StringBuilder joinSQL = new StringBuilder();

        //农历年周维表关联
        String lunarYearWeekTableName = SC.v("ssm.lunar.year.week.table.name", "bi_olap.ssm_lunar_year_week_cfg");
        String lunarYearWeekFieldName = getLunarYearWeekFieldName();

        //关联活动维表
        String lunarYearWeekJoinSql = String.format(" inner join %s ln_yw_cfg on %s = ln_yw_cfg.%s",
                lunarYearWeekTableName,
                this.commonDateFieldOriginalSelectExpression,
                lunarYearWeekFieldName);
        joinSQL.append(lunarYearWeekJoinSql);

        //大促活动维表
        String promoTableName = SC.v("ssm.promo.table.name", "bi_olap.ssm_promotion_cfg");

        //关联活动维表
        String promoJoinSql = String.format(" inner join %s promo on ln_yw_cfg.current_dt between promo.start_date and promo.end_date",
                promoTableName);
        joinSQL.append(promoJoinSql);

        return joinSQL.toString();
    }

    /**
     * 获取农历年周字段名称
     * @return
     */
    public String getLunarYearWeekFieldName() {

        switch (analysisCalcMode) {
            case TB_LN_YEAR_WEEK:
                return "lunar_year_week_1";
            case TB_LN_YEAR_WEEK_2:
                return "lunar_year_week_2";
            case TB_LN_YEAR_WEEK_3:
                return "lunar_year_week_3";
        }

        return "lunar_year_week_1";
    }

    @Override
    protected StringBuilder buildWhereClause() {
        StringBuilder whereSQL = super.buildWhereClause();

        //针对二次计算的指标，view不为空，不在最内层过滤活动标识
        IDataSourceViewBuilder builder = DataSourceViewBuilderFactory.create(model, config, cxt);
        if(builder != null){
            return whereSQL;
        }

        whereSQL.append(LunarYearWeekManager.buildLunarCurrentDateFilter(config));
        return whereSQL;
    }

    /**
     * 构建视图扩展的where子句
     * @return
     */
    public StringBuilder buildViewExtendWhereClause() {
        StringBuilder whereSQL = super.buildViewExtendWhereClause();
        whereSQL.append(LunarYearWeekManager.buildLunarCurrentDateFilter(config));
        return whereSQL;
    }
}
