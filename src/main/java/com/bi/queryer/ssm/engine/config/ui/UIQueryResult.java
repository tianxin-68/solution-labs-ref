package com.bi.queryer.ssm.engine.config.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 16:40 2024/11/13
 * @Description TODO
 **/
public class UIQueryResult {
    private List<UIQueryField> rowDimensions = new ArrayList<>();

    private List<UIQueryField> colDimensions = new ArrayList<>();

    private List<UIQueryField> measures = new ArrayList<>();

    public List<UIQueryField> getAllFields(){
        List<UIQueryField> fields = new ArrayList<>();
        fields.addAll(rowDimensions);
        fields.addAll(colDimensions);
        fields.addAll(measures);
        return fields;
    }
    public List<UIQueryField> getRowDimensions() {
        return rowDimensions;
    }

    public void setRowDimensions(List<UIQueryField> rowDimensions) {
        this.rowDimensions = rowDimensions;
    }

    public List<UIQueryField> getColDimensions() {
        return colDimensions;
    }

    public void setColDimensions(List<UIQueryField> colDimensions) {
        this.colDimensions = colDimensions;
    }

    public List<UIQueryField> getMeasures() {
        return measures;
    }

    public void setMeasures(List<UIQueryField> measures) {
        this.measures = measures;
    }

    public void addMeasure(UIQueryField measureField){
        if(this.measures == null){
            this.measures = new ArrayList<>();
        }
        if(!this.measures.contains(measureField)){
            this.measures.add(measureField);
        }
    }
}
