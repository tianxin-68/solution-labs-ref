package com.bi.queryer.util.component.datasource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.UpperCaseMap;
import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.ComponentSQLLog;
import com.bi.queryer.util.component.ComponentUtil;

public class ComponentDataSetProvider extends XmlDataSetProvider{

	private static final long serialVersionUID = 1L;
	
	private Component component = null;
	
	
	protected Map initDatasetParameters(Map queryParamMap) {
		Datasource datasource = component.getDatasource();
		
		// 外部查询参数和组件的数据源参数合并
		Map params = ComponentUtil.mergeQueryParameters(datasource, queryParamMap);
		if(params == null) {
			params = new HashMap();
		}
		params.put(ComponentConstants.Component_SQLSource,datasource.getType());
		
		//如果是datagrid组件，切配置需要分页，自动补上分页的sql片段
		if(!(queryParamMap!=null && queryParamMap.containsKey(ComponentConstants.Component_SQL) && !StringUtil.isEmpty(queryParamMap.get(ComponentConstants.Component_SQL).toString()))){
			params.put(ComponentConstants.Component_SQL,datasource.getSql());
		}
		return params;
	}
	
	@Override
	public String getDatasetSql(Map queryParamMap) {
		queryParamMap = this.initDatasetParameters(queryParamMap);
		return super.getDatasetSql(queryParamMap);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<BIMap> getDataSet(Map queryParamMap) {
		Map params = this.initDatasetParameters(queryParamMap);
		
		long t1 = System.currentTimeMillis();
		
		List<BIMap> dataset = super.getDataSet(params);
		
		long t2 = System.currentTimeMillis();
		
		ComponentSQLLog sqlLog = new ComponentSQLLog();
		
		if(!StringUtil.isEmpty(datasetSql)) {
			sqlLog.setRunSecond((t2 - t1) / 1000.0);
			sqlLog.setComponentSql(datasetSql);
			this.log(sqlLog, queryParamMap);
		}
		return dataset;
	}
	
	@Override
	public String getCountSql(Map queryParamMap) {
		queryParamMap = this.initCountParameters(queryParamMap);
		return super.getCountSql(queryParamMap);
	}
	
	/**
	 * 初始化计数参数
	 * @param queryParamMap
	 * @return
	 */
	protected Map initCountParameters(Map queryParamMap) {
		Datasource datasource = component.getDatasource();
		
		// 外部查询参数和组件的数据源参数合并
		Map params = ComponentUtil.mergeQueryParameters(datasource, queryParamMap);
		params.put(ComponentConstants.Component_SQL_Count,datasource.getSql());
		params.put(ComponentConstants.Component_SQLSource,datasource.getType());
		
		return params;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Integer getDataSetTotalSize(Map queryParamMap) {
		
		Map params = this.initCountParameters(queryParamMap);
		
		long t1 = System.currentTimeMillis();
		
		Integer count = super.getDataSetTotalSize(params);
		
		long t2 = System.currentTimeMillis();
		
		// count暂时不记录日志
		/*
		ComponentSQLLog sqlLog = new ComponentSQLLog();
		if(!StringUtil.isEmpty(countSql)) {
			sqlLog.setRunSecond((t2 - t1) / 1000.0);
			sqlLog.setComponentSql(countSql);
			this.log(sqlLog, queryParamMap);
		}
		*/
		return count;
	}
	
	/**
	 * 记录运行日志
	 * @param queryParamMap
	 */
	protected void log(ComponentSQLLog sqlLog, Map queryParamMap) {
		sqlLog.setLogId(Guid.id());
		if(component != null) {
			String cid = queryParamMap.get(ComponentConstants.Parameter_Component_ID) + "";
			sqlLog.setComponentId(StringUtil.isEmpty(cid) ? component.getId() : cid);
			
			String entityName = queryParamMap.get("entityName") + "";
			String entityId = queryParamMap.get("entityId") + "";
			sqlLog.setComponentName(StringUtil.isEmpty(entityName) ? component.getName() : entityName);
			sqlLog.setComponentType(component.getType().toString());
			sqlLog.setEntityId(StringUtil.isEmpty(entityId) ? component.getId() : entityId);
			sqlLog.setEntityName(StringUtil.isEmpty(entityName) ? component.getName() : entityName);
			sqlLog.setUserName(queryParamMap.get("userName") + "");
			ComponentUtil.log(sqlLog);
		}
	}

	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}
	
	
}
