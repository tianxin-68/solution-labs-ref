package com.bi.queryer.util.component.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.UpperCaseMap;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.ComponentUtil;

/**
 * 从XML文件中读取SQL
 * @author contributor
 *
 */
public class XmlDataSetProvider extends BaseDao implements IDataSetProvider{
	
	private static final long serialVersionUID = 1L;
	
	protected String datasetSql = "";
	
	protected String countSql = "";
	
	/**
	 * 是否是导出
	 * @param queryParamMap
	 * @return
	 */
	protected boolean isExport(Map queryParamMap) {
		boolean export = queryParamMap.containsKey("isExport");
		return export;
	}
	
	/**
	 * 是否是过滤器
	 * @param queryParamMap
	 * @return
	 */
	protected boolean isFilter(Map queryParamMap) {
		boolean filter = queryParamMap.containsKey("filterId");
		return filter;
	}
	
	public String getDatasetSql(Map queryParamMap) {
		this.buildSQLAndQuery(queryParamMap, false);
		return this.datasetSql;
	}
	
	public String getCountSql(Map queryParamMap) {
		this.buildSQLAndCount(queryParamMap, false);
		return this.countSql;
	}
	
	/**
	 * 构建SQL并查询数据
	 * @param queryParamMap 查询参数
	 * @param queryDataSet 是否查数据
	 * @return
	 */
	protected  List<BIMap> buildSQLAndQuery(Map queryParamMap, boolean queryDataSet) {
		long t1 = System.currentTimeMillis();
		String sql = StringUtil.ifNull(queryParamMap.get(ComponentConstants.Component_SQL));
		// 解析
		sql = DBUtil.parseSQL(sql, queryParamMap);
		List<BIMap> data = new ArrayList<BIMap>();
		if(!StringUtil.isEmpty(sql)){
			DataSourceType dsType = DataSourceType.getType(queryParamMap.get(ComponentConstants.Component_SQLSource) + "");
			if(DBType.Presto == DBType.getType(dsType.getDialect()) && !isExport(queryParamMap) && !isFilter(queryParamMap)){
				if(sql.toLowerCase().indexOf("limit") == -1){
					sql += " limit 200";
				}
			}
			if(queryDataSet) {
				data = (List<BIMap>) this.queryMapListBySQL(sql, dsType);
			}
			this.datasetSql = sql;
		}
		ComponentUtil.log("XmlDataSetProvider读取数据列表", "耗费时间" + (System.currentTimeMillis() - t1) + "毫秒");
		return data;
	
	}
	
	
	
	@Override
	public List<BIMap> getDataSet(Map queryParamMap) {
		return this.buildSQLAndQuery(queryParamMap, true);
	}
	
	/**
	 * 构建查询并计数
	 * @param queryParamMap
	 * @param queryDataSet
	 * @return
	 */
	protected Integer buildSQLAndCount(Map queryParamMap, boolean queryCount) {
		long t1 = System.currentTimeMillis();
		String sql = StringUtil.ifNull(queryParamMap.get(ComponentConstants.Component_SQL_Count));
		if(StringUtil.isEmpty(sql)){
			return -1;
		}
		// 解析
		sql = DBUtil.parseSQL(sql, queryParamMap);
		DataSourceType dsType = DataSourceType.getType(queryParamMap.get(ComponentConstants.Component_SQLSource) + "");
		Integer count = 0;
		if(queryCount) {
//			if(DBType.Presto == DBType.getType(dsType.getDialect())){
//				count = this.queryCountByPrestoSQL(sql, null);
//			}else{
//				count = this.queryCountBySQL(sql, dsType);
//			}
		}
		this.countSql = sql;
		ComponentUtil.log("XmlDataSetProvider读取数据Count记录数", "耗费时间" + (System.currentTimeMillis() - t1) + "毫秒");
		return count;
	
	}

	@Override
	public Integer getDataSetTotalSize(Map queryParamMap) {
		return this.buildSQLAndCount(queryParamMap, true);
	}

}
