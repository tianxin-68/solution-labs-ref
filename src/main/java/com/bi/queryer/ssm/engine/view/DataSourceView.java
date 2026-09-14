package com.bi.queryer.ssm.engine.view;

import com.alibaba.fastjson.JSON;

/**
 * @Author contributor
 * @Date 15:20 2025/3/17
 * @Description 数据源视图
 **/
public class DataSourceView{
    private String sql;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public DataSourceView clone(){
        DataSourceView copy = JSON.parseObject(JSON.toJSONString(this), DataSourceView.class);
        return copy;
    }

}
