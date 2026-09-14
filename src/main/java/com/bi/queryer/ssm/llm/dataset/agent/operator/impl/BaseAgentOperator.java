package com.bi.queryer.ssm.llm.dataset.agent.operator.impl;

import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.llm.dataset.agent.operator.AgentOperatorContext;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;

public class BaseAgentOperator {

   public AgentOperatorContext context;
   public LLMQueryConfig llmQueryConfig ;

   public BaseAgentOperator(LLMQueryConfig llmQueryConfig,AgentOperatorContext context ){
      this.llmQueryConfig = llmQueryConfig;
      this.context = context;
   }

   public ResultDataSet appendDataSet(ResultDataSet dataSet){
      return dataSet;
   }
}
