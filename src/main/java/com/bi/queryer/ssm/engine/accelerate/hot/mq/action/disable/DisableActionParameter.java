package com.bi.queryer.ssm.engine.accelerate.hot.mq.action.disable;

import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionParameter;

/**
 * @Author contributor
 * @Date 15:37 2024/8/23
 * @Description TODO
 **/
public class DisableActionParameter extends ActionParameter {
    private String sourceTableName;
    private String operator;
    public DisableActionParameter(String sourceTableName,String operator) {
        this.sourceTableName = sourceTableName;
        this.operator = operator;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
