package com.bi.queryer.util.component.datagrid.export;

import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

import com.bi.queryer.util.download.FileFieldItem;

/**
 * 导出样式
 * @author contributor
 *
 */
public interface IExportStyler {
	
	/**
	 * 表头样式
	 * @param fieldItems 导出字段信息
	 * @return
	 */
	public CellStyle getHeadCellStyle(Workbook workbook, Map<String, FileFieldItem> fieldItems);
	
	/**
	 * 表体样式
	 * @param fieldItems 导出字段信息
	 * @return
	 */
	public CellStyle getBodyCellStyle(Workbook workbook, Map<String, FileFieldItem> fieldItems);
}
