package com.bi.queryer.ssm.llm.function.impl;

import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.llm.util.LLMUtil;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.sys.enums.Enabled;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * 轮胎-商品品牌-机会-支付件数-贡献值
 */
public class TireBrandOpportunityPayCntGxzFunction extends BaseFunction {
    public TireBrandOpportunityPayCntGxzFunction(LLMQueryConfig config) {
        super(config);
    }


    @Override
    public LLMQueryConfig normalizeLlmQueryConfig(LLMQueryConfig llmQueryConfig) {

        LLMQueryResult result = llmQueryConfig.getResult();

        List<LLMQueryField> newDimensions = buildDimensions(result);
        result.setDimensions(newDimensions);

        List<LLMQueryField> metrics = result.getMetrics();
        metrics.add(new LLMQueryField("D_ORD_05480", "轮胎-销售商品件数B0"));
        metrics.add(new LLMQueryField("D_ORD_02745", "轮胎-APP-支付用户数"));
        metrics.add(new LLMQueryField("D_TFC_01271", "轮胎-app-关键页-商品曝光UV"));
        metrics.add(new LLMQueryField("[D_ORD_02745]/[D_TFC_01271]", "APP_轮胎商品关支率"));

        return llmQueryConfig;
    }

    /**
     * 构建附加的维度，并调整排序
     *
     * @param result
     * @return
     */
    public List<LLMQueryField> buildDimensions(LLMQueryResult result) {

        List<LLMQueryField> newDimensions = new ArrayList<>();
        for (LLMQueryField dim : result.getDimensions()) {
            if ("ALL".equalsIgnoreCase(dim.getField_code()) || "ABC".equalsIgnoreCase(dim.getField_code())) {
                continue;
            }

            newDimensions.add(dim);
        }

        newDimensions.add(new LLMQueryField("ALL_", "轮胎规格"));
        newDimensions.add(new LLMQueryField("ABC", "商品品牌"));

        return newDimensions;
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

        AnalysisTotalConfig totalConfig = new AnalysisTotalConfig();
        totalConfig.setIsActive(Enabled.YES.getId());
        AnalysisTotalItemConfig totalItemConfig = new AnalysisTotalItemConfig();
        totalItemConfig.setTotalType(AnalysisTotalType.COL_TOTAL);
        totalConfig.getItems().add(totalItemConfig);
        analysisConfig.put("total", totalConfig);

        return analysisConfig;
    }

    @Override
    public ResultDataSet appendDataSet(ResultDataSet dataSet) {

        List<ResultDataSetColumn> columns = dataSet.getColumns();
        columns = appendColumns(columns);

        String closeRateCode = columns.stream().filter(f -> "APP_轮胎商品关支率".equalsIgnoreCase(f.getTitle())).findAny()
                .get().getRawCode();

        Map<String, Object> colTotalData = new HashMap<>();

        //获取列总计
        for (Map<String, Object> dataMap : dataSet.getRows()) {
            Object value = dataMap.get("_grp_v");
            if (value != null) {
                AnalysisTotalType totalType = AnalysisTotalType.get(value.toString());
                if (AnalysisTotalType.COL_TOTAL == totalType) {
                    colTotalData = dataMap;
                    break;
                }
            }
        }

        //列名前缀
        String columnPrefix = getColumnPrefix();

        //步骤1 计算“轮胎规格”的“轮胎-客单件”年周同(-1)实际值= 各个“轮胎规格”的（“轮胎-销售商品件数B0”年周同(-1)实际值）/（“轮胎-APP-支付用户数”年周同(-1)实际值）
        Double tire_price_per_user_tb_yw_r_value = LLMUtil.doubleDivision(LLMUtil.getColumnDoubleValue(colTotalData, "D_ORD_05480_tb_yw_r_value")
                , LLMUtil.getColumnDoubleValue(colTotalData, "D_ORD_02745_tb_yw_r_value"));

        //步骤2 “轮胎规格”的“机会-支付件数””=各个“轮胎规格”的（”轮胎-app-关键页-商品曝光UV“本期值）*（“APP_轮胎商品关支率”年周同(-1)实际值）*（“轮胎-客单件”年周同(-1)实际值）-（“轮胎-销售商品件数B0”本期值）
        Double tire_opportunity_pay_cnt =
                LLMUtil.doubleSub(
                        LLMUtil.doubleMultiply(
                                LLMUtil.doubleMultiply(LLMUtil.getColumnDoubleValue(colTotalData, "D_TFC_01271"), LLMUtil.getColumnDoubleValue(colTotalData, closeRateCode + "_tb_yw_r_value"))
                                , tire_price_per_user_tb_yw_r_value)
                        , LLMUtil.getColumnDoubleValue(colTotalData, "D_ORD_05480")
                );


        for (Map<String, Object> dataMap : dataSet.getRows()) {

            //不处理汇总
            Object value = dataMap.get("_grp_v");
            if (value != null) {
                continue;
            }

            //步骤3 商品品牌”的“轮胎-客单件”年周同(-1)实际值=各个“商品品牌”的（“轮胎-销售商品件数B0”年周同(-1)实际值）/（“轮胎-APP-支付用户数”年周同(-1)实际值）
            Double price_per_user_tb_yw_r_value = LLMUtil.doubleDivision(LLMUtil.getColumnDoubleValue(dataMap, "D_ORD_05480_tb_yw_r_value"), LLMUtil.getColumnDoubleValue(dataMap, "D_ORD_02745_tb_yw_r_value"));

            //步骤4 商品品牌”的“机会-支付件数”=各个“商品品牌”的（”轮胎-app-关键页-商品曝光UV“本期值）*（“APP_轮胎商品关支率”年周同(-1)实际值）*（“轮胎-客单件”年周同(-1)实际值）-（“轮胎-销售商品件数B0”本期值）
            Double opportunity_pay_cnt = LLMUtil.doubleSub(
                    LLMUtil.doubleMultiply(
                            LLMUtil.doubleMultiply(LLMUtil.getColumnDoubleValue(dataMap, "D_TFC_01271"), LLMUtil.getColumnDoubleValue(dataMap, closeRateCode + "_tb_yw_r_value"))
                            , price_per_user_tb_yw_r_value)
                    , LLMUtil.getColumnDoubleValue(dataMap, "D_ORD_05480")
            );

            //步骤5 “商品品牌”的“机会-支付件数”贡献值 =（各个“商品品牌”的“机会-支付件数”）/（“商品品牌”对应“轮胎规格”整体的“机会-支付件数”）
            Double opportunity_pay_cnt_gxz = LLMUtil.doubleDivision(opportunity_pay_cnt, tire_opportunity_pay_cnt);

            dataMap.put(columnPrefix + "_OPPORTUNITY_PAY_CNT", LLMUtil.doubleFormat(opportunity_pay_cnt, "###,###,##0"));
            dataMap.put(columnPrefix + "_OPPORTUNITY_PAY_CNT_GXZ", LLMUtil.doubleFormat(opportunity_pay_cnt_gxz, "###,###,##0.00%"));

        }

        return dataSet;
    }

    /**
     * 附加列元信息
     *
     * @param columns
     * @return
     */
    public List<ResultDataSetColumn> appendColumns(List<ResultDataSetColumn> columns) {
        //【轮胎-商品品牌-机会-支付件数】
        ResultDataSetColumn tireBrandOpportunityPayCntColumn = new ResultDataSetColumn();
        tireBrandOpportunityPayCntColumn.setCode("TTIRE_BRAND_OPPORTUNITY_PAY_CNT");
        tireBrandOpportunityPayCntColumn.setTitle("轮胎_品牌_机会_支付件数");
        tireBrandOpportunityPayCntColumn.setDataType("double");
        tireBrandOpportunityPayCntColumn.setType("");
        columns.add(tireBrandOpportunityPayCntColumn);

        //【轮胎-商品品牌-机会-支付件数-贡献值】
        ResultDataSetColumn tireBrandOpportunityPayCntGxzColumn = new ResultDataSetColumn();
        tireBrandOpportunityPayCntGxzColumn.setCode("TIRE_BRAND_OPPORTUNITY_PAY_CNT_GXZ");
        tireBrandOpportunityPayCntGxzColumn.setTitle("轮胎_品牌_机会_支付件数_贡献值");
        tireBrandOpportunityPayCntGxzColumn.setDataType("double");
        tireBrandOpportunityPayCntGxzColumn.setType("");
        columns.add(tireBrandOpportunityPayCntGxzColumn);
        return columns;
    }

    /**
     * 获取列名前缀
     *
     * @return
     */
    public String getColumnPrefix() {
        return "TIRE_BRAND";
    }

}
