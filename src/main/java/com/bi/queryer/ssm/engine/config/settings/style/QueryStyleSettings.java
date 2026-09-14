package com.bi.queryer.ssm.engine.config.settings.style;

/**
 * @Author contributor
 * @Date 14:20 2023-11-28
 * @Description 查询样式设置
 **/
public class QueryStyleSettings {
    private String layout;

    /**
     * 上下排序
     */
    private QuerySortSettings upDownSortData = new QuerySortSettings();

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public QuerySortSettings getUpDownSortData() {
        return upDownSortData;
    }

    public void setUpDownSortData(QuerySortSettings upDownSortData) {
        this.upDownSortData = upDownSortData;
    }
}
