package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.MultiModelQuerySqlBuilder;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.dataset.*;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 20:20 2024-07-19
 * @Description 多模型分析场景sql构建器
 **/
public class AnalysisMultiModelSqlBuilder extends MultiModelQuerySqlBuilder {
    public AnalysisMultiModelSqlBuilder(QueryContext cxt) {
        super(cxt);
    }

    public AnalysisMultiModelSqlBuilder(QueryConfigure config, QueryContext cxt, List<StarModel> models) {
        super(config, cxt, models);
    }

    @Override
    protected List<String> buildSelectFragments() {
        List<String> fragments = super.buildSelectFragments();

        List<QueryTable> queryTables = models.stream().map(StarModel::getFactTable).collect(Collectors.toList());
        Map<String, String> measureExpressions = new LinkedHashMap<>();
        List<QueryField> selectFields = getRealSelectFields();
        List<QueryField> analysisFields = new ArrayList<>();
        for(QueryField selectField : selectFields) {
            if (selectField.isAnalysisCalc()) {
                continue;
            }

            if(Enabled.isTrue(selectField.getIsAnalysis())){
                // 此处不处理分析字段，分析字段在下面统一处理
                analysisFields.add(selectField);
                continue;
            }

            String selectExpression = selectField.getCode();
            if (selectField.isCustomMeasure()) {
                // 若是用户自定义计算字段，不在QueryTable中，需单独处理：使用子查询中已聚合的字段进行计算
                selectExpression = this.getCustomCalcMeasureFieldName(queryTables, selectField, Collections.emptyMap());
            }
            if (!selectField.isCustomMeasure()) {
                List<String> coalesceFields = new ArrayList<>();
                for (StarModel model : models) {
                    if (model.getFieldByCode(selectField.getCode()) != null) {
                        coalesceFields.add(model.getAlias() + "." + selectExpression);
                    }
                }
                selectExpression = fx.coalesce(coalesceFields);
            }
            if(selectField.isMeasure() && Enabled.value(selectField.getIsShow())){ // 去掉隐藏字段
                measureExpressions.put(selectField.getCode(), selectExpression);
            }
        }

        List<String> analysisFragments = this.buildAnalysisSelectFragments(analysisFields, measureExpressions);

        fragments.addAll(analysisFragments);

        return fragments;
    }

    protected List<QueryField> getRealSelectFields() {
        return getSelectFields().stream()
                .filter(f -> !f.isAppend() || SSDUtil.isAggFilter(f))
                .collect(Collectors.toList());
    }

    /**
     * 构建分析字段select片段
     * @return
     */
    protected List<String> buildAnalysisSelectFragments(List<QueryField> analysisFields, Map<String, String> measureExpressions) {
        List<String> fragments = new ArrayList<>();
        if (!config.hasAnalysis()) {
            return fragments;
        }
        Map<String, AnalysisItemConfig> analysisItemConfigMap = new HashMap<>();
        // 添加分析字段
        for (QueryField selectField : analysisFields) {
            if (Enabled.isFalse(selectField.getIsAnalysis())) {
                continue;
            }

            AnalysisItemConfig analysisItemConfig = selectField.getAnalysisConfig();
            if (analysisItemConfig != null) {
                int idx = analysisItemConfig.getCompareIndex() == null ? -1 : analysisItemConfig.getCompareIndex();

                String calcMode = analysisItemConfig.getRawThbCalcMode().getCode();
                analysisItemConfigMap.put(calcMode + idx, analysisItemConfig);
            }

            BaseOperator operator = getAnalysisOperator(analysisItemConfig);
            if (operator != null) {
                String expression = operator.preCalc(config, selectField, measureExpressions, models);
                if (BIUtil.isNotEmpty(expression)) {
                    String fragment = String.format("%s as %s", expression, selectField.getCode());
                    fragments.add(fragment);
                }
            }
        }

        // 分组标识
        List<String> coalesceFields = new ArrayList<>();
        for (StarModel model : models) {
            coalesceFields.add(model.getAlias() + "." + BIConsts.GROUPING_VALUE);
        }
        String fragment = String.format("%s as %s", fx.coalesce(coalesceFields), BIConsts.GROUPING_VALUE);
        fragments.add(fragment);

        // 添加对比日期
        QueryField commonDateField = config.getFilterCommonDateField();
        AnalysisSingleModelSqlBuilder singleModelSqlBuilder = this.getAnalysisSingleModelSqlBuilder();

        if (commonDateField != null && singleModelSqlBuilder != null) {
            //汇总不出对比日期
            if (!config.isAggQuery()) {
                List<String> dateCoalesces = new ArrayList<>();
                for (StarModel model : models) {
                    if (model.getFieldByCode(commonDateField.getCode()) != null) {
                        dateCoalesces.add(model.getAlias() + "." + commonDateField.getCode());
                    }
                }
                AnalysisCalcMode calcMode = singleModelSqlBuilder.getAnalysisCalcMode();
                if (calcMode != null && calcMode.isCompare()) {
                    String compareDateCode = commonDateField.getCode() + BIConsts.COMPARE_DATE_SUFFIX;
                    //String offsetDateExpression = compareOperator.getCompareOffsetDateExpression(fx.coalesce(dateCoalesces), calcMode, singleModelSqlBuilder.getCustomCompareIndex());
                    AnalysisItemConfig analysisItemConfig = analysisItemConfigMap.get(calcMode.getCode() + singleModelSqlBuilder.getCustomCompareIndex());
                    AnalysisDataSet dateSet = this.getCompareDataSet(commonDateField, fx.coalesce(dateCoalesces), calcMode, analysisItemConfig);
                    String offsetDateExpression = dateSet.getDateFieldExpression(null);

                    String compareDateExpression = "";
                    //业务日历处理
                    if(config.getSettings().isBusinessCalendar()&& !calcMode.isPromoTb()){
                        compareDateExpression = String.format("(%s) as %s", fx.coalesce(dateCoalesces), compareDateCode);
                    }else{
                        compareDateExpression = String.format("(%s) as %s", offsetDateExpression, compareDateCode);
                    }

                    fragments.add(compareDateExpression);
                }
            }

            setOffsetDateExpression(commonDateField, singleModelSqlBuilder, analysisItemConfigMap);

        }
        return fragments;
    }

    protected BaseOperator getAnalysisOperator(AnalysisItemConfig analysisItemConfig) {
        if (analysisItemConfig == null) {
            return null;
        }
        return OperatorFactory.getOperator(analysisItemConfig.getCalcMode());
    }

    //将日期偏移的表达式传递到最明细查询
    public void setOffsetDateExpression( QueryField commonDateField,AnalysisSingleModelSqlBuilder singleModelSqlBuilder,Map<String, AnalysisItemConfig> analysisItemConfigMap ) {

        try {

            AnalysisCalcMode calcMode = singleModelSqlBuilder.getAnalysisCalcMode();

            if(calcMode != null && calcMode.isCompare()){
                AnalysisItemConfig analysisItemConfig = analysisItemConfigMap.get(calcMode.getCode() + singleModelSqlBuilder.getCustomCompareIndex());

                AnalysisDataSet defaultCompareDateSet = this.getCompareDataSet(commonDateField, BIConsts.COMPARE_DATE_PLACEHOLDER, calcMode, analysisItemConfig);
                String defaultCompareDateExpression = defaultCompareDateSet.getDateFieldExpression(null);
                defaultCompareDateExpression = defaultCompareDateExpression.replace(String.format("%s.", defaultCompareDateSet.getName()
                ), "");
                singleModelSqlBuilder.setOffsetDateExpression(defaultCompareDateExpression);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    protected AnalysisSingleModelSqlBuilder getAnalysisSingleModelSqlBuilder(){
        if(singleModelSQLBuilder instanceof AnalysisSingleModelSqlBuilder){
            return (AnalysisSingleModelSqlBuilder) singleModelSQLBuilder;
        }
        if(singleModelSQLBuilder instanceof AnalysisSingleModelSqlBuilderAvgWrapper){
            AnalysisSingleModelSqlBuilderAvgWrapper wrapper = (AnalysisSingleModelSqlBuilderAvgWrapper) singleModelSQLBuilder;
            return wrapper.getDefaultAnalysisSingleModelSqlBuilder();
        }
        return null;
    }

    protected AnalysisDataSet getCompareDataSet(QueryField dateField, String dateFieldExpression,  AnalysisCalcMode calcMode, AnalysisItemConfig cfg){
        DateGranularity dateGranularity = DateGranularity.get(dateField.getQueryDateGranularity());
        AnalysisDataSet dataSet = null;

        //自定义对比
        if (AnalysisCalcMode.CUSTOM_COMPARE == calcMode &&  cfg != null) {
            // 因为自定义对比针对所有指标起效，则从任意有自定义对比分析的查询字段中获取分析
            dataSet = new CustomCompareAnalysisDataSet(dateFieldExpression, calcMode, cfg);
            dataSet.setConfig(this.config);
            return dataSet;
        }

        switch (dateGranularity) {
            case YEAR:
                dataSet = new YearAnalysisDataSet(dateFieldExpression, calcMode);
                break;
            case MONTH:
                dataSet = new MonthAnalysisDataSet(dateFieldExpression, calcMode);
                break;
            case QUARTER:
                dataSet = new QuarterAnalysisDataSet(dateFieldExpression, calcMode);
                break;
            case WEEK:
                dataSet = new WeekAnalysisDataSet(dateFieldExpression, calcMode);
                break;
            case DAY:
                dataSet = new DayAnalysisDataSet(dateFieldExpression, calcMode);
                dataSet.setConfig(this.config);
            default:
                break;
        }

        return dataSet;
    }
}
