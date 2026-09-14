package com.bi.queryer.ssm.llm.function.impl;

import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;

import java.util.ArrayList;
import java.util.List;

public class TirePNameOpportunityPayCntGxzFunction extends TireBrandOpportunityPayCntGxzFunction{
    public TirePNameOpportunityPayCntGxzFunction(LLMQueryConfig config) {
        super(config);
    }

    /**
     * 构建附加的维度，并调整排序
     * @param result
     * @return
     */
    public List<LLMQueryField> buildDimensions(LLMQueryResult result){

        List<LLMQueryField> newDimensions = new ArrayList<>();
        for(LLMQueryField dim : result.getDimensions()) {
            if ("ALL".equalsIgnoreCase(dim.getField_code()) || "ABG".equalsIgnoreCase(dim.getField_code())) {
                continue;
            }

            newDimensions.add(dim);
        }

        newDimensions.add( new LLMQueryField("ALL_", "轮胎规格"));
        newDimensions.add(new LLMQueryField("ABG", "商品名称"));

        return newDimensions;
    }

    /**
     * 附加列元信息
     * @param columns
     * @return
     */
    public  List<ResultDataSetColumn> appendColumns(List<ResultDataSetColumn> columns) {
        //【轮胎_商品名称_机会_支付件数】
        ResultDataSetColumn tirePNameOpportunityPayCntColumn = new ResultDataSetColumn();
        tirePNameOpportunityPayCntColumn.setCode("TIRE_PNAME_OPPORTUNITY_PAY_CNT");
        tirePNameOpportunityPayCntColumn.setTitle("轮胎_商品_机会_支付件数");
        tirePNameOpportunityPayCntColumn.setDataType("double");
        tirePNameOpportunityPayCntColumn.setType("");
        columns.add(tirePNameOpportunityPayCntColumn);

        //【轮胎_商品名称_机会_支付件数_贡献值】
        ResultDataSetColumn tirePNameOpportunityPayCntGxzColumn = new ResultDataSetColumn();
        tirePNameOpportunityPayCntGxzColumn.setCode("TIRE_PNAME_OPPORTUNITY_PAY_CNT_GXZ");
        tirePNameOpportunityPayCntGxzColumn.setTitle("轮胎_商品_机会_支付件数_贡献值");
        tirePNameOpportunityPayCntGxzColumn.setDataType("double");
        tirePNameOpportunityPayCntGxzColumn.setType("");
        columns.add(tirePNameOpportunityPayCntGxzColumn);
        return columns;
    }

    /**
     * 获取列名前缀
     * @return
     */
    public String getColumnPrefix() {
        return "TIRE_PNAME";
    }
}
