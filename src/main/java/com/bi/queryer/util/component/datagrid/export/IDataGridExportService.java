package com.bi.queryer.util.component.datagrid.export;

import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.download.FileDownloadInfo;

/**
 * RMI导出服务接口
 * @author contributor
 *
 */
public interface IDataGridExportService {
	
	/**
	 * 导出
	 * @param grid grid表格对象
	 * @param invokerServerIP 调用方服务器ip，用于回调onFinished
	 */
	public void export(DataGrid grid, FileDownloadInfo fileInfo);
	
	public void onFinished(FileDownloadInfo fileInfo);
	
	public byte[] download(FileDownloadInfo fileInfo) throws Exception;
}
