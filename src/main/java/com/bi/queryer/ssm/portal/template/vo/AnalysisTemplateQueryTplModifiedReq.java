package com.bi.queryer.ssm.portal.template.vo;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/6/27 13:37
 * @Description:
 */
public class AnalysisTemplateQueryTplModifiedReq {
    private Long dateTime;

    private List<String> queryTplIds;

    public AnalysisTemplateQueryTplModifiedReq(Long dateTime, List<String> queryTplIds) {
        this.dateTime = dateTime;
        this.queryTplIds = queryTplIds;
    }

    public AnalysisTemplateQueryTplModifiedReq() {
        this.dateTime = new Date().getTime();
        this.queryTplIds = Collections.emptyList();
    }

    public Long getDateTime() {
        return dateTime;
    }

    public void setDateTime(Long dateTime) {
        this.dateTime = dateTime;
    }

    public List<String> getQueryTplIds() {
        return queryTplIds;
    }

    public void setQueryTplIds(List<String> queryTplIds) {
        this.queryTplIds = queryTplIds;
    }
}
