package com.bi.queryer.ssm.engine.config;

import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.dom4j.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class QueryResult {
	private List<QueryField> fields = new ArrayList<QueryField>();

	private List<QueryField> rowDimensions = new ArrayList<>();

	private List<QueryField> colDimensions = new ArrayList<>();

	private List<QueryField> measures = new ArrayList<>();

	private QueryPivotConfig pivotConfig = new QueryPivotConfig(false);

	//自定义指标类型的过滤字段id集合
	private List<String> customMeasureFilterIds = new ArrayList<>();

	public List<QueryField> getFields() {
		return fields;
	}

	public QueryPivotConfig getPivotConfig() {
		return pivotConfig;
	}

	public void setFields(List<QueryField> fields) {
		this.fields = fields;
		if(BIUtil.isEmpty(fields)) {
			return;
		}
		measures.clear();
		rowDimensions.clear();
		colDimensions.clear();
		for(QueryField f : fields){
			switch (f.getQueryArea()){
				case Measure:
					measures.add(f);
					break;
				case ColumnDimension:
					colDimensions.add(f);
					break;
				case RowDimension:
					 default:
					rowDimensions.add(f);
					break;
			}
		}
	}

	public void load(JSONObject resultObject) {
		load(resultObject, null);
	}

	public void load(JSONObject resultObject, QueryMesoscopic meso) {
		if (resultObject == null || resultObject.isEmpty()) {
			return;
		}
		this.rowDimensions = this.loadFields(resultObject.getJSONArray("rowDimensions"), QueryArea.RowDimension);
		this.colDimensions = this.loadFields(resultObject.getJSONArray("colDimensions"), QueryArea.ColumnDimension);
		// 指标
		this.loadMeasures(resultObject.getJSONArray("measures"));

		this.addAdditionalField();

		// 将中观设置的维度赋值到结果行维度字段中
		// 前端功能已废弃
		/*
		if(meso != null){
			Map<String, Integer> mesoFields = meso.getFields().stream().filter(f->Enabled.isTrue(f.getIsAggQuery())).collect(Collectors.toMap(QueryField::getCode, QueryField::getIsAggQuery,(f1,f2)->f1));
			List<QueryField> newRowDimensions = new ArrayList<>();
			for(QueryField rowField : rowDimensions){
				if(mesoFields.containsKey(rowField.getCode())){
				    continue;
				}
                newRowDimensions.add(rowField);
			}
			this.fields.removeAll(this.rowDimensions);
			this.rowDimensions = newRowDimensions;
			this.fields.addAll(0, this.rowDimensions);
		}
		 */
	}

	/**
	 * 添加附加字段：用于添加计算字段的原子字段
	 */
	public void addAdditionalField(){
		List<QueryField> allAppendFields = new ArrayList<QueryField>();
		// 对计算字段分解（后台计算字段+前台用户自定义计算字段）

		Map<String, QueryField> lodAppendFields = new HashMap<>();
		for(QueryField qf : fields){

			if(FieldUtil.isCalcField(qf)){
				qf.setCalc(true);
				FieldUtil.setCalcFieldAtomFields(qf, fields);
				allAppendFields.addAll(qf.getCalcAtomFields().stream().filter(f->f.isAppend()).collect(Collectors.toList()));
				if(qf.getCustomFieldConfigure() != null && CustomFieldType.isLod(qf.getCustomFieldConfigure().getType())) {
					allAppendFields.forEach(a -> lodAppendFields.put(a.getId(), a));
				}

			}

		}
		// 添加附加字段到结果字段列表中
		for(QueryField appendField : allAppendFields){
			if(!fields.contains(appendField)){
				fields.add(appendField);
			}
		}
	}

	protected List<QueryField> loadFields(JSONArray elements, QueryArea queryArea){
		List<QueryField> queryFields = new ArrayList<>();
		if(BIUtil.isEmpty(elements)) {
			return queryFields;
		}

		Set<String> calcFieldsRefIdList = new HashSet<>();
		for (int i = 0; i < elements.size(); i++) {
			QueryField field = elements.getObject(i, QueryField.class);

			if (BIConsts.ALL_MEASURE_CODE.equals(field.getCode())) {
				//对行/列维度上放置的"所有指标"标识的处理
				if (QueryArea.RowDimension.equals(queryArea)) {
					this.pivotConfig.setMeasureOnRow(true);
				}
				continue;
			} else {
				if (QueryArea.ColumnDimension.equals(queryArea) && Enabled.isTrue(field.getIsShow())) {
					this.pivotConfig.addColDimension(field);
				}
			}

			//前端传过来的分析字段，不处理。V1.7.1 统一由后台处理
			if(Enabled.value(field.getIsAnalysis())){
				continue;
			}

			// 设置不显示，则不处理
			/*
			if(Enabled.isFalse(field.getIsShow())) {
				continue;
			}
 			*/
			field.setIsResult(true);
			field.setQueryArea(queryArea);
			field.setRawQueryArea(queryArea);
			field.init();


			//汇总查询时，行维度去掉公共日期
			if(QueryArea.RowDimension == queryArea && field.isAggQuery()){
				continue;
			}

			queryFields.add(field);

			//分析字段的计算字段用于筛选，也需要构造其依赖字段
			if(
					(Enabled.isTrue(field.getIsShow()) || customMeasureFilterIds.contains(field.getId()))
							&& field.getCustomFieldConfigure() != null && field.getCustomFieldConfigure().getExpression() != null) {
				if (field.isAnalysisCalc()) {
					List<CustomFieldExpressionIdMapping> idMappings = field.getCustomFieldConfigure().getExpressionIdMapping();
					if (idMappings != null) {
						for (CustomFieldExpressionIdMapping idMapping : idMappings) {
							if (idMapping == null) {
								continue;
							}
							if (idMapping.getAnalysisItemConfig() != null) {
								calcFieldsRefIdList.add(idMapping.getAnalysisItemConfig().getMeasureId());
							} else {
								// 依赖的是本期值
								calcFieldsRefIdList.add(idMapping.getId());
							}

						}
					}
				} else {
					String expression = field.getCustomFieldConfigure().getExpression();
					Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
					Matcher matcher = pattern.matcher(expression);
					while (matcher.find()) {
						calcFieldsRefIdList.add(matcher.group());
					}
				}
			}
		}



		//场景：计算指标 A/B（code=B,id=B1），其中指标B(code=B，id=B2)在指标区域，但其设置不可见。
		//此时计算字段的依赖指标B,因为id与表达式中的id不一致导致无法加入最后查询结果集导致查询sql构建异常
		//解决方案：通过指标code再筛选一次
		Set<String> calcFieldsRefCodeList = new HashSet<>();
		for(String calcFieldsRefId : calcFieldsRefIdList) {
			MetaField metaField = SSDMetaCacheManager.getField(calcFieldsRefId);
			if (metaField == null) {
				continue;
			}
			calcFieldsRefCodeList.add(metaField.getCode());
		}

		//1. 去掉不显示的字段，但若原字段不显示，但被其他计算字段引用，且计算字段显示状态，则也需要加载，只是在最终查询结果中不显示
		//2. 计算指标的筛选，也需要加载
		if(QueryArea.Measure == queryArea){
			queryFields = queryFields.stream().filter(f->Enabled.isTrue(f.getIsShow())
					|| calcFieldsRefIdList.contains(f.getId())
					|| customMeasureFilterIds.contains(f.getId())
					|| calcFieldsRefCodeList.contains(f.getCode()))
					.collect(Collectors.toList())
					;
		}else {
			queryFields = queryFields.stream().filter(f->Enabled.isTrue(f.getIsShow())).collect(Collectors.toList());
		}

		this.fields.addAll(queryFields);
		return queryFields;
	}

	/**
	 * 加载指标
	 */
	public void loadMeasures(JSONArray measureJsonArray) {
		this.measures = this.loadFields(measureJsonArray, QueryArea.Measure);
	}

	public void save(Element e) {
	}
	
	/**
	 * 通过code获取字段
	 * @param code
	 * @return
	 */
	public QueryField getFieldByCode(String code){
		QueryField field = null;
		if(code == null) return null;
		for(QueryField qf : fields){
			if(code.equals(qf.getCode())){
				return qf;
			}
		}
		return field;
	}

	/**
	 * 通过code获取字段
	 * @param id
	 * @return
	 */
	public QueryField getFieldById(String id){
		QueryField field = null;
		if(id == null) return null;
		for(QueryField qf : fields){
			if(id.equals(qf.getId())){
				return qf;
			}
		}
		return field;
	}
	
	public List<QueryField> getRowDimensions() {
//		rowDimensions = fields.stream().filter(f->!f.isMeasure()).collect(Collectors.toList());
		rowDimensions = rowDimensions.stream().distinct().collect(Collectors.toList());
		return rowDimensions;
	}

	public void setRowDimensions(List<QueryField> rowDimensions) {
		this.fields.removeAll(rowDimensions);
		this.rowDimensions = rowDimensions;
		this.fields.addAll(rowDimensions);

	}

	public List<QueryField> getMeasures() {
//		measures = fields.stream().filter(f->f.isMeasure()).collect(Collectors.toList());
		return measures;
	}

	public void setMeasures(List<QueryField> measures) {
		this.fields.removeAll(measures);
		this.measures = measures;
		this.fields.addAll(measures);
	}


	public List<QueryField> getColDimensions() {
		return colDimensions;
	}

	public void setColDimensions(List<QueryField> colDimensions) {
		this.fields.removeAll(colDimensions);
		this.colDimensions = colDimensions;
		this.colDimensions.forEach(f->{
			f.setQueryArea(QueryArea.ColumnDimension);
		});
		this.fields.addAll(colDimensions);
	}

	public void remove(QueryField field){
		/*
		if(field.getRawQueryArea() == QueryArea.ColumnDimension) {
			colDimensions.remove(field);
		}
		if(field.getRawQueryArea() == QueryArea.RowDimension) {
			rowDimensions.remove(field);
		}
		if(field.getRawQueryArea() == QueryArea.Measure) {
			measures.remove(field);
		}
		fields.remove(field);
		 */
		remove(field, field.getRawQueryArea());
	}

	public void remove(List<QueryField> removeFields){
		if(BIUtil.isEmpty(removeFields)){
			return;
		}
		for(QueryField f : removeFields){
			this.remove(f);
		}
	}

	public void remove(QueryField field, QueryArea queryArea){
		if(queryArea == QueryArea.ColumnDimension) {
			colDimensions.remove(field);
		}
		if(queryArea == QueryArea.RowDimension) {
			rowDimensions.remove(field);
		}
		if(queryArea == QueryArea.Measure) {
			measures.remove(field);
		}
		fields.remove(field);
		if(field.isCalc()){
			//计算字段依赖的原子指标的code
			Set<String> dependCodes = fields.stream()
					.filter(QueryField::isCalc)
					.map(QueryField::getCalcAtomFields)
					.flatMap(Collection::stream)
					.map(QueryField::getRawCode).collect(Collectors.toSet());
			Set<QueryField> calcAtomFields = field.getCalcAtomFields();
			for(QueryField atomField : calcAtomFields){
				List<QueryField> appendFields = fields.stream()
						//append字段有还有其他字段依赖的，不能删
						//解决场景：管理后台配置的计算指标，同时做普通查询和加入lod中进行查询，会报错
						.filter(f->f.isAppend() && !dependCodes.contains(f.getCode()) && f.getCode().equals(atomField.getCode()))
						.collect(Collectors.toList());
				fields.removeAll(appendFields);
			}
		}
	}

	public void remove(List<QueryField> fields, QueryArea queryArea){
		if(BIUtil.isEmpty(fields)){
			return;
		}
		for(QueryField f : fields){
			this.remove(f, queryArea);
		}
	}

	public void add(QueryField field) {
		this.add(field, field.getQueryArea());
		/*
		if(field.getQueryArea() == QueryArea.ColumnDimension) {
			colDimensions.add(field);
		}
		if(field.getQueryArea() == QueryArea.RowDimension) {
			rowDimensions.add(field);
		}
		if(field.getQueryArea() == QueryArea.Measure) {
			measures.add(field);
		}
		fields.add(field);
		 */
	}

	public void add(QueryField field, QueryArea queryArea) {
		if(queryArea == QueryArea.ColumnDimension && !colDimensions.contains(field)) {
			colDimensions.add(field);
		}
		if(queryArea == QueryArea.RowDimension && !rowDimensions.contains(field)) {
			rowDimensions.add(field);
		}
		if(queryArea == QueryArea.Measure && !measures.contains(field)) {
			measures.add(field);
		}
		field.setQueryArea(queryArea);
		if(!fields.contains(field)) {
			fields.add(field);
		}
	}

	public void addAll(List<QueryField> fields, QueryArea queryArea){
		fields.stream().forEach(f->add(f, queryArea));
	}


	public List<String> getCustomMeasureFilterIds() {
		return customMeasureFilterIds;
	}

	public void setCustomMeasureFilterIds(List<String> customMeasureFilterIds) {
		this.customMeasureFilterIds = customMeasureFilterIds;
	}

	public static void main(String[] args) {
		List<String> list = new ArrayList<>();
		list.add("a");
		list.add("b");
		list.add("c");

		List<String> list2 = new ArrayList<>();
		list2.add("1");
		list2.add("2");

		list.addAll(0, list2);
		System.out.println(list);

	}
}
