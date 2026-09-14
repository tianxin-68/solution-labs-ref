package com.bi.queryer.ssm.meta;

import java.util.List;

/**
 * @Description: 指标支持的数据切片粒度
 */
public class MetricSupportDataSliceGranularityRsp {

    /**
     * 指标编码
     */
    private String metricCode;

    /**
     * 数据切片粒度列表
     */
    private List<String> dataSliceGranularityList;

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public List<String> getDataSliceGranularityList() {
        return dataSliceGranularityList;
    }

    public void setDataSliceGranularityList(List<String> dataSliceGranularityList) {
        this.dataSliceGranularityList = dataSliceGranularityList;
    }

}
