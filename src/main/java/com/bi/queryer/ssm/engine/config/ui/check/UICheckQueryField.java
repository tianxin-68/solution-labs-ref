package com.bi.queryer.ssm.engine.config.ui.check;

import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.CalendarType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;

public class UICheckQueryField {


    private String id;

    private String code;

    private String name;

    private String title ;

    // 是否显示
    private Integer isShow = Enabled.YES.getId();



    // 过滤值
    private List<FieldValue> values = new ArrayList<FieldValue>();

    /**
     * 过滤值标题，用于前端显示
     */
    private String valuesTitle = "";

    private FieldSortType sortType = FieldSortType.NONE;

    // 显示名
    private String displayTitle = "";


    /**
     * 自定义字段配置
     */
    protected CustomFieldConfigure customFieldConfigure = new CustomFieldConfigure();

    /**
     * 是否是聚合汇总查询
     */
    protected Integer isAggQuery = Enabled.NO.getId();

    /**
     * 查询日期粒度：默认为日
     */
    protected String queryDateGranularity = DateGranularity.DAY.getCode();

    /**
     * 查询日历类型
     */
    protected String queryCalendarType = CalendarType.NATURAL.getCode();

    /**
     * 聚合类型：前端可设置其聚合表达式类型（如：默认、日均值）
     */
    protected String aggExpressionType = "";

    /**
     * 按日去重后的聚合模式：默认avg，可选sum
     */
    protected String distinctByDayAggMode = AggExpressionType.Avg.getCode();

    //小数点位数
    private Integer decimalPlaces;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getIsShow() {
        return isShow;
    }

    public void setIsShow(Integer isShow) {
        this.isShow = isShow;
    }

    public List<FieldValue> getValues() {
        return values;
    }

    public void setValues(List<FieldValue> values) {
        this.values = values;
    }

    public String getValuesTitle() {
        return valuesTitle;
    }

    public void setValuesTitle(String valuesTitle) {
        this.valuesTitle = valuesTitle;
    }

    public FieldSortType getSortType() {
        return sortType;
    }

    public void setSortType(FieldSortType sortType) {
        this.sortType = sortType;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public CustomFieldConfigure getCustomFieldConfigure() {
        return customFieldConfigure;
    }

    public void setCustomFieldConfigure(CustomFieldConfigure customFieldConfigure) {
        this.customFieldConfigure = customFieldConfigure;
    }

    public Integer getIsAggQuery() {
        return isAggQuery;
    }

    public void setIsAggQuery(Integer isAggQuery) {
        this.isAggQuery = isAggQuery;
    }

    public String getQueryDateGranularity() {
        return queryDateGranularity;
    }

    public void setQueryDateGranularity(String queryDateGranularity) {
        this.queryDateGranularity = queryDateGranularity;
    }

    public String getQueryCalendarType() {
        return queryCalendarType;
    }

    public void setQueryCalendarType(String queryCalendarType) {
        this.queryCalendarType = queryCalendarType;
    }

    public String getAggExpressionType() {
        return aggExpressionType;
    }

    public void setAggExpressionType(String aggExpressionType) {
        this.aggExpressionType = aggExpressionType;
    }

    public String getDistinctByDayAggMode() {
        return distinctByDayAggMode;
    }

    public void setDistinctByDayAggMode(String distinctByDayAggMode) {
        this.distinctByDayAggMode = distinctByDayAggMode;
    }

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }
}
