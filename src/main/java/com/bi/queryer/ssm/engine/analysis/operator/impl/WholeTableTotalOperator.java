package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:48 2023-08-01
 * @Description 整表总计
 **/
public class WholeTableTotalOperator extends TotalOperator {

    /**
     * 行总计计算
     * - 交叉表：行汇总在上一步已计算，此处只需引用：因为交叉表涉及去重指标，所以必须在模型构建sql时处理行总计
     * @param cxt
     * @param measureField
     * @return
     */
    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        // 若指定了指标（如：占整表总计），则指标直接相加
        if(cxt.analysisItemConfig instanceof AnalysisTotalItemConfig){
            if(((AnalysisTotalItemConfig)cxt.analysisItemConfig).isZbTotal()){
                return this.total(cxt, measureField);
            }
        }

        // 若未指定指标，则是默认上一步已计算，此处只需引用
        String expression = String.format("%s.%s", cxt.currentDataSet.getName(), measureField.getCode());
        return expression;
    }

    /**
     * 交叉维度：交叉维度项下的指定指标列相加后再指标行相加汇总
     * 明细：所有指标列相加后再指标行相加汇总
     * @param cxt
     * @param measureField
     * @return
     */
    @Override
    public String total(OperatorContext cxt, QueryField measureField) {
        /*
        RowTotalOperator rowTotalOperator = new RowTotalOperator();
        String rowTotalExpression = rowTotalOperator.total(cxt, measureField);
        String calcExpression = String.format("sum(%s) over()", rowTotalExpression);
         */
        String calcExpression = "";
        if (cxt.isCrossDimensionQuery) {
            String analysisMeasureCode = measureField.getAnalysisConfig().getMeasureCode().split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
            calcExpression = String.format("%s.%s", cxt.currentDataSet.getName(), analysisMeasureCode + "_" + AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode());
        }

        //行总计占整表处理,需要去掉_r_total
        if (AnalysisCalcMode.ROW_TOTAL == AnalysisCalcMode.get(measureField.getAnalysisConfig().getRawCalcMode())) {
            calcExpression = String.format("%s.%s", cxt.currentDataSet.getName(), measureField.getMeta().getCode() + "_" + AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode());
        }
        return calcExpression;
    }

    /**
     * 只有交叉表才支持
     * @param config
     * @param model
     * @return
     */
    @Override
    public List<String> groupingSets(QueryConfigure config, StarModel model) {
        AnalysisTotalItemConfig analysisItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.WHOLE_TABLE);
        /*
        if(analysisItemConfig != null && analysisItemConfig.isZbTotal()){
            return super.groupingSets(config, model);
        }
         */

        List<QueryField> colFields = this.getColumnDimensions(config);
        List<String> groupingSets = super.groupingSets(config, model);
        if(BIUtil.isNotEmpty(colFields)) {
            String groupingSet = "()";
            groupingSets.add(groupingSet);
        }
        return groupingSets;
    }

    @Override
    public boolean isSupportPivot(AnalysisItemConfig itemConfig) {
        return true;
    }

    /**
     * 交叉表时，将行总计转为
     * @return
     */
    @Override
    public String pivot(QueryConfigure config, QueryField measure) {
        /*
        // 若是占整表总计，则指标不需要提转置，再最后一步直接相加
        AnalysisTotalItemConfig analysisItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.WHOLE_TABLE);
        if(analysisItemConfig != null && analysisItemConfig.isZbTotal()){
            return super.pivot(config, measure);
        }
         */

        String pivotExpression = "";

        List<String> conditions = new ArrayList<>();

        Long rowDimCount = config.getResult().getRowDimensions().stream().filter(f->!f.isAppend()).count();
        Long colDimCount = config.getResult().getFields().stream().filter(f->!f.isAppend()).filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).count();

        int groupingValue = NumberUtil.binaryToInt(StringUtils.rightPad("", new Long(rowDimCount + colDimCount).intValue(), "1"));

        // 添加分组标识
        conditions.add(String.format("%s.%s = %s", BIConsts.PIVOT_TABLE_ALIAS, BIConsts.GROUPING_VALUE, groupingValue));
        String conditionExpression = BIUtil.listToStr(conditions, " and ");
        String measureExpression = String.format("%s.%s", BIConsts.PIVOT_TABLE_ALIAS, measure.getAnalysisConfig().getMeasureCode());
        pivotExpression = function.ifExpression(conditionExpression, measureExpression, "null");
        pivotExpression = String.format("max(%s) over()", pivotExpression);
        return pivotExpression;
    }

    /**
     * 获取转置后的字段名称
     * @param config
     * @param measure
     * @return
     */
    @Override
    public String getPivotFieldName(QueryConfigure config, QueryField measure) {
        return measure.getCode();
    }

    @Override
    public List<ResultDataSetColumn> createTotalColumns(QueryConfigure config) {
        // 返回叶子节点，不需要添加多表头
        return this.createTotalLeafColumns(config);
    }

    @Override
    public List<ResultDataSetColumn> createTotalLeafColumns(QueryConfigure config) {
        List<ResultDataSetColumn> leftColumns = new ArrayList<>();
        // 那指标创建列
        List<ResultDataSetColumn> measureCols = new ArrayList<>();
        List<QueryField> measureFields = config.getResult().getMeasures().stream().filter(f->!f.isAppend()).collect(Collectors.toList());
        for(QueryField measureField : measureFields){
            AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(measureField.getAnalysisConfig().getRawCalcMode());
            if(analysisCalcMode != AnalysisCalcMode.WHOLE_TABLE_TOTAL){
                continue;
            }

            ResultDataSetColumn measureCol = new ResultDataSetColumn(measureField.getCode(), measureField.getTitle());
            measureCol.setId(measureCol.getCode());
            measureCol.setRawCode(measureCol.getCode());
            measureCol.setRawTitle(measureCol.getTitle());
            measureCol.setRawQueryArea(QueryArea.Measure.toString());
            measureCol.setDataType(DataType.Double.toString());
            measureCol.setTotal(false);
            measureCol.setWholeTableTotal(true);
            measureCol.setIsShow(Enabled.NO.getId());
            measureCol.setDataFormat(measureField.getMeta().getShowFormatExpression());

            measureCols.add(measureCol);
        }

        leftColumns.addAll(measureCols);

        return leftColumns;
    }

    /**
     * 初始化配置：添加行总计列
     * - 明细表：只添加一个汇总列
     * - 交叉表：按指标添加汇总列，即每个指标都有一个汇总列
     * @param config
     * @param cxt
     */
    @Override
    public void initQueryConfig(QueryConfigure config, QueryContext cxt) {
        List<QueryField> measureFields = config.getResult().getMeasures().stream().filter(f->!Enabled.value(f.getIsAnalysis()) && !f.isAppend()).collect(Collectors.toList());
        List<QueryField> totalMeasures = new ArrayList<>();

        //如果没有列维度，不出整表总计
        List<QueryField> colDimensions = config.getResult().getColDimensions().stream().filter(f->!f.isAppend()).collect(Collectors.toList());
        if(CollUtil.isEmpty(colDimensions)) {
            return;
        }

        for(QueryField measureField : measureFields){
            if (measureField.isAnalysisCalc()) {
                //整表总计不出分析四则运算
                continue;
            }
            QueryField rowTotalField = new QueryField(measureField.getMeta());
            String code = measureField.getCode() + "_" + AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode();
            rowTotalField.setId(code);
            rowTotalField.setCode(code);
            rowTotalField.setName(code);
            String title = BIUtil.isEmpty(measureField.getDisplayTitle()) ? measureField.getTitle() : measureField.getDisplayTitle();
            rowTotalField.setTitle(title);
            rowTotalField.setQueryArea(QueryArea.Measure);
            rowTotalField.setRawQueryArea(QueryArea.Measure);
            rowTotalField.setIsResult(true);
            rowTotalField.setResult(true);

            // 添加分析
            rowTotalField.setIsAnalysis(Enabled.YES.getId());

            AnalysisTotalItemConfig analysisItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.WHOLE_TABLE,measureField.getId());
            // 优先从总体配置中获取总计配置
            AnalysisTotalItemConfig totalItemConfig = analysisItemConfig != null ? analysisItemConfig.clone() : new AnalysisTotalItemConfig();
            totalItemConfig.setMeasureCode(measureField.getCode());
            totalItemConfig.setMeasureId(measureField.getId());
            totalItemConfig.setCalcMode(AnalysisCalcMode.WHOLE_TABLE_TOTAL.getCode());
            rowTotalField.setAnalysisConfig(totalItemConfig);
            totalMeasures.add(rowTotalField);

            // 添加行总计列的字段权限
            cxt.getAclFields().put(code, code);
        }

        // 最后添加到结果区域
        totalMeasures.forEach(f->{
            config.getResult().add(f, QueryArea.Measure);
        });

    }

    @Override
    public AnalysisTotalType getTotalType() {
        return AnalysisTotalType.WHOLE_TABLE;
    }
}
