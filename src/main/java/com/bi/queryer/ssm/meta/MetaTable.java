package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;
import com.bi.queryer.ssm.mgr.exportImport.SSDExcel;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 事实表&维表元数据
 * @author contributor
 *
 */
public class MetaTable implements JSONSerializable, Cloneable {

	@SSDExcel(order = 1)
	private String id;

	/**
	 * 表数据库名
	 */
	@SSDExcel(order = 2)
	private String tableSchema;

	/**
	 * 表名
	 */
	@SSDExcel(order = 3)
	private String name;



	/**
	 * 是否是事实表
	 */
	@SSDExcel(order = 4,trueOrFalse = true)
	private Integer isFactTable;

	/**
	 * 粒度值
	 */
	@SSDExcel(order = 5)
	private Integer granularity;

	/**
	 * 原始表名
	 */
	@SSDExcel(order = 6)
	private String origTableName;

	/**
	 * 表描述
	 */
	@SSDExcel(order = 7)
	private String tableDesc;

	/**
	 * 表owner
	 */
	@SSDExcel(order = 9)
	private String tableOwner;

	/**
	 * 是否是指标库表
	 */
	@SSDExcel(order = 10,trueOrFalse = true)
	private Integer isKpiTable = 0;

	 //暂时保留，保证其他类不报错
	private Integer type;

	private String moduleCode;

	private String moduleName;

	@SSDExcel(order = 8,trueOrFalse = true)
	private Integer isActive;

	/**
	 *
	 * 支持的查询引擎：trino/doris，多个用逗号英文分割，默认trino
	 */
	@SSDExcel(order = 11)
	private String supportQueryEngines = "";

	/**
	 * etlJobs
	 */
	private List<String> etlJobs = new ArrayList<>();

	private String createdTime;
	private String createdBy;
	private String updatedTime;
	private String updatedBy;

	//字段
	private List<MetaField> fields = new ArrayList<MetaField>();;

	/**
	 * 时间粒度
	 */
	private String dateGranularity;

	/**
	 * 可用的时间粒度
	 */
	private List<String> availableDateGranularityList = new ArrayList<>();

	/**
	 * 原始全名：此属性在热化加速时会被修改
	 */
	private String rawFullName;

	private String dataEnv = DataEnv.OLD_SSM.getCode();
	// 数据类型
	private String dataProcessType;
	// 数据更新时间字段
	private String dataUpdateTimeField;
	//数据切片配置
	private String dataSliceCfg;

	//数据切片配置
	private TableDataSliceCfg dataSliceCfgObj;

	//视图依赖表
	private String viewDependTables;

	public String getDataEnv() {
		return dataEnv;
	}

	public void setDataEnv(String dataEnv) {
		this.dataEnv = dataEnv;
	}

	public List<MetaField> getFields() {
		return fields;
	}

	public void setFields(List<MetaField> fields) {
		this.fields = fields;
	}

	public String getDateGranularity() {
		return dateGranularity;
	}

	public void setDateGranularity(String dateGranularity) {
		this.dateGranularity = dateGranularity;
	}

	public String getModuleCode() {
		return moduleCode;
	}

	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getType() {
		return Enabled.value(isFactTable) ? MetaTableType.Fact.getId() : MetaTableType.Dimension.getId();
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFullName(){
		if(BIUtil.isEmpty(this.tableSchema)){
			return name;
		}
		if(!BIUtil.isEmpty(name) && name.indexOf(".") == -1){
			return this.tableSchema + "." + name;
		}
		return name;
	}

	public String getFullName(boolean isRawFullName){
		if(isRawFullName){
			if(BIUtil.isEmpty(this.rawFullName)){
				return this.getFullName();
			}else {
				return this.rawFullName;
			}
		}else {
			return this.getFullName();
		}
	}

	public String getTableDesc() {
		return tableDesc;
	}

	public void setTableDesc(String tableDesc) {
		this.tableDesc = tableDesc;
	}

	public Integer getIsFactTable() {
		return isFactTable;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MetaTable metaTable = (MetaTable) o;
		return id.equals(metaTable.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public void setIsFactTable(Integer isFactTable) {
		this.isFactTable = isFactTable;
	}

	public Integer getGranularity() {
		return granularity;
	}

	public void setGranularity(Integer granularity) {
		this.granularity = granularity;
	}

	public String getOrigTableName() {
		if(BIUtil.isNotEmpty(origTableName)){
			origTableName = origTableName.trim();
		}
		return origTableName;
	}

	public void setOrigTableName(String origTableName) {
		this.origTableName = origTableName;
	}

	public Integer getIsActive() {
		if(isActive == null){
			isActive = Enabled.NO.getId();
		}
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
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

	public String getTableOwner() {
		if(BIUtil.isEmpty(tableOwner)){
			tableOwner = BIUtil.isEmpty(updatedBy) ? createdBy : updatedBy;
		}
		return tableOwner;
	}

	public void setTableOwner(String tableOwner) {
		this.tableOwner = tableOwner;
	}

	public List<String> getEtlJobs() {
		return etlJobs;
	}

	public void setEtlJobs(List<String> etlJobs) {
		this.etlJobs = etlJobs;
	}

	public Integer getIsKpiTable() {
		return isKpiTable;
	}

	public void setIsKpiTable(Integer isKpiTable) {
		this.isKpiTable = isKpiTable;
	}

	public String getDataProcessType() {
		return dataProcessType;
	}

	public void setDataProcessType(String dataProcessType) {
		this.dataProcessType = dataProcessType;
	}

	public String getDataUpdateTimeField() {
		return dataUpdateTimeField;
	}

	public void setDataUpdateTimeField(String dataUpdateTimeField) {
		this.dataUpdateTimeField = dataUpdateTimeField;
	}

	public String getDataSliceCfg() {
		return dataSliceCfg;
	}

	public void setDataSliceCfg(String dataSliceCfg) {
		this.dataSliceCfg = dataSliceCfg;
	}

	public String getViewDependTables() {
		return viewDependTables;
	}

	public void setViewDependTables(String viewDependTables) {
		this.viewDependTables = viewDependTables;
	}

	public TableDataSliceCfg getDataSliceCfgObj() {
		return dataSliceCfgObj;
	}

	public void setDataSliceCfgObj(TableDataSliceCfg dataSliceCfgObj) {
		this.dataSliceCfgObj = dataSliceCfgObj;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}

	public MetaTable clone() {
		MetaTable clone = new MetaTable();
		try {
			clone = (MetaTable) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return clone;
	}

	public String getSupportQueryEngines() {
		if(BIUtil.isEmpty(supportQueryEngines)){
			supportQueryEngines = DBType.Trino.toString().toLowerCase();
		}
		return supportQueryEngines;
	}

	public void setSupportQueryEngines(String supportQueryEngines) {
		this.supportQueryEngines = supportQueryEngines;
	}

	public String getRawFullName() {
		return rawFullName;
	}

	public void setRawFullName(String rawFullName) {
		this.rawFullName = rawFullName;
	}

	public List<String> getAvailableDateGranularityList() {
		return availableDateGranularityList;
	}

	public void setAvailableDateGranularityList(List<String> availableDateGranularityList) {
		this.availableDateGranularityList = availableDateGranularityList;
	}

	@Override
	public String toString() {
		return "MetaTable{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
