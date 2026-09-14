package com.bi.queryer.ssm.engine.lod;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:49 2023-12-18
 * @Description lod查询配置
 **/
public class LodQueryConfigure {
    // 原始查询配置:不可直接查询
    private QueryConfigure rawConfig = null;

    // 可执行的配置
    private List<LodQueryConfigureItem> items = new ArrayList<>();

    private List<QueryField> analysisCalcFields;

    public LodQueryConfigure(QueryConfigure rawConfig) {
        this.rawConfig = rawConfig;
    }

    public QueryConfigure getRawConfig() {
        return rawConfig;
    }

    public void setRawConfig(QueryConfigure rawConfig) {
        this.rawConfig = rawConfig;
    }

    public List<LodQueryConfigureItem> getItems() {
        return items;
    }

    public void setItems(List<LodQueryConfigureItem> items) {
        this.items = items;
    }

    public void add(LodQueryConfigureItem item){
        this.items.add(item);
    }

    public List<QueryField> getAnalysisCalcFields() {
        return analysisCalcFields;
    }

    public void setAnalysisCalcFields(List<QueryField> analysisCalcFields) {
        this.analysisCalcFields = analysisCalcFields;
    }
}
