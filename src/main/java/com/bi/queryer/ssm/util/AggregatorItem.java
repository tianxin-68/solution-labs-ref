package com.bi.queryer.ssm.util;

/**
 * @Author contributor
 * @Date 18:01 2024/11/21
 * @Description TODO
 **/
public class AggregatorItem {
    /**
     * 聚合函数
     */
    private String aggregator;

    /**
     * 聚合函数内容
     */
    private String content;

    private Integer startIndex = 0;

    private Integer endIndex = 0;

    public String getAggregator() {
        return aggregator;
    }

    public AggregatorItem setAggregator(String aggregator) {
        this.aggregator = aggregator;
        return this;
    }

    public String getContent() {
        return content;
    }

    public AggregatorItem setContent(String content) {
        this.content = content;
        return this;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public AggregatorItem setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
        return this;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public AggregatorItem setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
        return this;
    }

    @Override
    public String toString() {
        return "aggregator=" + aggregator + "\tcontent=" + content;
    }
}
