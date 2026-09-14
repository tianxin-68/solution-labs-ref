package com.bi.queryer.ssm.engine.function.impl;


import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.function.impl.model.QueryProcess;
import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.ssm.engine.session.QuerySessionManager;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.facebook.presto.jdbc.PrestoStatement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PrestoFunction implements IFunction {

	@Override
	public String date2Char(String field, String format) {

		String formatField = field;

		if (Format_Week.equalsIgnoreCase(format)) {
			formatField = String.format("bi_get_week_id(%s)", field);
			return formatField;
		}

		//季度日期处理
		if(Format_Quarter.equalsIgnoreCase(format)){
			formatField = String.format("concat(substring(%s, 1, 4),'-Q',cast(quarter(date_parse(REPLACE(SUBSTR(%s, 1, 7), '-', ''), '%%Y%%m')) as varchar))", field, field);
			return formatField;
		}

		int subLength = 0;
		if (Format_DateTime.equalsIgnoreCase(format)) {
			subLength = 19;
		}
		if (Format_Date.equalsIgnoreCase(format)) {
			subLength = 10;
		}
		if (Format_Month.equalsIgnoreCase(format)) {
			subLength = 7;
		}
		if (Format_Year.equalsIgnoreCase(format)) {
			subLength = 4;
		}

		formatField = "SUBSTR(" + field + ",1" + "," + subLength + ")";
		// 月份去掉中划线
		if (Format_Month.equalsIgnoreCase(format)) {
			formatField = "REPLACE(" + formatField + "," + "'-'" + ",'')";
		}

		return formatField;
	}

	public String appendPagination(String sql, Integer pageNum, Integer pageSize) {
		StringBuilder newSql = new StringBuilder(sql);
		newSql.append(" LIMIT ").append(pageSize * pageNum);
		return newSql.toString();
	}

	@Override
	public String parseDate(String field, String format) {
		String dbFormatStr = getDbFormatString(format);
		String formatField = field;
		formatField = "date_parse(" + field + ", '" + dbFormatStr + "')";
		return formatField;
	}

	@Override
	public String formatDate(String field, String format) {
		String dbFormatStr = getDbFormatString(format);
		String formatField = field;
		formatField = "date_format(" + field + ", '" + dbFormatStr + "')";
		return formatField;
	}

	@Override
	public String mask(String field, String mask) {
		String fieldFullName = "CASE WHEN " + field + " IS NULL OR " + field + " = '' THEN '' " +
				"WHEN LENGTH(" + field + ")<4 THEN CONCAT(SUBSTR(" + field + "," + "1, 1" + "),'" + mask + "') " +
				"ELSE CONCAT(SUBSTR(" + field + "," + "1, 3" + "),'" + mask + "') END";
		return fieldFullName;
	}

	@Override
	public String coalesce(List<String> values) {
		if (BIUtil.isEmpty(values)) {
			return "";
		}
		if (values.size() == 1) {
			return values.get(0);
		}
		String coalesce = "coalesce(" + BIUtil.listToStr(values) + ")";
		return coalesce;
	}


	//完成率
	//bi_completion_rate(currentValue, targetValue, precision , isPositiveValue) ->double
	//入参：
	//currentValue：当前值，数值类型
	//targetValue：目标值，数值类型
	// precision：精度（小数位），必填，integer类型（正整数）
	//isPositiveValue：是否是正向值，整型 1=正向，0=负向
	//
	//出参：
	//当前值/目标值
	//
	//示例：
	//bi_completion_rate(88,100,2,1) -> 0.88
	@Override
	public String completionRate(String currentValue, String targetValue, String isPositiveValue) {
		return "bi_completion_rate(" + currentValue + "," + targetValue + "," + "6" + "," + isPositiveValue + ")";
	}

	public QueryProcess getQueryProcess(Connection conn, Statement stmt, QuerySettings querySettings) throws SQLException {
		QueryProcess queryProcess = new QueryProcess();
		PrestoStatement ps = stmt.unwrap(PrestoStatement.class);
		ps.setProgressMonitor(queryStats -> {    //设置监听器（可选），可监听presto任务执行状况
			queryProcess.getQueryId().set(queryStats.getQueryId());//获取presto任务ID（可用该ID终止任务）
			queryProcess.getQueryState().set(queryStats.getState());

		});
		return queryProcess;
	}

	@Override
	public QueryProcess monitor(Connection conn,Statement stmt, QuerySettings querySettings) {
		QueryProcess queryProcess = new QueryProcess();
		try {
			Integer timeoutSec = querySettings.getQueryTimeoutSec();
			queryProcess = getQueryProcess(conn, stmt, querySettings);
			AtomicReference<String> queryId = queryProcess.getQueryId();
			AtomicReference<String> queryState = queryProcess.getQueryState();
			Integer interval = querySettings.getQueryTimeoutDetectionInterval();

			new Thread(new Runnable() {
				public void run() {
					int i = 0;
					//循环次数
					while (i < timeoutSec) {
						try {
							Thread.sleep(interval * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if ("FINISHED".equalsIgnoreCase(queryState.toString())) {
							return;
						}
						i = i + interval;
					}
					if (queryId != null && queryState != null && !"FINISHED".equalsIgnoreCase(queryState.toString())) {
						killQueryById(queryId.toString(), String.format(BIConsts.SSM_ERROR_TIMEOUT, timeoutSec / 60), querySettings);
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queryProcess;
	}

	/**
	 * 通过session id kill
	 * @param sessionId
	 * @param killMessage
	 * @param querySettings
	 * @return
	 */
	public boolean killQuery(String sessionId, String killMessage, QuerySettings querySettings, DataSourceType defaultDataSourceType) {
		if(BIUtil.isEmpty(sessionId)) {
			return true;
		}

		// 通过sessionId获取queryId
        QuerySession session = QuerySessionManager.get(sessionId);
        if (session == null) {
            return true;
        }

        String queryId = session.getQueryId();

        DataSourceType dataSourceType = DataSourceType.Trino_Master;
        if(BIUtil.isNotEmpty(session.getDsKey())) {
            dataSourceType = DataSourceType.getType(session.getDsKey());
        }
        if(dataSourceType == null){
            dataSourceType = DataSourceType.Trino_Master;
        }
		querySettings.setQueryDatasourceKey(dataSourceType.getKey());

		return killQueryById(queryId, killMessage, querySettings);
	}

	/**
	 * 通过query id  kill
	 * @param queryId
	 * @param killMessage
	 * @param querySettings
	 * @return
	 */
	public boolean killQueryById(String queryId, String killMessage, QuerySettings querySettings){
		DataSourceType dataSourceType = DataSourceType.Trino_Master;
		if(BIUtil.isNotEmpty(querySettings.getQueryDatasourceKey())) {
			dataSourceType = DataSourceType.getType(querySettings.getQueryDatasourceKey());
		}
		if(BIUtil.isEmpty(queryId)) {
			return true;
		}
		Statement stmt = null;
		Connection conn = null;
		try {
			conn = DBUtil.getConn(dataSourceType);
			stmt = conn.createStatement();
			stmt.execute("CALL system.runtime.kill_query(query_id => '" + queryId + "', message => '" + killMessage + "')");
			return true;
		} catch (Exception e) {
			System.out.println(killMessage + "：" + e.getMessage());
			return false;
		} finally {
			try {
				if (stmt != null) stmt.close();
				if (conn != null) conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String tryCatch(String expression) {
		return String.format("try(%s)", expression);
	}

	protected String getDbFormatString(String format) {
		String dbFormatStr = "";
		if (Format_DateTime.equalsIgnoreCase(format)) {
			dbFormatStr = "%Y-%m-%d %H:%i:%S";
		}
		if (Format_Date.equalsIgnoreCase(format)) {
			dbFormatStr = "%Y-%m-%d";
		}
		if (Format_Month.equalsIgnoreCase(format)) {
			dbFormatStr = "%Y%m";
		}
		if (Format_Year.equalsIgnoreCase(format)) {
			dbFormatStr = "%Y";
		}

		return dbFormatStr;
	}

	@Override
	public String decode(String field, String rule) {
		return String.format("bi_decode(%s, '%s')", field, rule);
	}

}
