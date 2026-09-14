package com.bi.queryer.ssm.engine.analysis.cross;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.SingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.dataset.*;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.cross.CrossDimensionSqlBuilder;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import java.util.*;

/**
 * @Author contributor
 * @Date 19:49 2024-07-24
 * @Description 分析场景的交叉表sql构建器
 **/
public class AnalysisCrossDimensionSqlBuilder extends CrossDimensionSqlBuilder {

    protected Map<String, String> analysisFragments = new LinkedHashMap<>();
    protected Map<String, AnalysisItemConfig> analysisItemConfigMap = new HashMap<>();

    public AnalysisCrossDimensionSqlBuilder(QueryEngine engine) {
        super(engine);
    }

    @Override
    protected List<String> buildSelectFragments() {
        // 记录分析表达式，用于后续调整字段顺序使用
        this.analysisFragments = new LinkedHashMap<>(); // <key=expression, value=fieldCode>

        // 记录指标分析配置
        this.analysisItemConfigMap = new HashMap<>();

        List<String> selectFragments = super.buildSelectFragments();

        // 调整分析字段(行总计/整表)顺序：默认放置到最后
        selectFragments.removeAll(analysisFragments.keySet());
        selectFragments.addAll(analysisFragments.keySet());

        // 添加总计标记
        // 注意：此处需要使用min，不要使用max：解决列总计、列小计、行总计、整表总计同时存在时grouping值丢失的问题
        selectFragments.add(String.format("%s.%s as %s", tableAlias, BIConsts.GROUPING_VALUE, BIConsts.GROUPING_VALUE));

        // 添加对比日期
        //日期汇总不出对比日期
        if(!config.isAggQuery()){
            String compareDateExpression = this.buildCompareDateFragment();
            if(BIUtil.isNotEmpty(compareDateExpression)) {
                selectFragments.add(compareDateExpression);
            }
        }

        return selectFragments;
    }

    @Override
    public void buildPivotColDimValues() {
        super.buildPivotColDimValues();

        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        if (totalConfig.isActive()) {
            for (AnalysisTotalItemConfig item : totalConfig.getItems()) {
                if (AnalysisTotalType.ROW_TOTAL == item.getTotalType()) {
                    if (config.getResult().getPivotConfig().getColDimValues().contains(BIConsts.ROW_TOTAL_COLUMN_CODE)) {
                        continue;
                    }
                    config.getResult().getPivotConfig().getColDimValues().add(BIConsts.ROW_TOTAL_COLUMN_CODE);
                }
            }
        }
    }

    @Override
    protected String buildSelectMeasureExpression(QueryField measureField) {
        if(!Enabled.value(measureField.getIsAnalysis())) {
            return super.buildSelectMeasureExpression(measureField);
        }
        BaseOperator operator = OperatorFactory.getOperator(measureField.getAnalysisConfig().getCalcMode());
        String expression = "null";
        if(operator != null && operator.isSupportPivot(measureField.getAnalysisConfig())) {
            expression = operator.pivot(config, measureField); // String.format(" null as %s", fieldAlias);
        }
        String fieldAlias = this.getMeasureAlias(measureField);
        if(BIUtil.isNotEmpty(expression)) {
            String selectFragment = String.format("%s as %s", expression, fieldAlias);
            analysisFragments.put(selectFragment, measureField.getCode());
        }

        AnalysisItemConfig analysisItemConfig = measureField.getAnalysisConfig();
        if(analysisItemConfig != null) {
            int idx = analysisItemConfig.getCompareIndex() == null ? -1 : analysisItemConfig.getCompareIndex();
            String calcMode = analysisItemConfig.getRawThbCalcMode().getCode();
            analysisItemConfigMap.put(calcMode + idx, analysisItemConfig);
        }
        return expression;
    }

    @Override
    protected String getMeasureAlias(QueryField measureField) {
        if (!Enabled.value(measureField.getIsAnalysis())) {
            return super.getMeasureAlias(measureField);
        }
        String fieldAlias = super.getMeasureAlias(measureField);
        BaseOperator operator = OperatorFactory.getOperator(measureField.getAnalysisConfig().getCalcMode());
        if (operator != null && operator.isSupportPivot(measureField.getAnalysisConfig())) {
            fieldAlias = operator.getPivotFieldName(config, measureField);
        }

        //行总计处理
        if (AnalysisCalcMode.ROW_TOTAL == AnalysisCalcMode.get(measureField.getAnalysisConfig().getRawCalcMode())) {
            fieldAlias = operator.getPivotFieldName(config, measureField);
        }

        return fieldAlias;
    }

    /**
     * 获取对比日期sql片段
     * @return
     */
    protected String buildCompareDateFragment(){
        String compareDateExpression = "";
        // 添加对比日期
        QueryField commonDateField = config.getResultCommonDateField();
        if(commonDateField == null || commonDateField.isAggQuery()){
            return compareDateExpression;
        }
        AnalysisSingleModelSqlBuilder singleModelSqlBuilder = this.getAnalysisSingleModelSqlBuilder();
        if(commonDateField != null && singleModelSqlBuilder != null){
            AnalysisCalcMode calcMode = singleModelSqlBuilder.getAnalysisCalcMode();
            if(calcMode != null && calcMode.isCompare()){
                String compareDateCode = commonDateField.getCode() + BIConsts.COMPARE_DATE_SUFFIX;
                AnalysisItemConfig analysisItemConfig = analysisItemConfigMap.get(calcMode.getCode() + singleModelSqlBuilder.getCustomCompareIndex());
                String commonDateFullName = String.format("%s.%s", BIConsts.PIVOT_TABLE_ALIAS, commonDateField.getCode());
                AnalysisDataSet dateSet = this.getCompareDataSet(commonDateField, commonDateFullName , calcMode, analysisItemConfig);
                dateSet.setConfig(config);
                String offsetDateExpression = dateSet.getDateFieldExpression(null);

                //业务日历处理
                if(config.getSettings().isBusinessCalendar()&& !calcMode.isPromoTb()){
                    compareDateExpression = String.format("(%s) as %s", commonDateFullName, compareDateCode);
                }else{
                    compareDateExpression =String.format("(%s) as %s", offsetDateExpression, compareDateCode);
                }
            }
        }

        return compareDateExpression;
    }

    protected AnalysisSingleModelSqlBuilder getAnalysisSingleModelSqlBuilder(){
        SingleModelSqlBuilder singleModelSqlBuilder = this.engine.getSqlBuilder().getSingleModelSQLBuilder();
        if(singleModelSqlBuilder instanceof AnalysisSingleModelSqlBuilder){
            return (AnalysisSingleModelSqlBuilder) singleModelSqlBuilder;
        }
        if(singleModelSqlBuilder instanceof AnalysisSingleModelSqlBuilderAvgWrapper){
            AnalysisSingleModelSqlBuilderAvgWrapper wrapper = (AnalysisSingleModelSqlBuilderAvgWrapper) singleModelSqlBuilder;
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
