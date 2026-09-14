package com.bi.queryer.util.component.datagrid.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridAlarmParser;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.DataGridColumnGroup;
import com.bi.queryer.util.component.datagrid.alarm.Alarm;
import com.bi.queryer.util.component.datagrid.alarm.AlarmType;

/**
 * 列构建器
 * <br/>
 * 根据datagrid配置的列及其分组进行构造列属性
 */
public class DataGridColumnRender {
	
	/**
	 * 特殊字符
	 */
	public static final String SPECIAL_CHARS[] =  new String[]{"+","(" , ")" , "[", "]", "{", "}"};
	
	/**
	 * render时替换特殊字符
	 */
	public static final String ESCAPE_CHARS[] = new String[]{"_","_","_","_","_","_","_"};
	
	/**
	 * 列的最小宽度
	 */
	public static final int minWidth = 90;
	
	protected DataGrid grid = null;
	
	/**
	 * 已创建的组
	 */
	protected Map<String, DataGridColumnGroup> createdGroups = new LinkedHashMap<String, DataGridColumnGroup>();
	
	protected List<DataGridColumn> frozenColumns = new ArrayList<DataGridColumn>();
	
	protected List<DataGridColumn> noFrozenColumns = new ArrayList<DataGridColumn>();
	
	protected Map<String,String> colNameMap = new HashMap();
	
	/**
	 *  冻结列的级次数
	 */
	protected int frozenLevelCount = 1;
	
	/**
	 * 非冻结列的级次数
	 */
	protected int noFrozenLevelCount = 1; 
	
	public DataGridColumnRender(DataGrid grid){
		this.grid = grid;
	}
	
	/**
	 * 构建前置步骤
	 */
	public void preBuild(){
		//动态解析数据列名
		parseColNames(grid);
		// 计算列分组深度
		calcColumnGroupsDepth();
		// 计算最大级别
		calcLevelCount();
		// 计算列宽
		calcColumnsWidth();
		// 计算列跨行跨列数据
		calcColumnSpan();
	}
	
	private void parseColNames(DataGrid datagrid) {
		List<String> expressions = new ArrayList<String>();
		List<DataGridColumn> cols = new ArrayList<DataGridColumn>();
		DataGridColumn[] columns = datagrid.getColumns();
		Map<String,String> infoMap = datagrid.getDataSetQueryParam();
		for(DataGridColumn col : columns){
			if(!StringUtil.isEmpty(col.getNameExpression())){
//				expressions.add(BIUtil.injectExpressionValue(col.getNameExpression(),infoMap,"#"));
				cols.add(col);
			}else{
				colNameMap.put(col.getField(), col.getTitle());
			}
		}
		List<DataGridColumnGroup> groups = datagrid.getGroups();
		for(DataGridColumnGroup grp : groups){
			if(!StringUtil.isEmpty(grp.getNameExpression())){
//				expressions.add(BIUtil.injectExpressionValue(grp.getNameExpression(),infoMap,"#"));
				cols.add(grp);
			}else{
				colNameMap.put(grp.getId(), grp.getTitle());
			}
		}	
		/*
		 * try{ List<String> rstList = null;//BIUtil.parseSql(expressions); for(int i =
		 * 0;i<rstList.size();i++){ if(!StringUtil.isEmpty(rstList.get(i))){
		 * if(cols.get(i) instanceof DataGridColumnGroup){
		 * colNameMap.put(cols.get(i).getId(), rstList.get(i)); }else{
		 * colNameMap.put(cols.get(i).getFieldByCode(), rstList.get(i)); }
		 * //cols.get(i).setTitle(rstList.get(i)); } } }catch(Exception e){
		 * e.printStackTrace(); }
		 */
	}	
	/**
	 * 构建列信息
	 * @return
	 */
	public StringBuilder build(){
		StringBuilder columnHtml = new StringBuilder();
		try{

			// 构建前置步骤
			preBuild();
			
			// 解析预警
			DataGridAlarmParser alarmParser = new DataGridAlarmParser(grid);
			alarmParser.parse();
			
			// 锁定列
			columnHtml.append("frozenColumns:").append(buildColumns(frozenColumns, frozenLevelCount)).append(",");
			
			// 非锁定列
			columnHtml.append("columns:").append(buildColumns(noFrozenColumns, noFrozenLevelCount));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return columnHtml;
	}
	
	/**
	 * 构建冻结列
	 */
	protected StringBuilder buildColumns(List<DataGridColumn> columns, int maxLevel){
		StringBuilder columnScripts = new StringBuilder();
		
		// 初始化
		Map<Integer, List<StringBuilder>> levelScripts = new LinkedHashMap<Integer, List<StringBuilder>>();
		for(int i = 0; i < (maxLevel + 1); i++) {
			levelScripts.put((i+1), new ArrayList<StringBuilder>());
		}
		
		//  
		for(DataGridColumn col : columns){
			DataGridColumnGroup grp = grid.getGroup(col.getGroup());
			if(grp != null){
			}
			while(grp != null) {
				if(!createdGroups.containsKey(grp.getId())) {
					levelScripts.get(grp.getLevel()).add(buildColumn(grp));
					createdGroups.put(grp.getId(), grp);
				}
				grp = grp.getParent();
			}
			levelScripts.get(col.getLevel()).add(buildColumn(col));
		}
		
		boolean isFirst = true;
		columnScripts.append("[");
		for(List<StringBuilder> list : levelScripts.values()) {
			if(!isFirst) {
				columnScripts.append(",");
			}else {
				isFirst = false;
			}
			columnScripts.append(list.toString());
		}
		columnScripts.append("]");
//System.out.println(columnScripts);
		return columnScripts;
	}
	
	/**
	 * 构建列
	 * @param isFrozen
	 * @return
	 */
	public StringBuilder buildColumns(boolean isFrozen){
		StringBuilder script = new StringBuilder();
		script.append("[[");
		String colsObj = "";
		for(DataGridColumn col : grid.getColumns()){
			if(isFrozen) {
				if(col.isFrozen()){
					colsObj = colsObj + "," + buildColumn(col);
				}
			}else {
				if(!col.isFrozen()){
					colsObj = colsObj + "," + buildColumn(col);
				}
			}
		}
		colsObj = colsObj.replaceFirst(",", "");
		script.append(colsObj);
		script.append("]]");
		return script;
	}
	
	/**
	 * 构建列
	 * @param column
	 * @return
	 */
	public StringBuilder buildColumn(DataGridColumn column) {
		
		JSONObject json = new JSONObject();
		json.put("formatter", column.getAlarmExpression());
		json.put("valueFormat", column.getValueFormat());
		json.put("valuePrefix", column.getValuePrefix());
		json.put("valueSuffix", column.getValueSuffix());
		json.put("isFrozen", column.isFrozen());
		json.put("field", escapeField(column.getField()));
		String key = column instanceof DataGridColumnGroup?column.getId():column.getField();
		String title = StringUtil.ifNull(colNameMap.get(key));
		if(StringUtil.isEmpty(title)){
			title = column.getTitle();
		}
		json.put("title", title);
		json.put("align", column.getAlign());
		json.put("halign", column.getHalign());
		json.put("sortable", column.isRemoteSortable());
		json.put("localSortable", column.isLocalSortable());
		json.put("dataType", column.getDataType().toString());
//		json.put("canExternalLink", column.isCanExternalLink());
		json.put("externalUrl", column.getExternalUrl());
		if(BIUtil.isNotEmpty(column.getDefaultSortType())){
			json.put("order", column.getDefaultSortType().toLowerCase());
		}
		json.put("hidden", column.isHidden());
		json.put("checkbox", column.isCheck());
		Integer defaultWidth = grid.getDefaultMinColumnWidth() <= 0 ? minWidth : grid.getDefaultMinColumnWidth(); 
		json.put("width", column.getWidth()<=0 ? defaultWidth : column.getWidth());
		if(column.getRowspan() >= 1) {
			json.put("rowspan", column.getRowspan());
		}
		if(column.getColspan() >= 1) {
			json.put("colspan", column.getColspan());
		}
		
		json.put("jsForamtter", column.getJsForamtter());
		json.put("canDrill", column.isCanDrill());
		json.put("mergeCell", column.isMergeCell());
		json.put("exportable", column.isExportable());
		json.put("exportFormat", column.isExportFormat());
		json.put("isMessager", column.isMessager());
		json.put("nullValue", column.getNullValue());
		json.put("valuePrefix", column.getValuePrefix());
		json.put("valueSuffix", column.getValueSuffix());
		json.put("isClob", column.isClob());
		json.put("enableAlarm", column.isEnableAlarm());
		
		String alarmTypeStr = "";
		JSONArray alarmJsonArray = new JSONArray();
		if(column.getAlarms() != null && column.getAlarms().length > 0) {
			Alarm[] alarms = column.getAlarms();
			for(Alarm alarm : alarms) {
				JSONObject alarmJo = new JSONObject();
				AlarmType alarmType = alarm.getType();
				String[] alarmTypeArray = alarmType.toString().split("_");
				if(!StringUtil.isEmpty(alarmTypeArray[1])) {
					alarmTypeStr = alarmTypeArray[1];
					alarmJo.put("type", alarmTypeArray[0]);
					alarmJo.put("expression", alarm.expression());
				}
				alarmJsonArray.add(alarmJo);
			}
		}
		json.put("alarmType", alarmTypeStr);
		json.put("expressions", alarmJsonArray);
		StringBuilder jsonObj = new StringBuilder();
		jsonObj.append(json.toString());
		return jsonObj;
	}
	
	/**
	 * 构建预警
	 * @param column
	 * @return
	 */
	protected JSONArray buildAlarms(DataGridColumn column){
		JSONArray json = new JSONArray();
		if(column.isFrozen()) return json;
		for(Alarm alarm : column.getAlarms()){
			JSONObject obj = new JSONObject();
			obj.put("type", alarm.getType());
			obj.put("exp", alarm.expression());
			json.add(obj);
		}
		return json;
	}
	
	/**
	 * 计算列宽度，处理datagrid大小自适应
	 * 根据datagrid总宽度设置各个列宽，保证所有列能填充满grid
	 */
	protected void calcColumnsWidth(){
		if(!grid.isAutoSize()) return;
		
		if(grid.getWidth() <= 0) return;
		
		DataGridColumn[] columns = grid.getColumns();
		int usedWidth = 0;
		int noneWidthColCount = 0;
		for(DataGridColumn column : columns){
			if(column.isHidden()) {
				continue;
			}
			if(column.getWidth() > 0) {
				usedWidth += column.getWidth();
			}else {
				noneWidthColCount++;
			}
		}
		int borderTotalWidth = 6;
		int rowNumColWidth = 20;
		if(grid.isShowRowNo()) {
			rowNumColWidth += 30;
		}
		int avaliableWidth = grid.getWidth() - usedWidth - borderTotalWidth - rowNumColWidth;
		if(avaliableWidth <= 0) return;
		int colWidth = 0;
		if(noneWidthColCount > 0) {
			colWidth = avaliableWidth / noneWidthColCount;
		}
		Integer defaultWidth = grid.getDefaultMinColumnWidth() <= 0 ? minWidth : grid.getDefaultMinColumnWidth(); 
		if(colWidth < defaultWidth) colWidth = defaultWidth;
		for(DataGridColumn column : columns){
			if(column.getWidth() < 0) {// 设置未设宽度的列
				column.setWidth(colWidth);
			}
		}
	}
	
	protected void calcColumnGroupsDepth(){
		for(DataGridColumnGroup group : grid.getGroups()){
			while(group != null){
				if(group.getChildren() != null && !group.getChildren().isEmpty()) {
					group.setDepth(group.getDepth() + 1);
					group = group.getChildren().get(0);
				}else {
					break;
				}
			}
		}
	}
	
	/**
	 * 计算最大级别
	 */
	protected void calcLevelCount(){
		for(DataGridColumn col : grid.getColumns()){
			if(col.isFrozen()) {
				frozenColumns.add(col);
			}else {
				noFrozenColumns.add(col);
			}
		}
		frozenLevelCount = calcLevelCount(frozenColumns);
		noFrozenLevelCount = calcLevelCount(noFrozenColumns);
	}
	
	/**
	 * 计算最大级别
	 * @param columns
	 */
	protected int calcLevelCount(List<DataGridColumn> columns){
		int levelCount = 0;
		for(DataGridColumn column : columns){
			DataGridColumnGroup group = grid.getGroup(column.getGroup());
			while(group != null){
				int depth = group.getDepth();
				if(levelCount < depth) {
					levelCount = depth;
				}
				group = group.getParent();
			}
		}
		return levelCount;
	}
	
	/**
	 * 计算列跨行跨列数据
	 */
	protected void calcColumnSpan(){
		int rowspan = 1;
		for(DataGridColumn column : grid.getColumns()){
			// 设置分组跨列
			DataGridColumnGroup group = grid.getGroup(column.getGroup());
			if(group != null) {
				column.setLevel(group.getLevel() + 1); // 设置列级别
			}
			while(group != null) {
				group.setColspan(group.getColspan() + 1);
				group = group.getParent();
			}
			
			// 设置字段跨行
			if(column.isFrozen()) {
				rowspan = frozenLevelCount;
			}else {
				rowspan = noFrozenLevelCount;
			}
			rowspan = (rowspan + 1) - column.getLevel() + 1;
			column.setRowspan(rowspan);
		}
	}
	
	public static void main(String[] args) {
		List<Integer> list = new ArrayList<Integer>();
		for(int i = 0; i < 5; i++) {
			list.add(0, i);
		}
		for(Integer i : list) {
			System.out.println(i);
		}
	}
	
	/**
	 * 转义字段名
	 * @param field
	 * @return
	 */
	public static String escapeField(String field){
		String f = field;
		f = BIUtil.replaceEach(field, SPECIAL_CHARS, ESCAPE_CHARS);
		return f;
	}

	public DataGrid getGrid() {
		return grid;
	}

	public void setGrid(DataGrid grid) {
		this.grid = grid;
	}

	public Map<String, DataGridColumnGroup> getCreatedGroups() {
		return createdGroups;
	}

	public void setCreatedGroups(Map<String, DataGridColumnGroup> createdGroups) {
		this.createdGroups = createdGroups;
	}

	public List<DataGridColumn> getFrozenColumns() {
		return frozenColumns;
	}

	public void setFrozenColumns(List<DataGridColumn> frozenColumns) {
		this.frozenColumns = frozenColumns;
	}

	public List<DataGridColumn> getNoFrozenColumns() {
		return noFrozenColumns;
	}

	public void setNoFrozenColumns(List<DataGridColumn> noFrozenColumns) {
		this.noFrozenColumns = noFrozenColumns;
	}

	public int getFrozenLevelCount() {
		return frozenLevelCount;
	}

	public void setFrozenLevelCount(int frozenLevelCount) {
		this.frozenLevelCount = frozenLevelCount;
	}

	public int getNoFrozenLevelCount() {
		return noFrozenLevelCount;
	}

	public void setNoFrozenLevelCount(int noFrozenLevelCount) {
		this.noFrozenLevelCount = noFrozenLevelCount;
	}

	public Map<String, String> getColNameMap() {
		return colNameMap;
	}

	public void setColNameMap(Map<String, String> colNameMap) {
		this.colNameMap = colNameMap;
	}
	
}
