package com.bi.queryer.ssm.engine.accelerate.hot.mq;

import com.bi.queryer.ssm.engine.accelerate.hot.mq.action.ActionByMQ;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import com.tx.mq.consumer.ConsumeStatus;
import com.tx.mq.consumer.IConsumeCallback;
import com.tx.mq.exception.TxMqClientException;
import com.tx.mq.iface.consumer.IPushConsumer;
import com.tx.mq.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;

/**
 * @Author contributor
 * @Date 16:53 2024/8/19
 * @Description 热数据MQ接收类
 * <p>
 * root 容器和 DispatcherServlet 子容器都会扫描到子类（@Configuration），各自创建一次 Bean。
 * 子类的 {@code @Bean(initMethod="start")} 方法应先判断 {@code applicationContext.getParent() == null}，
 * 是则返回 {@link EmptyPushConsumer}——真正的消费者只在子容器（parent 必为 root）里创建。
 **/
public abstract class HotMQReceiver implements ApplicationContextAware {
    private final static Logger LOG = LoggerFactory.getLogger(HotMQReceiver.class);

    protected ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 判断：是否是可接收的MQ
     *
     * @return 源表表名（带库名）
     */
    abstract public List<ActionByMQ> accept(JSONObject jsonObject);

    public void receive(JSONObject jsonObject) {
        List<ActionByMQ> actions = this.accept(jsonObject);
        if (BIUtil.isEmpty(actions)) {
            return;
        }
        for (ActionByMQ action : actions) {
            if (action == null) {
                continue;
            }
            action.action();
        }
    }

    final IConsumeCallback consumeCallback = messageExt -> {
        try {
            // 处理业务逻辑，完成后返回CONSUME_SUCCESS状态
            Message message = messageExt.getMessage();
            byte[] data = message.getData();
            String messageBody = new String(data);
            LOG.info("hot table receive message: " + messageBody);
            if (BIUtil.isEmpty(messageBody)) {
                return ConsumeStatus.CONSUME_SUCCESS;
            }

            JSONObject jsonObject = JSONObject.parseObject(messageBody);
            receive(jsonObject);
            //业务逻辑
            return ConsumeStatus.CONSUME_SUCCESS;
        } catch (Exception ex) {

            LOG.info("consumeCallback error: " ,ex);
            //异常处理逻辑，业务上考虑如何处理异常
            //如果需要进行消息重试 return ConsumeStatus.RECONSUME_LATER
            //如果不需要进行重试 return ConsumeStatus.CONSUME_DISCARD，消息不会进入死信队列，用户认为可以丢弃，类似 ConsumeStatus.CONSUME_SUCCESS
            return ConsumeStatus.RECONSUME_LATER;
        }
    };

    protected static class EmptyPushConsumer implements IPushConsumer {

        @Override
        public void start() throws TxMqClientException {

        }

        @Override
        public void shutdown() {

        }
    }
}
