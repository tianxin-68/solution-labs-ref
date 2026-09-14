package com.bi.queryer.ssm.query.filter;

import com.bi.queryer.util.BIMap;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:11 2024-04-17
 * @Description TODO
 **/
public class MultiSelectFilterResult {
    protected List<BIMap> rows = new ArrayList<>();

    protected Integer total = 0;

    protected boolean cache = false;

    protected String sql = "";

    public MultiSelectFilterResult() {
    }

    public MultiSelectFilterResult(List<BIMap> rows, Integer total, String sql) {
        this.rows = rows;
        this.total = total;
        this.sql = sql;
    }

    public List<BIMap> getRows() {
        return rows;
    }

    public void setRows(List<BIMap> rows) {
        this.rows = rows;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }
}
