package com.bi.queryer.ssm.engine.config.ui.check;

import java.util.ArrayList;
import java.util.List;

public class UICheckQueryResult {

    private List<UICheckQueryField> rowDimensions = new ArrayList<>();

    private List<UICheckQueryField> colDimensions = new ArrayList<>();

    private List<UICheckQueryField> measures = new ArrayList<>();

    public List<UICheckQueryField> getRowDimensions() {
        return rowDimensions;
    }

    public void setRowDimensions(List<UICheckQueryField> rowDimensions) {
        this.rowDimensions = rowDimensions;
    }

    public List<UICheckQueryField> getColDimensions() {
        return colDimensions;
    }

    public void setColDimensions(List<UICheckQueryField> colDimensions) {
        this.colDimensions = colDimensions;
    }

    public List<UICheckQueryField> getMeasures() {
        return measures;
    }

    public void setMeasures(List<UICheckQueryField> measures) {
        this.measures = measures;
    }
}
