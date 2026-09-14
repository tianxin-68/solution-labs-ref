package com.bi.queryer.ssm.util;

import cn.hutool.core.util.NumberUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisTotalAggType;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @Auther: contributor
 * @Date: 2025/10/17 17:04
 * @Description: 处理汇总相关
 */
public final class AnalysisTotalUtil {

    // 获取group value对应的维度分组, 用于自定义聚合方式汇总的计算
    // key 为grouping value, value为grouping partition维度编码
    public static Map<Integer, List<String>> getGroupingPartitionMap(QueryConfigure config, Function<String, String> fieldSqlSupplier) {
        Map<Integer, List<String>> groupingPartitionMap = new LinkedHashMap<>();
        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        if (!totalConfig.isActive() || !totalConfig.getAggConfig().isActive()) {
            //没有总计, 或者没配置自定义聚合方式，直接返回
            return groupingPartitionMap;
        }

        // 是否有列总计
        boolean hasRowTotal = totalConfig.hasAnalysisTotalItem(AnalysisTotalType.ROW_TOTAL);

        // 是否有整表总计
        boolean hasWholeTableTotal = totalConfig.hasAnalysisTotalItem(AnalysisTotalType.WHOLE_TABLE);

        // 获取维度字段数
        int rowDimCount = 0;
        int colDimCount = 0;
        List<QueryField> rowDimFields = new ArrayList<>();
        QueryField colDimField = null;
        for (QueryField selectField : config.getResult().getFields()) {
            if (selectField.isAppend()) {
                continue;
            }
            if (QueryArea.RowDimension.equals(selectField.getRawQueryArea())) {
                rowDimCount++;
                rowDimFields.add(selectField);
            } else if (QueryArea.ColumnDimension.equals(selectField.getRawQueryArea())) {
                colDimCount++;
                colDimField = selectField;
            }
        }

        if (colDimCount == 0 && rowDimCount == 0) {
            // 没有行维度也没有列维度， 不需要处理
            return groupingPartitionMap;
        }

        String colDimSql = fieldSqlSupplier.apply(colDimField == null ? null : colDimField.getCode());

        int index = 0;
        List<String> partitionDimExpressions = new ArrayList<>();
        for (QueryField selectField : rowDimFields) {
            index++;

            // 列小计
            String fieldGroupingBit = StringUtils.rightPad("", index, "0");
            String groupingBit = StringUtils.rightPad(fieldGroupingBit, rowDimCount, "1") + StringUtils.rightPad("", colDimCount, "0");
            int groupingValue = NumberUtil.binaryToInt(groupingBit);

            String partitionSql = fieldSqlSupplier.apply(selectField.getCode());
            partitionDimExpressions.add(partitionSql);
            List<String> partitionBy = new ArrayList<>(partitionDimExpressions);
            if (colDimSql != null) {
                //如果有列维度，补上
                partitionBy.add(colDimSql);
            }
            groupingPartitionMap.put(groupingValue, partitionBy);

            // 行总计
            if (hasRowTotal) {
                fieldGroupingBit = StringUtils.rightPad("", index, "0");
                groupingBit = StringUtils.rightPad(fieldGroupingBit, rowDimCount, "1") + StringUtils.rightPad("", colDimCount, "1");
                groupingValue = NumberUtil.binaryToInt(groupingBit);
                groupingPartitionMap.put(groupingValue, new ArrayList<>(partitionDimExpressions));
            }
        }

        // 普通列维度的列总计
        int groupingValue = NumberUtil.binaryToInt(StringUtils.rightPad("", rowDimCount, "1") + StringUtils.rightPad("", colDimCount, "0"));
        List<String> partitionBy = new ArrayList<>();
        if (colDimSql != null) {
            //如果有列维度，补上
            partitionBy.add(colDimSql);
        }
        groupingPartitionMap.put(groupingValue, partitionBy);

        // 行总计的列总计 和 整表总计
        if (hasRowTotal || hasWholeTableTotal) {
            groupingValue = NumberUtil.binaryToInt(StringUtils.rightPad("", rowDimCount + colDimCount, "1"));
            groupingPartitionMap.put(groupingValue, new ArrayList<>());
        }
        return groupingPartitionMap;
    }

    // 处理汇总指标的自定义聚合方式
    //默认：根据指标自身聚合方式计算（和现在的默认计算方式相同）
    //求和(sum)：使用该层汇总的下一层所查询数据累加求和
    //均值(avg)：使用该层汇总的下一层所查询数据累加求和后求均值
    //平均个数（sum/默认）：使用该层汇总的下一层所查询数据累加求和/指标自身聚合方式计算数据
    public static String buildTotalMeasureAggExpression(QueryField selectField, String selectExpression,
                                                        Map<Integer, List<String>> groupingPartitionMap,
                                                        Function<String, String> groupValueExpressionSupplier,
                                                        String groupKey) {
        if (groupingPartitionMap.size() <= 1) {
            return selectExpression;
        }

        if (AnalysisTotalAggType.isDefault(selectField.getTotalAggType())) {
            // 默认不做任何处理
            return selectExpression;
        }

        String groupValueExpr = groupValueExpressionSupplier.apply(groupKey);
        StringBuilder sb = new StringBuilder();
        String aggType;
        if (AnalysisTotalAggType.AVG_COUNT.getCode().equals(selectField.getTotalAggType())) {
            // 求平均个数
            sb.append("( ");
            aggType = AnalysisTotalAggType.SUM.getCode();
        } else {
            aggType = selectField.getTotalAggType();
        }
        sb.append("case ");
        for (Map.Entry<Integer, List<String>> entry : groupingPartitionMap.entrySet()) {
            int groupingValue = entry.getKey();
            List<String> partitionFields = entry.getValue();
            // 汇总只能从明细汇上去, 先找出明细数据
            String detailSelectExpr = String.format(" if( %s = 0 ,%s ,null ) ", groupValueExpr, selectExpression);
            if (groupingValue == 0) {
                sb.append(" when ").append(groupValueExpr).append(" = 0 then ").append(selectExpression);
            } else if (CollectionUtils.isEmpty(partitionFields)) {
                String sql = String.format("%s(%s) over ()", aggType, detailSelectExpr);
                sb.append(" when ").append(groupValueExpr).append(" = ").append(groupingValue).append(" then ").append(sql);
            } else {
                String sql = String.format("%s(%s) over ( partition by %s)", aggType, detailSelectExpr, BIUtil.listToStr(partitionFields));
                sb.append(" when ").append(groupValueExpr).append(" = ").append(groupingValue).append(" then ").append(sql);
            }
        }
        sb.append(" else null end");

        if (AnalysisTotalAggType.AVG_COUNT.getCode().equals(selectField.getTotalAggType())) {
            String denominator = String.format(" if( %s = 0 ,1 ,%s * %s ) ", groupValueExpr, selectExpression, BIConsts.INT_TO_DOUBLE_PRECISION);
            sb.append(") / ").append(denominator);
        }
        return sb.toString();
    }
}
