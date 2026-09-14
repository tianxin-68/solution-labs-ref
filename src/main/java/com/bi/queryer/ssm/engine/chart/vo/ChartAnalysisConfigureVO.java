package com.bi.queryer.ssm.engine.chart.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/7/10 09:50
 * @Description:
 */

@Setter
@Getter
@ToString
public class ChartAnalysisConfigureVO {
    private String calcMode;

    private String percentFieldRatioUnit;

    private List<String> calcTypes;

    private String title;

    private List<String> compareDates;
}
