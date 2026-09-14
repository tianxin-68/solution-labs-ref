package com.bi.queryer.ssm.engine.accelerate.hot.mq;

import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionByMQ;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.cache.CacheAction;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.cache.CacheActionParameter;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.etl.EtlJobFinishAction;
import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.etl.EtlJobFinishActionParameter;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import com.tx.mq.TxMqClient;
import com.tx.mq.exception.TxMqClientException;
import com.tx.mq.iface.consumer.IPushConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:52 2024/8/19
 * @Description 接收datastudio-作业调度-作业完成后的mq
 **/
@Configuration
public class EtlMQReceiver extends HotMQReceiver {
    private final static String SUCCESS_JOB_STATUS = "Done";

    @Override
    public List<ActionByMQ> accept(JSONObject jsonObject) {
        //{"endTime":"2020-12-04 10:50:16","job":"OLD_SP_DM_MARKETINGPLATFORM_ABTEST_D","status":"Done"}
        String etlJob = jsonObject.getString("job");
        String jobStatus = jsonObject.getString("status");
        if (BIUtil.isEmpty(etlJob) || !SUCCESS_JOB_STATUS.equalsIgnoreCase(jobStatus)) {
            return Collections.emptyList();
        }
        List<ActionByMQ> actions = new ArrayList<>();
        EtlJobFinishActionParameter parameter = new EtlJobFinishActionParameter(etlJob.toUpperCase(), jsonObject.getString("endTime"));
        actions.add(new EtlJobFinishAction(parameter));
        //return Collections.singletonList(new EtlJobFinishAction(parameter));

        // 缓存action
        actions.add(new CacheAction(new CacheActionParameter(null,  Collections.singletonList(etlJob.toUpperCase()), "schedule")));

        return actions;
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public IPushConsumer etlJobStatusConsumer() throws TxMqClientException {
        if (applicationContext.getParent() == null) {
            return new EmptyPushConsumer();
        }
        RuntimeEnv env = BIUtil.getRuntimeEnv();
//        if (RuntimeEnv.UT == env) {
        if (RuntimeEnv.UT == env || RuntimeEnv.Test == env || RuntimeEnv.Dev == env) {
            return new EmptyPushConsumer();
        } else {
            return TxMqClient.createPushConsumer("bi.ssm.etl.job.status.sub", consumeCallback);
        }
    }
}
