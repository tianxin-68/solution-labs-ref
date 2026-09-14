package com.bi.queryer.ssm.api.vo.req;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询OLAP数据,时间请求参数
 */
public class QueryOlapDataQueryDateReq {

    /**
     * 时间粒度
     */
    private String granularity;

    private String aggregate;

    private List<String> values = new ArrayList<>();

    /**
     * 查询日期类型：nature（自然日历）/ biz（业务日历），默认 nature
     */
    private String type = "nature";

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public String getAggregate() {
        return aggregate;
    }

    public void setAggregate(String aggregate) {
        this.aggregate = aggregate;
    }

    public String getType() {
        return StrUtil.isNotBlank(type) ? type : "nature";
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEmpty() {
        return StrUtil.isEmpty(granularity) || CollUtil.isEmpty(values) || values.stream().allMatch(StrUtil::isBlank);
    }

}
