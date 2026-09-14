package com.bi.queryer.sys.kafka.authApproval;

import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.util.BIUtil;
import com.tx.mq.TxMqClient;
import com.tx.mq.consumer.ConsumeStatus;
import com.tx.mq.consumer.IConsumeCallback;
import com.tx.mq.exception.TxMqClientException;
import com.tx.mq.iface.consumer.IPushConsumer;
import com.tx.mq.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工单审批完成 MQ 消费，接入方式参照 {@link com.bi.queryer.ssm.engine.accelerate.hot.mq.AdhocMQReceiver}。
 * <p>
 * root 容器（ContextLoaderListener）和 DispatcherServlet 子容器都会扫描到本类，各自创建一次 Bean。
 * root 容器刷新时 {@link SC} 依赖的系统配置还未加载完成，因此只在子容器（parent 必为 root）里真正创建消费者——
 * 此时 root 早已完整刷新完毕，配置必定已加载。
 */
@Configuration
public class WorkOrderAuthMQReceiver implements ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(WorkOrderAuthMQReceiver.class);

    @Autowired
    private AuthApprovalConsumer authApprovalConsumer;

    private ApplicationContext applicationContext;

    private final IConsumeCallback consumeCallback = messageExt -> {
        try {
            Message message = messageExt.getMessage();
            byte[] data = message.getData();
            String messageBody = new String(data, "UTF-8");
            LOG.info("work order auth receive message: {}", messageBody);
            if (BIUtil.isEmpty(messageBody)) {
                return ConsumeStatus.CONSUME_SUCCESS;
            }
            return authApprovalConsumer.processMessage(messageBody);
        } catch (Exception ex) {
            LOG.error("work order auth consumeCallback error", ex);
            return ConsumeStatus.RECONSUME_LATER;
        }
    };

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public IPushConsumer workOrderAuthConsumer() throws TxMqClientException {
        if (applicationContext.getParent() == null) {
            return new EmptyPushConsumer();
        }
        RuntimeEnv env = BIUtil.getRuntimeEnv();
        if (RuntimeEnv.UT == env || RuntimeEnv.Dev == env) {
            return new EmptyPushConsumer();
        }
        String topic = SC.v("work.order.sub.topic", "bi.ssm.workorder.test.sub");
        return TxMqClient.createPushConsumer(topic, consumeCallback);
    }

    private static class EmptyPushConsumer implements IPushConsumer {

        @Override
        public void start() throws TxMqClientException {
        }

        @Override
        public void shutdown() {
        }
    }
}
