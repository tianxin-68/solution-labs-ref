package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:15 2023-07-27
 * @Description 列总计算子
 **/
public class ColumnTotalOperator extends TotalOperator {

    /**
     * 列总计的grp_v
     */
    public Integer columnTotalGroupingValue = 0;

    public ColumnTotalOperator(){}

    public ColumnTotalOperator(Integer columnTotalGroupingValue){
        this.columnTotalGroupingValue = columnTotalGroupingValue;
    }



    /**
     * 所有维度下的当前指标行相加汇总：用于列总计占比的分母
     * @param cxt
     * @param measureField
     * @return
     */
    @Override
    public String total(OperatorContext cxt, QueryField measureField) {

        List<QueryField> colDims = cxt.config.getResult().getFields().stream().filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).collect(Collectors.toList());

        String partitionByExpression = "";
        if(CollUtil.isNotEmpty(colDims)){
            partitionByExpression = String.format(" PARTITION by %s.%s ",cxt.currentDataSet.getName(),colDims.get(0).getCode());
        }

        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        String calcExpression = "";
        String formatString = "";
            // 占比：列总计、列小计
            formatString = "sum(if(%s.%s != %s,null,%s.%s)) over(%s)";
            calcExpression = String.format(
                    formatString,
                    cxt.currentDataSet.getName(),
                    BIConsts.GROUPING_VALUE,
                    this.columnTotalGroupingValue,
                    cxt.currentDataSet.getName(),
                    cfg.getMeasureCode(),
                    partitionByExpression
            );
        return calcExpression;
    }

    public String total(OperatorContext cxt,String measureValue,String tableAlias,String groupKey ) {

        List<QueryField> colDims = cxt.config.getResult().getFields().stream().filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension && !f.isAppend()).collect(Collectors.toList());

        String partitionByExpression = "";
        if (CollUtil.isNotEmpty(colDims)) {
            partitionByExpression = String.format(" PARTITION by %s.%s ", tableAlias, colDims.get(0).getCode());
        }

        String calcExpression = "";
        String formatString = "";
        // 占比：列总计、列小计
        formatString = "sum(if(%s.%s != %s,null,%s)) over(%s)";
        calcExpression = String.format(
                formatString,
                tableAlias,
                groupKey,
                this.columnTotalGroupingValue,
                measureValue,
                partitionByExpression
        );
        return calcExpression;
    }


    @Override
    public List<String> groupingSets(QueryConfigure config, StarModel model) {
        List<QueryField> resultFields = model.getFields().stream().filter(f -> f.getIsResult()).collect(Collectors.toList());
        List<String> groupingSets = new ArrayList<>();
        if (resultFields == null || resultFields.size() == 0) {
            return groupingSets;
        }

        // 列维度字段编码列表：注意，此处必须通过字段所属原始查询区域获取列维度字段，因此处列维度已被转为行维度
        List<QueryField> colDimensions = this.getColumnDimensions(config);
        List<String> colFieldCodes = colDimensions.stream().map(f-> f.getCode()).collect(Collectors.toList());

        Set<String> totalFieldNames = new LinkedHashSet<>();

        // 对结果字段按config排序
        resultFields = FieldUtil.sortByShowOrder(config, resultFields);
        for (QueryField field : resultFields) {
            if (field.isAppend()) { // 若是计算字段附带的结果字段，则不进行分组计算
                continue;
            }
            // 只取维度
            if (!field.isMeasure() && model.isExistsByMetaCode(field)) {
                String fieldName = String.format("%s.%s", model.getFactTable().getAlias(), field.getCode());
                if(BIUtil.isNotEmpty(fieldName)) {
                    // 列维度
                    if(colFieldCodes.contains(field.getCode())){
                        totalFieldNames.add(fieldName);
                    }
                }
            }
        }

        groupingSets.add(String.format("(%s)", BIUtil.listToStr(totalFieldNames)));
        return groupingSets;
    }

    @Override
    public List<QueryField> orderByFields(QueryConfigure config) {
        // 列总计，放置到首行，避免limit丢失
        List<QueryField> orderByFields = new ArrayList<>();
        orderByFields.addAll(super.orderByFields(config));

        QueryField totalField = new QueryField();
        totalField.setCode(BIConsts.GROUPING_VALUE);
        totalField.setSortType(FieldSortType.ASC);
        orderByFields.add(totalField);
        return orderByFields;
    }

    @Override
    public AnalysisTotalType getTotalType() {
        return AnalysisTotalType.COL_TOTAL;
    }
}
