package com.bi.queryer.util.component.datasource;

import java.util.List;
import java.util.Map;

import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.UpperCaseMap;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.datagrid.DataGrid;

public class DataGridDataSetProvider extends ComponentDataSetProvider {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<BIMap> getDataSet(Map queryParamMap) {
		DataGrid datagrid = (DataGrid)getComponent();
		Datasource datasource = datagrid.getDatasource();
		DataSourceType datasourceType = DataSourceType.getType(datasource.getType());
		String sql = datasource.getSql();
		DBType dbType = DBType.getType(datasourceType.getDialect());
		if(datagrid.isPagination() && !queryParamMap.containsKey("isExport")) {
			switch (dbType) {
			case Oracle:
				sql = buildOraclePaginationSQL(sql, queryParamMap);
				break;
			case Presto:
				sql = buildPrestoPaginationSQL(sql, queryParamMap);
				break;
			case MySQL:
			default:
				sql = buildMySQLPaginationSQL(sql, queryParamMap);
				break;
			}
		}
		queryParamMap.put(ComponentConstants.Component_SQL,sql);
		return super.getDataSet(queryParamMap);
	}
	
	/**
	 * 构建Oracle分页SQL
	 * @param sql
	 * @return
	 */
	protected String buildOraclePaginationSQL(String sql, Map queryParamMap) {
		String paginationSQL = "SELECT * FROM  ( SELECT t1.*,rownum f_row FROM  ( " + sql + " ) t1 WHERE rownum <= #pageRowUpper# ) t2 WHERE t2.f_row >= #pageRowLower#";
		return paginationSQL;
	}
	
	/**
	 * 构建MySQL分页SQL
	 * @param sql
	 * @return
	 */
	protected String buildMySQLPaginationSQL(String sql, Map queryParamMap) {
		String paginationSQL = sql + " LIMIT #pageRowLower2#, #prePageSize#";
		return paginationSQL;
	}
	
	/**
	 * 构建Preston分页SQL
	 * @param sql
	 * @return
	 */
	protected String buildPrestoPaginationSQL(String sql, Map queryParamMap) {
		if(sql == null) {
			return "";
		}
		String maxRowId = queryParamMap.get("_maxRowId_") + "";
		String currPageNo = queryParamMap.get("currPageNo") + "";
		//String regExp = "(?i)row_id[\\s]+between[\\s]+[\\d]+[\\s]+and+[\\s]+[\\d]+";
		//String replacement = "row_id between #pageRowLower# and #pageRowUpper#";
		String paginationSQL = sql;
		if(!"0".equals(currPageNo) && !StringUtil.isEmpty(maxRowId)) { // 非第0页时，需要添加过滤条件
			paginationSQL = "select _tmp.* from (" + sql + ") _tmp where row_id > " + maxRowId;
		}
		// 先判断是否有limit
		if(sql.toLowerCase().indexOf("limit ") == -1) {
			paginationSQL = paginationSQL + " LIMIT #prePageSize#";
		}
		return paginationSQL;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Integer getDataSetTotalSize(Map queryParamMap) {
		return super.getDataSetTotalSize(queryParamMap);
	}
}
