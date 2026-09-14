package com.bi.queryer.ssm.llm.function;

import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.meta.MetaField;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;
import java.util.Set;

public interface IFunction {

    LLMQueryConfig normalizeLlmQueryConfig(LLMQueryConfig config);

    Set<String> appendCalcModes(Set<String> calcModes);

    JSONObject appendThbItem(JSONObject thbItem, Map<String, MetaField> metaFields);

    JSONObject appendTotalZb(JSONObject analysisConfig);

    ResultDataSet appendDataSet(ResultDataSet dataSet);

}
