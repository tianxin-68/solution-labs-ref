package com.bi.queryer.util.component.datagrid.export;

import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.bi.queryer.util.download.FileFieldItem;

/**
 * 导出附加信息
 * @author contributor
 *
 */
public interface IExportAppender {
	
	/**
	 * 写title
	 * @param dataset 导出数据集
	 * @param fieldItems 导出字段
	 * @return 行数
	 */
	public int writeTitle(Workbook workbook, Sheet sheet, int currentRowIndex, Map<String, FileFieldItem> fieldItems);
	
	/**
	 * 写备注
	 * @param dataset 导出数据集
	 * @param fieldItems 导出字段
	 * @return 行数
	 */
	public int writeRemark(Workbook workbook, Sheet sheet, int currentRowIndex, Map<String, FileFieldItem> fieldItemMap);
}
