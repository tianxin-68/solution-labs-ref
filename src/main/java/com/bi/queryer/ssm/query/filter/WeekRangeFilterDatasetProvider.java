package com.bi.queryer.ssm.query.filter;

import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.util.period.DateUtil;
import com.bi.queryer.util.period.Week;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * User: contributor
 * Date: 2020/2/28
 * Time: 10:19
 * Description:
 */
@Component
@Scope("prototype")
public class WeekRangeFilterDatasetProvider implements IFilterDatasetProvider{

    @Override
    public Object buildDataset(MetaField metaField, Map<String, Object> params) {
        List<Week> weeks = DateUtil.getWeeks(params.get("year") + "");
        return weeks;
    }
}
