package com.bi.queryer.ssm.engine.chart.vo;

import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/7/10 10:01
 * @Description: 图表查询的前台配置
 */

@Setter
@Getter
@ToString
public class ChartQueryConfigVO {
    private List<QueryField> dimensions;

    private List<ChartAnalysisConfigureVO> analyses;

    //全局筛选，对lod生效
    private List<QueryField> globalFilters;

    //明细筛选，趋势图tab配置的筛选，对主查询生效
    private List<QueryField> filters;

    private List<QueryField> measures;

    private String sessionId;

    private QuerySettings setting;

    //自定义指标类型的过滤字段id集合
    private List<String> customMeasureFilterIds = new ArrayList<>();
}
