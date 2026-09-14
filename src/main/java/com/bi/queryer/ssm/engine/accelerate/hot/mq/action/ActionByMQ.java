package com.bi.queryer.ssm.engine.accelerate.hot.mq.action;

/**
 * @Author contributor
 * @Date 13:54 2024/8/23
 * @Description TODO
 **/
public abstract class ActionByMQ<T extends ActionParameter> {
    protected T parameter = null;

    public ActionByMQ(T parameter){
        this.parameter = parameter;
    }

    abstract public Object action();
}
