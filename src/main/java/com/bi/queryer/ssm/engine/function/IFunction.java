package com.bi.queryer.ssm.engine.function;

import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.function.impl.model.QueryProcess;
import com.bi.queryer.ssm.engine.session.QuerySessionProperty;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日期函数
 * @author contributor
 *
 */
public interface IFunction {
	Map<String, String> CLUSTER_UI_TOKEN_MAP = new ConcurrentHashMap<>();

	public static final String Format_DateTime = "yyyy-MM-dd hh24:mi:ss";

	public static final String Format_Date = "yyyy-MM-dd";

	/**
	 * 农历日期格式化
	 */
	public static final String Format_Lunar_Date = "L-yyyy-MM-dd";

	public static final String Format_Week = "yyyyww";

	public static final String Format_Month = "yyyyMM";

	public static final String Format_Quarter = "q";

	public static final String Format_Year = "yyyy";

	public static final String Format_Boolean = "boolean";

	public static final String Format_Map = "map";

	public String date2Char(String field, String format);

	public String parseDate(String field, String format);

	public String formatDate(String field, String format);

	public String mask(String field, String mask);

	public String coalesce(List<String> values);

	String completionRate(String currentValue, String targetValue, String isPositiveValue);

	public default String formatBoolean(Object value, String format){
		if(!Format_Boolean.equalsIgnoreCase(format)){
			return value + "";
		}
		String booleanValue = value + "";
		if("1".equals(booleanValue)){
			booleanValue = "是";
		}else if("0".equals(booleanValue)){
			booleanValue = "否";
		}else if(BIConsts.NULL_VALUE.equals(booleanValue)){
			booleanValue = "其他";
		}
		return booleanValue;
	}

	public default String coalesce(String... values){
		List<String> valueList = Arrays.asList(values);
		return coalesce(valueList);
	}

	public default String appendLimit(String sql, Integer limitRow){
		String limit = " LIMIT " + limitRow;
		sql = sql + limit;
		return sql;
	}

	public default String appendPagination(String sql, Integer pageNum , Integer pageSize){
		StringBuilder newSql = new StringBuilder(sql);
		newSql.append(" LIMIT ").append((pageNum - 1) * pageSize).append(",").append(pageSize);
		return newSql.toString();
	}

	public default QueryProcess monitor(Connection conn, Statement stmt, QuerySettings querySettings){
		return new QueryProcess();
	}

	/**
	 * 数据库引擎实际查询的query id
	 * @param process
	 * @param sessionId
	 * @return
	 */
	public default String getDbEngineQueryId(QueryProcess process, String sessionId, DataSourceType dataSourceType){
		if(process == null){
			return "";
		}
		return process.getQueryId().get();
	}

	public default QueryProcess getQueryProcess(Connection conn, Statement stmt) throws SQLException {
		return new QueryProcess();
	}

	public default boolean killQuery(String sessionId, String message, QuerySettings querySettings, DataSourceType dataSourceType){
		return true;
	}

	public default String max(String value){
		if(BIUtil.isEmpty(value)) {
			return "null";
		}
		return "max(" + value + ")";
	}

	public default String ifExpression(String condition, String trueExpression, String falseExpression){
		if(BIUtil.isEmpty(condition)) {
			return "null";
		}
		return String.format("if(%s,%s,%s)", condition, trueExpression, falseExpression);
	}

	public default String addDate(String dateField, DateGranularity dg, Integer interval){
		return String.format("date_add('%s',%s,%s)", dg.toString().toLowerCase(), interval, dateField);
	}

	public default String tryCatch(String expression){
		return String.format("(%s)", expression);
	}

	public default String lower(String expression){
		return String.format("lower(%s)", expression);
	}

	public default String upper(String expression){
		return String.format("upper(%s)", expression);
	}

	public default String addWeek(String dateField, DateGranularity dg, Integer interval) {

		String formatStr = "";

		switch (dg) {
			case WEEK:
				formatStr = String.format("bi_add_week(%s,%s)", dateField, interval);
				break;
			case YEAR:
				formatStr = String.format("bi_get_year_week_id(%s,%s)", dateField, interval);
				break;
		}

		return formatStr;
	}

	public default String getYearWeekDay(String dateField, Integer interval) {
		String formatStr = String.format("bi_get_year_week_day(%s,%s)", dateField, interval);
		return formatStr;
	}

	public default String getWeekFullName(String field) {
		String formatStr = String.format("bi_get_week_fullname(%s)", field);
		return formatStr;
	}

	public default String addMonth(String dateField,DateGranularity dg, Integer interval) {
		String formatStr = String.format("bi_add_month(%s,'%s',%s)", dateField, dg.toString().toLowerCase(), interval);
		return formatStr;
	}

	public default String addQuarter(String dateField, DateGranularity dg, Integer interval) {
		String formatStr = String.format("bi_add_quarter('%s',%s,%s)",dg.toString().toLowerCase(), dateField, interval);
		return formatStr;
	}

	public default String abs(String value){
		return String.format("abs(%s)", value);
	}

	public default String decode(String field, String rule){
		return field;
	}

	public default void setSessionProperties(Connection conn, List<QuerySessionProperty> properties, String sessionId, String sql){};

	public default String appendHint(List<QuerySessionProperty> properties, QuerySettings querySettings, String sql){
		return sql;
	};

	/**
	 *
	 * @param molecule 分子
	 * @param denominator 分母
	 * @param resultIsZeroByDenominatorIsZero 若分母是0，则结果为0
	 * @return
	 */
	public default String division(String molecule, String denominator, boolean resultIsZeroByDenominatorIsZero){
		if(BIUtil.isEmpty(molecule) || BIUtil.isEmpty(denominator)) {
			return "";
		}
		if(resultIsZeroByDenominatorIsZero && Integer.valueOf(molecule) == 0) {
			return "0";
		}
		return String.format("%s/%s", molecule, denominator);
	}

	public default String groupingId(List<String> fields){
		// 注意：此处不用grouping原生函数，因为存在没有grouping sets场景下需要调用此函数，如：行列转置后需要重新计算grouping值
		//return String.format("grouping(%s)", BIUtil.listToStr(fields));
		// udf 存在性能问题，偶发集群宕机
		// String groupingIdExpression = String.format("bi_grouping_id(array(%s))", BIUtil.listToStr(fields));

		// 原始grouping_id 在 grouping sets((dim1)) + sum(case when dim1='x')场景下，查询报错
		// String groupingIdExpression = String.format("grouping_id(%s)", BIUtil.listToStr(fields));

		String groupingIdExpression = "";
		int size = fields.size();
		List<String> gropingItemValues = new ArrayList<>();
		for (int i = 0; i < size; i++){
			String field = fields.get(i);
			String groupingItemValue = String.format("if(%s is null , pow(2,%s), 0)", field, size - i - 1);
			gropingItemValues.add(groupingItemValue);
		}
		groupingIdExpression = BIUtil.listToStr(gropingItemValues, " + ");
		return groupingIdExpression;
	}

	public default String formatString(String formatStyle, String field){
		return field;
	}

	public default String castToString(String field){
		String expr = String.format("cast(%s as varchar)", field);
		return expr;
	}

	/**
	 * 是否是查询超时消息
	 * @param message
	 * @return
	 */
	public default boolean isQueryTimeoutMessage(String message){
		return false;
	}

	/**
	 * 是否是手动kill消息
	 * @param message
	 * @return
	 */
	public default boolean isManualKillMessage(String message){
		return false;
	}

	/**
	 * 是否是熔断消息
	 * @param message
	 * @return
	 */
	public default boolean isBlockMessage(String message){
		return false;
	}

	/**
	 * 将输入字段信息hash计算
	 * @param field
	 * @return
	 */
	public default String hash(String field){
		return field;
	}

	/**
	 * 获取日期进度
	 * @param dateGranularity
	 * @param dateFieldName
	 * @return
	 */
	public default String getDateProgress(String dateGranularity,String dateFieldName){
		String formatStr = String.format("bi_get_date_progress('%s',%s)", dateGranularity, dateFieldName);
		return formatStr;
	}

	public default String quote(String fieldName) {
		return String.format("`%s`", fieldName);
	}

	/**
	 * 获取剩余天数
	 * @param dateGranularity
	 * @param dateFieldName
	 * @return
	 */
	public default String getDateLeftDays(String dateGranularity,String dateFieldName){
		String formatStr = String.format("bi_get_left_days('%s',%s)", dateGranularity, dateFieldName);
		return formatStr;
	}

	/**
	 * 获取门店开业月份
	 * 完整表达式 bi_shop_open_month(substring(vf.dt, 1, 10), 'd', vd1.online_month)
	 * statsDateExpression 统计日期  substring(vf.dt, 1, 10)
	 * dateGranularity 日期粒度 d
	 * openMonthFieldFullName 门店开业月份字段名称 vd1.online_month
	 * @return
	 */
	public default String getShopOpenMonthExpression(String statsDateExpression,String dateGranularity,String openMonthFieldFullName) {

		String shopOpenMonthExpression = String.format("getShopOpenMonthExpression(%s,'%s',%s)",
				statsDateExpression,
				dateGranularity,
				openMonthFieldFullName);

		return shopOpenMonthExpression;
	}

	/**
	 * 获取日期进度
	 * @param startDate
	 * @param endDate
	 * @param currentDate
	 * @return
	 */
	public default String getDateRangeProgress(String startDate,String endDate,String currentDate){
		return String.format("get_date_range_progress(%s,%s,'%s')", startDate, endDate, currentDate);
	}

	/**
	 * 日期差值计算
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public default String getPromoDateDiff(String startDate,String endDate){
		return "";
	}

}
