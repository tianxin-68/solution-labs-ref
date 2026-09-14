package com.bi.queryer.ssm.engine.aggregation;

import com.bi.queryer.ssm.enums.AggExpressionType;

/**
 * @Author contributor
 * @Date 14:40 2023-09-22
 * @Description 聚合器工场类
 **/
public abstract class AggregatorFactory {

    public static DefaultAggregator getAggregator(String aggExpressionType, AggregatorContext cxt) {
        return getAggregator(AggExpressionType.get(aggExpressionType), cxt);
    }

    public static DefaultAggregator getAggregator(AggExpressionType aggExpressionType, AggregatorContext cxt){
        DefaultAggregator aggregator = null;
        switch (aggExpressionType){
            case Avg_By_Day:
                aggregator = new AverageByDayAggregator(cxt);
                break;
            case Avg_By_Day_Real:
                aggregator = new AverageByRealDayAggregator(cxt);
                break;
            default:
                aggregator = new DefaultAggregator(cxt);
                break;
        }
        return aggregator;
    }

    public static DefaultAggregator getAnalysisAggregator(String aggExpressionType, AggregatorContext cxt) {
        return getAnalysisAggregator(AggExpressionType.get(aggExpressionType), cxt);
    }

    public static DefaultAggregator getAnalysisAggregator(AggExpressionType aggExpressionType, AggregatorContext cxt){
        DefaultAggregator aggregator = null;
        switch (aggExpressionType){
            case Avg_By_Day:
                aggregator = new AnalysisAverageByDayAggregator(cxt);
                break;
            case Avg_By_Day_Real:
                aggregator = new AnalysisAverageByRealDayAggregator(cxt);
                break;
            default:
                aggregator = new DefaultAggregator(cxt);
                break;
        }
        return aggregator;
    }
}
