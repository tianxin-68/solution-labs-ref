package com.bi.queryer.ssm.engine.config.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:27 2024/11/13
 * @Description 前端传递查询配置对应实体的封装：只对应config属性
 **/
public class UIQueryConfigure {
    protected List<UIQueryField> filter = new ArrayList<>();

    protected UIQueryResult result = new UIQueryResult();

    protected UIQuerySettings setting = new UIQuerySettings();

    protected UIQueryAnalysis analysis = new UIQueryAnalysis();

    private String sessionId;

    public List<UIQueryField> getFilter() {
        return filter;
    }

    public void setFilter(List<UIQueryField> filter) {
        this.filter = filter;
    }

    public UIQueryResult getResult() {
        return result;
    }

    public void setResult(UIQueryResult result) {
        this.result = result;
    }

    public UIQuerySettings getSetting() {
        return setting;
    }

    public void setSetting(UIQuerySettings setting) {
        this.setting = setting;
    }

    public UIQueryAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(UIQueryAnalysis analysis) {
        this.analysis = analysis;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
