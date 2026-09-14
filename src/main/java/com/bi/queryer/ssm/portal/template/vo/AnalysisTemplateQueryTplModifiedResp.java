package com.bi.queryer.ssm.portal.template.vo;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/6/27 13:37
 * @Description:
 */
public class AnalysisTemplateQueryTplModifiedResp {
    private Long dateTime;

    private List<QueryTplVO> queryTpls;

    public AnalysisTemplateQueryTplModifiedResp(Long dateTime, List<QueryTplVO> queryTpls) {
        this.dateTime = dateTime;
        this.queryTpls = queryTpls;
    }

    public AnalysisTemplateQueryTplModifiedResp() {
        this.dateTime = new Date().getTime();
        this.queryTpls = Collections.emptyList();
    }

    public Long getDateTime() {
        return dateTime;
    }

    public void setDateTime(Long dateTime) {
        this.dateTime = dateTime;
    }


    public List<QueryTplVO> getQueryTpls() {
        return queryTpls;
    }

    public void setQueryTpls(List<QueryTplVO> queryTpls) {
        this.queryTpls = queryTpls;
    }

    public static class QueryTplVO {
        private String queryTplId;
        private String datasetType;


        public QueryTplVO() {
        }

        public QueryTplVO(String queryTplId, String datasetType) {
            this.queryTplId = queryTplId;
            this.datasetType = datasetType;
        }

        public String getQueryTplId() {
            return queryTplId;
        }

        public void setQueryTplId(String queryTplId) {
            this.queryTplId = queryTplId;
        }

        public String getDatasetType() {
            return datasetType;
        }

        public void setDatasetType(String datasetType) {
            this.datasetType = datasetType;
        }
    }
}
