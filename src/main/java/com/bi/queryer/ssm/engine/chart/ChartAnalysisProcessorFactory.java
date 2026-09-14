package com.bi.queryer.ssm.engine.chart;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.chart.rt.SSMRTChartAnalysisProcessor;

public class ChartAnalysisProcessorFactory {

    public static ChartAnalysisProcessor getProcessor(QueryEngine engine) {

        if (engine.getConfig().isRtDatasetChartQuery()) {
            return new SSMRTChartAnalysisProcessor();
        }

        return new ChartAnalysisProcessor();
    }

}
