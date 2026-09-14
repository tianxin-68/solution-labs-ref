package com.bi.queryer.ssm.engine.promotion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilder;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.view.DataSourceViewBuilderFactory;
import com.bi.queryer.ssm.engine.view.IDataSourceViewBuilder;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.promotion.model.PromotionCfg;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述:活动日历对应的单个模型数据源sql构建器
 */
public class PromotionDefaultDataSourceSqlBuilder extends DefaultDataSourceSqlBuilder {
    public PromotionDefaultDataSourceSqlBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        super(model, config, cxt);
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
            if (commonDateFieldFullExpression.equalsIgnoreCase(selectFragment)) {
                selectFragments.add(String.format("promo.promo_identifier as %s", BIConsts.DATE_CODE));
            } else if(selectFragment.endsWith(BIConsts.MIN_DATE_GRANULARITY_CODE)){
                selectFragments.add(buildComdSelectFragment(selectFragment));
            }else{
                selectFragments.add(selectFragment);
            }
        }

        //添加活动持续天数，便于上层计算日均
        /**
         *    case
         *                   when (DATEDIFF('2025-11-12', promo.start_date) + 1 ) > promo.duration_days then promo.duration_days
         *                   else (DATEDIFF('2025-11-12', promo.start_date) + 1 )
         *                 end duration_days
         */
        String diffDayExpression = getDiffDayExpression();
        String durationDaysExpression = String.format("case when (%s + 1 ) > %s then %s else (%s + 1 ) end as %s",
                diffDayExpression,
                "promo.duration_days",
                "promo.duration_days",
                diffDayExpression,
                PromotionConsts.PROMOTION_DURATION_DAYS_CODE
                );

        selectFragments.add(durationDaysExpression);

        //添加时间进度计算
        selectFragments.add(String.format("%s as %s",
                function.getDateRangeProgress("promo.start_date","promo.end_date",config.getSettings().getDataSnapshotDate())
                , PromotionConsts.PROMOTION_DATE_RANGE_PROGRESS_CODE));

        String selectFieldStr = BIUtil.listToStr(selectFragments, ",");
        selectSql.append(String.format(" select %s ", selectFieldStr));
        return selectSql;

    }

    /**
     * 构建comd的select字段
     * @param selectFragment
     * @return
     */
    public String buildComdSelectFragment(String selectFragment) {
        return selectFragment;
    }

    /**
     * 获取时间间隔表达式
     * @return
     */
    public String getDiffDayExpression(){
        String diffDayExpression = function.getPromoDateDiff("promo.start_date", config.getSettings().getDataSnapshotDate());
        return diffDayExpression;
    }

    @Override
    public StringBuilder buildFromClause() {
        StringBuilder fromSQL = super.buildFromClause();

        String joinSql = buildJoinSql();
        fromSQL.append(joinSql);

        return fromSQL;
    }

    public String buildJoinSql() {
        String dateFieldSelectExpression = getDateFieldSelectExpression();

        //大促活动维表
        String promoTableName = SC.v("ssm.promo.table.name", "bi_olap.ssm_promotion_cfg");

        //关联活动维表
        String joinSql = String.format(" inner join %s promo on %s between promo.start_date and promo.end_date",
                promoTableName,
                dateFieldSelectExpression);

        return joinSql;
    }

    /**
     * 获取时间字段的表达式
     * @return
     */
    public String getDateFieldSelectExpression() {
        return this.commonDateFieldSelectExpression;
    }

    @Override
    protected StringBuilder buildWhereClause() {
        StringBuilder whereSQL = super.buildWhereClause();

        //针对二次计算的指标，view不为空，不在最内层过滤活动标识
        IDataSourceViewBuilder builder = DataSourceViewBuilderFactory.create(model, config, cxt);
        if(builder != null){
            return whereSQL;
        }

        List<String> promoIdentifier = getPromoIdentifierList();
        if(CollUtil.isNotEmpty(promoIdentifier)){
            whereSQL.append(String.format(" and promo.promo_identifier in (%s)", BIUtil.listToStr(promoIdentifier, ",")));
        }
        return whereSQL;
    }

    /**
     * 构建视图扩展的where子句
     * @return
     */
    public StringBuilder buildViewExtendWhereClause() {
        StringBuilder whereSQL = new StringBuilder();

        List<String> promoIdentifier = getPromoIdentifierList();
        if (CollUtil.isNotEmpty(promoIdentifier)) {
            whereSQL.append(String.format(" where promo.promo_identifier in (%s)", BIUtil.listToStr(promoIdentifier, ",")));
        }
        return whereSQL;
    }

    public List<String> getPromoIdentifierList() {

        List<String> promoIdentifierList = new ArrayList<>();

        QueryField commonDateField = config.getFilterCommonDateField();
        if (commonDateField == null) {
            return promoIdentifierList;
        }

        //添加活动标识过滤
        for (FieldValue fieldValue : commonDateField.getValues()) {
            promoIdentifierList.add(String.format("'%s'", fieldValue.getId()));
        }

        promoIdentifierList = promoIdentifierList.stream().distinct().collect(Collectors.toList());

        return promoIdentifierList;
    }

    /**
     * 将不同日历类型的时间统一转化为日粒度
     */
    @Override
    public List<FieldValue> normalizeToDay(List<FieldValue> values) {

        List<String> dateValueList = new ArrayList<>();

        /**
         * 将活动阶段转化为 开始日期 - 结束日期
         */
        for (FieldValue fieldValue : values) {
            PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(fieldValue.getId());
            if (promotionCfg == null) {
                continue;
            }

            dateValueList.add(promotionCfg.getStartDate());
            dateValueList.add(promotionCfg.getEndDate());
        }

        if (CollUtil.isEmpty(dateValueList)) {
            return values;
        }

        List<String> result = new ArrayList<>();
        Date dataSnapshotDate = DateUtil.parseDate(config.getSettings().getDataSnapshotDate());
        //截止时间处理
        for (String dateValue : dateValueList) {
            if(DateUtil.compare(DateUtil.parseDate(dateValue),dataSnapshotDate ) > 0){
                result.add(config.getSettings().getDataSnapshotDate());
            }else{
                result.add(dateValue);
            }
        }

        result = result.stream().distinct().collect(Collectors.toList());
        Collections.sort(result);

        List<FieldValue> fieldValueList = new ArrayList<>();
        fieldValueList.add(new FieldValue(result.get(0), result.get(0)));
        fieldValueList.add(new FieldValue(result.get(result.size() - 1), result.get(result.size() - 1)));

        return fieldValueList;
    }

}
