package com.bi.queryer.ssm.engine.grid.header;

import com.bi.queryer.ssm.engine.config.QueryFilter;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.grid.header.vo.GridHeaderFilterConfigVO;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.enums.FieldValueFilterType;
import com.bi.queryer.ssm.enums.FilterQueryRuleType;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isNotEmpty;

/**
 * @Auther: contributor
 * @Date: 2024/8/15 14:08
 * @Description: 表格头查询配置构造器
 */
public class GridHeaderQueryConfigBuilder {
    public static void buildQueryConfigure(GridHeaderQueryConfig queryConfigure) {
        List<GridHeaderFilterConfigVO> filterConfigs = JSONObject.parseArray(queryConfigure.getFilterConfigStr(), GridHeaderFilterConfigVO.class);
        if (BIUtil.isEmpty(filterConfigs)) {
            return;
        }

        Map<String, GridHeaderFilterConfigVO> filterConfigMap = filterConfigs.stream()
                .collect(Collectors.toMap(GridHeaderFilterConfigVO::getFieldId, v -> v, (x, y) -> x));
        List<QueryField> queryRowDimensions = queryConfigure.getResult().getRowDimensions();
        Map<String, QueryField> queryRowDimensionsMap = queryRowDimensions.stream()
                .collect(Collectors.toMap(QueryField::getId, v -> v, (x, y) -> x));
        List<QueryField> queryFilters = queryConfigure.getFilter().getFields();

        List<QueryField> globalFilters = new ArrayList<>();//lod筛选
        List<QueryField> filters = new ArrayList<>();//明细筛选

        Set<String> addedFieldIds = new HashSet<>();
        for (QueryField queryField : queryFilters) {
            //表格传入的所有筛选，都对lod有效
            globalFilters.add(queryField);
            GridHeaderFilterConfigVO filterConfig = filterConfigMap.get(queryField.getId());
            if (filterConfig != null && BIUtil.isNotEmpty(filterConfig.getFilterValues())) {
                addedFieldIds.add(filterConfig.getFieldId());
                if (isNotEmpty(filterConfig.getCode())) {
                    addedFieldIds.add(filterConfig.getCode());
                }
                QueryField newFilter = queryField.clone();
                newFilter.setValues(transferFilterValues(queryField, filterConfig.getFilterValues()));
                newFilter.setFilterValueType(FieldValueFilterType.include.toString());
                newFilter.init();
                newFilter.setIsFilter(true);
                filters.add(newFilter);
            } else {
                QueryField newFilter = queryField.clone();
                newFilter.init();
                newFilter.setIsFilter(true);
                filters.add(newFilter);
            }
        }

        for (GridHeaderFilterConfigVO headerFilterConfig : filterConfigs) {
            if (addedFieldIds.contains(headerFilterConfig.getFieldId()) ||
                    (isNotEmpty(headerFilterConfig.getCode()) && addedFieldIds.contains(headerFilterConfig.getCode())) ||
                    BIUtil.isEmpty(headerFilterConfig.getFilterValues())) {
                continue;
            }

            QueryField queryField = queryRowDimensionsMap.get(headerFilterConfig.getFieldId());
            if (queryField != null) {
                QueryField newFilter = queryField.clone();
                newFilter.setValues(transferFilterValues(queryField, headerFilterConfig.getFilterValues()));
                newFilter.setFilterValueType(FieldValueFilterType.include.toString());
                newFilter.init();
                newFilter.setIsFilter(true);
                filters.add(newFilter);
            }
        }

        QueryFilter globalFilter = new QueryFilter();
        globalFilter.setFields(globalFilters);
        queryConfigure.setGlobalFilter(globalFilter);
        queryConfigure.getFilter().setFields(filters);
    }

    private static List<FieldValue> transferFilterValues(QueryField field, List<FieldValue> values) {
        FieldFilterType filterType = FieldUtil.getFilterType(field);
        DataType dataType = DataType.getType(field.getMeta().getDataType());
        if (filterType == FieldFilterType.BooleanSelect) {
            for (FieldValue value : values) {
                if (dataType.isDecimal()) {
                    if ("是".equals(value.getId())) {
                        value.setId("1");
                    } else if ("否".equals(value.getId())) {
                        value.setId("0");
                    } else if ("其他".equals(value.getId())) {
                        value.setId(BIConsts.NULL_VALUE);
                    }
                }
            }
        } else if (filterType == FieldFilterType.Textarea) {
            String value = values.stream().map(FieldValue::getId)
                    .collect(Collectors.joining("\n"));
            field.setFilterQueryRule(FilterQueryRuleType.Exact.getCode());
            return Collections.singletonList(new FieldValue(value, value, filterType.getCode()));
        } else if (filterType == FieldFilterType.MultiSelect) {
            /*处理表头筛选枚举值映射
            String fieldCode = field.getCode();
            Map<String, String> fieldValueMap = SSDMetaCacheManager.getFieldValueMapByFieldCode(fieldCode);
            if (CollectionUtils.isEmpty(values) || fieldValueMap.isEmpty()) {
                return values;
            }
            Map<String, List<String>> map = fieldValueMap.entrySet().stream()
                    .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
            List<FieldValue> res = new ArrayList<>(values.size());
            for (FieldValue v : values) {
                List<String> orgValues = map.get(v.getId());
                if (CollectionUtils.isEmpty(orgValues)) {
                    res.add(v);
                } else {
                    orgValues.forEach(o -> res.add(new FieldValue(o, o, filterType.getCode())));
                }
            }
            return res;*/
        } else if (filterType != null && filterType.isRange()) {
            if (CollectionUtils.isEmpty(values)) {
                return values;
            }
            List<String> ids = values.stream().map(FieldValue::getId)
                    .map(v -> !field.isCommonDate() && filterType == FieldFilterType.MonthRange ? v.replace("-", "") : v)
                    .collect(Collectors.toList());
            List<FieldValue> res = new ArrayList<>(4);
            if (CollectionUtils.size(ids) == 1) {
                String id = ids.get(0);
                res.add(new FieldValue(id, id, filterType.getCode()));
                res.add(new FieldValue(id, id, filterType.getCode()));
            } else {
                ids.sort(Comparator.comparing(v -> v));
                String start = ids.get(0);
                String end = ids.get(ids.size() - 1);
                res.add(new FieldValue(start, start, filterType.getCode()));
                res.add(new FieldValue(end, end, filterType.getCode()));
            }
            return res;
        }
        return values;
    }
}
