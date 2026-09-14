package com.bi.queryer.ssm.engine.config.field;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldManager;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisConfigFactory;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.result.ResultDataSetTargetConfig;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 前端展示和查询引擎字段信息
 * @author contributor
 *
 */
public class QueryField implements JSONSerializable, Comparable<QueryField>, Cloneable{

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

	/**
	 * 筛选对象
	 * 筛选方式 = 结果筛选，且有汇总时生效
	 * 筛选明细和汇总 = all 仅筛选明细 detail
	 */
	private String filterObject = "all";

	// 过滤值
	private List<FieldValue> values = new ArrayList<FieldValue>();

	// 元信息
	private MetaField meta = null;

	// 互斥字段
	private List<MetaField> mutexList = new ArrayList<MetaField>();

	// 查询类型
	private Boolean isFilter = false;

	private Boolean isResult = false;

	/**
	 * 字段所属的查询表
	 */
	private QueryTable table = null;

	/**
	 * 是否有效
	 */
	private boolean active = true;

	/**
	 * 是否是级联过滤字段
	 */
	private boolean cascadeFilter = false;

	/**
	 * 是否是层级字段:只有层次level字段才为true，便于json序列化
	 */
	private boolean isHierarchy =  false;

	/**
	 * 过滤值标题，用于前端显示
	 */
	private String valuesTitle = "";

	// 别名
	private String alias = "";

	private FieldSortType sortType = FieldSortType.NONE;

	private boolean isCalc = false ;// 是否是计算字段

	private Set<QueryField> calcAtomFields = new HashSet<QueryField>(); // 该字段为计算字段时，存储计算表达式关联的原子字段编码

	// 前台自定义计算字段的直接依赖字段
	private Set<QueryField> cusCalcDependFields = new HashSet<>();

	// 是否是表达式字段附加字段，即表达式中的原子字段为附加字段
	// 附加字段不参与计算，但参与过滤
	private boolean isAppend = false;

	// 逻辑id相同的字段列表，如月、年，原生字段属于同一个字段SO_DATE，则月字段的此列表包含年字段
	private List<QueryField> logicIdSameList = new ArrayList<QueryField>();

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

	protected FieldCategoryAggregation categoryAggregation = new FieldCategoryAggregation();

	/**
	 * 是否是虚拟字段，此类字段在最终查询时为null
	 */
	protected boolean virtual = false;

	/**
	 * 是否是被扩展的字段：如：源字段=日期，被扩展的字段=年月
	 */
	protected boolean isExtended = false;

	protected QueryArea queryArea = QueryArea.RowDimension;

	/**
	 * 原始查询区域
	 */
	protected QueryArea rawQueryArea = QueryArea.RowDimension;

	protected QueryField parent = null;

	protected List<QueryField> children = new ArrayList<>();

	protected QueryField sourceField = null;

	/**
	 * 原始字段编码
	 */
	protected String rawCode = "";

	protected Map<String, Object> ext = new HashMap<>();

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

	/**
	 * 字段类型
	 */
	private String fieldType;

	/**
	 * 汇总聚合类型， 没设置则为默认
	 */
	private String totalAggType = AnalysisTotalAggType.DEFAULT.getCode();
	/**
	 * 字段路径
	 */
	private List<String> path = new ArrayList<>();

	//小数点位数
	private Integer decimalPlaces;

	public QueryField(){

	}

	public QueryField(MetaField meta){
		this.setMeta(meta);
	}

	public void init(){
		// 设置元数据
		this.meta = SSDMetaCacheManager.getField(this.id);
		if(this.meta == null && BIUtil.isNotEmpty(this.code)){
			// 通过id未找到，则通过code查找
			List<MetaField> metaFields = SSDMetaCacheManager.getFieldByCode(this.code);
			if(BIUtil.isNotEmpty(metaFields)){
				this.meta = metaFields.get(0);
			}
		}
        if(this.meta == null) {
			meta = CustomFieldManager.getMetaField(this,false);
		}

		//后台配置的计算维度
//		buildBackendCustomDim();

		//跨模型字段
		buildCrossModelMeasure();

		FieldUtil.initializeQueryField(this.meta,this);
	}

	/**
	 * 构建后台配置的计算字段
	 */
	public void buildBackendCustomDim() {
		if (FieldUtil.isCalcField(this) && !this.isCustomDimension() && !this.isMeasure()) {
			this.customFieldConfigure = new CustomFieldConfigure();
			customFieldConfigure.setIsMeasure(Enabled.NO.getId());
			customFieldConfigure.setEnable(Enabled.YES.getId());
			customFieldConfigure.setExpression(meta.getAggExpression());
			meta = CustomFieldManager.getMetaField(this, true);
		}
	}

	/**
	 * 构建跨模型字段
	 */
	public void buildCrossModelMeasure() {

		if (meta == null) {
			return;
		}

		if (FieldType.CROSS_MODEL_MEASURE == FieldType.get(meta.getFieldType())) {
			if(this.customFieldConfigure == null){
				this.customFieldConfigure = new CustomFieldConfigure();
			}
			customFieldConfigure.setIsMeasure(Enabled.YES.getId());
			customFieldConfigure.setEnable(Enabled.YES.getId());

			//在ui格式化后，跨模型字段的表达式可能会发生变更，不能赋值meta的表达式
			//场景1 实时数据集，指标id优化后变更
			if(StrUtil.isEmpty(this.customFieldConfigure.getExpression())){
				customFieldConfigure.setExpression(meta.getAggExpression());
			}

			String showFormatExpression = meta.getShowFormatExpression();
			meta = CustomFieldManager.getMetaField(this, true);

			//跨模型字段的格式，从白皮书指标的配置中取
			if (StrUtil.isNotEmpty(showFormatExpression)) {
				meta.setShowFormatExpression(showFormatExpression);
			}

			this.setFieldType(FieldType.CROSS_MODEL_MEASURE.getCode());
		}
	}

	public QueryField clone(){
		QueryField copy = new QueryField();
		try {
			copy = (QueryField) super.clone();
			if(meta != null){
				copy.meta = meta.clone();
				copy.setMeta(copy.meta);
			}
			if(values != null){
				copy.values = new ArrayList<FieldValue>();
				for(FieldValue v : values){
					copy.values.add(v.clone());
				}
			}
			if(calcAtomFields != null) {
				copy.calcAtomFields = new HashSet<>();
				copy.calcAtomFields.addAll(this.calcAtomFields);

			}
			if(customFieldConfigure != null){
				copy.customFieldConfigure = JSON.parseObject(JSON.toJSONString(this.customFieldConfigure), CustomFieldConfigure.class);
			}
			if(categoryAggregation != null){
				copy.categoryAggregation = JSON.parseObject(BIUtil.toJSONString(this.categoryAggregation), FieldCategoryAggregation.class);
			}
			if(analysisConfig != null){
				copy.analysisConfig = JSON.parseObject(BIUtil.toJSONString(this.analysisConfig), AnalysisConfigFactory.get(analysisConfig.getCalcMode()).getClass());
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return copy;
	}

	// 逻辑id=表名+字段名
	public String getLogicId(){
		if(meta == null){
			return "";
		}
		String logicId = "";
		if(!BIUtil.isEmpty(meta.getDimTableName())){
			logicId = meta.getDimTableName();
		}
		if(!BIUtil.isEmpty(meta.getFactTableName())){
			logicId = meta.getFactTableName();
		}
//		logicId = logicId + "." + meta.getName();
		logicId = logicId + "." + meta.getCode();
		return logicId;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		return code.equals(((QueryField)obj).getCode());
	}

	@Override
	public int hashCode() {
		if(code == null) return -1;
		return code.hashCode();
	}

	@Override
	public String toString() {
		return code + "\t" + name + "\t" + title + "\t" + id;
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

	public List<FieldValue> getValues() {
		return values;
	}

	public void setValues(List<FieldValue> values) {
		this.values = values;
	}

	public MetaField getMeta() {
		return meta;
	}

	public void setMeta(MetaField meta) {
		this.meta = meta;
		this.id = meta.getId();
		this.name = meta.getName();
		this.code = meta.getCode();
		this.title = meta.getTitle();
	}

	public List<MetaField> getMutexList() {
		return mutexList;
	}

	public void setMutexList(List<MetaField> mutexList) {
		this.mutexList = mutexList;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		if(meta != null && BIUtil.isEmpty(this.name)){
			return meta.getName();
		}
		return name;
	}

	public String getKeyName(){
		if(meta != null && !BIUtil.isEmpty(meta.getFieldKeyName())){
			return meta.getFieldKeyName();
		}else {
			return name;
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		if(BIUtil.isEmpty(title)){
			if(meta != null){
				title = meta.getTitle();
			}
		}
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", this.getId());
		json.put("name", this.getName());
		json.put("code", this.getCode());
		json.put("title", this.getTitle());
		json.put("filterValueType", filterValueType);
		json.put("filterValueMode", filterValueMode);
		json.put("showOrder", showOrder);
		json.put("sortType", this.sortType.toString());
		json.put("valuesTitle", this.valuesTitle);
		json.put("isMeasure", meta.getIsMeasure());
		JSONArray valueArray = new JSONArray();
		for(FieldValue v : values){
			valueArray.add(v.toJSON());
		}
		json.put("Values", valueArray);
		return json;
	}

	public String getCode() {
		// 分析字段：直接取code，不从元数据中获取
		if(Enabled.value(isAnalysis)) {
			return code;
		}
		if(meta != null  && BIUtil.isEmpty(this.name)){
			return meta.getCode();
		}
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Boolean getIsFilter() {
		return isFilter;
	}

	public void setIsFilter(Boolean isFilter) {
		this.isFilter = isFilter;
	}

	public Boolean getIsResult() {
		return isResult;
	}

	public void setIsResult(Boolean isResult) {
		this.isResult = isResult;
	}

	@Override
	public int compareTo(QueryField o) {
		//Double num = this.showOrder - o.showOrder;
		//return num > 0 ? 1 : -1;
		if (this.showOrder == null || o.showOrder == null) {
			return -1;
		}
		return Double.compare(this.showOrder, o.showOrder);
	}

	public QueryTable getTable() {
		return table;
	}

	public void setTable(QueryTable table) {
		this.table = table;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isCascadeFilter() {
		return cascadeFilter;
	}

	public void setCascadeFilter(boolean cascadeFilter) {
		this.cascadeFilter = cascadeFilter;
	}

	public boolean isHierarchy() {
		return isHierarchy;
	}

	public void setHierarchy(boolean isHierarchy) {
		this.isHierarchy = isHierarchy;
	}

	public String getValuesTitle() {
		return valuesTitle;
	}

	public void setValuesTitle(String valuesTitle) {
		this.valuesTitle = valuesTitle;
	}

	public String getAlias() {
		if(BIUtil.isEmpty(alias)){
			alias = getName();
		}
		if(alias != null) {
			alias = alias.trim();
		}
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public FieldSortType getSortType() {
		return sortType;
	}

	public void setSortType(FieldSortType sortType) {
		this.sortType = sortType;
	}

	/**
	 * 是否是度量
	 * @return
	 */
	public boolean isMeasure(){
		boolean flag = false;
		if(meta == null) return flag;
		/*if(MetaFieldType.getType(meta.getType()) == MetaFieldType.Measure){
			flag = true;
		}*/
		return Enabled.value(meta.getIsMeasure());
	}

	public boolean isDimension(){
		return !isMeasure();
	}

	public boolean isCalc() {
		isCalc = FieldUtil.isCalcField(this);
		return isCalc;
	}

	//是否包含分析项指标的计算指标
	public boolean isAnalysisCalc() {
		isCalc = FieldUtil.isCalcField(this);
		return isCalc && this.getMeta() != null && this.getMeta().getAggExpression().contains(CustomFieldType.ANALYSIS.getIdentifier());
	}

	public boolean isTargetValue() {
		return this.getAnalysisConfig() != null &&
				this.getAnalysisConfig().getTargetConfig() != null &&
				this.getAnalysisConfig().getTargetConfig().isActive();
	}

	// 出分析项的目标值
	public boolean isTargetRawValue() {
		if (!isTargetValue()) {
			return false;
		}

		AnalysisItemConfig analysisItemConfig = this.getAnalysisConfig();
		if (StringUtils.isEmpty(analysisItemConfig.getCalcMode())) {
			ResultDataSetTargetConfig targetConfig = analysisItemConfig.getTargetConfig();
			if (AnalysisCalcMode.TIME_PROGRESS.getCode().equals(targetConfig.getTargetCalcMode()) ||
					AnalysisCalcMode.PREDICT_VALUE.getCode().equals(targetConfig.getTargetCalcMode())) {
				return false;
			} else {
				return true;
			}

		}
		return false;
	}

	public void setCalc(boolean isCalc) {
		this.isCalc = isCalc;
	}

	public boolean isAppend() {
		return isAppend;
	}

	public void setAppend(boolean isAppend) {
		this.isAppend = isAppend;
	}

	public Set<QueryField> getCalcAtomFields() {
		return calcAtomFields;
	}

	public void setCalcAtomFields(Set<QueryField> calcAtomFields) {
		this.calcAtomFields = calcAtomFields;
	}

	public List<QueryField>  getLogicIdSameList() {
		return logicIdSameList;
	}

	public void setLogicIdSameList(List<QueryField> logicIdSameList) {
		this.logicIdSameList = logicIdSameList;
	}

	public String getDisplayTitle() {
		return displayTitle;
	}

	public void setDisplayTitle(String displayTitle) {
		this.displayTitle = displayTitle;
	}

	public Boolean getFilter() {
		return isFilter;
	}

	public void setFilter(Boolean filter) {
		isFilter = filter;
	}

	public CustomFieldConfigure getCustomFieldConfigure() {
		return customFieldConfigure;
	}

	public void setCustomFieldConfigure(CustomFieldConfigure customFieldConfigure) {
		this.customFieldConfigure = customFieldConfigure;
	}

	/**
	 * 是否是分类聚合字段
	 * 1、字段必须为日期类型或是筛选条件为日期类型
	 * 2、启用
	 * @return
	 */
	public boolean isCategoryAggregation(){
		// 功能已废弃
		return false;
		/*
		if(meta == null) {
			return false;
		}
		if(this.isMeasure()) {
			return false;
		}
		if(Enabled.value(meta.getIsSensitive())){ // 敏感字段
			return false;
		}
		FieldFilterType fieldFilterType = FieldFilterType.get(meta.getFilterShowType());
		FieldDataType dataType = FieldDataType.getType(meta.getDataType());
		if(!(dataType == FieldDataType.Date || dataType == FieldDataType.Datetime || FieldFilterType.isDateRange(fieldFilterType))) {
			return false;
		}
		if(this.categoryAggregation == null || !Enabled.value(this.categoryAggregation.getEnable())) {
			return false;
		}
		if(BIUtil.isEmpty(this.categoryAggregation.getCategories())) {
			return false;
		}
		return true;
		 */
	}

	public FieldCategoryAggregation getCategoryAggregation() {
		return categoryAggregation;
	}

	public void setCategoryAggregation(FieldCategoryAggregation categoryAggregation) {
		this.categoryAggregation = categoryAggregation;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	/**
	 * 是否是自定义计算字段
	 * @return
	 */
	public boolean isCustom(){
		return this.customFieldConfigure != null && !this.customFieldConfigure.isEmpty();
	}

	public Boolean getResult() {
		return isResult;
	}

	public void setResult(Boolean result) {
		isResult = result;
	}

	public boolean isCustomMeasure(){
		return this.isMeasure() && this.isCustom();
	}

	public boolean isCustomDimension(){

		return !this.isMeasure() && this.isCustom();
	}

	public boolean isExtended() {
		if(meta != null) {
			isExtended = BIUtil.isNotEmpty(meta.getExtendSrcId());
		}
		return isExtended;
	}

	public void setExtended(boolean extended) {
		isExtended = extended;
	}

	public String getFilterQueryRule() {
		return filterQueryRule;
	}

	public void setFilterQueryRule(String filterQueryRule) {
		this.filterQueryRule = filterQueryRule;
	}

	public QueryArea getQueryArea() {
		return queryArea;
	}

	public void setQueryArea(QueryArea queryArea) {
		this.queryArea = queryArea;
	}

	public QueryField getParent() {
		return parent;
	}

	public void setParent(QueryField parent) {
		this.parent = parent;
	}

	public List<QueryField> getChildren() {
		return children;
	}

	public void setChildren(List<QueryField> children) {
		this.children = children;
	}

	public QueryField getSourceField() {
		return sourceField;
	}

	public void setSourceField(QueryField sourceField) {
		this.sourceField = sourceField;
	}

	public String getRawCode() {
		/**
		if(Enabled.value(isAnalysis)) {
			return analysisConfig.getMeasureCode();
		}
		 */
		if(BIUtil.isEmpty(rawCode)){
			return code;
		}
		return rawCode;
	}

	public void setRawCode(String rawCode) {
		this.rawCode = rawCode;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

	public void setExt(Map<String, Object> ext) {
		this.ext = ext;
	}

	/**
	 * 字段是否本身就是后台计算字段
	 */
	public Boolean isMetaComputed(){
		return this.getMeta() != null && BIUtil.isNotEmpty(this.getMeta().getAggExpression()) && this.getMeta().getAggExpression().indexOf("[") != -1;
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

	public QueryArea getRawQueryArea() {
		return rawQueryArea;
	}

	public void setRawQueryArea(QueryArea rawQueryArea) {
		this.rawQueryArea = rawQueryArea;
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

	/**
	 * 是否是公共日期
	 * @return
	 */
	public boolean isCommonDate(){
		if(meta != null && Enabled.isTrue(meta.getIsCommonDate())){
			return true;
		}
		return false;
	}

	/**
	 * 是否是汇总查询
	 * @return
	 */
	public boolean isAggQuery() {
		/*
		if (isCommonDate() && Enabled.value(isAggQuery)) {
			return true;
		}
		return false;
		 */
		return Enabled.isTrue(isAggQuery);
	}

	public String getModuleCtgId() {
		return moduleCtgId;
	}

	public void setModuleCtgId(String moduleCtgId) {
		this.moduleCtgId = moduleCtgId;
	}

	public String getAggExpressionType() {
		return aggExpressionType;
	}

	public void setAggExpressionType(String aggExpressionType) {
		this.aggExpressionType = aggExpressionType;
	}

	public Integer getIsShow() {
		return isShow;
	}

	public void setIsShow(Integer isShow) {
		this.isShow = isShow;
	}

	/**
	 * 是否是lod字段
	 */
	public boolean isLodField(){
		if(this.customFieldConfigure != null && CustomFieldType.isLod(this.customFieldConfigure.getType())){
			return true;
		}
		return false;
	}

	/**
	 * 是不是百分比字段
	 * @return
	 */
	public boolean isPercentField() {

		if (meta == null) {
			return false;
		}

		String formatString = meta.getShowFormatExpression();
		if (BIUtil.isEmpty(formatString)) {
			return false;
		}

		if (formatString.contains("%")) {
			return true;
		}

		return false;
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

	public String getTotalAggType() {
		return totalAggType;
	}

	public void setTotalAggType(String totalAggType) {
		this.totalAggType = totalAggType;
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

	public Integer getDecimalPlaces() {
		return decimalPlaces;
	}

	public void setDecimalPlaces(Integer decimalPlaces) {
		this.decimalPlaces = decimalPlaces;
	}

	public Set<QueryField> getCusCalcDependFields() {
		return cusCalcDependFields;
	}

	public void setCusCalcDependFields(Set<QueryField> cusCalcDependFields) {
		this.cusCalcDependFields = cusCalcDependFields;
	}

	public String getQueryCalendarType() {
		return queryCalendarType;
	}

	public void setQueryCalendarType(String queryCalendarType) {
		this.queryCalendarType = queryCalendarType;
	}
}

