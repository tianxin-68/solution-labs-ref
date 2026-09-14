package com.bi.queryer.ssm.llm.function.impl;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.function.IFunction;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.meta.MetaField;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;

public class BaseFunction implements IFunction {

    protected LLMQueryConfig config;

    public BaseFunction(LLMQueryConfig config){
        this.config = config;
    }

    @Override
    public LLMQueryConfig normalizeLlmQueryConfig(LLMQueryConfig config) {
        return config;
    }

    @Override
    public Set<String> appendCalcModes(Set<String> calcModes) {
        return calcModes;
    }

    @Override
    public JSONObject appendThbItem(JSONObject thbItem, Map<String, MetaField> metaFields) {
        return thbItem;
    }

    @Override
    public JSONObject appendTotalZb(JSONObject analysisConfig) {
        return analysisConfig;
    }

    @Override
    public ResultDataSet appendDataSet(ResultDataSet dataSet) {
        return dataSet;
    }

}
