package com.bi.queryer.ssm.llm.function.impl;

import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮胎_商品ID_机会_支付件数_贡献值
 */
public class TirePidOpportunityPayCntGxzFunction extends TireBrandOpportunityPayCntGxzFunction{
    public TirePidOpportunityPayCntGxzFunction(LLMQueryConfig config) {
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
            if ("ALL".equalsIgnoreCase(dim.getField_code()) || "ASV".equalsIgnoreCase(dim.getField_code())) {
                continue;
            }

            newDimensions.add(dim);
        }

        newDimensions.add( new LLMQueryField("ALL_", "轮胎规格"));
        newDimensions.add(new LLMQueryField("ASV", "商品ID"));

        return newDimensions;
    }

    /**
     * 附加列元信息
     * @param columns
     * @return
     */
    public  List<ResultDataSetColumn> appendColumns(List<ResultDataSetColumn> columns) {
        //【轮胎_商品ID_机会_支付件数】
        ResultDataSetColumn tirePidOpportunityPayCntColumn = new ResultDataSetColumn();
        tirePidOpportunityPayCntColumn.setCode("TIRE_PID_OPPORTUNITY_PAY_CNT");
        tirePidOpportunityPayCntColumn.setTitle("轮胎_商品_机会_支付件数");
        tirePidOpportunityPayCntColumn.setDataType("double");
        tirePidOpportunityPayCntColumn.setType("");
        columns.add(tirePidOpportunityPayCntColumn);

        //【轮胎_商品ID_机会_支付件数_贡献值】
        ResultDataSetColumn tirePidOpportunityPayCntGxzColumn = new ResultDataSetColumn();
        tirePidOpportunityPayCntGxzColumn.setCode("TIRE_PID_OPPORTUNITY_PAY_CNT_GXZ");
        tirePidOpportunityPayCntGxzColumn.setTitle("轮胎_商品_机会_支付件数_贡献值");
        tirePidOpportunityPayCntGxzColumn.setDataType("double");
        tirePidOpportunityPayCntGxzColumn.setType("");
        columns.add(tirePidOpportunityPayCntGxzColumn);
        return columns;
    }

    /**
     * 获取列名前缀
     * @return
     */
    public String getColumnPrefix() {
        return "TIRE_PID";
    }

}

