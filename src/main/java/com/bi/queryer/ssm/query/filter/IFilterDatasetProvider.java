package com.bi.queryer.ssm.query.filter;

import com.bi.queryer.ssm.meta.MetaField;

import java.util.Map;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 12:39
 * Description:
 */
public interface IFilterDatasetProvider {
    public Object buildDataset(MetaField metaField, Map<String, Object> params);
}
