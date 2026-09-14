package com.bi.queryer.ssm.export;

import au.com.bytecode.opencsv.CSVWriter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.export.log.SSDExportLogEntity;
import com.bi.queryer.ssm.export.log.SSDExportLogService;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.download.FileDownloadType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * 多维分析导出类(同步导出)
 * 
 * @author contributor
 *
 */
public class SSDExporter {
	protected QueryConfigure config = null;

	protected String fileName = "多维分析";
	
	protected Integer configId = -1;
	/**
	 * 文件格式类型
	 */
	protected FileDownloadType fileType = FileDownloadType.CSV;

	/**
	 * 输出流，用于同步导出
	 */
	protected OutputStream outStream = null;
	
	/**
	 * 导出日志
	 */
	protected SSDExportLogEntity log = new SSDExportLogEntity();

	public SSDExporter() {
	}
	
	public SSDExporter(QueryConfigure config) {
		this.config = config;
	}

	public void export(HttpServletRequest request, HttpServletResponse response) throws BIException {
		try {
			if(StringUtil.isEmpty(fileName)) fileName = "多维分析";
			response.setContentType("application/x-msdownload;charset=UTF-8");
			String name = new String(fileName.getBytes("GB2312"), "ISO8859-1") + fileType.getExtName();
			log.setFileExt(fileType.getExtName());
			log.setFileName(fileName);
			response.setHeader("Content-disposition", "attachment; filename=\"" + name + "\"");
			if (outStream == null) {
				outStream = response.getOutputStream();
			}
			
			log.setBeginTime(Calendar.getInstance().getTime());
			export(outStream, request);
			log.setEndTime(Calendar.getInstance().getTime());
		} catch (Exception e1) {
			log.setSuccess(Enabled.NO.getId());
			log.setInfo(e1.getMessage());
			e1.printStackTrace();
		}
		
		// 记录日志
		SSDExportLogService logService = (SSDExportLogService) SpringContextUtil.getBean("SSDExportLogService");
		logService.log(log);
	}

	/**
	 * 导出数据到输出流中
	 * 
	 * @param outStream
	 */
	public void export(OutputStream outStream, HttpServletRequest request) throws BIException {
		CSVWriter writer = null;
		Writer oswriter = null;
		Statement stmt = null;
		ResultSet res = null;
		Connection conn = null;
		BIException biException = null;
		if(config == null){
			throw new BIException("模板配置为空");
		}
		try {
			String sysEncoding = System.getProperty("file.encoding");
			// System.out.println("file.encoding:" + sysEncoding);
			oswriter = new OutputStreamWriter(outStream);
			// 根据系统编码添加bom文件头，处理csv乱码问题
			if ("UTF-8".equalsIgnoreCase(sysEncoding)) {
				oswriter.write(0xFEFF);
				oswriter.flush();
				writer = new CSVWriter(oswriter, ',');
			} else {
				writer = new CSVWriter(oswriter, ',');
			}

			QueryEngine engine = new QueryEngine(config, new QueryContext(request));

			Long t1 = System.currentTimeMillis();
			System.out.println("====================SSD Export Data Begin");
			// 获取查询字段
			List<QueryField> configResultFields = config.getResult().getFields();
//			engine.setSameFieldAlias(configResultFields);

			List<QueryField> resultFields = new ArrayList<QueryField>();
			for(QueryField field : configResultFields){
				if(field.isAppend()) { // 附加字段不显示
					continue;
				}
				resultFields.add(field);
			}

			int colCount = resultFields.size(); // 总列数
			
			// 写表头
			String[] fieldTitles = new String[colCount];
			for (int i = 0; i < colCount; i++) {
				fieldTitles[i] = resultFields.get(i).getMeta().getTitle();
			}
			writer.writeNext(fieldTitles);

			// 写表体
			conn = DBUtil.getConn(DBUtil.getDataSourceType());
			stmt = conn.createStatement();
			
			String sql = engine.buildSql();
			res = stmt.executeQuery(sql);
			String values[] = null;
			int rowIndex = 0;
			while (res.next()) {
				rowIndex++;
				values = new String[colCount];
				for (int i = 0; i < colCount; i++) {
					Object valueObj = res.getObject((i+1));
					if (valueObj == null) {
						values[i] = "";
					} else {
						values[i] = String.valueOf(valueObj);
					}
				}
				writer.writeNext(values);
			}
			Long t2 = System.currentTimeMillis();
			System.out.println("====================SSD Export Data End, Consume " + ((t2 - t1) / 1000) + "s");
			writer.flush();
			log.setRows(rowIndex);
			log.setSuccess(Enabled.YES.getId());
			log.setSuccess(1);
		} catch (Throwable e) {
			biException = new BIException(e);
		} finally {
			try {
				if (res != null) {
					res.close();
				}
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
				if (null != writer) {
					writer.close();
				}
				if (null != oswriter) {
					oswriter.close();
				}
			} catch (Exception e) {
				biException = new BIException(e);
			}
		}
		
		if(biException != null){
			log.setSuccess(0);
			throw biException;
		}
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public QueryConfigure getConfig() {
		return config;
	}

	public void setConfig(QueryConfigure config) {
		this.config = config;
	}

	public FileDownloadType getFileType() {
		return fileType;
	}

	public void setFileType(FileDownloadType fileType) {
		this.fileType = fileType;
	}

	public OutputStream getOutStream() {
		return outStream;
	}

	public void setOutStream(OutputStream outStream) {
		this.outStream = outStream;
	}
}
