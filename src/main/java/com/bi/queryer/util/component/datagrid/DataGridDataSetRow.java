package com.bi.queryer.util.component.datagrid;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.formula.functions.Column;

import com.alibaba.fastjson.JSONObject;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.render.DataGridColumnRender;

public class DataGridDataSetRow implements JSONSerializable{
	
	private DataGrid grid = null;
	
	private JSONSerializable data = null;
	
	private boolean isExport = false;// 是否是导出数据行
	
	public DataGridDataSetRow(DataGrid grid, JSONSerializable data){
		this.grid = grid;
		this.data = data;
	}
	
	@Override
	public JSONObject toJSON() {
		DataGridColumn[] columns = grid.getColumns();
		Map<String, DataGridColumn> colMap = new HashMap<String, DataGridColumn>();
		for(DataGridColumn dgc : columns){
			colMap.put(dgc.getField(), dgc);
		}
		return format(colMap, data);
	}
	
	/**
	 * 格式化行数据
	 * @param colMap
	 * @param rowJSON
	 */
	public JSONObject format(Map<String, DataGridColumn> colMap, JSONSerializable row){
		JSONObject newRow = new JSONObject();
		JSONObject rowObject = row.toJSON();
		for(Object key : rowObject.keySet()){
			String fieldKey = (key +"").toUpperCase();
			DataGridColumn col = colMap.get(fieldKey);
			String valueStr = "";
			if(col == null){
				continue;
			}
			// 不是导出数据&列有格式化 || 是导出数据&列有格式化&列需要导出格式
			boolean needFormat = (col.getValueFormatter() != null && (!isExport || (isExport && col.isExportFormat())));
			if(needFormat){
				if(rowObject.get(key) != null){
					try{
						Double value = Double.valueOf(rowObject.get(key) + "");
						valueStr = col.getValuePrefix() + col.getValueFormatter().format(value) + col.getValueSuffix();
					}catch(Exception e){
						e.printStackTrace();
					}
				}else{
					valueStr = col.getNullValue();
				}
			}else{
				Object valueObj = rowObject.get(key);
				if(valueObj == null){
					valueStr = col.getNullValue();
				}else{
					valueStr = col.getValuePrefix() + valueObj + col.getValueSuffix();
				}
			}
			
			// 超链接特殊处理
			if(!StringUtil.isEmpty(valueStr) && (
					valueStr.toLowerCase().startsWith("http") ||
					valueStr.toLowerCase().startsWith("https") ||
					valueStr.toLowerCase().startsWith("www")
					) && grid.getFormatLink()){
				valueStr = "<a href='" + valueStr + "' target='_blank'>" + valueStr + "</a>";
			}
			
			if(grid.isColumnFieldCaseSensitive()) { // 大小写敏感
				newRow.put(DataGridColumnRender.escapeField(col.getOriginField() + ""), valueStr);
			}else {
				newRow.put(DataGridColumnRender.escapeField(key + ""), valueStr);
			}
		}
		
		return newRow;
	}

	public boolean isExport() {
		return isExport;
	}

	public void setExport(boolean isExport) {
		this.isExport = isExport;
	}

}
