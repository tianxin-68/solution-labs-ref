package com.bi.queryer.ssm.engine.analysis.operator;

import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.dataset.AnalysisDataSet;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;

import java.util.List;

/**
 * @Author contributor
 * @Date 20:14 2023-07-31
 * @Description 算子上下文
 **/
public class OperatorContext {
    public List<QueryField> dimFields;

    public List<QueryField> measureFields;

    public AnalysisDataSet currentDataSet;

    public  AnalysisDataSet analysisDataSet;

    public boolean isCrossDimensionQuery = false;

    public AnalysisItemConfig analysisItemConfig = null;

    public QueryConfigure config = null;

    public OperatorContext() {
    }

    public OperatorContext(List<QueryField> dimFields, List<QueryField> measureFields, AnalysisDataSet currentDataSet, AnalysisDataSet analysisDataSet) {
        this.dimFields = dimFields;
        this.measureFields = measureFields;
        this.currentDataSet = currentDataSet;
        this.analysisDataSet = analysisDataSet;
    }

    public OperatorContext(List<QueryField> dimFields, List<QueryField> measureFields, AnalysisDataSet currentDataSet) {
        this.dimFields = dimFields;
        this.measureFields = measureFields;
        this.currentDataSet = currentDataSet;
    }

    public OperatorContext(List<QueryField> dimFields, AnalysisDataSet currentDataSet) {
        this.dimFields = dimFields;
        this.currentDataSet = currentDataSet;
    }

    public List<QueryField> getDimFields() {
        return dimFields;
    }

    public void setDimFields(List<QueryField> dimFields) {
        this.dimFields = dimFields;
    }

    public List<QueryField> getMeasureFields() {
        return measureFields;
    }

    public void setMeasureFields(List<QueryField> measureFields) {
        this.measureFields = measureFields;
    }

    public AnalysisDataSet getCurrentDataSet() {
        return currentDataSet;
    }

    public void setCurrentDataSet(AnalysisDataSet currentDataSet) {
        this.currentDataSet = currentDataSet;
    }

    public AnalysisDataSet getAnalysisDataSet() {
        return analysisDataSet;
    }

    public void setAnalysisDataSet(AnalysisDataSet analysisDataSet) {
        this.analysisDataSet = analysisDataSet;
    }

    public boolean isCrossDimensionQuery() {
        return isCrossDimensionQuery;
    }

    public void setCrossDimensionQuery(boolean crossDimensionQuery) {
        isCrossDimensionQuery = crossDimensionQuery;
    }

    public AnalysisItemConfig getAnalysisItemConfig() {
        return analysisItemConfig;
    }

    public void setAnalysisItemConfig(AnalysisItemConfig analysisItemConfig) {
        this.analysisItemConfig = analysisItemConfig;
    }

    public QueryConfigure getConfig() {
        return config;
    }

    public void setConfig(QueryConfigure config) {
        this.config = config;
    }
}
