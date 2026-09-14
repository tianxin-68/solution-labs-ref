package com.bi.queryer.ssm.engine.model;

import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.view.DataSourceView;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.MetaTableType;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板查询表，用于SQL引擎使用
 * @author contributor
 *
 */
public class QueryTable implements Comparable<QueryTable>{

	/**
	 * 表别名前缀
	 */
	protected static final String aliasPrefix = "F";
	
	/**
	 * 元表
	 */
	private MetaTable meta  = null;
	
	/**
	 * 字段
	 */
	private List<QueryField> fields = new ArrayList<QueryField>();

	/**
	 * 字段map：提升排重性能
	 */
	private Map<String, QueryField> fieldMap = new HashMap<>();
	
	/**
	 * 别名
	 */
//	private String alias = "";
	
	/**
	 * 子查询视图SQL
	 */
//	private String viewSql = "";

	/**
	 *  子查询视图
	 */
	private DataSourceView view = null;

	private boolean active = true;
	
	private Double weight = (double)-1;

	/**
	 * 是否是虚拟表
	 */
	private Boolean virtual = false;

	protected StarModel model = null;

	public QueryTable(){
		
	}
	
	public QueryTable(MetaTable meta){
		this.meta = meta;
	}

	/**
	 * 表格式化：重新处理字段信息初始化
	 */
	public void format(){
		// 处理计算字段的扩展字段
		List<QueryField> allAppendFields = new ArrayList<>();
		for(QueryField field : fields){
			/*
			MetaField meta = field.getMeta();
			if (null != meta) {
				String expression = meta.getAggExpression();
				if ((!StringUtil.isEmpty(expression) && expression.indexOf("[") != -1)
						|| (field.getCustomFieldConfigure() != null && !field.getCustomFieldConfigure().isEmpty())) {
					field.setCalc(true);

					CustomFieldParser.setCalcFieldAtomFields(field, fields);
					allAppendFields.addAll(field.getCalcAtomFields().stream().filter(f->f.isAppend()).collect(Collectors.toList()));
				}
			}
			 */
			if(FieldUtil.isCalcField(field)){
				field.setCalc(true);

				FieldUtil.setCalcFieldAtomFields(field, fields);
				allAppendFields.addAll(field.getCalcAtomFields().stream().filter(f->f.isAppend()).collect(Collectors.toList()));
			}
		}


		// 添加附加字段到结果字段列表中
		for(QueryField appendField : allAppendFields){
			if(!fieldMap.containsKey(appendField.getCode()) && SSDMetaCacheManager.existByCode(this.getId(), appendField.getCode())){
				this.addField(appendField);
			}
		}
	}

	public QueryTable clone(){
		QueryTable copy = new QueryTable(this.meta);
		copy.active = this.active;
		copy.virtual = this.virtual;
		//copy.viewSql = this.viewSql;
		if(view != null){
			copy.view = view.clone();
		}
		copy.weight = this.weight;

		copy.fields = new ArrayList<>();
		copy.fieldMap = new HashMap<>();
		this.fields.forEach(f->{
			copy.addField(f.clone());
		});
		copy.format();

		return copy;
	}
	
	@Override
	public String toString() {
		String str = getAlias();
		if(meta != null){
			str = str + "\t" + meta.toString();
		}
		return str;
	}
	
	/**
	 * 获取表类型：维度还是事实
	 * @return
	 */
	public MetaTableType getType(){
		if(meta == null){
			return MetaTableType.None;
		}else {
			return Enabled.value(this.meta.getIsFactTable()) ? MetaTableType.Fact : MetaTableType.Dimension;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		return this.getId().equals(((QueryTable)obj).getId());
	}
	
	public void addField(QueryField field){
		// 此处需先设置字段所关联表，处理字段名一样，但字段含义不一样，比如：日、月
		field.setTable(this);
		if(fieldMap.containsKey(field.getCode())){//if(fields.contains(field)){
			return;
		}
		fields.add(field);
		fieldMap.put(field.getCode(), field);
		/*
		if(isExistsByLogicId(field)){
			String logicId = field.getLogicId();
			for(QueryField f : fields){
				if(logicId.equalsIgnoreCase(f.getLogicId())){
					// 添加该字段的逻辑相同
					if(!f.getLogicIdSameList().contains(field)){
						f.getLogicIdSameList().add(field);
					}
				}
			}
			//return;
		}
		 */
	}
	
	public QueryField getFieldByCode(String code){
		if(StringUtil.isEmpty(code)){
			return null;
		}
		return fieldMap.get(code);
		/*
		for(QueryField m : fields){
			if(code.equals(m.getCode())){
				return m;
			}
		}
		return null;
		 */
	}
	
	/**
	 * 通过字段逻辑id获取查询字段
	 * @param logicId
	 * @return
	 */
	public QueryField getFieldByLogicId(String logicId){
		if(StringUtil.isEmpty(logicId)){
			return null;
		}
		for(QueryField f : fields){
			if(logicId.equals(f.getLogicId())){
				return f;
			}
		}
		return null;
	}

	public List<QueryField> getFields() {
		return fields;
	}

	public List<QueryField> getDimFields(){
		return fields.stream().filter(f->!f.isMeasure()).collect(Collectors.toList());
	}

	public List<QueryField> getMeasureFields(){
		return fields.stream().filter(f->f.isMeasure()).collect(Collectors.toList());
	}

	public void setFields(List<QueryField> fields) {
		this.fields = fields;
	}

//	public String getViewSql() {
//		return viewSql;
//	}
//
//	public void setViewSql(String viewSql) {
//		this.viewSql = viewSql;
//	}

	public MetaTable getMeta() {
		return meta;
	}

	public void setMeta(MetaTable meta) {
		this.meta = meta;
	}

	public String getId() {
		if(meta != null){
			return meta.getId();
		}
		return null;
	}

	public String getAlias() {
		String alias = "";
		if(model != null){
			alias = aliasPrefix + model.getIndex();
		}
		return alias;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * 通过逻辑id判断字段是否在表中存在
	 * @param field
	 * @return
	 */
	public boolean isExistsByLogicId(QueryField field){
		String logicId = field.getLogicId();
		if(logicId == null){
			return false;
		}
		for(QueryField f : fields){
			if(logicId.equalsIgnoreCase(f.getLogicId())){
				return true;
			}
		}
		return false;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(QueryTable o) {
		Double num = this.weight - o.getWeight();
		return num > 0 ? 1 : -1;
	}

	public boolean isExistsByCode(QueryField field){
		for(QueryField f : fields){
			if(field.getCode().equalsIgnoreCase(f.getCode())){
				return true;
			}
		}
		return false;
	}

	public boolean isExistsByMetaCode(QueryField field){
		if(meta == null) {
			return false;
		}
		List<MetaField> metaFields = SSDMetaCacheManager.getTableFields(meta.getId());
		for(MetaField f : metaFields){
			if(field.getCode().equalsIgnoreCase(f.getCode())){
				return true;
			}
		}
		return false;
	}

	public Boolean getVirtual() {
		return virtual;
	}

	public void setVirtual(Boolean virtual) {
		this.virtual = virtual;
	}

	public List<QueryField> dropFieldsByCode(Collection<String> codes) {
		List<QueryField> dropFields = new ArrayList<>();
		List<QueryField> newFields = new ArrayList<>();
		Map<String, QueryField> newFieldMap = new HashMap<>();
		for(QueryField f : fields){
			if(codes.contains(f.getCode())) {
				dropFields.add(f);
				continue;
			}
			newFields.add(f);
			newFieldMap.put(f.getCode(), f);
		}
		fields = newFields;
		fieldMap = newFieldMap;
		return dropFields;
	}

    public StarModel getModel() {
        return model;
    }

    public void setModel(StarModel model) {
        this.model = model;
    }

	public DataSourceView getView() {
		return view;
	}

	public void setView(DataSourceView view) {
		this.view = view;
	}
}
