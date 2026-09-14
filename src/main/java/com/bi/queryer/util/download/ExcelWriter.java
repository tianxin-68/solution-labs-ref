package com.bi.queryer.util.download;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.alarm.AlarmType;

/**
 * 复杂表头(多表头)通过字段名中^分割上级表头
 * @author contributor
 *
 */
public class ExcelWriter{
	protected final static Log log = LogFactory.getLog(ExcelWriter.class.getClass());
	
	public final static String defaultSheetName = "sheet1";

	protected Workbook workbook = null;
	
	//protected Sheet sheet = null;

	// 行索引
	protected int rowIndex = 0;

	// 列索引
	protected int colIndex = 0;

	protected int defaultColWidth = 100;

	// html像素转换为excel比例
	protected final double PIXEL_RATIO = 35.7;

	protected Map<String, Integer> colIndexMap = new HashMap<String, Integer>();
	
	protected String[] fieldNames = null;
	protected String[] fieldTitles = null;
	protected String [] excludeFields = null; // 排除的字段
//	protected Map<String, DataType> fieldDataTypes = new HashMap<String, DataType>();
	protected Map<String, FileFieldItem> fieldItemMap = new HashMap<String, FileFieldItem>();
	protected boolean multiSheet = false;
	protected List<ExcelDataSet> datasetList = null;
	private Component component = null;
	
	// 标题样式列表，提前创建样式，提示性能
	private Map<String, CellStyle> bodyCellStyles = new HashMap<String, CellStyle>(); 
	
	public ExcelWriter() {
		workbook = new SXSSFWorkbook(-1);

		// body单元格样式
		initBodyCellStyles();
	}

	public ExcelWriter(FileDownloadType fileType) {
		switch(fileType){
		case Excel03:
			workbook = new HSSFWorkbook();
			break;
		case Excel07:
		default:
			workbook = new SXSSFWorkbook(-1);
			break;
		}

		// body单元格样式
		initBodyCellStyles();
	}
	
	protected void initialize(){
		// 行索引
		rowIndex = 0;

		// 列索引
		colIndex = 0;
		colIndexMap.clear();
		fieldItemMap.clear();
	}
	
	
	/**
	 * 支持多sheet
	 * @param os
	 * @param datasetList
	 */
	public void write(OutputStream os, List<ExcelDataSet> datasetList) throws IOException{
		this.datasetList = datasetList;
		if(datasetList == null || datasetList.isEmpty()){
			return;
		}
		multiSheet = true;
		int index = 1;
		for(ExcelDataSet dataset : datasetList){
			initialize();
			String name = dataset.getName();
			if(StringUtils.isEmpty(name)){
				name = "sheet" + (index++);
			}
			write(os, dataset.getFields(), dataset.getDataList(), name);
		}
		workbook.write(os);
	}
	
	public void write(OutputStream os, Class<?> clazz, List<? extends JSONSerializable> dataList) throws IOException {
		String sheetName = defaultSheetName;
		if(this.component != null && !StringUtil.isEmpty(this.component.getName())) {
			sheetName = this.component.getName();
		}
		write(os, clazz, dataList, sheetName);
	}
	public void write(OutputStream os, Class<?> clazz, List<? extends JSONSerializable> dataList, String sheetName) throws IOException {
		FileFieldParser parser = new FileFieldParser(clazz);
		parser.setExcludeFields(excludeFields);
		parser.parse();
		fieldNames = parser.getFieldNames();
		fieldTitles = parser.getFieldTitles();
		fieldItemMap = parser.getFieldItemMap();
		write(os, fieldNames, fieldTitles, dataList, sheetName);
	}
	
	public void write(OutputStream os,List<FileFieldItem> fields, List<? extends JSONSerializable> dataList, String sheetName) throws IOException {
		FileFieldParser parser = new FileFieldParser(null);
		parser.parse(fields);
		fieldNames = parser.getFieldNames();
		fieldTitles = parser.getFieldTitles();
		fieldItemMap = parser.getFieldItemMap();
		write(os, fieldNames, fieldTitles, dataList, sheetName);
		
	}
	public void write(OutputStream os,List<FileFieldItem> fields, List<? extends JSONSerializable> dataList) throws IOException {
		String sheetName = defaultSheetName;
		if(this.component != null && !StringUtil.isEmpty(this.component.getName())) {
			sheetName = this.component.getName();
		}
		write(os, fields, dataList, sheetName);
	}
	
	public void write(OutputStream os, String[] fieldNames, String[] fieldTitles, List<? extends JSONSerializable> dataList) throws IOException {
		String sheetName = defaultSheetName;
		if(this.component != null && !StringUtil.isEmpty(this.component.getName())) {
			sheetName = this.component.getName();
		}
		write(os, fieldNames, fieldTitles, dataList, sheetName);
	}
	public void write(OutputStream os, String[] fieldNames, String[] fieldTitles, List<? extends JSONSerializable> dataList, String sheetName) throws IOException {
		try {
			if(StringUtils.isEmpty(sheetName)) sheetName = defaultSheetName;
			Sheet sheet = workbook.createSheet(sheetName);
			this.fieldNames = fieldNames;
			this.fieldTitles = fieldTitles;
			writeTitle(sheet);
			writeHead(sheet);
			writeBody(sheet, dataList);
			writeRemark(sheet);
			if(!multiSheet){
				workbook.write(os);
			}
		} catch (Exception e) {
			log.error("excel writer 错误:" + e.getMessage());
			e.printStackTrace();
			//throw e;
		}
	}

	/**
	 * 写标题
	 */
	protected void writeTitle(Sheet sheet) {
		if(component == null 
				|| !(component instanceof DataGrid)){
			return;
		}
		DataGrid grid = (DataGrid) component;
		if(grid.getExportAppender() != null) {
			rowIndex = grid.getExportAppender().writeTitle(workbook, sheet, rowIndex, fieldItemMap);
		}
	}
	
	/**
	 * 表头
	 */
	protected void writeHead(Sheet sheet) {
		int headRowCount = 1;// 表头行数
		CellStyle cellStyle = getHeadCellStyle(); // 表头样式
		
		ExcelHeadBuilder headBuilder = new ExcelHeadBuilder(this.fieldNames, this.fieldTitles);
		List<ExcelHeadCell> headCells = headBuilder.build();
		headRowCount = headBuilder.maxRowCount;
		int currentRowIndex = rowIndex; 
		for (int n = 0; n < headRowCount; n++) {
			Row row = sheet.createRow(rowIndex);
			rowIndex++;
			Cell cell = null;
			int colWidth = -1;
			
			for(int i = 0; i < fieldNames.length; i++){
				String fn = fieldNames[i];
				cell = row.createCell(i);
				cell.setCellValue("-");
				colWidth = defaultColWidth;
				if(fieldItemMap.get(fn).getWidth() > defaultColWidth) {
					colWidth = fieldItemMap.get(fn).getWidth();
				}
				sheet.setColumnWidth(i, (short) (PIXEL_RATIO * colWidth));
				cell.setCellStyle(cellStyle);
			}
			
			for(int i = 0; i < headCells.size(); i++){
				ExcelHeadCell headCell = headCells.get(i);
				if(headCell.rowIndex != n){
					continue;
				}
				if(!headCell.isGroup){
					colIndexMap.put(headCell.field, headCell.colIndex);
				}
				//合并单元格  
				int fromRow = (headCell.rowIndex + currentRowIndex);
				int toRow = (headCell.rowIndex + currentRowIndex) + headCell.rowSpan - 1;
				int fromCol = headCell.colIndex;
				int toCol = headCell.colIndex + headCell.colSpan - 1;
				CellRangeAddress cra = new CellRangeAddress(fromRow, toRow, fromCol, toCol);
				sheet.addMergedRegion(cra); 
				cell = row.getCell(headCell.colIndex);
				String cellValue = headCell.title;
				cell.setCellValue(cellValue);
			}
		}
		
		/*
		int headRowCount = 1;// 表头行数
		CellStyle cellStyle = getHeadCellStyle(); // 表头样式
		for (int n = 0; n < headRowCount; n++) {
			Row row = sheet.createRow(rowIndex);
			rowIndex++;
			Cell cell = null;
			int colWidth = -1;
			int colIndex = -1;
			for (int i = 0; i < fieldNames.length; i++) {
				colIndex++;
				colIndexMap.put(fieldNames[i], colIndex);
				cell = row.createCell(colIndex);
				String cellValue = fieldTitles[i];
				cell.setCellValue(cellValue);
				colWidth = defaultColWidth;
				sheet.setColumnWidth(colIndex, (short) (PIXEL_RATIO * colWidth));
				cell.setCellStyle(cellStyle);
			}
		}
		*/
	}
	
	/**
	 * 表体
	 */
	protected void writeBody(Sheet sheet, List<? extends JSONSerializable> dataList) {
		int size = dataList.size();
		Row row = null;
		int colSize = fieldNames.length;
		Cell cell = null;
		Object value = null;
		Integer colIndex = null;      
		JSONObject jsonObj = null;
		//用于单元格求和只算一个的要求
		List<JSONObject> newList = new ArrayList<JSONObject>();
		// 合并单元格
		if (size >= 1) {
			newList.add(dataList.get(0).toJSON());
			for (int i = 0; i < colSize; i++) {
				Boolean isFirst = true;
				FileFieldItem preFieldItem = null;
				for (int j = 0; j < size - 1; j++) {
					if(j+1>=newList.size()) 
					{
					jsonObj = dataList.get(j+1).toJSON();
					newList.add(jsonObj);
					}
					}
				if(i!=0) {
					preFieldItem = this.fieldItemMap.get(fieldNames[i-1]);
					isFirst = false;
				}
				FileFieldItem fieldItem = this.fieldItemMap.get(fieldNames[i]);
				if ((fieldItem.isMergeCell()&&isFirst)||(fieldItem.isMergeCell()&&preFieldItem.isMergeCell())) {
					colIndex = colIndexMap.get(fieldNames[i]);
					int fromRow = rowIndex;
					int toRow = rowIndex;
					int fromCol = colIndex;
					int toCol = colIndex;
					for (int j = 0; j < size - 1; j++) {
						Object preCurrentValue = "";
						Object preNextValue = "";
						Boolean preAllSame = true;
						//判断前面所有列前后两行是否相等，若有一列不等，则不能合并
						if(!isFirst) 
						{
						for(int k=0;k<i;k++) {
						 preCurrentValue = dataList.get(j).toJSON().get(fieldNames[k]);
						 preNextValue = dataList.get(j + 1).toJSON().get(fieldNames[k]);
						 if(preCurrentValue == null){
								preCurrentValue = "";
							}
						if(preNextValue == null){
								preNextValue = "";
							}
						if(!preCurrentValue.equals(preNextValue)) {
							preAllSame = false;
						}
						 }
						}
						Object currentValue = dataList.get(j).toJSON().get(fieldNames[i]);
						Object nextValue = dataList.get(j + 1).toJSON().get(fieldNames[i]);
						if(currentValue == null){
							currentValue = "";
						}
						if(nextValue == null){
							nextValue = "";
						}
						if (currentValue.equals(nextValue)&&preAllSame) {
							toRow++;
							if (toRow - rowIndex == size - 1) {
								CellRangeAddress cra = new CellRangeAddress(fromRow, toRow, fromCol, toCol);
								sheet.addMergedRegion(cra);
							}
							newList.get(j+1).put(fieldNames[i], null);
						} else {
							CellRangeAddress cra = new CellRangeAddress(fromRow, toRow, fromCol, toCol);
							sheet.addMergedRegion(cra);
							fromRow = toRow + 1;
							toRow = fromRow;
						}
					}
				}
			}
		}

		for(int i = 0; i < size; i++){
			row = sheet.createRow(rowIndex);
			rowIndex++;
			jsonObj = newList.get(i);
			for(int j = 0; j < colSize; j++){
				colIndex = colIndexMap.get(fieldNames[j]);
				if(colIndex == null){
					continue;
				}
				cell = row.createCell(colIndex);
				value = jsonObj.get(fieldNames[j]);
				if(value == null){
					value = "";
				}
				value = value.toString().replaceAll("&nbsp;", "  ");
				DataType dataType = fieldItemMap.get(fieldNames[j]).getDataType();
				String valueStr = value.toString();
				if(!valueStr.equals("%")){
					valueStr = valueStr.replaceAll("[\\%|\\,]", "");
				}
				if(!StringUtil.isEmpty(valueStr)){
					if(dataType.isInteger()){
						if(!StringUtil.isEmpty(valueStr)){
							Double d = Double.valueOf(valueStr);
							cell.setCellValue(d.intValue());
						}else{
							cell.setCellValue(valueStr);
						}
					}else if(dataType == DataType.Double){
						cell.setCellValue(Double.valueOf(valueStr));
					}else{
						cell.setCellValue(value.toString());
					}
				}else{
					cell.setCellValue("");
				}
				FileFieldItem fieldItem = this.fieldItemMap.get(fieldNames[j]);
				// 设置单元格样式
				this.setBodyCellStyle(cell, valueStr, fieldItem, i);
			}
		}
	}
	
	/**
	 * 设置body单元格样式
	 * @param cell
	 * @param valueStr
	 * @param fieldItem
	 * @param bodyRowIndex
	 */
	protected void setBodyCellStyle(Cell cell, String valueStr, FileFieldItem fieldItem, int bodyRowIndex){
		// 设置交替色
		CellStyle cellStyle = null;
		if(bodyRowIndex % 2 != 0) {
			cellStyle = bodyCellStyles.get("evenNormalCellStyle_"+fieldItem.getAlign());
		}else {
			cellStyle = bodyCellStyles.get("oddNormalCellStyle_"+fieldItem.getAlign());
		}
		
		// 告警
		if(!StringUtil.isEmpty(valueStr) &&
				fieldItem.getAlarmTypes() != null && !fieldItem.getAlarmTypes().isEmpty()) {
			CellStyle alarmCellStyle = this.getCellAlarmStyle(valueStr, fieldItem, bodyRowIndex);
			if(alarmCellStyle != null) {
				cellStyle = alarmCellStyle;
			}
		}
		
		// 合并之后的单元格居中显示
		if (fieldItem.isMergeCell()) {
//			cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);//居中
			cellStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);//垂直  
		}
		
		// 设置单元格样式
		if(cellStyle != null) {
			cell.setCellStyle(cellStyle);
		}
	}
	
	/**
	 * 设置单元格告警样式
	 * @param cell
	 * @param valueStr
	 * @param fieldItem
	 */
	protected CellStyle getCellAlarmStyle(String valueStr, FileFieldItem fieldItem, int bodyRowIndex) {
		CellStyle cellStyle = null;
		if(fieldItem == null) {
			return cellStyle ;
		}
		if(fieldItem.getAlarmTypes() == null || fieldItem.getAlarmTypes().isEmpty()) {
			return cellStyle;
		}
		// 目前暂时只支持当前值与0比较
		if(StringUtil.isEmpty(valueStr)) {
			return cellStyle;
		}
		
		valueStr = valueStr.replaceAll("[\\%|\\,]", "");
		try{
			Double doubleValue = Double.valueOf(valueStr);
			AlarmType lessThanZeroAramType = fieldItem.getAlarmTypes().get(0);
			if(doubleValue < 0) {
				cellStyle = this.getCellColorStyle(lessThanZeroAramType, bodyRowIndex,fieldItem);
			}
			AlarmType greaterThanZeroAramType = fieldItem.getAlarmTypes().get(2);
			if(doubleValue > 0) {
				cellStyle = this.getCellColorStyle(greaterThanZeroAramType, bodyRowIndex,fieldItem);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return cellStyle;
	}
	
	
	protected CellStyle getCellColorStyle(AlarmType aramType, int bodyRowIndex, FileFieldItem fieldItem){
		if(aramType == null) {
			return null;
		}
		if(bodyRowIndex % 2 != 0) {
			if((aramType.toString().toLowerCase().indexOf("red")) != -1) { // 红色
				return bodyCellStyles.get("evenRedCellStyle_"+fieldItem.getAlign());
			}
			if((aramType.toString().toLowerCase().indexOf("green")) != -1) { // 绿色
				return bodyCellStyles.get("evenGreenCellStyle_"+fieldItem.getAlign());
			}
		}else {
			if((aramType.toString().toLowerCase().indexOf("red")) != -1) { // 红色
				return bodyCellStyles.get("oddRedCellStyle_"+fieldItem.getAlign());
			}
			if((aramType.toString().toLowerCase().indexOf("green")) != -1) { // 绿色
				return bodyCellStyles.get("oddGreenCellStyle_"+fieldItem.getAlign());
			}
		}
		return null;
	}
	
	/**
	 * 写备注
	 * @param sheet
	 */
	public void writeRemark(Sheet sheet){
		if(component == null 
				|| !(component instanceof DataGrid)){
			return;
		}
		DataGrid grid = (DataGrid) component;
		if(grid.getExportAppender() != null) {
			rowIndex = grid.getExportAppender().writeRemark(workbook, sheet, rowIndex, fieldItemMap);
		}
	}

	// 表头单元格样式
	protected CellStyle getHeadCellStyle() {
		if(component != null && (component instanceof DataGrid)){
			DataGrid grid = (DataGrid) component;
			if(grid.getExportStyler() != null) {
				CellStyle cellStyle = grid.getExportStyler().getHeadCellStyle(workbook,fieldItemMap);
				if(cellStyle != null) {
					return cellStyle;
				}
			}
		}
		
		// 默认样式
		CellStyle cellStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		cellStyle.setFont(font);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);

		cellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(HSSFColor.BLACK.index);
		cellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(HSSFColor.BLACK.index);
		cellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(HSSFColor.BLACK.index);
		cellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(HSSFColor.BLACK.index);

		return cellStyle;
	}
	
	// 设置合并表头样式
	protected void setMergeHeadCellStyle(Sheet sheet, CellRangeAddress region , CellStyle cs){
		for (int i = region.getFirstRow(); i <= region.getLastRow(); i ++) {
            Row row = sheet.getRow(i);
            if(row == null) continue;
           // if(region.getFirstColumn() != region.getLastColumn()){
	            for (int j = region.getFirstColumn(); j <= region.getLastColumn(); j++) {
	                Cell cell = row.getCell((short)j);
	                if(cell != null){
	                	cell.setCellStyle(cs);
	                }
	            }
            //}
        }
	}
	

	/**
	 * 表体样式
	 * 
	 * @return
	 */
	protected CellStyle getBodyCellStyle() {
		if(component != null && (component instanceof DataGrid)){
			DataGrid grid = (DataGrid) component;
			if(grid.getExportStyler() != null) {
				CellStyle cellStyle = grid.getExportStyler().getBodyCellStyle(workbook,fieldItemMap);
				if(cellStyle != null) {
					return cellStyle;
				}
			}
		}
		
		// 默认样式
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(HSSFColor.BLACK.index);
		cellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(HSSFColor.BLACK.index);
		cellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(HSSFColor.BLACK.index);
		cellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(HSSFColor.BLACK.index);

		return cellStyle;
	}
	
	/**
	 * 初始化标题样式
	 */
	protected void initBodyCellStyles(){
		this.bodyCellStyles.clear();
		
		// 字体
		Font redFont = workbook.createFont();
		redFont.setColor(HSSFColor.RED.index);
		redFont.setFontName("宋体");
		
		Font greenFont = workbook.createFont();
		greenFont.setColor(HSSFColor.GREEN.index);
		greenFont.setFontName("宋体");
		
		// 颜色
		XSSFColor altBgColor = new XSSFColor(new java.awt.Color(244, 244, 244));
		
		/*******************************奇数行*****************************/
		// 默认奇数行单元格样式-正常-居中
		CellStyle oddNormalCenterCellStyle = this.getBodyCellStyle(); 
		oddNormalCenterCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddNormalCenterCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		oddNormalCenterCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddNormalCenterCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddNormalCenterCellStyle; 
			
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		bodyCellStyles.put("oddNormalCellStyle_center", oddNormalCenterCellStyle);
		
		// 默认奇数行单元格样式-正常-居左
		CellStyle oddNormalLeftCellStyle = this.getBodyCellStyle(); 
		oddNormalLeftCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddNormalLeftCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		oddNormalLeftCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddNormalLeftCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddNormalLeftCellStyle; 
			
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		bodyCellStyles.put("oddNormalCellStyle_left", oddNormalLeftCellStyle);
		
		// 默认奇数行单元格样式-正常-居右
		CellStyle oddNormalRightCellStyle = this.getBodyCellStyle(); 
		oddNormalRightCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddNormalRightCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		oddNormalRightCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddNormalRightCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddNormalRightCellStyle; 
			
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		bodyCellStyles.put("oddNormalCellStyle_right", oddNormalRightCellStyle);
		
		// 默认奇数行单元格样式-红-居中
		CellStyle oddRedCenterCellStyle = this.getBodyCellStyle(); 
		oddRedCenterCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddRedCenterCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		oddRedCenterCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddRedCenterCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddRedCenterCellStyle; 
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		oddRedCenterCellStyle.setFont(redFont);
		bodyCellStyles.put("oddRedCellStyle_center", oddRedCenterCellStyle);
		
		// 默认奇数行单元格样式-红-居左 
		CellStyle oddRedLeftCellStyle = this.getBodyCellStyle(); 
		oddRedLeftCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddRedLeftCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		oddRedLeftCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddRedLeftCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddRedLeftCellStyle; 
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		oddRedLeftCellStyle.setFont(redFont);
		bodyCellStyles.put("oddRedCellStyle_left", oddRedLeftCellStyle);
		
		// 默认奇数行单元格样式-红-居右
		CellStyle oddRedRightCellStyle = this.getBodyCellStyle(); 
		oddRedRightCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddRedRightCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		oddRedRightCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddRedRightCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddRedRightCellStyle; 
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		oddRedRightCellStyle.setFont(redFont);
		bodyCellStyles.put("oddRedCellStyle_right", oddRedRightCellStyle);
		
		// 默认奇数行单元格样式-绿-居中
		CellStyle oddGreenCenterCellStyle = this.getBodyCellStyle(); 
		oddGreenCenterCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddGreenCenterCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		oddGreenCenterCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddGreenCenterCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddGreenCenterCellStyle; 
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		oddGreenCenterCellStyle.setFont(greenFont);
		bodyCellStyles.put("oddGreenCellStyle_center", oddGreenCenterCellStyle);
		
		// 默认奇数行单元格样式-绿-居左
		CellStyle oddGreenLeftCellStyle = this.getBodyCellStyle(); 
		oddGreenLeftCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddGreenLeftCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		oddGreenLeftCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddGreenLeftCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddGreenLeftCellStyle; 
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		oddGreenLeftCellStyle.setFont(greenFont);
		bodyCellStyles.put("oddGreenCellStyle_left", oddGreenLeftCellStyle);
		
		// 默认奇数行单元格样式-绿-居右
		CellStyle oddGreenRightCellStyle = this.getBodyCellStyle(); 
		oddGreenRightCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		oddGreenRightCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		oddGreenRightCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		if((oddGreenRightCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) oddGreenRightCellStyle; 
			xsytle.setFillForegroundColor(HSSFColor.WHITE.index);
		}
		oddGreenRightCellStyle.setFont(greenFont);
		bodyCellStyles.put("oddGreenCellStyle_right", oddGreenRightCellStyle);
		
		/*******************************偶数行*****************************/
		
		// 默认偶数行单元格样式-正常-居中
		CellStyle evenNormalCenterCellStyle = this.getBodyCellStyle(); 
		evenNormalCenterCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenNormalCenterCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenNormalCenterCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		if((evenNormalCenterCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenNormalCenterCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		bodyCellStyles.put("evenNormalCellStyle_center", evenNormalCenterCellStyle);
		
		// 默认偶数行单元格样式-正常-居左
		CellStyle evenNormalLeftCellStyle = this.getBodyCellStyle(); 
		evenNormalLeftCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenNormalLeftCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenNormalLeftCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		if((evenNormalLeftCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenNormalLeftCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		bodyCellStyles.put("evenNormalCellStyle_left", evenNormalLeftCellStyle);
		
		// 默认偶数行单元格样式-正常-居右
		CellStyle evenNormalRightCellStyle = this.getBodyCellStyle(); 
		evenNormalRightCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenNormalRightCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenNormalRightCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		if((evenNormalRightCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenNormalRightCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		bodyCellStyles.put("evenNormalCellStyle_right", evenNormalRightCellStyle);
		
		// 默认偶数行单元格样式-红-居中
		CellStyle evenRedCenterCellStyle = this.getBodyCellStyle(); 
		evenRedCenterCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenRedCenterCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenRedCenterCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		if((evenRedCenterCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenRedCenterCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		evenRedCenterCellStyle.setFont(redFont);
		bodyCellStyles.put("evenRedCellStyle_center", evenRedCenterCellStyle);
		
		// 默认偶数行单元格样式-红-居左
		CellStyle evenRedLeftCellStyle = this.getBodyCellStyle(); 
		evenRedLeftCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenRedLeftCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenRedLeftCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		if((evenRedLeftCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenRedLeftCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		evenRedLeftCellStyle.setFont(redFont);
		bodyCellStyles.put("evenRedCellStyle_left", evenRedLeftCellStyle);
		
		// 默认偶数行单元格样式-红-居右
		CellStyle evenRedRightCellStyle = this.getBodyCellStyle(); 
		evenRedRightCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenRedRightCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenRedRightCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		if((evenRedRightCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenRedRightCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		evenRedRightCellStyle.setFont(redFont);
		bodyCellStyles.put("evenRedCellStyle_right", evenRedRightCellStyle);
		
		// 默认偶数行单元格样式-绿-居中
		CellStyle evenGreenCenterCellStyle = this.getBodyCellStyle(); 
		evenGreenCenterCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenGreenCenterCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenGreenCenterCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		if((evenGreenCenterCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenGreenCenterCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		evenGreenCenterCellStyle.setFont(greenFont);
		bodyCellStyles.put("evenGreenCellStyle_center", evenGreenCenterCellStyle);
		
		// 默认偶数行单元格样式-绿-居左
		CellStyle evenGreenLeftCellStyle = this.getBodyCellStyle(); 
		evenGreenLeftCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenGreenLeftCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenGreenLeftCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		if((evenGreenLeftCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenGreenLeftCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		evenGreenLeftCellStyle.setFont(greenFont);
		bodyCellStyles.put("evenGreenCellStyle_left", evenGreenLeftCellStyle);
		
		// 默认偶数行单元格样式-绿-居右
		CellStyle evenGreenRightCellStyle = this.getBodyCellStyle(); 
		evenGreenRightCellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		evenGreenRightCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		evenGreenRightCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		if((evenGreenRightCellStyle instanceof XSSFCellStyle)) {
			XSSFCellStyle xsytle = (XSSFCellStyle) evenGreenRightCellStyle; 
			xsytle.setFillForegroundColor(altBgColor);
		}
		evenGreenRightCellStyle.setFont(greenFont);
		bodyCellStyles.put("evenGreenCellStyle_right", evenGreenRightCellStyle);
	}
	
	public static void main(String[] args) {
		System.out.println("begin...");
	}

	public String[] getExcludeFields() {
		return excludeFields;
	}

	public void setExcludeFields(String[] excludeFields) {
		this.excludeFields = excludeFields;
	}

	public String[] getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

	public String[] getFieldTitles() {
		return fieldTitles;
	}

	public void setFieldTitles(String[] fieldTitles) {
		this.fieldTitles = fieldTitles;
	}

	public boolean isMultiSheet() {
		return multiSheet;
	}

	public void setMultiSheet(boolean multiSheet) {
		this.multiSheet = multiSheet;
	}

	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}
}
