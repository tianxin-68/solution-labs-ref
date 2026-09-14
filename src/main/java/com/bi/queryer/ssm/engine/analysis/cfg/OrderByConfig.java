package com.bi.queryer.ssm.engine.analysis.cfg;

/**
 * @Auther: contributor
 * @Date: 2024/7/10 17:37
 * @Description:
 */
public class OrderByConfig {
    // 精简后的orderBy语句
    private final String orderByExpression;

    //需要上移到select里的依赖字段
    private final String selectExpression;

    public OrderByConfig(String orderByExpression, String selectExpression) {
        this.orderByExpression = orderByExpression;
        this.selectExpression = selectExpression;
    }

    public String getSelectExpression() {
        return selectExpression;
    }

    public String getOrderByExpression() {
        return orderByExpression;
    }
}
