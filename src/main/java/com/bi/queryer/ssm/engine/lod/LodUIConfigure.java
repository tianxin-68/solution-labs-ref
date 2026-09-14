package com.bi.queryer.ssm.engine.lod;

import com.bi.queryer.ssm.engine.config.field.QueryField;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 15:41 2023-12-18
 * @Description lod配置
 **/
public class LodUIConfigure {
    private String dateGranularity = "";

    private List<QueryField> dimensionList = new ArrayList<>();

    private String measureId = "";

    private String measureCode = "";

    private String aggExpressionType = "";

    private String moduleCtgId;

    /**
     * 维度配置类型，auto = 自动支持模版中可支持维度  manual = 手动指定维度
     */
    private String dimConfigType = "";

    /**
     * lod的时间范围
     * 默认为空，为空则使用模版中设置的时间范围
     */
    private List<String> dateRange = new ArrayList<>();

    public String getDateGranularity() {
        return dateGranularity;
    }

    public void setDateGranularity(String dateGranularity) {
        this.dateGranularity = dateGranularity;
    }

    public List<QueryField> getDimensionList() {
        return dimensionList;
    }

    public void setDimensionList(List<QueryField> dimensionList) {
        this.dimensionList = dimensionList;
    }

    public String getMeasureId() {
        return measureId;
    }

    public void setMeasureId(String measureId) {
        this.measureId = measureId;
    }

    public String getAggExpressionType() {
        return aggExpressionType;
    }

    public void setAggExpressionType(String aggExpressionType) {
        this.aggExpressionType = aggExpressionType;
    }

    public String getMeasureCode() {
        return measureCode;
    }

    public void setMeasureCode(String measureCode) {
        this.measureCode = measureCode;
    }

    public String getModuleCtgId() {
        return moduleCtgId;
    }

    public void setModuleCtgId(String moduleCtgId) {
        this.moduleCtgId = moduleCtgId;
    }

    public String getDimConfigType() {
        return dimConfigType;
    }

    public void setDimConfigType(String dimConfigType) {
        this.dimConfigType = dimConfigType;
    }

    public List<String> getDateRange() {
        return dateRange;
    }

    public void setDateRange(List<String> dateRange) {
        this.dateRange = dateRange;
    }
}
