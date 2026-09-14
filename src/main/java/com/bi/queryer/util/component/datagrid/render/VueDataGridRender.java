package com.bi.queryer.util.component.datagrid.render;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.common.FunctionItem;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.DataGridColumnGroup;

public class VueDataGridRender extends DataGridRender{

	public VueDataGridRender(DataGrid grid) {
		super(grid);
	}
	
	@Override
	protected JSONObject buildColumnOptions() {
		VueDataGridColumnRender columnRender = new VueDataGridColumnRender(grid);
		return columnRender.buildOptions();
	}
	
	@Override
	public JSONObject buildOptions() {
		JSONObject gridOption = super.buildOptions();
		
		// id
		if(StringUtil.isEmpty(grid.getId())) {
			gridOption.put("id", "grid_" + Guid.id());
		}
		
		List<DataGridColumnGroup> grps = grid.getGroups();
		gridOption.put("hasGroup", grps != null && grps.size() > 0);
		
		// 表格操作
		List<FunctionItem> operations = new ArrayList<FunctionItem>();
		DataGridColumn[] cols = grid.getColumns();
		for(DataGridColumn col : cols) {
			if(col.isOperator()) {
				operations = col.getOperations();
				break;
			}
		}
		JSONArray operationArray = BIUtil.toJSONArray(operations);
		gridOption.put("operations", operationArray);
		return gridOption;
	}
	
}
