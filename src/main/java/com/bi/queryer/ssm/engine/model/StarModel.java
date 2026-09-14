package com.bi.queryer.ssm.engine.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 星型模型
 * @author contributor
 *
 */
public class StarModel implements Comparable<StarModel>{

	/**
	 * 模型别名前缀
	 */
	protected static final String modelAliasPrefix = "M";

	/**
	 * 事实表，可以为空
	 */
	private QueryTable factTable = null;
	
	/** 
	 * 维度表
	 */
	private List<QueryTable> dimTables = new ArrayList<QueryTable>();
	
	/**
	 * 事实表子查询视图SQL
	 */
	private String factSubQuery = null;
	
	/**
	 * 子查询别名
	 */
	private String factSubQueryAlias = "";
	
	/**
	 * 有直接关联的model，且按权重大小排序
	 */
	private List<StarModel> joinModels = new ArrayList<StarModel>();
	
	/**
	 * 是否是join模型：true：此模型只参与join，不参与聚合
	 */
	private Boolean onlyJoinModel = false; 
	
	/**
	 * 权重
	 */
	private Double weight = (double)-1;
	
	private String modelId = "-1";
	
	private Integer granularity = -1;

	private String alias = "";

	/**
	 * 星型模型在整体星型模型列表中的索引号：用于获取别名
	 */
	private Integer index = 0;

	/**
	 * 数据更新时间
	 */
	private String dataUpdateTime = "";

	public StarModel(){
		
	}
	
	public StarModel(QueryTable factTable){
		this.factTable = factTable;
		if(factTable != null){
			this.modelId = factTable.getId();
			if(factTable.getMeta() == null){
				throw new BIException("表[" + factTable + "]元数据未找到");
			}
			this.granularity = factTable.getMeta().getGranularity();
		}
	}
	
	@Override
	public String toString() {
		String str = "id:" + modelId + "\t weight:" + weight;
		if(factTable != null){
			str = str + "\t" + factTable.toString();
		}
		return str;
	}
	
	public Boolean isSubQueryModel(){
		return !BIUtil.isEmpty(factSubQuery);
	}
	
	/**
	 * 添加关联model
	 * @param model
	 */
	public void addJoinModel(StarModel model){
		if(joinModels.contains(model)){
			return;
		}
		joinModels.add(model);
	}
	
	/**
	 * 删除join model
	 * @param model
	 */
	public void removeJoinModel(StarModel model){
		joinModels.remove(model);
	}
	
	public void addDimTable(QueryTable dimTable){
		if(dimTables.contains(dimTable)){
			return;
		}
		dimTables.add(dimTable);
		dimTable.setModel(this);
	}
	
	/**
	 * 删除维度表
	 * @param dimTable
	 */
	public void removeDimTable(QueryTable dimTable){
		dimTables.remove(dimTable);
	}

	public QueryTable getFactTable() {
		return factTable;
	}

	public void setFactTable(QueryTable factTable) {
		this.factTable = factTable;
		this.factTable.setModel(this);
	}

	public List<QueryTable> getDimTables() {
		return dimTables;
	}

	public void setDimTables(List<QueryTable> dimTables) {
		this.dimTables = dimTables;
		if(BIUtil.isNotEmpty(this.dimTables)) {
			this.dimTables.forEach(t->t.setModel(this));
		}
	}

	public Boolean isOnlyJoinModel() {
		return onlyJoinModel;
	}

	public void setOnlyJoinModel(Boolean onlyJoinModel) {
		this.onlyJoinModel = onlyJoinModel;
	}

	public List<StarModel> getJoinModels() {
		return joinModels;
	}

	public void setJoinModels(List<StarModel> joinModels) {
		this.joinModels = joinModels;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(StarModel o) {
		Double num = (weight - o.getWeight()) * -1;
		return num > 0 ? 1 : -1;
	}
	
	public boolean equals(Object obj) {
		if(obj == null || (factTable == null)){
			return false;
		}
		return modelId.equals(((StarModel)obj).modelId);
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public Integer getGranularity() {
		return granularity;
	}

	public void setGranularity(Integer granularity) {
		this.granularity = granularity;
	}

	public String getFactSubQuery() {
		return factSubQuery;
	}

	public void setFactSubQuery(String factSubQuery) {
		this.factSubQuery = factSubQuery;
	}

	public String getFactSubQueryAlias() {
		return factSubQueryAlias;
	}

	public void setFactSubQueryAlias(String factSubQueryAlias) {
		this.factSubQueryAlias = factSubQueryAlias;
	}

	public List<QueryField> getFields() {
		List<QueryField> fields = new ArrayList<>();
		if (factTable != null) {
			fields.addAll(factTable.getFields());
		}
		if (BIUtil.isNotEmpty(dimTables)) {
			for (QueryTable d : dimTables) {
				fields.addAll(d.getFields());
			}
		}

		//去重, 取权重大的， 解决拉链维表和普通维表有相同的维度，优先级不一样的问题
		List<QueryField> distinctFields = new ArrayList<>(fields.size());
		for (QueryField f : fields) {

			//viewId=4e4ac093797847299d9ed0e89138fc26 下载报错，原因是此处code存在空数据
			//添加兼容逻辑
			if(StrUtil.isEmpty(f.getCode())){
				continue;
			}
			int idx = distinctFields.indexOf(f);
			if (idx == -1) {
				distinctFields.add(f);
			} else {
				QueryField old = distinctFields.get(idx);
				if (old.getMeta() != null && f.getMeta() != null && old.getMeta().getWeight() < f.getMeta().getWeight()) {
					distinctFields.set(idx, f);
				}
			}
		}
		// fields = distinctFields.stream().distinct().collect(Collectors.toList());
		return distinctFields;
	}

	public List<QueryTable> getTables(){
		List<QueryTable> tables = new ArrayList<>();
		if(factTable != null) {
			tables.add(factTable);
		}
		tables.addAll(dimTables);
		return tables;
	}

	public boolean isExistsByMetaCode(QueryField queryField) {
		boolean isExist = false;
		List<QueryTable>  tables = this.getTables();

		Set<QueryField> fields = new HashSet<>();
		if(queryField.isCustom()&& CollUtil.isNotEmpty(queryField.getCalcAtomFields())) {
			// 若是自定义字段，则找到其原子字段
			fields = queryField.getCalcAtomFields();
		}else {
			fields.add(queryField);
		}
		if(BIUtil.isEmpty(fields)) {
			return false;
		}
		for(QueryField f : fields){
			for(QueryTable table : tables){
				isExist = isExist || table.isExistsByMetaCode(f);
			}
		}

		return isExist;
	}

	public List<QueryField> getMeasureFields() {
		List<QueryField> fields = new ArrayList<>();
		if(factTable != null) {
			return factTable.getMeasureFields();
		}
		return fields;
	}

	public List<QueryField> getDimFields(){
		return this.getFields().stream().filter(f->!f.isMeasure()).collect(Collectors.toList());
	}

	public Boolean getOnlyJoinModel() {
		return onlyJoinModel;
	}

	public String getAlias() {
		alias = modelAliasPrefix + index;
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public QueryField getFieldByCode(String fieldCode) {
		List<QueryField> fields = this.getFields();
		for(QueryField f : fields){
			if(f.getCode().equalsIgnoreCase(fieldCode)) {
				return f;
			}
		}
		return null;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public String getDataUpdateTime() {
		return dataUpdateTime;
	}

	public void setDataUpdateTime(String dataUpdateTime) {
		this.dataUpdateTime = dataUpdateTime;
	}
}
