package com.bi.queryer.ssm.engine.pivot;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.BIConsts;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-23  13:48
 * @Description: 指标放在行上的场景
 */
public class MeasurePivotEngine extends PivotEngine {

    public MeasurePivotEngine(QueryConfigure queryConfig, ResultDataSet resultDataSet, QueryEngine queryEngine) {
        super(queryConfig, resultDataSet, queryEngine);
    }

    @Override
    public void buildRowDimColumns(List<ResultDataSetColumn> columns) {
        super.buildRowDimColumns(columns);

        //添加指标
        ResultDataSetColumn rowColumn = new ResultDataSetColumn();
        rowColumn.setTitle(BIConsts.ALL_MEASURE_NAME);
        rowColumn.setCode(BIConsts.ALL_MEASURE_CODE);
        rowColumn.setId(BIConsts.ALL_MEASURE_CODE);
        rowColumn.setRawQueryArea(QueryArea.RowDimension.toString());
        rowColumn.setChildren(new ArrayList<>());
        columns.add(rowColumn);
    }

    /**
     * 构建指标列
     *
     * @param columns
     */
    public void buildMeasureColumnsWithoutColDim(List<ResultDataSetColumn> columns) {
        //构造指标+同环比
        if (CollUtil.isNotEmpty(pivotMeta.getAnalysisCols().values())) {
            for (ResultDataSetColumn analysis : pivotMeta.getAnalysisCols().values()) {
                //不出占行总计
                AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(analysis.getCalcMode());
                if (AnalysisCalcMode.ZB_ROW_TOTAL == analysisCalcMode) {
                    continue;
                }

                ResultDataSetColumn analysisCol = analysis.clone();
                analysisCol.setParentCode("");
                analysisCol.setRawCode(pivotMeta.buildAnalysisKey(analysisCol));
                columns.add(analysisCol);
            }
        } else {
            //需要新增一个当期值的列
            ResultDataSetColumn col = new ResultDataSetColumn();
            col.setId(this.pivotMeta.COL_CUR_CODE);
            col.setCode(this.pivotMeta.COL_CUR_CODE);
            col.setTitle(this.pivotMeta.COL_CUR_TITLE);
            col.setDataType(DataType.Double.toString());
            col.setRawCode("_");
            col.setRawQueryArea(QueryArea.Measure.toString());
            columns.add(col);
        }

    }

    /**
     * 构建末级节点
     */
    public ResultDataSetColumn buildColColumns(String colDimCode, String colDimValue, int idx) {
        ResultDataSetColumn column = new ResultDataSetColumn();
        if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(colDimValue)) {
            column.setCode(BIConsts.ROW_TOTAL_COLUMN_CODE + "_" + idx);
            column.setId(BIConsts.ROW_TOTAL_COLUMN_CODE);
        } else {
            column.setCode("c_d_" + idx);
            column.setId(colDimValue + BIConsts.SEPARATOR);
        }

        column.setRawQueryArea(QueryArea.ColumnDimension.toString());
        column.setTitle(colDimValue);
        column.setRawCode(colDimCode);
        column.setRawTitle(pivotMeta.getDimCodeTitleMap().get(colDimCode));
        //构造同环比
        for (ResultDataSetColumn thb : pivotMeta.getAnalysisCols().values()) {
            // 目标值不出行总计
//            if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(colDimValue) &&
//                    thb.getTargetConfig() != null && thb.getTargetConfig().isActive()) {
//                continue;
//            }
            ResultDataSetColumn thbCol = thb.clone();
            String analysisKey = pivotMeta.buildAnalysisKey(thbCol);
            if (BIConsts.ROW_TOTAL_COLUMN_CODE.equals(colDimValue) && "_".equals(analysisKey)) {
                thbCol.setCode(column.getCode() + "__" + AnalysisCalcType.REAL_VALUE.getCode());
            } else {
                thbCol.setCode(column.getCode() + "_" + analysisKey);
            }
            thbCol.setParentCode(column.getCode());
            thbCol.setId(thbCol.getCode());
            thbCol.setRawCode(thbCol.getCalcMode() + "_" + thbCol.getCalcType());
            column.addChild(thbCol);
        }
        return column;
    }

    /**
     * 添加行维度的结果，此处扩展指标
     *
     * @param result
     */
    public void extendRowResultList(List<List<String>> result, List<String> list, boolean emptyRowDim) {
        for (String measureCode : pivotMeta.getMeasureCols().keySet()) {
            List<String> ls = new ArrayList<>(list);
            ls.add(measureCode);
            result.add(ls);
        }

    }

    public void buildColTotalResult(List<String> rows, List<List<String>> result) {
        for (String measureCode : pivotMeta.getMeasureCols().keySet()) {
            List<String> colTotalValueList = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (i == 0) {
                    colTotalValueList.add(AnalysisTotalType.COL_TOTAL.getCode());
                } else if (i == rows.size() - 1) {
                    colTotalValueList.add(measureCode);
                } else {
                    colTotalValueList.add("");
                }
            }
            result.add(colTotalValueList);
        }
    }
}
