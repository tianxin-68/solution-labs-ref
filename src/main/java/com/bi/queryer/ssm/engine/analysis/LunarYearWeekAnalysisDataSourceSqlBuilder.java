package com.bi.queryer.ssm.engine.analysis;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisDataSourceCfg;
import com.bi.queryer.ssm.engine.analysis.lunaryearweek.LunarYearWeekManager;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.view.DataSourceViewBuilderFactory;
import com.bi.queryer.ssm.engine.view.IDataSourceViewBuilder;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LunarYearWeekAnalysisDataSourceSqlBuilder extends AnalysisDataSourceSqlBuilder{
    public LunarYearWeekAnalysisDataSourceSqlBuilder(StarModel model, QueryConfigure config, QueryContext cxt, AnalysisDataSourceCfg cfg) {
        super(model, config, cxt, cfg);
    }


    public StringBuilder buildSelectClause() {
        super.buildSelectClause();

        StringBuilder selectSql = new StringBuilder();
        List<String> selectFragments = new ArrayList<>();

        /**
         * 将公共日期的字段表达式替换成活动的唯一标识
         */
        String commonDateFieldFullExpression = String.format(String.format("%s as %s", this.commonDateFieldSelectExpression, BIConsts.DATE_CODE));
        for (String selectFragment : this.sqlSelectFragments) {
            //实时趋势图，不替换时间字段，在DefaultDataSourceSqlBuilder中已处理
            if (commonDateFieldFullExpression.equalsIgnoreCase(selectFragment) && !config.isRtDatasetChartQuery()) {
                selectFragments.add(String.format("ln_yw_cfg.current_dt as %s", BIConsts.DATE_CODE));
            } else if(selectFragment.endsWith(BIConsts.MIN_DATE_GRANULARITY_CODE)){
                selectFragments.add(String.format("ln_yw_cfg.current_dt as %s", BIConsts.MIN_DATE_GRANULARITY_CODE));
            }else{
                selectFragments.add(selectFragment);
            }
        }

        String selectFieldStr = BIUtil.listToStr(selectFragments, ",");
        selectSql.append(String.format(" select %s ", selectFieldStr));
        return selectSql;
    }


    @Override
    public StringBuilder buildFromClause() {
        StringBuilder fromSQL = super.buildFromClause();

        //不是农历年周时，则返回父类sql
        if(!analysisCalcMode.isTblnyw()){
            return fromSQL;
        }

        //使用原始的日期字段表达式
        String dateFieldSelectExpression = this.commonDateFieldOriginalSelectExpression;

        //农历年周维表
        String lunarYearWeekTableName = SC.v("ssm.lunar.year.week.table.name", "bi_olap.ssm_lunar_year_week_cfg");
        String lunarYearWeekFieldName = getLunarYearWeekFieldName();
        //关联活动维表
        String joinSql = String.format(" inner join %s ln_yw_cfg on %s = ln_yw_cfg.%s",
                lunarYearWeekTableName,
                dateFieldSelectExpression,
                lunarYearWeekFieldName);
        fromSQL.append(joinSql);

        return fromSQL;
    }

    @Override
    protected StringBuilder buildWhereClause() {
        StringBuilder whereSQL = super.buildWhereClause();

        //针对二次计算的指标，view不为空，不在最内层过滤活动标识
        IDataSourceViewBuilder builder = DataSourceViewBuilderFactory.create(model, config, cxt);
        if (builder != null) {
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
        StringBuilder whereSQL = new StringBuilder();
        whereSQL.append(" where 1=1 ");
        whereSQL.append(LunarYearWeekManager.buildLunarCurrentDateFilter(config));
        return whereSQL;
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
}
