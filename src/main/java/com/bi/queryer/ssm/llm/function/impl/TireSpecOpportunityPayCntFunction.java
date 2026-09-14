package com.bi.queryer.ssm.llm.function.impl;

import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.llm.util.LLMUtil;
import com.bi.queryer.ssm.meta.MetaField;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * 轮胎-机会-支付件数
 */
public class TireSpecOpportunityPayCntFunction extends BaseFunction{
    public TireSpecOpportunityPayCntFunction(LLMQueryConfig config) {
        super(config);
    }

    public LLMQueryConfig normalizeLlmQueryConfig(LLMQueryConfig llmQueryConfig) {
        LLMQueryResult result = llmQueryConfig.getResult();

        List<LLMQueryField> dimensions = result.getDimensions();

        List<LLMQueryField> newDimensions = new ArrayList<>();
        for(LLMQueryField dim : dimensions) {
            if ("ALL".equalsIgnoreCase(dim.getField_code()) ) {
                continue;
            }

            newDimensions.add(dim);
        }

        newDimensions.add( new LLMQueryField("ALL_", "轮胎规格"));

        result.setDimensions(newDimensions);

        List<LLMQueryField> metrics = result.getMetrics();
        metrics.add(new LLMQueryField("D_ORD_05480", "轮胎-销售商品件数B0"));
        metrics.add(new LLMQueryField("D_ORD_02745", "轮胎-APP-支付用户数"));
        metrics.add(new LLMQueryField("D_TFC_01271", "轮胎-app-关键页-商品曝光UV"));
        metrics.add(new LLMQueryField("[D_ORD_02745]/[D_TFC_01271]", "APP_轮胎商品关支率"));

        return llmQueryConfig;
    }

    @Override
    public Set<String> appendCalcModes(Set<String> calcModes) {
        calcModes.add(AnalysisCalcMode.TB_YEAR_WEEK.getCode());
        return calcModes;
    }

    @Override
    public JSONObject appendThbItem(JSONObject thbItem, Map<String, MetaField> metaFields) {
        return super.appendThbItem(thbItem, metaFields);
    }

    @Override
    public JSONObject appendTotalZb(JSONObject analysisConfig) {
        return analysisConfig;
    }

    @Override
    public ResultDataSet appendDataSet(ResultDataSet dataSet) {

        List<ResultDataSetColumn> columns = dataSet.getColumns();

        //【轮胎-机会-支付件数】
        ResultDataSetColumn tireBrandOpportunityPayCntColumn = new ResultDataSetColumn();
        tireBrandOpportunityPayCntColumn.setCode("TIRE_SPEC_OPPORTUNITY_PAY_CNT");
        tireBrandOpportunityPayCntColumn.setTitle("轮胎规格_机会_支付件数");
        tireBrandOpportunityPayCntColumn.setDataType("double");
        tireBrandOpportunityPayCntColumn.setType("");
        columns.add(tireBrandOpportunityPayCntColumn);

        String closeRateCode = columns.stream().filter(f -> "APP_轮胎商品关支率".equalsIgnoreCase(f.getTitle())).findAny()
                .get().getRawCode();

        for (Map<String, Object> dataMap : dataSet.getRows()) {

            //步骤1 计算“轮胎规格”的“轮胎-客单件”年周同(-1)实际值= 各个“轮胎规格”的（“轮胎-销售商品件数B0”年周同(-1)实际值）/（“轮胎-APP-支付用户数”年周同(-1)实际值）
            Double tire_price_per_user_tb_yw_r_value =  LLMUtil.doubleDivision(LLMUtil.getColumnDoubleValue(dataMap, "D_ORD_05480_tb_yw_r_value")
                    , LLMUtil.getColumnDoubleValue(dataMap, "D_ORD_02745_tb_yw_r_value"));

            //步骤2 “轮胎规格”的“机会-支付件数””=各个“轮胎规格”的（”轮胎-app-关键页-商品曝光UV“本期值）*（“APP_轮胎商品关支率”年周同(-1)实际值）*（“轮胎-客单件”年周同(-1)实际值）-（“轮胎-销售商品件数B0”本期值）
            Double tire_opportunity_pay_cnt =
                    LLMUtil.doubleSub(
                            LLMUtil.doubleMultiply(
                                    LLMUtil.doubleMultiply(LLMUtil.getColumnDoubleValue(dataMap, "D_TFC_01271"),LLMUtil.getColumnDoubleValue(dataMap, closeRateCode+"_tb_yw_r_value"))
                                    ,tire_price_per_user_tb_yw_r_value)
                            ,LLMUtil.getColumnDoubleValue(dataMap, "D_ORD_05480")
                    );


            dataMap.put("TIRE_SPEC_OPPORTUNITY_PAY_CNT", LLMUtil.doubleFormat(tire_opportunity_pay_cnt, "###,###,##0"));

        }

        return dataSet;
    }

}
