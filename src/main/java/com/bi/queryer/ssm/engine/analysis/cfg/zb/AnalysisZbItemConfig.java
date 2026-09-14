package com.bi.queryer.ssm.engine.analysis.cfg.zb;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-22  15:33
 * @Description: 分析-占比-配置
 */
public class AnalysisZbItemConfig {

    /**
     * 统计维度id列表
     */
    private List<String> measureIdList;

    /**
     * 占比方式
     */
    private String calcMode;

    /**
     * 单元柱状图是否开启
     */
    private Boolean showPercentCell;

    public List<String> getMeasureIdList() {
        return measureIdList;
    }

    public void setMeasureIdList(List<String> measureIdList) {
        this.measureIdList = measureIdList;
    }

    public String getCalcMode() {
        return calcMode;
    }

    public void setCalcMode(String calcMode) {
        this.calcMode = calcMode;
    }

    public Boolean getShowPercentCell() {
        return showPercentCell;
    }

    public void setShowPercentCell(Boolean showPercentCell) {
        this.showPercentCell = showPercentCell;
    }
}
