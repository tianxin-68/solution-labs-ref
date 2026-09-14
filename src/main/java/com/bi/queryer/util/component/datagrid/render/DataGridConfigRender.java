package com.bi.queryer.util.component.datagrid.render;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;

public class DataGridConfigRender extends DataGridRender{

	public DataGridConfigRender(DataGrid grid) {
		super(grid);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 构建钻取信息,暂时只支持单列可钻取
	 * @return
	 */
	@Override
	protected JSONObject buildDrillOption(){
		JSONArray opts = new JSONArray();
		JSONArray parameters = new JSONArray();
		for(DataGridColumn col : grid.getColumns()){
			if(!col.isHidden() && col.isCanDrill()){
				JSONObject option = new JSONObject();
				option.put("mode", col.getDrillMode());
				option.put("field", col.getField());
				option.put("idField", col.getDrillIDName());
				option.put("levelField", col.getDrillLevelName());
				opts.add(option);
			}
			if(col.isCanDrill() || col.isDrillParameter()){
				parameters.add(col.getOriginField());
			}
		}
		for(int i = 0;i<opts.size();i++){
			JSONObject option = opts.getJSONObject(i);
			option.put("parameters", parameters);
		}
		JSONObject obj = new JSONObject();
		obj.put("data", opts);
		return obj;
	}
}
