package com.bi.queryer.ssm.engine.grid.header;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryFilter;
import com.bi.queryer.ssm.engine.config.QueryResult;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.grid.header.vo.GridHeaderFilterConfigVO;
import com.bi.queryer.ssm.enums.QueryConfigureType;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Auther: contributor
 * @Date: 2024/8/15 14:02
 * @Description:
 */
public class GridHeaderQueryConfig extends QueryConfigure {
    private final String filterConfigStr;

    public GridHeaderQueryConfig(SSDQueryTemplate templateEntity, String filterConfigStr) {
        super(templateEntity);
        this.filterConfigStr = filterConfigStr;
    }

    @Override
    public void load(String config) {
        super.load(config);
        GridHeaderQueryConfigBuilder.buildQueryConfigure(this);
    }

    @Override
    public List<QueryField> getAllQueryOriginFields() {
        List<QueryField> allFields = new ArrayList<>();
        try {
            JSONObject root = JSONObject.parseObject(getConfig());
            QueryFilter filter = new QueryFilter();
            filter.load(root.getJSONArray("filter"));

            QueryResult result = new QueryResult();
            result.setCustomMeasureFilterIds(filter.getCustomMeasureFilterIds());
            result.load(root.getJSONObject("result"));

            allFields.addAll(result.getFields());
            allFields.addAll(filter.getFields());

            List<GridHeaderFilterConfigVO> filterConfigs = JSONObject.parseArray(this.filterConfigStr, GridHeaderFilterConfigVO.class);
            if (BIUtil.isEmpty(filterConfigs)) {
                return allFields;
            }

            Map<String, GridHeaderFilterConfigVO> filterConfigMap = filterConfigs.stream()
                    .collect(Collectors.toMap(GridHeaderFilterConfigVO::getFieldId, v -> v, (x, y) -> x));

            for (QueryField f : allFields) {
                GridHeaderFilterConfigVO filterConfig = filterConfigMap.get(f.getId());
                if (filterConfig != null) {
                    f.setFilter(true);
                    f.setValues(filterConfig.getFilterValues());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return allFields;
    }

    @Override
    public QueryConfigure clone() {
        return new GridHeaderQueryConfig(this.getTemplateEntity(), this.filterConfigStr);
    }

    public String getFilterConfigStr() {
        return filterConfigStr;
    }

    public QueryConfigureType getType(){
        return QueryConfigureType.GridHeader;
    }
}
