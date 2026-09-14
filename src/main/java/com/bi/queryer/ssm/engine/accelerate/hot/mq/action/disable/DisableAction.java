package com.bi.queryer.ssm.engine.accelerate.hot.mq.action.disable;

import com.bi.queryer.ssm.engine.accelerate.hot.HotTableManager;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionByMQ;

/**
 * @Author contributor
 * @Date 13:51 2024/8/23
 * @Description 禁用Action
 **/
public class DisableAction extends ActionByMQ<DisableActionParameter> {
    public DisableAction(DisableActionParameter parameter) {
        super(parameter);
    }

    @Override
    public Object action() {
        HotTableManager.disableBySource(parameter.getSourceTableName(),parameter.getOperator());
        return null;
    }
}
