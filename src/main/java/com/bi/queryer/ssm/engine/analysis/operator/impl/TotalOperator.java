package com.bi.queryer.ssm.engine.analysis.operator.impl;

import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.enums.QueryArea;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 14:39 2023-08-03
 * @Description 总计/小计算子
 **/
public abstract class TotalOperator extends BaseOperator {

    /**
     * 获取排序字段
     * @param config
     * @return
     */
    public List<QueryField> orderByFields(QueryConfigure config){
        List<QueryField> resultFields = config.getResult().getFields();
        List<QueryField> orderByFields = resultFields.stream()
                .filter(f->f.getSortType() != FieldSortType.NONE && f.getRawQueryArea() != QueryArea.ColumnDimension)
                .collect(Collectors.toList());
        return orderByFields;
    }

    /**
     * 指标相加
     * @param cxt
     * @param measureField
     * @return
     */
    public String total(OperatorContext cxt, QueryField measureField){
        return "null";
    }

    public abstract AnalysisTotalType getTotalType();

    /**
     * 创建总计列
     * @param config
     * @return
     */
    public List<ResultDataSetColumn> createTotalColumns(QueryConfigure config){
        return new ArrayList<>();
    }

    /**
     * 创建总计叶子列：用于交叉表或多表头
     * @param config
     * @return
     */
    public List<ResultDataSetColumn> createTotalLeafColumns(QueryConfigure config){
        return new ArrayList<>();
    }

}
