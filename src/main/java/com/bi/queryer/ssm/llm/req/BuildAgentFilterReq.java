package com.bi.queryer.ssm.llm.req;

import com.bi.queryer.ssm.engine.config.field.QueryField;

import java.util.ArrayList;
import java.util.List;

public class BuildAgentFilterReq {

    private List<QueryField> globalFilters = new ArrayList<>();
    private List<QueryField> filters = new ArrayList<>();

    public List<QueryField> getGlobalFilters() {
        return globalFilters;
    }

    public void setGlobalFilters(List<QueryField> globalFilters) {
        this.globalFilters = globalFilters;
    }

    public List<QueryField> getFilters() {
        return filters;
    }

    public void setFilters(List<QueryField> filters) {
        this.filters = filters;
    }
}
