package com.bi.queryer.ssm.engine.function.impl;

import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.enums.DateGranularity;

import java.util.List;

public class MySQLFunction implements IFunction {

	protected String getDbFormatString(String format){
		String dbFormatStr = "";
		if(Format_DateTime.equalsIgnoreCase(format)){
			dbFormatStr = "%Y-%m-%d %H:%i:%S";
		}
		if(Format_Date.equalsIgnoreCase(format)){
			dbFormatStr = "%Y-%m-%d";
		}
		if(Format_Month.equalsIgnoreCase(format)){
			dbFormatStr = "%Y%m";
		}
		if(Format_Year.equalsIgnoreCase(format)){
			dbFormatStr = "%Y";
		}

		return dbFormatStr;
	}

	@Override
	public String date2Char(String field, String format) {
		String formatField = field;
		String dbFormatStr = getDbFormatString(format);
		formatField = "DATE_FORMAT(" + field + ", '" + dbFormatStr + "')";
		return formatField;
	}

	@Override
	public String parseDate(String field, String format) {
		String dbFormatStr = getDbFormatString(format);
		String formatField = field;
		formatField = "STR_TO_DATE(" + field + ", '" + dbFormatStr + "')";
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
	public String addDate(String dateField, DateGranularity dg, Integer interval) {
		return String.format("date_add(%s, interval %s %s)", dateField, interval, dg.toString());
	}

	@Override
	public String coalesce(List<String> values) {
		return null;
	}

	@Override
	public String completionRate(String currentValue, String targetValue, String isPositiveValue) {
		return null;
	}
}
