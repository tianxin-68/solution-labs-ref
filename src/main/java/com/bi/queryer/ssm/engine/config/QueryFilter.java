package com.bi.queryer.ssm.engine.config;

import com.bi.queryer.ssm.engine.config.field.FieldOperator;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QueryFilter implements JSONSerializable {
	private List<QueryField> fields = new ArrayList<QueryField>();

	private List<FieldOperator> operators = new ArrayList<FieldOperator>();

	//自定义指标类型的过滤字段id集合
	private List<String> customMeasureFilterIds = new ArrayList<>();

	public List<QueryField> getFields() {
		return fields;
	}

	public void setFields(List<QueryField> fields) {
		this.fields = fields;
	}

	public List<FieldOperator> getOperators() {
		return operators;
	}

	public void setOperators(List<FieldOperator> operators) {
		this.operators = operators;
	}

	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		JSONObject fieldsObj = new JSONObject();
		JSONArray fieldArray = new JSONArray();
		for(QueryField f : fields){
			if(f.isHierarchy()){
				continue;
			}
			fieldArray.add(f.toJSON());
		}
		fieldsObj.put("Fields", fieldArray);
		result.put("Filter", fieldsObj);
		return result;
	}

	public void load(JSONArray elements) {
		if (elements == null || elements.isEmpty()) {
			return;
		}
		for (int i = 0; i < elements.size(); i++) {
			QueryField field = elements.getObject(i, QueryField.class);
			List<FieldValue> values = field.getValues();
			if (BIUtil.isEmpty(values)) {

				//公共日期必须选择时间范围
				if(BIConsts.DATE_CODE.equalsIgnoreCase(field.getCode())){
					throw new RuntimeException("请先选择日期，再点击查询！");
				}

				continue;
			}
			values = values.stream().filter(v -> BIUtil.isNotEmpty(v.getId())).collect(Collectors.toList());
			if (BIUtil.isEmpty(values)) {
				continue;
			}
			field.setIsFilter(true);

			//计算指标过滤，设置查询区域为指标
			//计算指标的code生成规则依赖queryArea
			if (FieldType.CUSTOM_MEASURE == FieldType.get(field.getFieldType())) {
				field.setQueryArea(QueryArea.Measure);
				customMeasureFilterIds.add(field.getId());
			}

			field.init();
			fields.add(field);
		}

		List<QueryField> allAppendFields = new ArrayList<QueryField>();
		// 对计算字段分解
		for (QueryField qf : fields) {
			/*
			MetaField meta = qf.getMeta();
			if (null != meta) {
				String expression = meta.getAggExpression();
				if ((!StringUtil.isEmpty(expression) && expression.indexOf("[") != -1)
						|| (qf.getCustomFieldConfigure() != null && !qf.getCustomFieldConfigure().isEmpty())) {
					qf.setCalc(true);
					CustomFieldParser.setCalcFieldAtomFields(qf, fields);
					allAppendFields.addAll(qf.getCalcAtomFields().stream().filter(f->f.isAppend()).collect(Collectors.toList()));
				}
			}
			 */

			if (FieldUtil.isCalcField(qf)) {
				qf.setCalc(true);
				FieldUtil.setCalcFieldAtomFields(qf, fields);

				// 计算指标筛选在查询区域必须存在
				// 其依赖字段不加入字段列表中，避免后续因为其isAppend字段导致选表异常
				if (FieldType.CUSTOM_MEASURE == FieldType.get(qf.getFieldType())) {
					continue;
				}

				allAppendFields.addAll(qf.getCalcAtomFields().stream().filter(f -> f.isAppend()).collect(Collectors.toList()));
			}
		}
		// 添加附加字段到结果字段列表中
		for (QueryField appendField : allAppendFields) {
			if (!fields.contains(appendField)) {
				fields.add(appendField);
			}
		}

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

	public List<String> getCustomMeasureFilterIds() {
		return customMeasureFilterIds;
	}

	public void setCustomMeasureFilterIds(List<String> customMeasureFilterIds) {
		this.customMeasureFilterIds = customMeasureFilterIds;
	}
}

