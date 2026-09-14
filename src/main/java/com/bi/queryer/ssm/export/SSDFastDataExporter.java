package com.bi.queryer.ssm.export;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.export.QueryExportEngine;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.ZipUtil;
import com.bi.queryer.util.download.FileDownloadType;
import com.csvreader.CsvWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 快速导出datagrid数据，用于处理无格式大数据量导出，支持csv和xlsx
 * ！！！注，此导出类仅用于报表设计器配置的datagrid
 * 导出步骤：
 * 1、先通过数据源获取sql
 * 2、通过jdbc接口获取列元数据，用于确定列的顺序和标题
 * 3、对于不可分页的数据源（presto）
 * 	  3.1 一次性查询所有记录
 *    3.2 每10W条记录存储一个文件
 * 4、对于可分页的数据源
 *    4.1 每次分页查询10W条记录
 *    4.2 每10W条记录存储一个文件
 * 5、对所有文件进行zip压缩返回到response输出流中
 * @author contributor
 *
 */
public class SSDFastDataExporter {
	protected HttpServletRequest request;

	protected HttpServletResponse response;

	protected QueryEngine engine;

	protected String fileName = "";

	protected FileDownloadType fileType = FileDownloadType.CSV;

	protected Integer totalSize = -1;

	protected OutputStream out = null;

	/**
	 * 单个文件最大记录数
	 */
	protected Integer singleFileMaxSize = 50000;

	/**
	 * 导出时的线程数
	 */
	protected Integer exportThreadCount = 5;

	/**
	 * 文件绝对路径前缀
	 */
	protected String fileAbsPathPrefix = "grid";

	/**
	 * 导出sql
	 */
	protected String exportSql = "";

	public SSDFastDataExporter(HttpServletRequest request, HttpServletResponse response, Integer totalSize, QueryEngine engine) {
		this.request = request;
		this.response = response;
		this.engine = engine;
		this.totalSize = totalSize;

		singleFileMaxSize = Integer.valueOf(SC.v("export.single.file.max.size",  "50000"));
	}

	public ResponseMessage export(Double maxSize, String exportSql) {
		boolean success = false;
		String exceptionFlag = "false";
		fileType = FileDownloadType.CSV; // 暂时只支持csv

		long t1 = System.currentTimeMillis();

		// 初始化
		this.init(exportSql);

		BigDecimal maxExportRows;
		if(maxSize!=null&&maxSize>0) {
			maxExportRows = new BigDecimal(maxSize);
		}else{
			maxExportRows = new BigDecimal(SC.v("ssm.export.rows","200"));
		}

		BigDecimal bigDecimal = maxExportRows.multiply(new BigDecimal(10000));
		BigDecimal total = new BigDecimal(totalSize);
		if(total.compareTo(bigDecimal)==1) {
			// 通知前端
			String name = SC.v("ssd.admin", "contributor(contributor)");
			String resMsgStr = "【多维分析】导出数据量已超过" + maxExportRows + "万条，无法正常导出。若导出更多数据，请邮件申请，具体申请内容和流程见多维分析常见问题QA中序号3：https://docs.qq.com/doc/DT09SYWRib0xVTVVF";
			ResponseMessage resMsg = new ResponseMessage(false, resMsgStr);
			return resMsg;
		}

		String resultFileName = exportCSV();

		InputStream is = null;
		try {
			response.setHeader("rptRows", String.valueOf(totalSize));//导出条数

			String name = fileName+ (resultFileName.contains(".zip") ? ".zip" : fileType.getExtName());
			response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(name, "utf-8"));

			// 文件输出
			is = new FileInputStream(new File(resultFileName));
			byte[] tempBytes = new byte[1024];
			int byteRead = 0;
			// 读入多个字节到字节数组中，byteread为一次读入的字节数
			while ((byteRead = is.read(tempBytes)) != -1) {
				out.write(tempBytes, 0, byteRead);
			}
			out.flush();
			success = true;


		}catch(Exception e) {
			e.printStackTrace();
			exceptionFlag = e.getMessage();
			success = false;

		} finally {
			try {
				response.setHeader("Exception", URLEncoder.encode(exceptionFlag,"UTF-8"));//是否报错
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		long t2 = System.currentTimeMillis();
		System.out.println("导出总耗时(毫秒)：" + (t2 - t1)  );

		return  new ResponseMessage();
	}

	/**
	 * 导出
	 */
	public ResponseMessage export(Double maxSize,Map<String, Object> params, String exportSql) {
		boolean success = false;
        String exceptionFlag = "false";
		fileType = FileDownloadType.CSV; // 暂时只支持csv

		long t1 = System.currentTimeMillis();

		// 初始化
		this.init(exportSql);

		BigDecimal maxExportRows;
		if(maxSize!=null&&maxSize>0) {
			maxExportRows = new BigDecimal(maxSize);
		}else{
			maxExportRows = new BigDecimal(SC.v("ssm.export.rows","200"));
		}

		BigDecimal bigDecimal = maxExportRows.multiply(new BigDecimal(10000));
		BigDecimal total = new BigDecimal(totalSize);
		if(total.compareTo(bigDecimal)==1) {
			// 通知前端
			String name = SC.v("ssd.admin", "contributor(contributor)");
			String resMsgStr = "【多维分析】导出数据量已超过" + maxExportRows + "万条，无法正常导出。若导出更多数据，请邮件申请，具体申请内容和流程见多维分析常见问题QA中序号3：https://docs.qq.com/doc/DT09SYWRib0xVTVVF";
			ResponseMessage resMsg = new ResponseMessage(false, resMsgStr);
			return resMsg;
		}

		InputStream is = null;
		try {

			String realFileNamePrefix = request.getRealPath("/").replaceAll("\\\\", "/") + "/download/" + fileName ;

			QueryExportEngine exporter = new QueryExportEngine(engine);
			exporter.export(realFileNamePrefix + ".csv", exportSql);

			String resultFileName = realFileNamePrefix + ".zip";

			ZipUtil.zip(resultFileName, realFileNamePrefix + ".csv", false);

            response.setHeader("rptRows", String.valueOf(totalSize));//导出条数

			String name = fileName+ (resultFileName.contains(".zip") ? ".zip" : fileType.getExtName());
			response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(name, "utf-8"));

			// 文件输出
			is = new FileInputStream(new File(resultFileName));
			byte[] tempBytes = new byte[1024];
			int byteRead = 0;
			// 读入多个字节到字节数组中，byteread为一次读入的字节数
			while ((byteRead = is.read(tempBytes)) != -1) {
				out.write(tempBytes, 0, byteRead);
			}
			out.flush();
			success = true;


		}catch(Exception e) {
			e.printStackTrace();
            exceptionFlag = e.getMessage();
			success = false;

		} finally {
            try {
                response.setHeader("Exception", URLEncoder.encode(exceptionFlag,"UTF-8"));//是否报错
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		long t2 = System.currentTimeMillis();
		System.out.println("导出总耗时(毫秒)：" + (t2 - t1)  );

		return  new ResponseMessage();
	}


	protected void init(String exportSql) {
		try {
			response.setContentType("application/x-msdownload;charset=UTF-8");
			if (out == null) {
				out = response.getOutputStream();
			}

			if(totalSize <= 0) {
				this.exportSql = exportSql;//engine.buildSql();
				totalSize = engine.getDataSetTotalSize(this.exportSql);
			}

		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 分隔的文件列表
	 * @return
	 */
	protected List<String> splitFileNames(){
		// 分页，每次查询10W条记录,每10W条为一个文件
		Integer prePageSize = this.singleFileMaxSize;// 每页记录数
		Integer totalPageCount = ((totalSize - 1) / prePageSize) + 1; // 总页数

		String realPath = request.getRealPath("/");
		fileAbsPathPrefix = realPath.replaceAll("\\\\", "/") + "/download/" + fileName;

		boolean needZip = (totalPageCount > 1); // 是否需要压缩
		List<String> fileNames = new ArrayList<String>();
		for (int i = 1; i <= totalPageCount; i++) {
			String fileName = "";
			if (needZip) {
				fileName = fileAbsPathPrefix + "_" + i + fileType.getExtName();
			} else {
				fileName = fileAbsPathPrefix + fileType.getExtName();
			}
			fileNames.add(fileName);
		}
		return fileNames;
	}

	protected String exportCSV() {
		String resultFileName = exportCSVByNoPagination();
		return resultFileName;
	}

	/**
	 * 不分页导出csv
	 * @return 文件名
	 */
	protected String exportCSVByNoPagination() {
		String resultFileName = "";  // 最终返回的文件名

		final List<String> fileNames = splitFileNames();
		if(fileNames == null || fileNames.isEmpty()) {
			return resultFileName;
		}

		// 线程数
		int threadCount = fileNames.size() <= exportThreadCount ? fileNames.size() :  exportThreadCount;
		Map<Integer, List<Integer>> threadProcessFiles = new HashMap<Integer, List<Integer>>(); // <线程索引,List<文件索引>>
		for(int i = 0 ; i < fileNames.size(); i++) {
			Integer threadId = i % threadCount;
			if(threadProcessFiles.containsKey(threadId)) {
				threadProcessFiles.get(threadId).add(i);
			}else {
				List<Integer> fileIndexes = new ArrayList<Integer>();
				fileIndexes.add(i);
				threadProcessFiles.put(threadId, fileIndexes);
			}
		}

		// 数据集
		final SSDFastDataset dataset = buildDataset(null);
		final List<String> titles = dataset.titles;
		final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
		for(int i = 0; i < threadCount; i++) {
			final List<Integer> exportFileIndexes = threadProcessFiles.get(i);
			ExecutorService executorService = Executors.newSingleThreadExecutor();

			executorService.execute(new Runnable() {
				@Override
				public void run() {
					for(Integer index : exportFileIndexes) {
						Integer fromIndex = index * singleFileMaxSize;
						Integer toIndex = (index + 1) * singleFileMaxSize;
						if(toIndex > totalSize) {
							toIndex = totalSize;
						}
						if(dataset.rows == null) {
							continue;
						}
						if(dataset.rows.size() < toIndex){
							toIndex = dataset.rows.size();
						}
						List<List<String>> rows = dataset.rows.subList(fromIndex, toIndex);
						exportCSV(fileNames.get(index), titles, rows);
					}
					// 子线程处理完后，通知主线程减少计数
					countDownLatch.countDown();
				}
			});
		}

		try {
			countDownLatch.await();
			resultFileName = this.getResultFileName(fileNames);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultFileName;
	}

	/**
	 * 获取结果文件名
	 * @param fileNames
	 * @return
	 */
	protected String getResultFileName(List<String> fileNames) {
		String resultFileName = "";  // 最终返回的文件名
		try {
			boolean needZip = fileNames.size() > 1;
			if (needZip) {
				File[] files = new File[fileNames.size()];
				for(int i = 0; i < files.length; i++) {
					files[i] = new File(fileNames.get(i));
				}
				resultFileName = fileAbsPathPrefix + ".zip";
				ZipUtil.zip(resultFileName, files, false); // 压缩并删除源文件
			}else {
				resultFileName = fileNames.get(0);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return resultFileName;
	}

	/**
	 * csv导出
	 */
	protected void exportCSV(String fileName, List<String> titles, List<List<String>> rows) {
		if(titles == null || titles.isEmpty() || rows == null ) {
			return;
		}
		long t1 = System.currentTimeMillis();
		OutputStream fileOut = null;
		CsvWriter csvWriter = null;
		try {
			String sysEncoding = System.getProperty("file.encoding");
			fileOut = new FileOutputStream(new File(fileName));
        	/*
			if ("UTF-8".equalsIgnoreCase(sysEncoding)) {
				fileOut.write(0xFEFF);
				fileOut.flush();
			}
			*/

			// 根据系统编码添加bom文件头，处理csv乱码问题
			byte[] uft8bom={(byte)0xef,(byte)0xbb,(byte)0xbf};
			fileOut.write(uft8bom);
			fileOut.flush();

			csvWriter = new CsvWriter(fileOut, ',', Charset.forName("UTF-8"));

			// 写表头
			String[] headers = new String[titles.size()];
			titles.toArray(headers);
			csvWriter.writeRecord(headers);

			// 写内容
			for(List<String> row : rows) {
				String[] content = new String[row.size()];
				row.toArray(content);
				csvWriter.writeRecord(content,true);
			}

			csvWriter.flush();
			fileOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(csvWriter != null) {
					csvWriter.close();
				}
				if(fileOut != null) {
					fileOut.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		long t2 = System.currentTimeMillis();
		System.out.println("导出单个文件【" + fileName + "】耗时(毫秒)：" + (t2 - t1)  );
	}

	/**
	 * 获取数据集
	 * @return
	 */
	protected SSDFastDataset buildDataset(Integer pageNum) {
		SSDFastDataset dataset = new SSDFastDataset();
		//String sql = engine.buildSql();

		if(StringUtil.isEmpty(exportSql)) {
			return dataset;
		}

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {

			conn = DBUtil.getConn(DBUtil.getDataSourceType().getKey());
			stmt = conn.createStatement();

			long t1 = System.currentTimeMillis();

			stmt.setFetchSize(20000);
			stmt.execute(exportSql);
			rs = stmt.getResultSet();

			long t2 = System.currentTimeMillis();
			System.out.println("查询SQL耗时(毫秒)：" + (t2 - t1)  );

			if(rs == null) {
				return dataset;
			}
			rs.setFetchSize(20000);
			// 元数据
			ResultSetMetaData metadata = rs.getMetaData();

			int columnCount = metadata.getColumnCount();

			List<String> titles = new ArrayList<String>();

			List<Integer> exportIndexes = new ArrayList<Integer>();
			Map<Integer, String> appendStringMap = new HashMap<>();

			//迭代元数据
			for (int i = 1; i <= columnCount; i++) {
				String columnName = metadata.getColumnLabel(i);
				String title = columnName;
				if(columnName.equalsIgnoreCase(BIConsts.ROW_TOTAL_COLUMN_CODE)) {
					// 行总计特殊处理
					title = AnalysisTotalType.ROW_TOTAL.getDesc();
					appendStringMap.put(i, "");
				}else {
					ResultDataSetColumn setColumn = this.getColumn(columnName);
					if(setColumn == null || !setColumn.isExportable()) {
						continue;
					}
					appendStringMap.put(i, setColumn.getExportAppendString() == null ? "" : setColumn.getExportAppendString());
					title = this.parseTitle(setColumn.getTitle());
				}
				exportIndexes.add(i);
				titles.add(title);
			}
			long t3 = System.currentTimeMillis();
			System.out.println("获取元数据耗时(毫秒)：" + (t3 - t2)  );

			//迭代结果集
			int titleLength = titles.size();
			Integer[] exportIndexArray = new Integer[exportIndexes.size()];
			exportIndexes.toArray(exportIndexArray);
			List<List<String>> rows = new ArrayList<List<String>>(totalSize);
			while(rs.next()){
				List<String> row = new ArrayList<String>(titleLength);
				for(Integer exportIndex : exportIndexArray) {
					Object value = rs.getObject(exportIndex);
					// 处理NaN数字，避免查询格式化后出现乱码
					if(value instanceof  Double) {
						Double d = (Double) value;
						if (Double.isNaN(d) || Double.isInfinite(d)) {
							value = null;
						}
					}
					row.add(value == null ? "" : appendStringMap.get(exportIndex) + value);
				}
				rows.add(row);
			}

			long t4 = System.currentTimeMillis();
			System.out.println("获取所有记录(" + rows.size() + ")耗时(毫秒)：" + (t4 - t3)  );

			dataset.titles = titles;
			dataset.rows = rows;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if(rs != null) {
					rs.close();
				}
				if(stmt != null) {
					stmt.close();
				}
				if(conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return dataset;
	}

	/**
	 * 通过列名获取列对象
	 * @param columnName
	 * @return
	 */
	protected ResultDataSetColumn getColumn(String columnName) {
		ResultDataSetColumn result = null;

		List<ResultDataSetColumn> columns = engine.buildColumns();
		if(BIUtil.isEmpty(columns)) {
			return result;
		}

		for(ResultDataSetColumn col : columns){
			if(col.getCode().equalsIgnoreCase(columnName)) {
				col.setExportable(true);
				return col;
			}
		}

		return result;
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

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public QueryEngine getEngine() {
		return engine;
	}

	public void setEngine(QueryEngine engine) {
		this.engine = engine;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public FileDownloadType getFileType() {
		return fileType;
	}

	public void setFileType(FileDownloadType fileType) {
		this.fileType = fileType;
	}

	public Integer getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(Integer totalSize) {
		this.totalSize = totalSize;
	}

}

/**
 * 数据集
 * @author contributor
 *
 */
class SSDFastDataset{
	protected List<String> titles = null;

	protected List<List<String>> rows = null;

	protected void clear() {
		if(titles != null) {
			titles.clear();
		}
		if(rows != null) {
			rows.clear();
		}
	}
}
