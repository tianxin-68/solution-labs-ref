package com.bi.queryer.ssm.engine.accelerate.hot.mq.action.cache;

import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionParameter;

import java.util.List;

/**
 * @Author contributor
 * @Date 11:21 2025/1/10
 * @Description TODO
 **/
public class CacheActionParameter extends ActionParameter {
    private List<String> tableNames;

    private List<String> etlJobs;

    private String operator ;

    public CacheActionParameter() {
    }

    public CacheActionParameter(List<String> tableNames, List<String> etlJobs, String operator) {
        this.tableNames = tableNames;
        this.etlJobs = etlJobs;
        this.operator = operator;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public void setTableNames(List<String> tableNames) {
        this.tableNames = tableNames;
    }

    public List<String> getEtlJobs() {
        return etlJobs;
    }

    public void setEtlJobs(List<String> etlJobs) {
        this.etlJobs = etlJobs;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
