package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:00 2023-09-14
 * @Description 贡献率算子
 **/
public class ContributionRateOperator extends BaseOperator {
    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        if (BIUtil.isEmpty(cxt.dimFields)) {
            return super.calc(cxt, measureField);
        }

        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        String measureCode = cfg.getMeasureCode();
        String diffValue;
        if (measureField.isTargetValue()) {
            // 计算目标值的贡献率
            diffValue = String.format(" %s.%s_%s_ctr_ratio ",
                    cxt.currentDataSet.getName(),
                    measureCode,
                    measureField.getAnalysisConfig().getTargetConfig().getTargetCalcMode());
        } else {
            // 差额
            diffValue = String.format(" COALESCE(%s.%s,0) - COALESCE(%s.%s,0) ",
                    cxt.currentDataSet.getName(),
                    measureCode,
                    cxt.analysisDataSet.getName(),
                    measureCode);
        }

        String ctrExpression = getCtrCalcExpression(
                cxt,
                diffValue,
                cxt.currentDataSet.getName(),
                cxt.analysisDataSet.getName(),
                BIConsts.GROUPING_VALUE);
        return ctrExpression;
    }

    public String getCtrCalcExpression(OperatorContext cxt,String diffValue,String tableAlias,String analysisTableAlias,String groupKey) {
        if (BIUtil.isEmpty(cxt.dimFields)) {
            return "null";
        }
        // 总和
        List<QueryField> colDims = cxt.config.getResult().getFields().stream().filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension && !f.isAppend())
                .collect(Collectors.toList());
        int colDimCount = colDims.size();

        AnalysisTotalItemConfig analysisTotalItemConfig = cxt.getConfig().getAnalysis().getTotal().getItems().stream()
                .filter(item -> AnalysisCalcMode.COL_SUBTOTAL.getCode().equals(item.getCalcMode())).findAny().orElse(null);
        List<String> dimIdList = new ArrayList<>();
        if (analysisTotalItemConfig != null && BIUtil.isNotEmpty(analysisTotalItemConfig.getDimIdList())) {
            dimIdList = analysisTotalItemConfig.getDimIdList();
        }

        List<String> dimCodeList = new ArrayList<>();
        if (BIUtil.isNotEmpty(dimIdList)) {
            dimIdList.forEach(id -> {
                QueryField queryField = cxt.config.getResult().getFieldById(id);
                if (queryField != null) {
                    dimCodeList.add(queryField.getCode());
                }
            });
        }

        int index = 0;
        List<QueryField> dimFields = cxt.dimFields;
        Map<Integer, String> groupingPartitionMap = new LinkedHashMap<>();
        List<String> partitionDimExpressions = new ArrayList<>();
        List<String> tmpSqlList = new ArrayList<>(dimFields.size());
        for (QueryField selectField : dimFields) {
            index++;
            String fieldGroupingBit = StringUtils.rightPad("", index, "0");
            String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount, "0");
            int groupingValue = NumberUtil.binaryToInt(groupingBit);
            String partitionSql = String.format(" COALESCE(%s,%s) ", tableAlias + "." + selectField.getCode(),
                    analysisTableAlias + "." + selectField.getCode());
            partitionDimExpressions.add(partitionSql);
            if (dimCodeList.contains(selectField.getCode()) || groupingValue == 0) {
                groupingPartitionMap.put(groupingValue, BIUtil.listToStr(tmpSqlList));
                tmpSqlList.clear();
                tmpSqlList.addAll(partitionDimExpressions);
            }
        }

        List<String> whenExpressionList = new ArrayList<>();
        String groupValueConstField = String.format(" COALESCE(%s,%s) ",
                tableAlias + "." + groupKey,
                analysisTableAlias + "." + groupKey);

        String colFieldPartitionExpression = "";
        if (colDimCount > 0) {
            colFieldPartitionExpression = String.format(" COALESCE(%s,%s) ", tableAlias + "." + colDims.get(0).getCode(),
                    analysisTableAlias + "." + colDims.get(0).getCode());
        }

        for (Integer groupingValue : groupingPartitionMap.keySet()) {
            String partitionDimExpression = groupingPartitionMap.get(groupingValue);

            if (colDimCount > 0) {
                if (StrUtil.isEmpty(partitionDimExpression)) {
                    partitionDimExpression += colFieldPartitionExpression;
                } else {
                    partitionDimExpression += "," + colFieldPartitionExpression;
                }
            }

            String whenExpression = String.format(
                    "when %s = %s then sum(if(%s = %s, %s, null)) over(%s %s)",
                    groupValueConstField,
                    groupingValue,
                    groupValueConstField,
                    groupingValue,
                    diffValue,
                    BIUtil.isEmpty(partitionDimExpression) ? "" : "partition by",
                    partitionDimExpression
            );
            whenExpressionList.add(whenExpression);
        }

        //列总计处理
        String fieldGroupingBit = StringUtils.rightPad("", -1, "0");
        String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount, "0");
        int groupingValue = NumberUtil.binaryToInt(groupingBit);

        String whenExpression = String.format(
                "when %s = %s then (%s)",
                groupValueConstField,
                groupingValue,
                diffValue
        );
        whenExpressionList.add(whenExpression);

        //行总计贡献度处理
        AnalysisTotalConfig totalCfg = cxt.getConfig().getAnalysis().getTotal();
        AnalysisTotalItemConfig rowTotalConfig = totalCfg.getItem(AnalysisTotalType.ROW_TOTAL);
        if (colDimCount > 0 && rowTotalConfig != null) {
            for (Integer rowTotalGroupingValue : groupingPartitionMap.keySet()) {
                String partitionDimExpression = groupingPartitionMap.get(rowTotalGroupingValue);

                String rowTotalWhenExpression = String.format(
                        "when %s = %s then sum(if(%s = %s, %s, null)) over(%s %s)",
                        groupValueConstField,
                        rowTotalGroupingValue + 1,
                        groupValueConstField,
                        rowTotalGroupingValue + 1,
                        diffValue,
                        BIUtil.isEmpty(partitionDimExpression) ? "" : "partition by",
                        partitionDimExpression
                );
                whenExpressionList.add(rowTotalWhenExpression);
            }

            //行总计的列总计
            whenExpressionList.add(String.format(
                    "when %s = %s then (%s)",
                    groupValueConstField,
                    groupingValue + 1,
                    diffValue
            ));
        }


        // 总和
        String totalValue = String.format("case %s else null end", BIUtil.listToStr(whenExpressionList, ""));
        totalValue = function.abs(totalValue);


        String ctrExpression = function.division(diffValue, totalValue, true);

        return ctrExpression;
    }

    public String getLodCtrExpression(OperatorContext cxt,String diffValue){
        return getCtrCalcExpression(cxt, diffValue,BIConsts.MAIN_TABLE_ALIAS,BIConsts.MAIN_TABLE_ALIAS, BIConsts.GROUPING_KEY);
    }

    @Override
    public boolean isSupportPivot(AnalysisItemConfig itemConfig) {
        //避免在有列维度的场景，行总计贡献率因为添加后缀，导致异常
        AnalysisCalcMode calcMode = AnalysisCalcMode.get(itemConfig.getRawCalcMode());
        if (AnalysisCalcMode.ROW_TOTAL == calcMode) {
            return true;
        }
        return false;
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
}
