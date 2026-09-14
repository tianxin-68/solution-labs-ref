package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.mgr.exportImport.SSDExcel;
import com.bi.queryer.sys.enums.DateType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

/**
 * 字段元数据
 * @author contributor
 *
 */
public class MetaField  implements Cloneable, Comparable<MetaField>, JSONSerializable {

	@SSDExcel(order = 1)
	// 标示
	private String id;

	@SSDExcel(order = 2)
	// 编码
	private String code;

	@SSDExcel(order = 3)
	// 名称
	private String name;

	@SSDExcel(order = 4)
	// 标题
	private String title;

	@SSDExcel(order = 5)
	// 数据类型
	private String dataType;

	@SSDExcel(order = 6)
	// 权重：用于字段关联
	private Integer weight ;

	@SSDExcel(order = 7,trueOrFalse = true)
	private Integer isMeasure = Enabled.YES.getId();

	@SSDExcel(order = 8)
	// 聚合表达式
	private String aggExpression = "";

	@SSDExcel(order = 9,trueOrFalse = true)
	// 是否在前台可显示
	private Integer isShow = Enabled.YES.getId();

	@SSDExcel(order = 10)
	// 前台格式化表达式
	private String showFormatExpression = "";

	@SSDExcel(order = 11,trueOrFalse = true)
	// 结果字段
	private Integer isResult = Enabled.YES.getId();

	@SSDExcel(order = 12,trueOrFalse = true)
	// 是否是过滤条件
	private Integer isFilter = Enabled.NO.getId();

	@SSDExcel(order = 13)
	// 过滤器显示类型
	private String filterShowType = "";

	@SSDExcel(order = 14)
	private String filterTableId;

	@SSDExcel(order = 15)
	private String filterTableName;

	@SSDExcel(order = 16)
	// 筛选值方式
	private String filterValueMode;

	@SSDExcel(order = 17)
	private String filterSQL;

	@SSDExcel(order = 18)
	// 显示顺序
	private Double showOrder;

	@SSDExcel(order = 19,trueOrFalse = true)
	// 敏感字段
	private Integer isSensitive = Enabled.NO.getId();

	@SSDExcel(order = 20)
	// 导出时的附加信息
	private String sensitiveDataType;

	private String fieldKeyName;
	private String fieldKeyType;

	@SSDExcel(order = 21)
	//字段自动扩展类型列表
	private String fieldExtendList;
	//key字段id
	@SSDExcel(order = 22)
	private String fieldKeyId;



	@SSDExcel(order = 23)
	// 所属类别id
	private String categoryId;

	//所属类别集合
	private List<String> categoryIdList = new ArrayList<>();

	// 所属类别名称
	private String categoryName;

	// 所属类别显示顺序
	private Integer categoryShowOrder;

	@SSDExcel(order = 24)
	// 虚拟目录id
	private String virtualCategoryId;

	//虚拟目录名称
	private String virtualCategoryName;



	@SSDExcel(order = 25,trueOrFalse = true)
	private Integer isActive;



	@SSDExcel(order = 26)
	private String remark;

	// 类型
	// private Integer type = MetaFieldType.Dimension.getId();




	@SSDExcel(order = 27)
	// 事实表id
	private String factTableId;

	@SSDExcel(order = 28)
	// 事实表名称
	private String factTableName;

	@SSDExcel(order = 29)
	// 维表id
	private String dimTableId ;

	@SSDExcel(order = 30)
	// 维表名称
	private String dimTableName;

	// 导出时的附加信息
	private String exportAppendString;

	// 可否作为列维度
	@SSDExcel(order = 31, trueOrFalse = true)
	private Integer canColDim = 0;

	// 是否是公共日期
	@SSDExcel(order = 32, trueOrFalse = true)
	private Integer isCommonDate = Enabled.NO.getId();

	// 日期粒度
	@SSDExcel(order = 33)
	private String dateGranularity = "";

	// 白皮书指标
	private String kpiName;

	// 白皮书编码
	@SSDExcel(order = 34)
	private String kpiNo;

	@JsonIgnore
	private List<MetaField> sameCodeFieldList = new ArrayList<>();



	public String getFilterValueMode() {
		return filterValueMode;
	}

	public void setFilterValueMode(String filterValueMode) {
		this.filterValueMode = filterValueMode;
	}


	private Integer dimeOrMetric = 0;

	private String categoryPath = "";

	private String categoryNamePath = "";

	/**
	 * 扩展字段id列表
	 */
	@JsonIgnore
	private List<MetaField> extendFields = new ArrayList<MetaField>();

	/**
	 * 扩展字段源id
	 */
	private String extendSrcId = "";


	private String createdTime;
	private String createdBy;
	private String updatedTime;
	private String updatedBy;

	/**
	 * 过滤器提示文字
	 */
	private String filterTips;

	/**
	 * 大小写敏感
	 */
	protected Integer isCaseSensitive = Enabled.NO.getId();

	/**
	 * 自定义字段配置
	 */
	protected CustomFieldConfigure customFieldConfigure = new CustomFieldConfigure();

	public CustomFieldConfigure getCustomFieldConfigure() {
		return customFieldConfigure;
	}

	/**
	 * 数据权限
	 */
	protected String dataAuthModuleCode = "";
	protected String dataAuthDimCode = "";
	protected String dataAuthMode = "";
	protected Integer dataAuthIsApply;

	protected String dataAuthWhitePaperCode = "";

	/**
	 * 原始聚合表达式：用于存储嵌套表达式转换之前的原始表达式
	 */
	@JsonIgnore
	protected String rawAggExpression = "";

	/**
	 * 字段为计算字段时，存储计算表达式关联的原子字段编码
	 */
	@JsonIgnore
	private Set<MetaField> calcAtomFields = new HashSet<MetaField>();

	/**
	 * 数据环境
	 */
	@JsonIgnore
	private String dataEnv = DataEnv.OLD_SSM.getCode();

	//模块目录id
	private String moduleCtgId;

	//模块数据类型
	private String ctgDataType;

	/**
	 * 日期聚合类型 all,begin,end
	 */
	private String dateAggType;

	/**
	 * 是否按日期不聚合
	 */
	private Integer isDateNonAgg = Enabled.NO.getId();

	/**
	 * 可用的时间粒度
	 */
	private List<String> availableDateGranularityList = new ArrayList<>();

	/**
	 * 枚举映射值
	 */
	private List<Map<String, String>> dimValueMap = new ArrayList<>();

	/**
	 * 字段类型
	 */
	private String fieldType;

	/**
	 * 计算字段表达式
	 */
	private String calcCodeExpression;

	/**
	 * 敏感级别c1~c4
	 */
	private String sensitiveLevel = "c1";

	public String getDataEnv() {
		return dataEnv;
	}

	public void setDataEnv(String dataEnv) {
		this.dataEnv = dataEnv;
	}

	public void setCustomFieldConfigure(CustomFieldConfigure customFieldConfigure) {
		this.customFieldConfigure = customFieldConfigure;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public Integer getIsCaseSensitive() {
		return isCaseSensitive;
	}

	public void setIsCaseSensitive(Integer isCaseSensitive) {
		this.isCaseSensitive = isCaseSensitive;
	}


	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	private String belongTableName;

	public String getBelongTableName() {
		return belongTableName;
	}

	public void setBelongTableName(String belongTableName) {
		this.belongTableName = belongTableName;
	}

	@Override
	public String toString() {
		return id + "\t " + name + "\t" + title + "\t" + fieldKeyName + "\t" + dimTableName + "\t" + factTableName;
	}

	public void addExtend(MetaField ext) {
		if(!extendFields.contains(ext)) {
			extendFields.add(ext);
		}
		ext.setExtendSrcId(this.id);
	}

	public MetaField clone(){
		MetaField copy = new MetaField();
		try {
			copy = (MetaField) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return copy;
	}

	public String getDataType() {
		return dataType;
	}

	public DateType getDateType(){
		DateType dateType = null;
		switch (FieldFilterType.get(this.getFilterShowType())){
			case YearRange:
				dateType = DateType.Year;
				break;
			case MonthRange:
				dateType = DateType.Month;
				break;
			case DateRange:
				dateType = DateType.Day;
				break;
		}

		return dateType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
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

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public Integer getIsShow() {
		return isShow;
	}

	public void setIsShow(Integer isShow) {
		this.isShow = isShow;
	}

	public Integer getIsFilter() {
		return isFilter;
	}

	public void setIsFilter(Integer isFilter) {
		this.isFilter = isFilter;
	}

	public String getFilterShowType() {
		if(Enabled.value(isFilter) && BIUtil.isEmpty(filterShowType)) {
			filterShowType = FieldFilterType.Textarea.getCode();
		}
		return filterShowType;
	}

	public void setFilterShowType(String filterShowType) {
		this.filterShowType = filterShowType;
	}

	public Integer getIsResult() {
		return isResult;
	}

	public void setIsResult(Integer isResult) {
		this.isResult = isResult;
	}

	public Integer getIsSensitive() {
		return isSensitive;
	}

	public void setIsSensitive(Integer isSensitive) {
		this.isSensitive = isSensitive;
	}

	public String getAggExpression() {
		return aggExpression;
	}

	public void setAggExpression(String aggExpression) {
		this.aggExpression = aggExpression;
	}

	public String getShowFormatExpression() {
		if(showFormatExpression == null){
			showFormatExpression = "";
		}
		return showFormatExpression;
	}

	public void setShowFormatExpression(String showFormatExpression) {
		this.showFormatExpression = showFormatExpression;
	}

	public String getDimTableId() {
		return dimTableId;
	}

	public void setDimTableId(String dimTableId) {
		this.dimTableId = dimTableId;
	}

	public String getDimTableName() {
		return dimTableName;
	}

	public void setDimTableName(String dimTableName) {
		this.dimTableName = dimTableName;
	}

	public String getFactTableId() {
		return factTableId;
	}

	public void setFactTableId(String factTableId) {
		this.factTableId = factTableId;
	}

	public String getFactTableName() {
		return factTableName;
	}

	public void setFactTableName(String factTableName) {
		this.factTableName = factTableName;
	}

	public Integer getWeight() {
		return weight == null ? 1 : weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public Integer getCategoryShowOrder() {
		return categoryShowOrder;
	}

	public void setCategoryShowOrder(Integer categoryShowOrder) {
		this.categoryShowOrder = categoryShowOrder;
	}

	public Double getShowOrder() {
		if(showOrder == null) {
			showOrder = (double)-1;
		}
		return showOrder;
	}

	public void setShowOrder(Double showOrder) {
		this.showOrder = showOrder;
	}

	public String getFieldKeyName() {
		return fieldKeyName;
	}

	public void setFieldKeyName(String fieldKeyName) {
		this.fieldKeyName = fieldKeyName;
	}

	public String getFilterTableName() {
		return filterTableName;
	}

	public void setFilterTableName(String filterTableName) {
		this.filterTableName = filterTableName;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Integer getDimeOrMetric() {
		return dimeOrMetric;
	}

	public void setDimeOrMetric(Integer dimeOrMetric) {
		this.dimeOrMetric = dimeOrMetric;
	}

	@Override
	public int compareTo(MetaField o) {
		return this.weight - o.getWeight();
	}

	public String getFilterTableId() {
		return filterTableId;
	}

	public void setFilterTableId(String filterTableId) {
		this.filterTableId = filterTableId;
	}

	public String getCategoryPath() {
		return categoryPath;
	}

	public void setCategoryPath(String categoryPath) {
		this.categoryPath = categoryPath;
	}

	public String getVirtualCategoryId() {
		return virtualCategoryId;
	}

	public void setVirtualCategoryId(String virtualCategoryId) {
		this.virtualCategoryId = virtualCategoryId;
	}

	public String getFilterSQL() {
		if(filterSQL == null) {
			return "";
		}
		filterSQL = filterSQL.trim();
		return filterSQL;
	}

	public void setFilterSQL(String filterSQL) {
		this.filterSQL = filterSQL;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getExtendSrcId() {
		return extendSrcId;
	}

	public void setExtendSrcId(String extendSrcId) {
		this.extendSrcId = extendSrcId;
	}

	@JsonBackReference(value = "metaFieldExtendFields")
	public List<MetaField> getExtendFields() {
		return extendFields;
	}

	public void setExtendFields(List<MetaField> extendFields) {
		this.extendFields = extendFields;
	}

	public String getFieldKeyType() {
		return fieldKeyType;
	}

	public void setFieldKeyType(String fieldKeyType) {
		this.fieldKeyType = fieldKeyType;
	}

	public Integer getIsMeasure() {
		return isMeasure;
	}

	public void setIsMeasure(Integer isMeasure) {
		this.isMeasure = isMeasure;
	}

	public String getFieldKeyId() {
		return fieldKeyId;
	}

	public void setFieldKeyId(String fieldKeyId) {
		this.fieldKeyId = fieldKeyId;
	}

	public String getVirtualCategoryName() {
		return virtualCategoryName;
	}

	public void setVirtualCategoryName(String virtualCategoryName) {
		this.virtualCategoryName = virtualCategoryName;
	}

	public String getFilterTips() {
		return filterTips;
	}

	public void setFilterTips(String filterTips) {
		this.filterTips = filterTips;
	}

	public String getFieldExtendList() {
		return fieldExtendList;
	}

	public void setFieldExtendList(String fieldExtendList) {
		this.fieldExtendList = fieldExtendList;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}

	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MetaField metaField = (MetaField) o;
		return id.equals(metaField.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public String getExportAppendString() {
		return exportAppendString;
	}

	public void setExportAppendString(String exportAppendString) {
		this.exportAppendString = exportAppendString;
	}

	public String getSensitiveDataType() {
		return sensitiveDataType;
	}

	public void setSensitiveDataType(String sensitiveDataType) {
		this.sensitiveDataType = sensitiveDataType;
	}

	public String getCategoryNamePath() {
		return categoryNamePath;
	}

	public void setCategoryNamePath(String categoryNamePath) {
		this.categoryNamePath = categoryNamePath;
	}

	public String getDataAuthModuleCode() {
		return dataAuthModuleCode;
	}

	public void setDataAuthModuleCode(String dataAuthModuleCode) {
		this.dataAuthModuleCode = dataAuthModuleCode;
	}

	public String getDataAuthDimCode() {
		return dataAuthDimCode;
	}

	public void setDataAuthDimCode(String dataAuthDimCode) {
		this.dataAuthDimCode = dataAuthDimCode;
	}

	public String getDataAuthMode() {
		return dataAuthMode;
	}

	public void setDataAuthMode(String dataAuthMode) {
		this.dataAuthMode = dataAuthMode;
	}

	public Integer getDataAuthIsApply() {
		return dataAuthIsApply;
	}

	public void setDataAuthIsApply(Integer dataAuthIsApply) {
		this.dataAuthIsApply = dataAuthIsApply;
	}

	public String getKpiName() {
		return kpiName;
	}

	public void setKpiName(String kpiName) {
		this.kpiName = kpiName;
	}

	public Integer getCanColDim() {
		return canColDim;
	}

	public void setCanColDim(Integer canColDim) {
		this.canColDim = canColDim;
	}

	public String getTableId(){
		return BIUtil.isEmpty(this.dimTableId) ? this.factTableId : this.dimTableId;
	}

	public Integer getIsCommonDate() {
		return isCommonDate;
	}

	public void setIsCommonDate(Integer isCommonDate) {
		this.isCommonDate = isCommonDate;
	}

	public String getDateGranularity() {
		return dateGranularity;
	}

	public void setDateGranularity(String dateGranularity) {
		this.dateGranularity = dateGranularity;
	}

	@JsonBackReference(value = "metaFieldSameCodeFields")
	public List<MetaField> getSameCodeFieldList() {
		return sameCodeFieldList;
	}

	public void setSameCodeFieldList(List<MetaField> sameCodeFieldList) {
		this.sameCodeFieldList = sameCodeFieldList;
	}

	public String getKpiNo() {
		return kpiNo;
	}

	public void setKpiNo(String kpiNo) {
		this.kpiNo = kpiNo;
	}

	public String getRawAggExpression() {
		return rawAggExpression;
	}

	public void setRawAggExpression(String rawAggExpression) {
		this.rawAggExpression = rawAggExpression;
	}

	public List<String> getCategoryIdList() {
		return categoryIdList;
	}

	public void setCategoryIdList(List<String> categoryIdList) {
		this.categoryIdList = categoryIdList;
	}

	@JsonBackReference(value = "metaFieldCalcAtomFields")
	public Set<MetaField> getCalcAtomFields() {
		return calcAtomFields;
	}

	public void setCalcAtomFields(Set<MetaField> calcAtomFields) {
		this.calcAtomFields = calcAtomFields;
	}

	public void addCalcAtomField(MetaField atomField){
		if(!this.calcAtomFields.contains(atomField)){
			this.calcAtomFields.add(atomField);
		}
	}

	public String getDataAuthWhitePaperCode() {
		return dataAuthWhitePaperCode;
	}

	public void setDataAuthWhitePaperCode(String dataAuthWhitePaperCode) {
		this.dataAuthWhitePaperCode = dataAuthWhitePaperCode;
	}

	public String getModuleCtgId() {
		return moduleCtgId;
	}

	public void setModuleCtgId(String moduleCtgId) {
		this.moduleCtgId = moduleCtgId;
	}

	public String getCtgDataType() {
		return ctgDataType;
	}

	public void setCtgDataType(String ctgDataType) {
		this.ctgDataType = ctgDataType;
	}

	public String getDateAggType() {
		return dateAggType;
	}

	public void setDateAggType(String dateAggType) {
		this.dateAggType = dateAggType;
	}

	public Integer getIsDateNonAgg() {
		return isDateNonAgg;
	}

	public void setIsDateNonAgg(Integer isDateNonAgg) {
		this.isDateNonAgg = isDateNonAgg;
	}

	public List<String> getAvailableDateGranularityList() {
		return availableDateGranularityList;
	}

	public void setAvailableDateGranularityList(List<String> availableDateGranularityList) {
		this.availableDateGranularityList = availableDateGranularityList;
	}

	public List<Map<String, String>> getDimValueMap() {
		return dimValueMap;
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public String getCalcCodeExpression() {
		return calcCodeExpression;
	}

	public void setCalcCodeExpression(String calcCodeExpression) {
		this.calcCodeExpression = calcCodeExpression;
	}

	public String getSensitiveLevel() {
		return sensitiveLevel;
	}

	public void setSensitiveLevel(String sensitiveLevel) {
		this.sensitiveLevel = sensitiveLevel;
	}
}
