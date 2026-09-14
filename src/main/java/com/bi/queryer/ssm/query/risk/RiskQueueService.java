package com.bi.queryer.ssm.query.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class RiskQueueService {

    private static final Logger logger = LoggerFactory.getLogger(RiskQueueService.class);

    /**
     * 任务队列
     */
    private final LinkedBlockingQueue<RiskEngine> tasks = new LinkedBlockingQueue<>(500);

    private ExecutorService service = Executors.newFixedThreadPool(3);

    private boolean running = true;

    /**
     * 消费队列的线程数
     */
    private int threadNum = 2;

    @PostConstruct
    public void init() {
        for (int i = 0; i < threadNum; i++) {

            service.submit(new Thread(new Runnable() {
                @Override
                public void run() {
                    while (running) {
                        try {
                            RiskEngine riskEngine = tasks.take();
                            riskEngine.process();
                        } catch (Exception e) {
                            logger.error("队列消费异常",e);
                            e.printStackTrace();
                        }
                    }

                }
            }));
        }

        System.out.println("风控上报处理队列启动......");
    }

    /**
     * 添加到任务队列
     *
     * @param dataHandler
     * @return
     */
    public boolean addQueue(RiskEngine dataHandler) {

        try {
            boolean success = tasks.offer(dataHandler);
            if (!success) {
                logger.warn("队列已满！..");
            }
        } catch (Exception e) {
            logger.error("加入队列失败", e);
        }

        return true;
    }

    /**
     * 启动队列
     */
    public void startQueue() {
        if (!this.running) {
            this.running = true;
            init();
        }
    }

    /**
     * 停止队列消费
     */
    public void stopQueue() {
        if (this.running) {
            this.running = false;
        }

    }

    /**
     * 消费队列数量
     *
     * @return
     */
    public int getTaskNum() {
        return tasks.size();
    }
}
