package com.bi.queryer.ssm.llm.dataset.agent.operator;

import com.bi.queryer.ssm.llm.entity.LLMQueryField;

public class AgentOperatorContext {

  private LLMQueryField operatorField = new LLMQueryField();

  private LLMQueryField atomField = new LLMQueryField();

    public LLMQueryField getOperatorField() {
        return operatorField;
    }

    public void setOperatorField(LLMQueryField operatorField) {
        this.operatorField = operatorField;
    }

    public LLMQueryField getAtomField() {
        return atomField;
    }

    public void setAtomField(LLMQueryField atomField) {
        this.atomField = atomField;
    }
}
