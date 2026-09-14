package com.bi.queryer.ssm.engine.config.ui.check;

import java.util.ArrayList;
import java.util.List;

public class UICheckQueryConfigure {

    protected List<UICheckQueryField> filter = new ArrayList<>();

    protected UICheckQueryResult result = new UICheckQueryResult();

    protected UICheckQueryAnalysis analysis = new UICheckQueryAnalysis();

    protected UICheckQuerySettings setting = new UICheckQuerySettings();

    public List<UICheckQueryField> getFilter() {
        return filter;
    }

    public void setFilter(List<UICheckQueryField> filter) {
        this.filter = filter;
    }

    public UICheckQueryResult getResult() {
        return result;
    }

    public void setResult(UICheckQueryResult result) {
        this.result = result;
    }

    public UICheckQueryAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(UICheckQueryAnalysis analysis) {
        this.analysis = analysis;
    }

    public UICheckQuerySettings getSetting() {
        return setting;
    }

    public void setSetting(UICheckQuerySettings setting) {
        this.setting = setting;
    }
}
