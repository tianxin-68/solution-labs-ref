package com.bi.queryer.ssm.engine.chart;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.QueryConfigureType;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Auther: contributor
 * @Date: 2024/7/9 17:28
 * @Description: 趋势图的查询配置
 */
public class ChartQueryConfigure extends QueryConfigure {

    public ChartQueryConfigure(SSDQueryTemplate templateEntity) {
        super(templateEntity);
    }

    @Override
    public void load(String config) {
        ChartQueryConfigureBuilder.buildQueryConfigure(config, this);

        // 后处理
        this.post();
    }

    @Override
    public List<QueryField> getAllQueryOriginFields() {
        List<QueryField> fields = super.getAllFields();
        return fields.stream().filter(v -> !v.isAppend()).collect(Collectors.toList());
    }

    @Override
    public QueryConfigure clone() {
        return new ChartQueryConfigure(this.getTemplateEntity());
    }

    public QueryConfigureType getType(){
        return QueryConfigureType.Chart;
    }
}
