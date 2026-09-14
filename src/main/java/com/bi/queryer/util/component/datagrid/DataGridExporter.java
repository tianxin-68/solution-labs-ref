package com.bi.queryer.util.component.datagrid;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.UpperCaseMap;
import com.bi.queryer.util.ZipUtil;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.datagrid.export.DataGridExportUtil;
import com.bi.queryer.util.component.datagrid.render.DataGridColumnRender;
import com.bi.queryer.util.component.datagrid.render.DataGridRender;
import com.bi.queryer.util.component.datasource.IDataSetProvider;
import com.bi.queryer.util.component.exception.ComponentException;
import com.bi.queryer.util.download.ExcelDataSet;
import com.bi.queryer.util.download.ExcelHeadBuilder;
import com.bi.queryer.util.download.FileDownloadInfo;
import com.bi.queryer.util.download.FileDownloadType;
import com.bi.queryer.util.download.FileDownloadUtil;
import com.bi.queryer.util.download.FileFieldItem;

/**
 * DataGrid导出类
 * 
 * @author contributor
 *
 */
public class DataGridExporter extends Thread {

	protected final Log log = LogFactory.getLog(getClass());

	// protected DataGrid grid = null;

	protected HttpServletRequest request = null;
	/**
	 * 导出文件名，不需要设置路径和后缀名
	 */
	protected String fileName = "";

	/**
	 * 文件格式类型
	 */
	protected FileDownloadType fileType = FileDownloadType.CSV;

//	protected FileDownloadService downloadService;

	// protected IDataGridDataSetProvider dataSetProvider = null;

	// protected Map dataSetQueryParam = null;

	protected FileDownloadInfo downloadInfo = null;

	/**
	 * 是否异步导出，默认异步导出
	 */
	protected boolean asynchronous = true;

	/**
	 * 输出流，用于同步导出
	 */
	protected OutputStream outStream = null;

	protected HttpServletResponse response = null;

	protected boolean formatData = true; // 格式数据,如果不格式化，可提高导出效率

	protected List<DataGrid> grids = null;

	protected boolean singleGrid = true; // 是否是单grid，用于标示

	protected String username;
	
	/**
	 * 支持同时导出多个grid到一个excel,每个grid一个sheet页。适用于少于5W记录！！！
	 * 
	 * @param grids
	 * @param request
	 * @param response
	 */
	public DataGridExporter(List<DataGrid> grids, HttpServletRequest request, HttpServletResponse response) {
		try {
			this.grids = grids;
			this.request = request;
			this.response = response;
			this.request.setCharacterEncoding("utf-8");
			// dataSetProvider = grid.getDataSetProvider();
			// dataSetQueryParam = grid.getDataSetQueryParam();
			// asynchronous =
			// "true".equalsIgnoreCase(dataSetQueryParam.get("asynchronous") +
			// "");
			if (grids == null || grids.isEmpty()) {
				throw new Exception("必须设置导出的grids");
			}
			DataGrid grid = grids.get(0);
			// asynchronous =
			// "true".equalsIgnoreCase(grid.getDataSetQueryParam().get("asynchronous")
			// + "");
			asynchronous = (grids.size() == 1); // 多个grid，只支持同步导出
			singleGrid = (grids.size() == 1);
			//ApplicationContext context = SpringServiceLocator.getInstance().getContext();
			//downloadService = (FileDownloadService) context.getBean("downloadService");
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
	}

	public DataGridExporter(DataGrid grid, HttpServletRequest request, HttpServletResponse response) {
		try {
			this.request = request;
			this.response = response;
			this.request.setCharacterEncoding("utf-8");
			singleGrid = true;
			this.grids = new ArrayList<DataGrid>();
			this.grids.add(grid);
			asynchronous = "true".equalsIgnoreCase(grid.getDataSetQueryParam().get("asynchronous") + "");
			//ApplicationContext context = SpringServiceLocator.getInstance().getContext();
			//downloadService = (FileDownloadService) context.getBean("downloadService");
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}

	}

	/**
	 * 推荐使用DataGridExporter(DataGrid grid, HttpServletRequest request,
	 * HttpServletResponse response)
	 * 
	 * @param grid
	 * @param request
	 */
	@Deprecated
	public DataGridExporter(DataGrid grid, HttpServletRequest request) {
		this(grid, request, null);
	}

	@Override
	public void run() {
		asyncExport();
	}

	@Override
	public synchronized void start() {
		boolean success = false;
		try {
			User user = BIUtil.getLocalUser();
			if (user != null){
				this.username = user.getName();
			}
			if (asynchronous) {
				super.start();// 另起线程处理
			} else {
				syncExport();// 主线程处理
			}
			success = true;
		}catch(Exception e){
			success = false;
			e.printStackTrace();
		}finally {
			// 通知ui更新导出状态
			DataGridExportUtil.doPost(this.request, success);
		}
	}

	/**
	 * 同步导出
	 */
	protected void syncExport() {
		try {
			response.setContentType("application/x-msdownload;charset=UTF-8");
			String name = new String(fileName.getBytes("UTF-8"), "ISO8859-1") + fileType.getExtName();
			response.setHeader("Content-disposition", "attachment; filename=\"" + name + "\"");
			if (outStream == null) {
				outStream = response.getOutputStream();
			}
			IDataSetProvider dataSetProvider = null;
			List<ExcelDataSet> exportDataSetList = new ArrayList<ExcelDataSet>();
			List<DataGridDataSetRow> rows = null;
			List<? extends JSONSerializable> dataList = null;
			DataGrid dg = null;
			for (DataGrid grid : grids) {
				setPagination(grid, null, null);
				dg = grid;
				dataSetProvider = grid.getDataSetProvider();
				DataGridRender render = new DataGridRender(grid);
				render.addColumnSortParameter(grid.getDataSetQueryParam());
				dataList = dataSetProvider.getDataSet(grid.getDataSetQueryParam());
				ExcelDataSet eds = null;
				if (formatData) {
					rows = render.buildDataSetRow(dataList, true);
					eds = new ExcelDataSet(grid.getName(), getFileFieldItems(grid), rows);
				} else {
					eds = new ExcelDataSet(grid.getName(), getFileFieldItems(grid), dataList);
				}
				exportDataSetList.add(eds);
			}
			if (singleGrid) {
				if (formatData) {
					// 创建文件
					FileDownloadUtil.createFile(outStream, getFileFieldItems(dg), rows, fileType, dg);
				} else {
					FileDownloadUtil.createFile(outStream, getFileFieldItems(dg), dataList, fileType, dg);
				}
			} else {// 导出多个Sheet
				FileDownloadUtil.createExcelXFile(outStream, exportDataSetList, dg);
			}
			outStream.flush();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 初始化异步导出，处理相关参数后response主线程直接返回
	 */
	public void initialize() {
		if (!asynchronous) {
			return;
		}
		if (downloadInfo == null) {
			createDownloadInfo();
		}
		// 先返回
		try {
			request.setCharacterEncoding("utf-8");
			response.setCharacterEncoding("utf-8");
			PrintWriter pw = getResponse().getWriter();
			JSONObject result = new JSONObject();
			// !!一定要添加这个参数
			result.put("flag", "ok");
			pw.print(result.toString());
			pw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 异步导出:只支持一个grid
	 */
	protected void asyncExport() throws ComponentException {
		User u = new User(username);
		FileDownloadInfo info = downloadInfo;
		if (info == null) {
			throw new ComponentException("导出文件对象为空，先调用初始化initialize()");
		}
		DataGrid grid = grids.get(0);
		IDataSetProvider dataSetProvider = grid.getDataSetProvider();
		Map dataSetQueryParam = grid.getDataSetQueryParam();
		int count = dataSetProvider.getDataSetTotalSize(dataSetQueryParam);
		if (count == 0) {
//			downloadService.update(info.getId(), info.getFilePath(), FileDownloadInfo.NO_DATA_FLAG, "查询无数据");
			return;
		}
		// 分页，每次查询5W条记录,每5W条为一个文件
		Integer currPageNo = 1; // 当前页号
		Integer prePageSize = BIConsts.Export_Page_Size;// 每页记录数
		Integer totalPageCount = ((count - 1) / prePageSize) + 1; // 总页数

		String fileAbsPath = info.getFileRealPath();

		String fileAbsPathPrefix = fileAbsPath.substring(0, fileAbsPath.lastIndexOf("."));
		String fileExtName = fileAbsPath.substring(fileAbsPath.lastIndexOf("."), fileAbsPath.length());

		boolean isSuccess = true;
		Long t1 = System.currentTimeMillis();
		String errorInfo = "";
		boolean needZip = (totalPageCount > 1); // 是否需要压缩
		try {
			File file = null;
			List<File> fileList = new ArrayList<File>();
			FileDownloadType downType = FileDownloadType.getType(fileExtName);
			for (int i = 1; i <= totalPageCount; i++) {
				if (needZip) {
					file = new File(fileAbsPathPrefix + "_" + i + fileExtName);
				} else {
					file = new File(fileAbsPathPrefix + fileExtName);
				}
				currPageNo = i;
				setPagination(grid, currPageNo, prePageSize);
				DataGridRender render = new DataGridRender(grid);
				render.addColumnSortParameter(dataSetQueryParam);
				List<? extends JSONSerializable> dataList = dataSetProvider.getDataSet(dataSetQueryParam);
				if (formatData) {
					List<DataGridDataSetRow> rows = render.buildDataSetRow(dataList);
					// 创建文件
					isSuccess = isSuccess && FileDownloadUtil.createFile(file, getFileFieldItems(grid), rows, downType);
				} else {
					// 创建文件
					isSuccess = isSuccess && FileDownloadUtil.createFile(file, getFileFieldItems(grid), dataList, downType);
				}

				String printInfo = "导出" + file.getName() + ":" + ((i - 1) * prePageSize + 1) + "~" + (i * prePageSize);
				System.out.println(printInfo);
				log.info(printInfo);
				fileList.add(file);
			}
			if (needZip) {
				File[] files = new File[fileList.size()];
				fileList.toArray(files);
				ZipUtil.zip(fileAbsPathPrefix + ".zip", files, true); // 压缩并删除源文件
			}
		} catch (Exception e) {
			e.printStackTrace();
			errorInfo = e.toString();
			log.error(e);
		} finally {
			String filePath = info.getFilePath();
			if (needZip) {
				filePath = filePath.substring(0, filePath.lastIndexOf(".")) + ".zip";
			}

			// 处理完后需要重置完成标示
			if (isSuccess) {
				Long t2 = System.currentTimeMillis();
				String successInfo = "共" + count + "条记录" + " 耗时:" + (t2 - t1) + "毫秒";
				log.info(successInfo);
//				downloadService.update(info.getId(), filePath, FileDownloadInfo.LOADED_FLAG, successInfo);
			} else {
				errorInfo = "文件生成错误：" + errorInfo;
				log.info(errorInfo);
				int len = errorInfo.length();
				if (len > 200)
					len = 200;
//				downloadService.update(info.getId(), filePath, FileDownloadInfo.ERROR_FLAG, errorInfo.substring(0, len) + "...");
			}
		}

	}
	
	/**
	 * 通过grid获取导出文件字段
	 * 
	 * @return
	 */
	public List<FileFieldItem> getFileFieldItems(DataGrid grid) {
		List<FileFieldItem> fields = new ArrayList<FileFieldItem>();
		DataGridColumn[] columns = grid.getColumns();
		if (columns == null) {
			return fields;
		}
		// 构建column，计算其宽度
		DataGridColumnRender columnBuilder = new DataGridColumnRender(grid);
		columnBuilder.build();
		
		int index = 0;
		for (DataGridColumn column : columns) {
			if (!column.isExportable() || column.isCheck()) {
				continue;
			}
			FileFieldItem field = new FileFieldItem();

			String fieldName = column.getField();
			if (formatData) {
				fieldName = DataGridColumnRender.escapeField(fieldName);
			}
			field.setName(fieldName);
			field.setDataType(column.getDataType());
			field.setTitle(getFieldPathTitle(grid, column));
			field.setOrder(index++);
			field.setWidth(column.getWidth());
			field.setAlarmTypes(column.getExportAlarmTypes());
			field.setMergeCell(column.isMergeCell());
			field.setAlign(column.getAlign());
			fields.add(field);
		}
		return fields;
	}

	public String getFieldPathTitle(DataGrid grid, DataGridColumn col) {
		DataGridColumnGroup grp = grid.getGroup(col.getGroup());
		List<String> names = new ArrayList<String>();
		String colTitle = parseTitle(col.getTitle());
		if (fileType == FileDownloadType.CSV) {
			return colTitle;
		}
		while (grp != null) {
			names.add(grp.getTitle());
			grp = grp.getParent();
		}
		String path = "";
		for (String name : names) {
			path = name + ExcelHeadBuilder.Split_Flag;
		}
		path = path + colTitle;
		return path;

	}

	/**
	 * 解析title，将html标签格式转化为导出格式
	 * 
	 * @param title
	 * @return
	 */
	protected String parseTitle(String title) {
		if (BIUtil.isEmpty(title))
			return "";
		String newTitle = title;
		String[] searchList = new String[] { "<br/>", "<BR/>", "&nbsp;" };
		char enter = 10;
		String[] replacementList = new String[] { String.valueOf(enter), String.valueOf(enter), " " };
		newTitle = BIUtil.replaceEach(title, searchList, replacementList);

		return newTitle;
	}

	/**
	 * 创建下载item
	 */
	protected FileDownloadInfo createDownloadInfo() {

		try {
			// 添加导出信息到导出表
			downloadInfo = new FileDownloadInfo();
			
			downloadInfo.setId(Guid.id());
			downloadInfo.setRmiServerIP(SC.v("rmi.ip.request"));
			downloadInfo.setFormatData(formatData);
			downloadInfo.setFileExtName(fileType.getExtName());
			downloadInfo.setAsync(asynchronous ? Enabled.YES.getId() : Enabled.NO.getId());
			
			FileDownloadUtil.setFileDownloadInfo(request, downloadInfo, fileName, fileType);
			User user = (User) request.getSession().getAttribute(BIConsts.SESSION_KEY_USER);
			if (user != null) {
				downloadInfo.setUserName(user.getName());
			}
			String funcCode = "";
			DataGrid grid = grids.get(0);
			Map dataSetQueryParam = grid.getDataSetQueryParam();
			if (dataSetQueryParam.get("menuId") != null) {
				funcCode = dataSetQueryParam.get("menuId") + "";
			}
			if (BIUtil.isEmpty(funcCode)) {
				funcCode = "dg_export";
			}
			downloadInfo.setFuncId(funcCode);
			downloadInfo.setFileInfo(dataSetQueryParam.get("exportFileInfo") + "");// 暂时写死

			// 添加导出信息
//			downloadService.add(downloadInfo);

			// 导出数据到服务器
			String realPath = request.getRealPath("/");
			String fileAbsPath = realPath.replaceAll("\\\\", "/") + downloadInfo.getFilePath();
			downloadInfo.setFileRealPath(fileAbsPath);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return downloadInfo;
	}

	/**
	 * 设置分页参数
	 * 
	 * @param currPageNo
	 * @param prePageSize
	 */
	protected void setPagination(DataGrid grid, Integer currPageNo, Integer prePageSize) {
		Map dataSetQueryParam = grid.getDataSetQueryParam();
		if (dataSetQueryParam != null) {
			if (currPageNo != null && prePageSize != null) {
				int pageRowLower = (currPageNo - 1) * prePageSize + 1;
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageRowLower, pageRowLower);
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageRowLower2, pageRowLower);
				
				int pageRowUpper = currPageNo * prePageSize;
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageRowUpper, pageRowUpper);
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageSize, prePageSize);
			} else {
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageRowLower, 0);
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageRowLower2, 0);
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageRowUpper, Integer.MAX_VALUE);
				dataSetQueryParam.put(ComponentConstants.DataGrid_Parameter_PageSize, Integer.MAX_VALUE);
			}
		}
	}
	
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param exportFileName
	 *            导出文件名，不需要路径和后缀名
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public FileDownloadType getFileType() {
		return fileType;
	}

	public void setFileType(FileDownloadType fileType) {
		this.fileType = fileType;
	}

	public FileDownloadInfo getDownloadInfo() {
		return downloadInfo;
	}

	public void setDownloadInfo(FileDownloadInfo downloadInfo) {
		this.downloadInfo = downloadInfo;
	}

	public boolean isAsynchronous() {
		return asynchronous;
	}

	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}

	public OutputStream getOutStream() {
		return outStream;
	}

	public void setOutStream(OutputStream outStream) {
		this.outStream = outStream;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public boolean isFormatData() {
		return formatData;
	}

	public void setFormatData(boolean formatData) {
		this.formatData = formatData;
	}

}
