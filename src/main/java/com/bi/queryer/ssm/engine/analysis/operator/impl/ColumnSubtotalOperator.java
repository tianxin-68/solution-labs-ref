package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.cross.CrossDimensionQueryEngine;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 18:04 2023-07-31
 * @Description 列小计
 **/
public class ColumnSubtotalOperator extends ColumnTotalOperator {

    /**
     * 最后一个维度的指标行相加汇总：用于列小计占比的分母
     *
     * @param cxt
     * @param measureField
     * @return
     */
    @Override
    public String total(OperatorContext cxt, QueryField measureField) {
        List<String> totalOverFragments = new ArrayList<>(); // 分类汇总的窗口表达式
        int index = -1;
        for (QueryField selectField : cxt.dimFields) {
            index++;
            if (index < cxt.dimFields.size() - 1) {
                totalOverFragments.add(String.format("%s.%s", cxt.currentDataSet.getName(), selectField.getCode()));
            }
        }


        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        String calcExpression = "";
        String formatString = "";
        String partitionBy = "";
        if (BIUtil.isEmpty(totalOverFragments)) {
            return super.total(cxt, measureField);
        }
        partitionBy = String.format("partition by %s", BIUtil.listToStr(totalOverFragments));
        formatString = "sum(if(%s.%s > 0,null,%s.%s)) over(%s)";
        calcExpression = String.format(
                formatString,
                cxt.currentDataSet.getName(),
                BIConsts.GROUPING_VALUE,
                cxt.currentDataSet.getName(),
                cfg.getMeasureCode(),
                partitionBy
        );
        return calcExpression;
    }

    @Override
    public List<String> groupingSets(QueryConfigure config, StarModel model) {
        AnalysisTotalConfig totalCfg = config.getAnalysis().getTotal();
        List<String> groupingSets = new ArrayList<>();
        if (!totalCfg.isActive()) {
            return groupingSets;
        }

        List<QueryField> colDimFields = this.getColumnDimensions(config);

        AnalysisTotalItemConfig columnSubtotalCfg = totalCfg.getItem(AnalysisTotalType.COL_SUBTOTAL);
        if (columnSubtotalCfg == null || !columnSubtotalCfg.isActive()) {
            return groupingSets;
        }

        // 行维度
        List<QueryField> rowDimFields = config.getResult().getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());

        if (BIUtil.isEmpty(rowDimFields)) {
            return groupingSets;
        }

        final List<String> groupingDimCodeList = this.getTotalDimCodeList(config, columnSubtotalCfg);
        // 按行维度对grouping维度编码的次序倒序排序：保障多个小计是的顺序是从多到少
        List<String> sortedGroupingDimCodeList = rowDimFields.stream().filter(f -> groupingDimCodeList.contains(f.getCode())).map(f -> f.getCode()).collect(Collectors.toList());
        sortedGroupingDimCodeList = ListUtil.reverse(sortedGroupingDimCodeList);

        for (String groupingDimCode : sortedGroupingDimCodeList) {
            Set<String> totalFieldNames = new LinkedHashSet<>();
            Set<String> parentDimNames = new LinkedHashSet<>(); // 上级维度名称列表
            // 先添加行维度
            for (QueryField field : rowDimFields) {
                if (model != null && !model.isExistsByMetaCode(field)) {
                    continue;
                }
                String fieldName = field.getCode();

                if (model != null) {
                    fieldName = String.format("%s.%s", model.getFactTable().getAlias(), field.getCode());
                }
                // 小计
                if (groupingDimCode.equalsIgnoreCase(field.getCode())) {
                    // 先添加父维度，再添加当前维度
                    totalFieldNames.addAll(parentDimNames);
                    totalFieldNames.add(fieldName);
                }
                parentDimNames.add(fieldName);
            }

            // 再添加列维度
            for (QueryField colField : colDimFields) {
                if (model != null) {
                    totalFieldNames.add(String.format("%s.%s", model.getFactTable().getAlias(), colField.getCode()));
                } else {
                    totalFieldNames.add(String.format("%s", colField.getCode()));
                }
            }

            if (BIUtil.isNotEmpty(totalFieldNames)) {
                groupingSets.add(String.format("(%s)", BIUtil.listToStr(totalFieldNames)));
            }
        }

        return groupingSets;
    }

    protected List<String> getTotalDimCodeList(QueryConfigure queryConfig, AnalysisTotalItemConfig itemConfig) {
        List<String> codeList = new ArrayList<>();
        List<String> dimIdList = itemConfig.getDimIdList();
        if (BIUtil.isEmpty(dimIdList)) {
            return codeList;
        }

        for (String id : dimIdList) {
            QueryField queryField = queryConfig.getResult().getFieldById(id);
            if (queryField != null) {
                codeList.add(queryField.getCode());
            }
        }
        return codeList;
    }

    @Override
    public List<QueryField> orderByFields(QueryConfigure config) {
        List<QueryField> orderByFields = new ArrayList<>();
        List<QueryField> resultFields = config.getResult().getFields();
        List<QueryField> orderByDimensions = resultFields.stream().
                filter(f -> f.isDimension()
                        && f.getRawQueryArea() == QueryArea.RowDimension
                        && !f.isAppend())
                .collect(Collectors.toList());
        if (BIUtil.isNotEmpty(orderByDimensions)) {
            // 维度排序：列小计时默认所有维度排序，去掉默认按第一个指标排序
            //orderByDimensions.remove(orderByDimensions.size() - 1);
        }

        // 添加列小计标识：保证列小计在组内的最后面
        QueryField groupingField = new QueryField();
        groupingField.setCode(BIConsts.GROUPING_VALUE);
        groupingField.setSortType(FieldSortType.ASC);
        orderByDimensions.add(groupingField);

        List<QueryField> orderByMeasures = resultFields.stream().filter(f -> f.getSortType() != FieldSortType.NONE && f.isMeasure()).collect(Collectors.toList());
        orderByFields.addAll(orderByDimensions);
        orderByFields.addAll(orderByMeasures);
        return orderByFields;
    }

    @Override
    public AnalysisTotalType getTotalType() {
        return AnalysisTotalType.COL_SUBTOTAL;
    }
}
