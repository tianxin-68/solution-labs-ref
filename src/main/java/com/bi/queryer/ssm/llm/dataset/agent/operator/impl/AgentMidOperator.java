package com.bi.queryer.ssm.llm.dataset.agent.operator.impl;

import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.llm.dataset.agent.operator.AgentOperatorContext;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.util.LLMUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgentMidOperator extends BaseAgentOperator{
    public AgentMidOperator(LLMQueryConfig llmQueryConfig, AgentOperatorContext context) {
        super(llmQueryConfig, context);
    }

    public ResultDataSet appendDataSet(ResultDataSet dataSet) {
        List<ResultDataSetColumn> columns = dataSet.getColumns();

        LLMQueryField operatorField = context.getOperatorField();
        LLMQueryField atomField = context.getAtomField();


        Optional<ResultDataSetColumn> atomColumnOpl = columns.stream().filter(f -> atomField.getField_title().equalsIgnoreCase(f.getTitle())).findAny();
        if (!atomColumnOpl.isPresent()) {
            return dataSet;
        }

        //添加【轮胎-商品品牌-结构效应】
        ResultDataSetColumn tireBrandCtrStructColumn = new ResultDataSetColumn();
        tireBrandCtrStructColumn.setCode(operatorField.getField_code());
        tireBrandCtrStructColumn.setTitle(operatorField.getField_title());
        tireBrandCtrStructColumn.setDataType("double");
        tireBrandCtrStructColumn.setType("");
        columns.add(tireBrandCtrStructColumn);

        String atomCode = atomColumnOpl.get().getRawCode();

        List<Double> atomValues = new ArrayList<>();
        for (Map<String, Object> dataMap : dataSet.getRows()) {

            //不处理汇总
            Object value = dataMap.get("_grp_v");
            if (value != null) {
                continue;
            }

            atomValues.add(LLMUtil.getColumnDoubleValue(dataMap, atomCode));
        }

        Double medianValue = LLMUtil.calculateMedian(atomValues);
        for (Map<String, Object> dataMap : dataSet.getRows()) {
            dataMap.put(operatorField.getField_code(), LLMUtil.doubleFormat(medianValue,  atomColumnOpl.get().getDataFormat()));
        }

        return dataSet;
    }
}
