package com.bi.queryer.ssm.engine.aggregation;

import com.bi.queryer.ssm.engine.config.field.QueryField;

/**
 * @Author contributor
 * @Date 14:39 2023-09-22
 * @Description 日均值(有数据日期)聚合器
 **/
public class AnalysisAverageByRealDayAggregator extends AverageByRealDayAggregator{
    public AnalysisAverageByRealDayAggregator(AggregatorContext cxt) {
        super(cxt);
    }

    @Override
    public String aggregate(QueryField field) {
        return super.aggregate(field);
    }

    /*
    @Override
    protected List<String> getEndDateList(QueryField field, AggregatorContext cxt) {
        List<String> endDateList = new ArrayList<>();
        if(cxt.compareIndex < 0 && !cxt.isThb){
            return endDateList;
        }
        List<List<String>> excludeDateSegments = AnalysisUtil.getExcludeAnalysisDateFilterRange(cxt.config, cxt.analysisCalcMode, cxt.compareIndex);

        if(BIUtil.isEmpty(excludeDateSegments)){
            return endDateList;
        }

        for(List<String> segment : excludeDateSegments){
            String endDate = DateUtil.offset(DateUtil.parseDate(segment.get(0)), DateField.DAY_OF_YEAR, -1).toDateStr();
            endDateList.add(endDate);
        }

        return endDateList;
    }
     */
}
