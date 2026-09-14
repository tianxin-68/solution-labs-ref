package com.bi.queryer.ssm.engine.config.ui;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.CalendarType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author contributor
 * @Date 16:29 2024/11/13
 * @Description TODO
 **/
public class UIQueryField {
    private String id;

    private String code;

    private String name;

    private String title ;

    // 是否显示
    private Integer isShow = Enabled.YES.getId();

    // 显示顺序
    private Double showOrder = (double)-1;

    // 过滤值类型
    private String filterValueType = "";

    // 筛选方式
    private String filterValueMode = "";

    // 查询规则
    private String filterQueryRule;

    // 过滤值
    private List<FieldValue> values = new ArrayList<FieldValue>();

    /**
     * 过滤值标题，用于前端显示
     */
    private String valuesTitle = "";

    private FieldSortType sortType = FieldSortType.NONE;

    // 显示名
    private String displayTitle = "";

    //模块目录id
    private String moduleCtgId;

    // 所属目录id
    private String ctgId;

    /**
     * 自定义字段配置
     */
    protected CustomFieldConfigure customFieldConfigure = new CustomFieldConfigure();

    /**字段分析相关**/
    protected Integer isAnalysis = Enabled.NO.getId();

    protected AnalysisItemConfig analysisConfig = new AnalysisItemConfig();

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

    protected String ctgDataType = "";

    private String fieldType;

    private String filterObject;

    /**
     * 字段路径
     */
    private List<String> path = new ArrayList<>();

    //小数点位数
    private Integer decimalPlaces;

    private boolean isInResultArea = false;

    private boolean isInFilterArea = false;

    public boolean isInResultArea() {
        return isInResultArea;
    }

    public void setInResultArea(boolean inResultArea) {
        isInResultArea = inResultArea;
    }

    public boolean isInFilterArea() {
        return isInFilterArea;
    }

    public void setInFilterArea(boolean inFilterArea) {
        isInFilterArea = inFilterArea;
    }

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

    public Double getShowOrder() {
        return showOrder;
    }

    public void setShowOrder(Double showOrder) {
        this.showOrder = showOrder;
    }

    public String getFilterValueType() {
        return filterValueType;
    }

    public void setFilterValueType(String filterValueType) {
        this.filterValueType = filterValueType;
    }

    public String getFilterValueMode() {
        return filterValueMode;
    }

    public void setFilterValueMode(String filterValueMode) {
        this.filterValueMode = filterValueMode;
    }

    public String getFilterQueryRule() {
        return filterQueryRule;
    }

    public void setFilterQueryRule(String filterQueryRule) {
        this.filterQueryRule = filterQueryRule;
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
        if (displayTitle == null) {
            displayTitle = "";
        }
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public String getModuleCtgId() {
        return moduleCtgId;
    }

    public void setModuleCtgId(String moduleCtgId) {
        this.moduleCtgId = moduleCtgId;
    }

    public CustomFieldConfigure getCustomFieldConfigure() {
        return customFieldConfigure;
    }

    public void setCustomFieldConfigure(CustomFieldConfigure customFieldConfigure) {
        this.customFieldConfigure = customFieldConfigure;
    }

    public Integer getIsAnalysis() {
        return isAnalysis;
    }

    public void setIsAnalysis(Integer isAnalysis) {
        this.isAnalysis = isAnalysis;
    }

    public AnalysisItemConfig getAnalysisConfig() {
        return analysisConfig;
    }

    public void setAnalysisConfig(AnalysisItemConfig analysisConfig) {
        this.analysisConfig = analysisConfig;
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

    public String getAggExpressionType() {
        return aggExpressionType;
    }

    public void setAggExpressionType(String aggExpressionType) {
        this.aggExpressionType = aggExpressionType;
    }

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public boolean isLodField(){
        if(this.customFieldConfigure != null && CustomFieldType.isLod(this.customFieldConfigure.getType())){
            return true;
        }
        return false;
    }

    //是否包含分析项指标的计算指标
    public boolean isAnalysisCalc() {
        return this.customFieldConfigure != null && StrUtil.isNotEmpty(this.customFieldConfigure.getExpression()) &&
                this.customFieldConfigure.getExpression().contains(CustomFieldType.ANALYSIS.getIdentifier());
    }

    public boolean isCalcField(){
        boolean isCalc = this.getCustomFieldConfigure() != null && BIUtil.isNotEmpty(this.getCustomFieldConfigure().getExpression());
        return isCalc;
    }

    public List<String> getCalcRefFieldIdList(){
        List<String> refFieldIdList = new ArrayList<>();
        if(!isCalcField()){
            return refFieldIdList;
        }

        CustomFieldConfigure customFieldConfigure = this.getCustomFieldConfigure();
        String expression = customFieldConfigure.getExpression();
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()){
            String fieldId = matcher.group();
            refFieldIdList.add(fieldId);
        }
        return refFieldIdList;
    }

    public boolean isCommonDate(){
        MetaField meta = SSDMetaCacheManager.getField(id);
        if(meta != null && Enabled.isTrue(meta.getIsCommonDate())){
            return true;
        }
        return false;
    }

    public String getCtgDataType() {
        return ctgDataType;
    }

    public void setCtgDataType(String ctgDataType) {
        this.ctgDataType = ctgDataType;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        return code.equals(((UIQueryField)obj).getCode());
    }

    @Override
    public int hashCode() {
        if(code == null) return -1;
        return code.hashCode();
    }

    @Override
    public String toString() {
        return "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", title='" + title + '\'';
    }

    public String getDistinctByDayAggMode() {
        return distinctByDayAggMode;
    }

    public void setDistinctByDayAggMode(String distinctByDayAggMode) {
        this.distinctByDayAggMode = distinctByDayAggMode;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFilterObject() {
        return filterObject;
    }

    public void setFilterObject(String filterObject) {
        this.filterObject = filterObject;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public String getCtgId() {
        return ctgId;
    }

    public void setCtgId(String ctgId) {
        this.ctgId = ctgId;
    }

    public String getQueryCalendarType() {
        return queryCalendarType;
    }

    public void setQueryCalendarType(String queryCalendarType) {
        this.queryCalendarType = queryCalendarType;
    }
}
