package com.bi.queryer.ssm.engine.function.impl.model;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author contributor
 * @Date 11:02 2022-10-21
 * @Description 查询进程
 **/
public class QueryProcess {
    protected AtomicReference<String> queryId = new AtomicReference<>();
    protected AtomicReference<String> queryState = new AtomicReference<>();

    public AtomicReference<String> getQueryId() {
        return queryId;
    }

    public void setQueryId(AtomicReference<String> queryId) {
        this.queryId = queryId;
    }

    public AtomicReference<String> getQueryState() {
        return queryState;
    }

    public void setQueryState(AtomicReference<String> queryState) {
        this.queryState = queryState;
    }
}
