package com.bi.queryer.sys.db;

import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.JdbcType;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datasource.DynamicSQLParser;
import com.bi.queryer.util.component.datasource.tag.DynamicSQLTagException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DBUtil {

	private static BaseDao dao = null;

	public static synchronized BaseDao getBaseDao() {
		if (dao == null) {
			dao = ((BaseDao)SpringContextUtil.getBean("baseDao"));
		}
		return dao;
	}

	public static DataSourceType getDataSourceType() {
		DataSourceType dsType = DataSourceRouter.getCurrentDataSourceType();
		if(dsType == null) {
			String ssd = SC.v("ssm.datasource.key", DataSourceType.Trino_Master.toString());
			dsType = DataSourceType.getType(ssd);
		}
		return dsType;
	}

	public static DBType getDBType() {
		DataSourceType dsType = getDataSourceType();
		return DBType.getType(dsType.getDialect());
	}

	/*
	public static synchronized MyBaseDao getMyBaseDao() {
		return (MyBaseDao)SpringContextUtil.getBean("myBaseDao");
	}
	*/

	public static Connection getConn() throws SQLException {
		return getConn(DataSourceType.Default);
	}

	public static Connection getConn(String dsType) throws SQLException{
		return getConn(DataSourceType.getType(dsType));
	}

	public static Connection getConn(DataSourceType dsType) throws SQLException {
		BasicDataSource dataSource = (BasicDataSource) SpringContextUtil.getBean(dsType.getName());
		Connection conn = dataSource.getConnection();
		return conn;
	}

	/**
	 * 将数据转化为json字符串
	 *
	 * @param dataList
	 * @return
	 */
	public static JSONObject toDataGridData(List<? extends JSONSerializable> dataList, int totalCount) {
		JSONObject resultObj = new JSONObject();
		JSONArray rows = new JSONArray();
		resultObj.put("rows", rows);

		// 添加总记录数
		resultObj.put("total", totalCount);
		if (dataList == null || dataList.size() == 0)
			return resultObj;

		// 添加记录
		for (JSONSerializable json : dataList) {
			rows.add(json.toJSON());
		}
		resultObj.put("rows", rows);

		return resultObj;
	}

	/**
	 * 将clob字段转换为字符串
	 * */
	public static String oracleClob2Str(Clob clob) throws Exception {
		return (clob != null ? clob.getSubString(1, (int) clob.length()) : null);
	}

	public static List<String> getTableNamesBySql(String sql){
		List<String> tableNames = new ArrayList<>();
		try{
	//		tableNames = LineageParser.parseTable(sql, DbType.TRINO);
		}catch (Exception e) {
			e.printStackTrace();
		}

		return tableNames;
	}
	
	/**
	 * SQL解析
	 * @param sql
	 * @param queryParamMap
	 * @return
	 */
	public static String parseSQL(String sql, Map queryParamMap) {
		if(queryParamMap == null) {
			queryParamMap = new HashMap();
		}
		for(Object key : queryParamMap.keySet()){
			try{
			    queryParamMap.put(key,EnvVariableManager.value(StringUtil.ifNull(queryParamMap.get(key))));//支持表达式
			}catch(Exception e){
				continue;
			}
		}
	    DynamicSQLParser parser = new DynamicSQLParser(sql, queryParamMap);
		try {
			sql = parser.parse();
			// 替换内置变量
			sql = BIUtil.replaceEnvVariables(sql);
		} catch (DynamicSQLTagException e) {
			e.printStackTrace();
		}
		return sql;
	}

	public static DataType getDataType(int jdbcColumnType) {
		JdbcType jdbcType = JdbcType.get(jdbcColumnType);
		return getDataType(jdbcType);
	}

	public static DataType getDataType(String jdbcColumnType) {
		JdbcType jdbcType = JdbcType.get(jdbcColumnType);
		return getDataType(jdbcType);
	}

	public static DataType getDataType(JdbcType jdbcType) {
		switch (jdbcType) {
			case CHAR:
			case NCHAR:
			case VARCHAR:
			case LONGVARCHAR:
			case NVARCHAR:
			case LONGNVARCHAR:
			case CLOB:
			case NCLOB:
			case BINARY:
			case VARBINARY:
			case BLOB:
			case LONGVARBINARY:
			case BOOLEAN:
			case BIT:
			case NULL:
				return DataType.String;
			case SMALLINT:
			case TINYINT:
			case INTEGER:
				return DataType.Integer;
			case BIGINT:
				return DataType.BigInt;
			case NUMERIC:
			case DECIMAL:
			case FLOAT:
			case REAL:
			case DOUBLE:
				return DataType.Double;
			case TIME:
			case DATE:
			case TIMESTAMP:
				return DataType.Date;
		}

		return DataType.String;

	}

	/**
	 * 查询元数据
	 * @param sql
	 * @param dsType
	 * @return
	 */
	public static List<ResultDataSetColumn> getMetadata(String sql, DataSourceType dsType) {
		Statement stmt = null;
		ResultSet res = null;
		Connection conn = null;
		System.out.println("====================MyBaseDao getMetadata(" + dsType.getDesc() + ")...====================");
		Long t1 = System.currentTimeMillis();
		List<ResultDataSetColumn> columns = new ArrayList<>();
		try {

			conn = DBUtil.getConn(dsType);
			stmt = conn.createStatement();
			String noDataSql = String.format("select _no_data2.* from (%s) _no_data2 where 1=2", sql);
			res = stmt.executeQuery(noDataSql);
			ResultSetMetaData metaData = res.getMetaData();
			int columnCount = metaData.getColumnCount();

			for (int i = 1; i <= columnCount; i++) {
				ResultDataSetColumn column = new ResultDataSetColumn();
				column.setSortId(new Double(i));
				column.setCode(metaData.getColumnLabel(i));
				DataType dataType = DBUtil.getDataType(metaData.getColumnType(i));
				if(dataType.isDecimal()) {
					column.setDataType(DataType.Double.toString().toLowerCase());
				}else{
					column.setDataType(DataType.String.toString().toLowerCase());
				}
				columns.add(column);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			try {
				if (res != null) {
					res.close();
				}
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Long t2 = System.currentTimeMillis();
		System.out.println("====================MyBaseDao getMetadata, Consume " + ((t2 - t1) / 1000) + "s====================");

		return columns;
	}
}
