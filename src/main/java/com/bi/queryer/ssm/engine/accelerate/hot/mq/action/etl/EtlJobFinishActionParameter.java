package com.bi.queryer.ssm.engine.accelerate.hot.mq.action.etl;

import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionParameter;

/**
 * @Author contributor
 * @Date 15:38 2024/8/23
 * @Description TODO
 **/
public class EtlJobFinishActionParameter extends ActionParameter {
    private String etlJobFinishTime = "";

    private String etlJobName = "";

    public EtlJobFinishActionParameter(String etlJobName, String etlJobFinishTime) {
        this.etlJobFinishTime = etlJobFinishTime;
        this.etlJobName = etlJobName;
    }

    public String getEtlJobFinishTime() {
        return etlJobFinishTime;
    }

    public void setEtlJobFinishTime(String etlJobFinishTime) {
        this.etlJobFinishTime = etlJobFinishTime;
    }

    public String getEtlJobName() {
        return etlJobName;
    }

    public void setEtlJobName(String etlJobName) {
        this.etlJobName = etlJobName;
    }
}
