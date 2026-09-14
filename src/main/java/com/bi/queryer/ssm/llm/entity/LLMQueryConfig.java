package com.bi.queryer.ssm.llm.entity;

import java.util.ArrayList;
import java.util.List;

public class LLMQueryConfig {

    private LLMQueryTop top;

    private LLMQueryResult result = new LLMQueryResult();

    private List<LLMQueryField> filter = new ArrayList<>();

    private LLMQuerySetting settings = new LLMQuerySetting();

    public LLMQueryTop getTop() {
        return top;
    }

    public void setTop(LLMQueryTop top) {
        this.top = top;
    }

    public LLMQueryResult getResult() {
        return result;
    }

    public void setResult(LLMQueryResult result) {
        this.result = result;
    }

    public List<LLMQueryField> getFilter() {
        return filter;
    }

    public void setFilter(List<LLMQueryField> filter) {
        this.filter = filter;
    }

    public LLMQuerySetting getSettings() {
        return settings;
    }

    public void setSettings(LLMQuerySetting settings) {
        this.settings = settings;
    }
}
