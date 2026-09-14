package com.bi.queryer.ssm.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:44 2023-12-19
 * @Description 查询sql片段
 **/
public class QuerySqlFragments {
    private List<String> selectFragments = new ArrayList<>();

    private List<String> fromFragments = new ArrayList<>();

    private List<String> whereFragments = new ArrayList<>();

    private List<String> groupByFragments = new ArrayList<>();

    private List<String> havingFragments = new ArrayList<>();

    private List<String> orderByFragments = new ArrayList<>();

    public QuerySqlFragments() {
    }

    public QuerySqlFragments(List<String> selectFragments) {
        this.selectFragments = selectFragments;
    }

    public List<String> getSelectFragments() {
        return selectFragments;
    }

    public void setSelectFragments(List<String> selectFragments) {
        this.selectFragments = selectFragments;
    }

    public List<String> getFromFragments() {
        return fromFragments;
    }

    public void setFromFragments(List<String> fromFragments) {
        this.fromFragments = fromFragments;
    }

    public List<String> getWhereFragments() {
        return whereFragments;
    }

    public void setWhereFragments(List<String> whereFragments) {
        this.whereFragments = whereFragments;
    }

    public List<String> getGroupByFragments() {
        return groupByFragments;
    }

    public void setGroupByFragments(List<String> groupByFragments) {
        this.groupByFragments = groupByFragments;
    }

    public List<String> getHavingFragments() {
        return havingFragments;
    }

    public void setHavingFragments(List<String> havingFragments) {
        this.havingFragments = havingFragments;
    }

    public List<String> getOrderByFragments() {
        return orderByFragments;
    }

    public void setOrderByFragments(List<String> orderByFragments) {
        this.orderByFragments = orderByFragments;
    }
}
