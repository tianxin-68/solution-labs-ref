package com.bi.queryer.ssm.engine.analysis.operator.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.Fraction;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryResult;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.enums.PercentFieldRatioUnitType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 20:20 2023-07-31
 * @Description 占比：列小计
 **/
public class ZbColumnSubtotalOperator extends ZbColumnTotalOperator{

    @Override
    public String calc(OperatorContext cxt, QueryField measureField) {
        Map<String, QueryField> measureFieldMap = cxt.measureFields.stream().collect(Collectors.toMap(QueryField::getId, QueryField -> QueryField, (f1, f2) -> f1));
        AnalysisItemConfig cfg = measureField.getAnalysisConfig();
        QueryField rawMeasureField = measureFieldMap.get(cfg.getMeasureId());
        String calcExpression = "";

        String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

        // 百分比指标
        if (FieldUtil.isPercentField(rawMeasureField)) {
            ratioUnit = cfg.getPercentFieldRatioUnit();
        }

        String measureValue = String.format("%s.%s", cxt.currentDataSet.getName(), cfg.getMeasureCode());
        calcExpression = getCalcExpression(cxt, measureValue, cxt.currentDataSet.getName(), BIConsts.GROUPING_VALUE, ratioUnit);

        return calcExpression;
    }

    @Override
    public String lodCalc(OperatorContext cxt,String measureName,String ratioUnit) {
        return getCalcExpression(cxt, measureName, BIConsts.MAIN_TABLE_ALIAS, BIConsts.GROUPING_KEY,ratioUnit);
    }

    public String getCalcExpression(OperatorContext cxt,String measureValue,String tableAlias,String groupKey,String ratioUnit) {

        String calcExpression = "";
        String formatString = "";

        /**
         * 获取分子和分母
         */
        Fraction fraction = getCalcFraction(cxt, measureValue, tableAlias, groupKey);
        formatString = "(%s * %s)/(%s)";

        //如果是百分比指标，占比 = 当期 - 汇总
        PercentFieldRatioUnitType ratioUnitType = PercentFieldRatioUnitType.get(ratioUnit);
        if(PercentFieldRatioUnitType.PT == ratioUnitType){
            formatString = "(%s * %s) - (%s)";
        }

        calcExpression = String.format(
                formatString,
                fraction.getNumerator(),
                BIConsts.INT_TO_DOUBLE_PRECISION,
                fraction.getDenominator()
        );

        calcExpression = function.tryCatch(calcExpression);

        return calcExpression;
    }


    /**
     * 获取占比的分子与分母
     */
    public Fraction getCalcFraction(OperatorContext cxt,String measureValue,String tableAlias,String groupKey){

        Fraction fraction = new Fraction();

        AnalysisTotalConfig totalCfg = cxt.getConfig().getAnalysis().getTotal();
        AnalysisTotalItemConfig columnSubtotalCfg = totalCfg.getItem(AnalysisTotalType.COL_SUBTOTAL);

        ColumnSubtotalOperator columnSubtotalOperator = new ColumnSubtotalOperator();
        List<String> dimCodeList = columnSubtotalOperator.getTotalDimCodeList(cxt.getConfig(), columnSubtotalCfg);


        /** 模板sql
         *  case when ds_cur._grp_v = 0 then sum(if(ds_cur._grp_v = 3, ds_cur.pay_orderid,NULL )) over(
         *           partition by ds_cur.dt,
         *           ds_cur.dim_analysis_business
         *         )
         *         when ds_cur._grp_v = 3 then sum(if(ds_cur._grp_v = 7, ds_cur.pay_orderid,null)) over(
         *           partition by ds_cur.dt
         *         )
         *         when ds_cur._grp_v = 7 then ds_cur.pay_orderid
         *         else null end
         *
         */
        List<QueryField> colDims = cxt.config.getResult().getFields().stream().filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension && !f.isAppend())
                .collect(Collectors.toList());
        int colDimCount = colDims.size();

        int index = 0;
        List<QueryField> dimFields = cxt.dimFields;
        Map<Integer, String> groupingPartitionMap = new LinkedHashMap<>();


        /**
         * 记录分组grp_v的上下级关系
         */
        Map<Integer, Integer> groupingValueParentMap = new HashMap<>();
        Integer parentGroupingValue = null;

        List<String> partitionDimExpressions = new ArrayList<>();

        for (QueryField selectField : dimFields) {
            index++;
            String fieldGroupingBit = StringUtils.rightPad("", index, "0");
            String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount, "0");
            int groupingValue = NumberUtil.binaryToInt(groupingBit);

            partitionDimExpressions.add(tableAlias + "." + selectField.getCode());

            //只记录配置了列小计的维度
            if (dimCodeList.contains(selectField.getCode())) {
                groupingPartitionMap.put(groupingValue, BIUtil.listToStr(partitionDimExpressions));
                groupingValueParentMap.put(groupingValue, parentGroupingValue);
                parentGroupingValue = groupingValue;
            }
        }

        //添加基础行的分组信息
        groupingPartitionMap.put(0, "");
        groupingValueParentMap.put(0, parentGroupingValue);

        List<String> whenExpressionList = new ArrayList<>();
        String groupValueConstField = tableAlias + "." + groupKey;

        for (Integer groupingValue : groupingPartitionMap.keySet()) {
            Integer grp_v_parent = groupingValueParentMap.get(groupingValue);
            String partitionDimExpression = groupingPartitionMap.get(grp_v_parent);

            if (colDimCount > 0) {
                if (StrUtil.isEmpty(partitionDimExpression)) {
                    partitionDimExpression += tableAlias + "." + colDims.get(0).getCode();
                } else {
                    partitionDimExpression += "," + tableAlias + "." + colDims.get(0).getCode();
                }

            }

            String whenExpression = "";
            //没有上级分组，取其本身，占比= 100%
            if (grp_v_parent == null) {
                whenExpression = String.format(
                        "when %s = %s then %s ",
                        groupValueConstField,
                        groupingValue,
                        measureValue
                );
            } else {
                whenExpression = String.format(
                        "when %s = %s then sum(if(%s = %s, %s, null)) over(%s %s)",
                        groupValueConstField,
                        groupingValue,
                        groupValueConstField,
                        grp_v_parent,
                        measureValue,
                        BIUtil.isEmpty(partitionDimExpression) ? "" : "partition by",
                        partitionDimExpression
                );
            }

            whenExpressionList.add(whenExpression);
        }

        //处理行总计的占列小计
        AnalysisTotalItemConfig rowTotalConfig = totalCfg.getItem(AnalysisTotalType.ROW_TOTAL);
        if (colDimCount > 0 && rowTotalConfig != null) {
            for (Integer groupingValue : groupingPartitionMap.keySet()) {
                Integer grp_v_parent = groupingValueParentMap.get(groupingValue);
                String partitionDimExpression = groupingPartitionMap.get(grp_v_parent);

                String whenExpression = "";
                //没有上级分组，取其本身，占比= 100%
                if (grp_v_parent == null) {
                    whenExpression = String.format(
                            "when %s = %s then %s ",
                            groupValueConstField,
                            groupingValue + 1,
                            measureValue
                    );
                } else {
                    whenExpression = String.format(
                            "when %s = %s then sum(if(%s = %s, %s, null)) over(%s %s)",
                            groupValueConstField,
                            groupingValue + 1,
                            groupValueConstField,
                            grp_v_parent + 1,
                            measureValue,
                            BIUtil.isEmpty(partitionDimExpression) ? "" : "partition by",
                            partitionDimExpression
                    );
                }

                whenExpressionList.add(whenExpression);
            }
        }

        String denominator = String.format("case %s else null end", BIUtil.listToStr(whenExpressionList, ""));

        fraction.setNumerator(measureValue);
        fraction.setDenominator(denominator);
        return fraction;

    }

    /**
     * 列小计时，需要计算分母，即把需要汇总的列配置到汇总配置中
     * @param config
     * @param cxt
     */
    @Override
    public void initQueryConfig(QueryConfigure config, QueryContext cxt) {

        //判断是否已经设置了列小计
        //设置了则不做初始化处理
        AnalysisTotalItemConfig totalItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.COL_SUBTOTAL);
        if(totalItemConfig != null){
            return;
        }

        // 默认值：当有占比时，默认有汇总：有列维度时按列维度汇总，无列维度，则按所有维度汇总
        QueryResult result = config.getResult();

        List<String> statsFieldIdList = new ArrayList<>();
        List<QueryField> rowFields = result.getRowDimensions();
        // 添加需要汇总的行维度：添加倒数第二个行维度
        if(BIUtil.isNotEmpty(rowFields)){
            if(rowFields.size() > 1) {
                statsFieldIdList.add(rowFields.get(rowFields.size() - 2).getId());
            }
        }

        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        if(totalConfig == null) {
            totalConfig = new AnalysisTotalConfig();
        }
        totalConfig.setIsActive(Enabled.YES.getId());

        AnalysisTotalItemConfig itemConfig = new AnalysisTotalItemConfig();
        itemConfig.setTotalType(AnalysisTotalType.COL_SUBTOTAL);
        itemConfig.setDimIdList(statsFieldIdList);
        totalConfig.add(itemConfig);
        itemConfig.setZbTotal(true);

        config.getAnalysis().setTotal(totalConfig);
    }
}
