package com.bi.queryer.ssm.engine.function.impl;


import com.bi.queryer.ssm.engine.function.IFunction;

import java.util.List;

public class SQLServerFunction implements IFunction {

	@Override
	public String date2Char(String field, String format) {
		String formatField = field;
		int subLength = 0;
		if(Format_DateTime.equalsIgnoreCase(format)){
			subLength = 19;
		}
		if(Format_Date.equalsIgnoreCase(format)){
			subLength = 10;
		}
		if(Format_Month.equalsIgnoreCase(format)){
			subLength = 7;
		}
		if(Format_Year.equalsIgnoreCase(format)){
			subLength = 4;
		}
		
		formatField = "CONVERT(varchar(" + subLength + ")," + field + ",121)" ;
		// 月份去掉中划线
		if(Format_Month.equalsIgnoreCase(format)){
			formatField = "REPLACE(" + formatField + "," + "'-','')";
		}
		return formatField;
	}

	@Override
	public String parseDate(String field, String format) {

		return field;
	}

	@Override
	public String formatDate(String field, String format) {
		return field;
	}

	@Override
	public String mask(String field, String mask) {
		String fieldFullName = "CASE WHEN " + field + " IS NULL OR " + field + " = '' THEN '' " +
				"WHEN DATALENGTH(" + field + ")<7 THEN CONCAT(SUBSTRING(" + field + "," + "1, 1" + "),'" + mask + "') " +
				"ELSE CONCAT(SUBSTRING(" + field + "," + "1, 3" + "),'" + mask + "') END";

		return fieldFullName;
	}

	@Override
	public String coalesce(List<String> values) {
		return null;
	}

	@Override
	public String completionRate(String currentValue, String targetValue, String isPositiveValue) {
		return null;
	}

	@Override
	public String appendLimit(String sql, Integer limitRow) {
		String limit = " TOP " + limitRow + " ";
		StringBuffer tempSql = new StringBuffer(sql);
		Integer offset = sql.indexOf("SELECT");
		tempSql = tempSql.insert(offset + 7,limit);
		sql = tempSql.toString();
		return sql;
	}

	public String appendPagination(String sql, Integer pageNum , Integer pageSize){
		StringBuilder newSql = new StringBuilder(sql);
		newSql.append(" Offset ").append((pageNum - 1) * pageSize).append(" rows ");
		newSql.append(" Fetch next ").append(pageSize).append(" rows only");
		return newSql.toString();
	}
}
