package com.bi.queryer.ssm.llm.function.impl;

import cn.hutool.core.collection.ListUtil;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbItemConfig;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.llm.util.LLMUtil;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.sys.enums.Enabled;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TireBrandGxdFunction extends BaseFunction{
    public TireBrandGxdFunction(LLMQueryConfig config) {
        super(config);
    }

    @Override
    public LLMQueryConfig normalizeLlmQueryConfig(LLMQueryConfig llmQueryConfig) {

        LLMQueryResult result = llmQueryConfig.getResult();
        List<LLMQueryField> newDimensions = buildDimensions(result);
        result.setDimensions(newDimensions);

        List<LLMQueryField> metrics = result.getMetrics();
        if(!metrics.contains("D_TFC_01271")){
            LLMQueryField field = new LLMQueryField("D_TFC_01271", "轮胎-app-关键页-商品曝光UV");
            metrics.add(field);
        }

        LLMQueryField field = new LLMQueryField("[D_ORD_02745]/[D_TFC_01271]", "APP_轮胎商品关支率");
        metrics.add(field);

        return llmQueryConfig;
    }

    /**
     * 构建附加的维度，并调整排序
     * @param result
     * @return
     */
    public List<LLMQueryField> buildDimensions(LLMQueryResult result){

        List<LLMQueryField> newDimensions = new ArrayList<>();
        for(LLMQueryField dim : result.getDimensions()) {
            if ("ALL".equalsIgnoreCase(dim.getField_code()) || "ABC".equalsIgnoreCase(dim.getField_code())) {
                continue;
            }

            newDimensions.add(dim);
        }

        newDimensions.add( new LLMQueryField("ALL_", "轮胎规格"));
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

        MetaField metaField = metaFields.get("D_TFC_01271");

        List<AnalysisZbThbConfig> zbThbConfigs = new ArrayList<>();
        AnalysisZbThbConfig zbThbConfig = new AnalysisZbThbConfig();
        zbThbConfig.setMeasureId(metaField.getId());
        zbThbConfig.setCalcModeList(ListUtil.toList(AnalysisCalcMode.ZB_COL_TOTAL.getCode()));
        zbThbConfigs.add(zbThbConfig);
        thbItem.put("zbThbConfigs", zbThbConfigs);

        return thbItem;
    }

    @Override
    public JSONObject appendTotalZb(JSONObject analysisConfig) {

        AnalysisTotalConfig totalConfig = new AnalysisTotalConfig();
        totalConfig.setIsActive(Enabled.YES.getId());
        AnalysisTotalItemConfig totalItemConfig = new AnalysisTotalItemConfig();
        totalItemConfig.setTotalType(AnalysisTotalType.COL_TOTAL);
        totalItemConfig.setDimIdList(ListUtil.toList("all"));
        totalConfig.getItems().add(totalItemConfig);
        analysisConfig.put("total", totalConfig);

        AnalysisZbConfig zbConfig = new AnalysisZbConfig();
        zbConfig.setIsActive(Enabled.YES.getId());

        AnalysisZbItemConfig zbItemConfig = new AnalysisZbItemConfig();
        zbItemConfig.setMeasureIdList(ListUtil.toList("all"));
        zbItemConfig.setCalcMode(AnalysisCalcMode.ZB_COL_TOTAL.getCode());

        analysisConfig.put("zb", zbConfig);

        return analysisConfig;
    }

    @Override
    public ResultDataSet appendDataSet(ResultDataSet dataSet) {

        List<ResultDataSetColumn> columns = dataSet.getColumns();
        columns = appendColumns(columns);

        String closeRateCode = columns.stream().filter(f -> "APP_轮胎商品关支率".equalsIgnoreCase(f.getTitle())).findAny()
                .get().getRawCode();

        //列名前缀
        String columnPrefix = getColumnPrefix();

        for (Map<String, Object> dataMap : dataSet.getRows()) {

            //“结构效应”指标，计算公式为“曝光UV占比”的年周同比  *  “APP_轮胎商品关支率”本期值
            // D_TFC_01271_zb_ct_tb_yw_ratio  _ctm_m__x_3
            Double ctrStructValue = LLMUtil.doubleMultiply(LLMUtil.getColumnDoubleValue(dataMap, "D_TFC_01271_zb_ct_tb_yw_ratio")
                    , LLMUtil.getColumnDoubleValue(dataMap, closeRateCode));

            //“转化效应”指标，计算公式为“曝光UV占比“的年周同(-1)实际值  *   “APP_轮胎商品关支率”的年周同比(-1)；
            //D_TFC_01271_zb_ct_tb_yw_r_value _ctm_m__x_3_tb_yw_ratio
            Double ctrTransValue = LLMUtil.doubleMultiply(LLMUtil.getColumnDoubleValue(dataMap, "D_TFC_01271_zb_ct_tb_yw_r_value")
                    , LLMUtil.getColumnDoubleValue(dataMap, closeRateCode + "_tb_yw_ratio"));

            //“品牌贡献度”的计算方法等于“结构效应”+“转化效应”；

            Double ctrValue = LLMUtil.doubleAdd(ctrStructValue, ctrTransValue);

            dataMap.put(columnPrefix + "_GXD_STRUCT", LLMUtil.doubleFormat(ctrStructValue, "###,###,##0.00%"));
            dataMap.put(columnPrefix + "_GXD_TRANS", LLMUtil.doubleFormat(ctrTransValue, "###,###,##0.00%"));
            dataMap.put(columnPrefix + "_GXD", LLMUtil.doubleFormat(ctrValue, "###,###,##0.00%"));

        }

        return dataSet;
    }

    /**
     * 附加列元信息
     * @param columns
     * @return
     */
    public  List<ResultDataSetColumn> appendColumns(List<ResultDataSetColumn> columns) {
        //添加【轮胎-商品品牌-结构效应】
        ResultDataSetColumn tireBrandCtrStructColumn = new ResultDataSetColumn();
        tireBrandCtrStructColumn.setCode("TIRE_BRAND_GXD_STRUCT");
        tireBrandCtrStructColumn.setTitle("轮胎_品牌_结构效应");
        tireBrandCtrStructColumn.setDataType("double");
        tireBrandCtrStructColumn.setType("");
        columns.add(tireBrandCtrStructColumn);

        //【轮胎-商品品牌-转化效应】
        ResultDataSetColumn tireBrandCtrTranSColumn = new ResultDataSetColumn();
        tireBrandCtrTranSColumn.setCode("TIRE_BRAND_GXD_TRANS");
        tireBrandCtrTranSColumn.setTitle("轮胎_品牌_转化效应");
        tireBrandCtrTranSColumn.setDataType("double");
        tireBrandCtrTranSColumn.setType("");
        columns.add(tireBrandCtrTranSColumn);

        //【轮胎品牌贡献度】
        ResultDataSetColumn tireBrandCtrColumn = new ResultDataSetColumn();
        tireBrandCtrColumn.setCode("TIRE_BRAND_GXD");
        tireBrandCtrColumn.setTitle("轮胎_品牌_贡献度");
        tireBrandCtrColumn.setDataType("double");
        tireBrandCtrColumn.setType("");
        columns.add(tireBrandCtrColumn);

        return columns;
    }

    /**
     * 获取列名前缀
     * @return
     */
    public String getColumnPrefix() {
        return "TIRE_BRAND";
    }

}
