package com.bi.queryer.ssm.engine.accelerate.hot.mq.action.etl;

import com.bi.queryer.ssm.engine.accelerate.hot.HotTableManager;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionByMQ;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.util.SpringContextUtil;

/**
 * @Author contributor
 * @Date 13:52 2024/8/23
 * @Description 作业完成后处理类
 **/
public class EtlJobFinishAction extends ActionByMQ<EtlJobFinishActionParameter> {
    public EtlJobFinishAction(EtlJobFinishActionParameter parameter) {
        super(parameter);
    }

    @Override
    public Object action() {
        HotTableManager.onEtlJobFinish(parameter.getEtlJobName(), parameter.getEtlJobFinishTime());
        updateEtlLastEndTime(parameter.getEtlJobName(), parameter.getEtlJobFinishTime());
        return null;
    }

    /**
     * 更新作业结束时间
     * 用于查询作业的最后更新时间
     * @param etlJobName
     * @param etlJobFinishTime
     */
    public void updateEtlLastEndTime(String etlJobName, String etlJobFinishTime) {

        try {

            QueryFieldService queryFieldService = (QueryFieldService) SpringContextUtil.getBean("queryFieldService");
            queryFieldService.updateEtlLastEndTime(etlJobName, etlJobFinishTime);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
