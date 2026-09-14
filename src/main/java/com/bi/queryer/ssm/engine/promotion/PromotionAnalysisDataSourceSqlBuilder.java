package com.bi.queryer.ssm.engine.promotion;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.AnalysisDataSourceSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisDataSourceCfg;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PromotionAnalysisDataSourceSqlBuilder extends PromotionDefaultDataSourceSqlBuilder {

    // 分析依据的日期字段
    private QueryField analysisDateField = null;

    // 附加的分析所需的日期范围
    protected List<String> compareDateRange = null;

    /**
     * 分析计算方式
     */
    protected AnalysisCalcMode analysisCalcMode;

    /**
     * 自定义对比的索引
     */
    protected Integer customCompareIndex = -1;

    /**
     * 偏移的日期表达式
     */
    protected String offsetDateExpression;

    public PromotionAnalysisDataSourceSqlBuilder(StarModel model, QueryConfigure config, QueryContext cxt, AnalysisDataSourceCfg cfg) {
        super(model, config, cxt);

        this.setAnalysisCalcMode(cfg.getAnalysisCalcMode());
        this.setCustomCompareIndex(cfg.getCustomCompareIndex());
        this.setOffsetDateExpression(cfg.getOffsetDateExpression());
    }

    @Override
    protected StringBuilder buildWhereClause() {
        analysisDateField = this.getAnalysisDateField(config);
        compareDateRange = AnalysisUtil.getCompareDateRange(config, analysisCalcMode, customCompareIndex);
        return super.buildWhereClause();
    }

    @Override
    public List<String> getPromoIdentifierList() {

        List<String> promoIdentifierList = new ArrayList<>();

        QueryField commonDateField = config.getFilterCommonDateField();
        if (commonDateField == null) {
            return promoIdentifierList;
        }

        Integer promoYear = config.getSettings().getPromoYear();
        Integer promoYearOffset = config.getSettings().getPromoYear();
        if(analysisCalcMode!=null && analysisCalcMode.isPromoTb()){
            promoYearOffset = promoYear + analysisCalcMode.getOffset();
        }

        //添加活动标识过滤
        for (FieldValue fieldValue : commonDateField.getValues()) {
            String value = fieldValue.getId().replace(promoYear.toString(),promoYearOffset.toString());
            promoIdentifierList.add(String.format("'%s'", value));
        }

        promoIdentifierList = promoIdentifierList.stream().distinct().collect(Collectors.toList());

        return promoIdentifierList;
    }

    @Override
    public List<String> buildExtendWhereFragments(List<QueryField> filterFields) {
        return new ArrayList<>();
    }

    /**
     * 获取门店开业月份的时间字段表达式
     *
     * @return
     */
    public String buildShopOpenMonthDateFieldExpression() {
        String dtExpression = super.buildShopOpenMonthDateFieldExpression();

        //将日期占位符，替换为实际的日期表达式
        if(StrUtil.isNotEmpty(this.offsetDateExpression)){
            dtExpression = this.offsetDateExpression.replace(BIConsts.COMPARE_DATE_PLACEHOLDER, dtExpression);
        }

        return dtExpression;
    }


    /**
     * 重写父类日期转换天，解决mtd、ytd同环比等问题
     *
     * @param field
     * @param values
     * @return
     */
    @Override
    protected List<FieldValue> convertDateFilterByDay(QueryField field, List<FieldValue> values) {
        List<FieldValue> newValues = super.convertDateFilterByDay(field, values);
        if (analysisDateField == null || BIUtil.isEmpty(compareDateRange)) {
            return newValues;
        }

        // 添加分析的附加过滤值：只有和分析日期自动同源的过滤字段才需添加，即字段编码和分析日期自动编码一致
        boolean isAnalysisFilterField = AnalysisUtil.isAnalysisDateField(field, analysisDateField.getCode());

        if (!isAnalysisFilterField) {
            // 无分析日期字段（如：占比）或没有附加的日期范围则返回父类构建sql代码
            return newValues;
        }
        if (BIUtil.isEmpty(newValues)) {
            return values;
        }

        if (isAnalysisFilterField && BIUtil.isNotEmpty(compareDateRange)) {
            String start = compareDateRange.get(0);
            String end = compareDateRange.get(compareDateRange.size() - 1);

            newValues = new ArrayList<>();
            newValues.add(new FieldValue(start, start));
            newValues.add(new FieldValue(end, end));
            newValues = FieldUtil.getDateFieldFilterValues(field, newValues, this.config);

            FieldValue endValue = newValues.get(newValues.size() - 1);
            String mtdLastDay = this.getMtdLastDay();
            //只处理时间字段为日粒度的mtd
            if ("true".equalsIgnoreCase(SC.v("ssm.only.day.calc.mtd", "false"))) {
                if (BIUtil.isNotEmpty(mtdLastDay) && endValue.getId().compareTo(mtdLastDay) > 0 && DateGranularity.DAY == DateGranularity.get(field.getMeta().getDateGranularity())) {
                    endValue.setId(mtdLastDay);
                }
            } else {
                if (BIUtil.isNotEmpty(mtdLastDay) && endValue.getId().compareTo(mtdLastDay) > 0) {
                    endValue.setId(mtdLastDay);
                }
            }
        }


        return newValues;
    }

    protected String getMtdLastDay() {
        List<List<String>> excludeAnalysisDateFilterRangeList = AnalysisUtil.getExcludeAnalysisDateFilterRange(config, analysisCalcMode, customCompareIndex);
        if (BIUtil.isEmpty(excludeAnalysisDateFilterRangeList)) {
            return null;
        }
        List<String> list = excludeAnalysisDateFilterRangeList.get(0);
        if (BIUtil.isEmpty(list)) {
            return null;
        }
        String mtdLastDay = DateUtil.offsetDay(DateUtil.parse(list.get(0)), -1).toDateStr();
        return mtdLastDay;
    }


    /**
     * 获取时间间隔表达式
     * @return
     */
    public String getDiffDayExpression() {

        String dataSnapshotDate = config.getSettings().getDataSnapshotDate();
        //同阶段同比，按阶段偏移
        if (analysisCalcMode != null && analysisCalcMode.isPromoTb()) {
            dataSnapshotDate = this.compareDateRange.get(this.compareDateRange.size() - 1);
        }

        String diffDayExpression = function.getPromoDateDiff("promo.start_date", dataSnapshotDate);
        return diffDayExpression;
    }

    /**
     * 获取分析的日期字段
     *
     * @return
     */
    protected QueryField getAnalysisDateField(QueryConfigure config) {

        if (config.isAggQuery()) {
            return config.getFilterCommonDateField();
        }

        List<QueryField> analysisFields = config.getResult().getFields().stream().filter(f -> Enabled.value(f.getIsAnalysis())).collect(Collectors.toList());
        ;
        for (QueryField queryField : analysisFields) {
            String dateFieldCode = queryField.getAnalysisConfig().getDateFieldCode();
            if (BIUtil.isNotEmpty(dateFieldCode)) {
                return config.getResult().getFieldByCode(dateFieldCode);
            }
        }

        return null;
    }

    /**
     * 获取时间字段的表达式
     * @return
     */
    public String getDateFieldSelectExpression() {

        if (analysisCalcMode != null && !analysisCalcMode.isPromoTb()) {
            String dtExpression = this.offsetDateExpression.replace(BIConsts.COMPARE_DATE_PLACEHOLDER, this.commonDateFieldSelectExpression);
            dtExpression = String.format("(%s)", dtExpression);
            return dtExpression;
        }

        return this.commonDateFieldSelectExpression;
    }

    @Override
    protected DefaultDataSourceSqlBuilderAvgWrapper createDefaultDataSourceSqlBuilderAvgWrapper(String datasourceSql, StarModel model, QueryConfigure config, QueryContext cxt) {
        return new AnalysisDataSourceSqlBuilderAvgWrapper(datasourceSql, model, config, cxt);
    }

    public AnalysisCalcMode getAnalysisCalcMode() {
        return analysisCalcMode;
    }

    public void setAnalysisCalcMode(AnalysisCalcMode analysisCalcMode) {
        this.analysisCalcMode = analysisCalcMode;
    }

    public Integer getCustomCompareIndex() {
        return customCompareIndex;
    }

    public void setCustomCompareIndex(Integer customCompareIndex) {
        this.customCompareIndex = customCompareIndex;
    }

    public List<String> getCompareDateRange() {
        return compareDateRange;
    }

    public void setCompareDateRange(List<String> compareDateRange) {
        this.compareDateRange = compareDateRange;
    }

    public String getOffsetDateExpression() {
        return offsetDateExpression;
    }

    public void setOffsetDateExpression(String offsetDateExpression) {
        this.offsetDateExpression = offsetDateExpression;
    }
}
