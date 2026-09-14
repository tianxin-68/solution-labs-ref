package com.bi.queryer.util.component.datagrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.component.datagrid.alarm.Alarm;
import com.bi.queryer.util.component.exception.ComponentException;

/**
 * 预警解析器,解析为前台js可执行代码
 * @author contributor
 *
 */
public class DataGridAlarmParser {
	
	private DataGrid grid = null;
	
	public DataGridAlarmParser(DataGrid grid){
		this.grid = grid;
	}
	
	/**
	 * 解析
	 * @return
	 */
	public void parse(){
		DataGridColumn[] columns = grid.getColumns();

		for(DataGridColumn column : columns){
			Alarm[] alarms = column.getAlarms();
			if(alarms == null || alarms.length == 0){
				continue;
			}
			Arrays.sort(alarms);
			column.setAlarmExpression(parse(alarms, columns));
		}
	}
	
	/**
	 * [field1] + [field2] > value 
	 * 解析预警
	 * @param alarms 已排序后的预警
	 * @return
	 */
	protected String parse(Alarm[] alarms, DataGridColumn[] columns){
		StringBuilder sb = null;
		try{
			sb = new StringBuilder();
			sb.append("(false || fx(value,row,index){");
			sb.append(" var newValue = value;");
			for(int i = 0; i < alarms.length; i++){
				if(i > 0 ){
					sb.append(" else ");
				}
				sb.append("if(");
				sb.append(parseExpression(alarms[i], columns));
				sb.append(")").append("{");
				sb.append(" newValue = ").append("\"<span class='DataGrid_" + alarms[i].getType().toString() + "'>\"")
				.append("+").append("value").append("+").append("\"</span>\"").append(";");
				sb.append("}");
			}
			sb.append(" return newValue;");
			sb.append("})");
		}catch(Exception e){
			sb = new StringBuilder();
			e.printStackTrace();
		}
		return  sb.toString();
	}
	
	/**
	 * 解析表达式
	 * @param alarm
	 * @param columns
	 * @return
	 */
	protected String parseExpression(Alarm alarm, DataGridColumn[] columns){
		String expression = alarm.expression();
		if(BIUtil.isEmpty(expression)) return expression;
		String newExp = expression;
		
		for(DataGridColumn column : columns){
			String replacement = "_parse(row['"  + column.getField() + "'], '" + column.getValuePrefix() + "', '" + column.getValueSuffix() + "')";
			String regexp = "\\[\\s*" + column.getField() + "\\s*\\]";
			newExp = newExp.replaceAll(regexp , replacement);
		}
		
		// 表达式中不存在的字段
		boolean hasErrorField = false;
		try{
			String regExp = "\\[\\s*[^\\']*\\s*\\]";
			Pattern pattern = Pattern.compile(regExp);  
	        Matcher matcher = pattern.matcher(newExp);
	        hasErrorField = matcher.find();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(hasErrorField){
			throw new ComponentException("解析Column[" + alarm.getColumn() + "]预警表达式错误，存在错误字段。");
		}
		
		return newExp;
	}
	
	public static void main(String[] args) {
		List<DataGridColumn> columns = new ArrayList<DataGridColumn>();
		for(int i = 0; i < 10; i++){
			DataGridColumn col = new DataGridColumn();
			col.setField("field" + (i+1));
			columns.add(col);
		}
		
		String exp = "[field1] + [field2]/[field3] > (value+[field4])";
		exp = exp.toUpperCase();
		
		String newExp = exp;
		for(DataGridColumn column : columns){
			String replacement = "_parse(row['"  + column.getField() + "'], '" + column.getValuePrefix() + "', '" + column.getValueSuffix() + "')";
			newExp = newExp.replaceAll("\\[\\s*" + column.getField() + "\\s*\\]" , replacement);
		}
		
		
		String regExp = "\\[\\s*[^\\']*\\s*\\]";
		System.out.println(newExp);
		
		Pattern pattern = Pattern.compile(regExp);  
        Matcher matcher = pattern.matcher(newExp);
        System.out.println("metched:" +matcher.find()); 
		
		System.out.println(newExp.replaceAll(regExp, ""));
		/*
		int index = exp.indexOf("[", 0);
		int preIndex = 0;
		String appendStr = "_trimFieldValue(row['";
		String newExp = "";
		while(index != -1){
			newExp = newExp + exp.substring(preIndex, index);
			index = index + 1;
			preIndex = index;
			if(index >= (exp.length()  - 1)){
				break;
			}
			newExp = newExp + appendStr;
			index = exp.indexOf("[", index);
		}
		*/
		
		/*
		String newExp = exp.replaceAll("\\[", "_trimFieldValue(row['")
						   .replaceAll("\\]", "'])");
		*/
		
		/*
		String reg = "field1";
		Pattern pattern = Pattern.compile(reg);  
        Matcher matcher = pattern.matcher(exp);
        System.out.println(matcher.matches()); 
        */
		
		//System.out.println(newExp);
	}

	public DataGrid getGrid() {
		return grid;
	}

	public void setGrid(DataGrid grid) {
		this.grid = grid;
	}
}
