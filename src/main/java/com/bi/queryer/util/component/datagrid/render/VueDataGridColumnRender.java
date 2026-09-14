package com.bi.queryer.util.component.datagrid.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.DataGridColumnGroup;
import com.bi.queryer.util.component.datagrid.alarm.Alarm;
import com.bi.queryer.util.component.datagrid.alarm.AlarmType;

public class VueDataGridColumnRender extends DataGridColumnRender{
	
	public VueDataGridColumnRender(DataGrid grid) {
		super(grid);
	}
	
	/**
	 * 暂时只支持2级表头
	 * @return
	 */
	public JSONObject buildOptions() {
		this.preBuild();
		DataGridColumn[] cols = grid.getColumns();
		Set<String> createdCols = new HashSet<String>();
		JSONArray colOptions = new JSONArray();
		for(DataGridColumn col : cols) {
			if(col.isHidden()) {
				continue;
			}
			if(createdCols.contains(col.getField())) {
				continue;
			}
			String grpId = col.getGroup();
			if(StringUtil.isEmpty(grpId)) {
				colOptions.add(buildColumnOption(col));
				createdCols.add(col.getField());
			}else {
				DataGridColumnGroup group = grid.getGroup(grpId);
				if(group == null) {
					continue;
				}
				
				// 先构建组
				JSONObject grpOption = buildColumnOption(group);
				List<DataGridColumn> grpCols = group.getColumns();
				if(grpCols == null || grpCols.isEmpty()) {
					continue;
				}
				
				grpOption.put("hasChild", true);
				JSONArray grpChildren = new JSONArray();
				for(DataGridColumn grpCol : grpCols) {
					if(grpCol.isHidden()) {
						continue;
					}
					if(createdCols.contains(grpCol.getField())) {
						continue;
					}
					grpChildren.add(buildColumnOption(grpCol));
					createdCols.add(grpCol.getField());
				}
				
				// 组添加子节点
				grpOption.put("children", grpChildren);
				
				// 添加组
				colOptions.add(grpOption);
				createdCols.add(group.getId());
			}
		}
		JSONObject result = new JSONObject();
		result.put("columns", colOptions);
		return result;
	}
	
	@Override
	protected void calcColumnsWidth() {
		// do nothing
	}
	
	protected JSONObject buildColumnOption(DataGridColumn column) {
		JSONObject json = new JSONObject();
		json.put("formatter", column.getAlarmExpression());
		json.put("valueFormat", column.getValueFormat());
		json.put("valuePrefix", column.getValuePrefix());
		json.put("valueSuffix", column.getValueSuffix());
		json.put("isFrozen", column.isFrozen());
//		json.put("field", escapeField(column.getFieldByCode()));
		json.put("field", escapeField(column.getOriginField()));
		String key = column instanceof DataGridColumnGroup?column.getId():column.getField();
		if(key != null) key = key.toUpperCase();
		String title = StringUtil.ifNull(colNameMap.get(key));
		if(StringUtil.isEmpty(title)){
			title = column.getTitle();
		}
		json.put("title", title);
		json.put("align", column.getAlign());
		json.put("halign", column.getHalign());
		json.put("remoteSortable", column.isRemoteSortable());
		json.put("localSortable", column.isLocalSortable());
		json.put("dataType", column.getDataType().toString());
		json.put("externalUrl", column.getExternalUrl());
		if(BIUtil.isNotEmpty(column.getDefaultSortType())){
			json.put("order", column.getDefaultSortType().toLowerCase());
		}
		json.put("hidden", column.isHidden());
		json.put("checkbox", column.isCheck());
		if(column.getWidth() > 0) {
			json.put("width", column.getWidth());
		}
		if(column.getRowspan() >= 1) {
			json.put("rowspan", column.getRowspan());
		}
		if(column.getColspan() >= 1) {
			json.put("colspan", column.getColspan());
		}
		
		json.put("jsForamtter", column.getJsForamtter());
		json.put("canDrill", column.isCanDrill());
		json.put("mergeCell", column.isMergeCell());
		json.put("exportable", column.isExportable());
		json.put("exportFormat", column.isExportFormat());
		json.put("isMessager", column.isMessager());
		json.put("nullValue", column.getNullValue());
		json.put("valuePrefix", column.getValuePrefix());
		json.put("valueSuffix", column.getValueSuffix());
		json.put("clob", column.isClob());
		json.put("enableAlarm", column.isEnableAlarm());
		
		String alarmTypeStr = "";
		JSONArray alarmJsonArray = new JSONArray();
		if(column.getAlarms() != null && column.getAlarms().length > 0) {
			Alarm[] alarms = column.getAlarms();
			for(Alarm alarm : alarms) {
				JSONObject alarmJo = new JSONObject();
				AlarmType alarmType = alarm.getType();
				String[] alarmTypeArray = alarmType.toString().split("_");
				if(!StringUtil.isEmpty(alarmTypeArray[1])) {
					alarmTypeStr = alarmTypeArray[1];
					alarmJo.put("type", alarmTypeArray[0]);
					alarmJo.put("expression", alarm.expression());
				}
				alarmJsonArray.add(alarmJo);
			}
		}
		json.put("alarmType", alarmTypeStr);
		json.put("expressions", alarmJsonArray);
		
		json.put("canFilter", column.isCanFilter());
		json.put("canHide", column.isCanHide());
		json.put("operator", column.isOperator());
		json.put("operations", column.getOperations());
		
		Map<String, Object> rules = column.getValueDisplayRules();
		if(rules == null) rules = new HashMap<String, Object>();
		json.put("hasValueDisplayRules", rules.size() > 0);
		json.put("valueDisplayRules", rules);
		
		return json;
	}
}
