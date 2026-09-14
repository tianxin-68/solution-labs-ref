package com.bi.queryer.util.component;

import java.util.Map;

import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;

/**
 * 管理service接口
 * @author contributor
 *
 */
public interface IComponentManagerService extends IDataGridDataSetProvider{
	
	/**
	 * 创建表格
	 * @param paramMap
	 * @return DataGrid
	 */
	public DataGrid buildDataGrid(Map<String, String> paramMap);
	
	/**
	 * 通过id获取实体
	 * @param id
	 * @return
	 */
	public ComponentEntity getById(String id);
	
	public ComponentType getType();
}
