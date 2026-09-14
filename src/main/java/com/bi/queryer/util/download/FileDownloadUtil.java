package com.bi.queryer.util.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.Component;

import au.com.bytecode.opencsv.CSVWriter;

public abstract class FileDownloadUtil {

	protected final static Log log = LogFactory.getLog(FileDownloadUtil.class.getClass());

	/**
	 * 设置下载信息
	 * 
	 * @param info
	 * @param request
	 */
	public static void setFileDownloadInfo(HttpServletRequest request, FileDownloadInfo info, String fileShortName, FileDownloadType fileType) {
		String ip = info.getRmiServerIP();
		if(StringUtil.isEmpty(ip)){
			ip = request.getServerName();
			try {
				ip = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		// String serverBaseUrl = "http://" + ip + ":" + request.getServerPort()
		// + request.getContextPath() + "/";
		String serverBaseUrl = "http://" + ip + ":8080" + request.getContextPath() + "/";
		info.setServerBaseUrl(serverBaseUrl);

		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

		Calendar c = Calendar.getInstance();
		if(StringUtils.isEmpty(fileShortName)) fileShortName = "file";
		String fileName = fileShortName + "_" + df.format(c.getTime());
		String filePath = fileName + fileType.getExtName();
		info.setFilePath(filePath);

		info.setTitle(fileName);
		info.setBeginTime(Calendar.getInstance().getTime());
	}

	/**
	 * 创建文件,将数据写到文件
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(File file, Class<?> clazz, List<? extends JSONSerializable> dataList, FileDownloadType fileType) {
		return createFile(file, clazz, dataList, fileType, null);
	}
	
	/**
	 * 创建文件
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(File file, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList, FileDownloadType fileType) {
		boolean isSuccess = false;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			isSuccess = createFile(out, fields, dataList, fileType);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error(e);
				}
			}
		}
		
		return isSuccess;
	}
	
	/**
	 * 创建文件
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(File file, Class<?> clazz, List<? extends JSONSerializable> dataList, FileDownloadType fileType, String[] excludeFields) {
		boolean isSuccess = false;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			isSuccess = createFile(out, clazz, dataList, fileType, excludeFields);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error(e);
				}
			}
		}
		
		return isSuccess;
	}
	
	
	/**
	 * 创建文件，数据写到out流中
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(OutputStream out, Class<?> clazz, List<? extends JSONSerializable> dataList, FileDownloadType fileType) {
		return createFile(out, clazz, dataList, fileType, null);
	}
	
	/**
	 * 创建文件，数据写到out流中
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(OutputStream out, Class<?> clazz, List<? extends JSONSerializable> dataList, FileDownloadType fileType, String[] excludeFields, Component component) {
		boolean isSuccess = false;
		switch (fileType) {
		case Excel03:
			isSuccess = createExcelFile(out, clazz, dataList, excludeFields, component);
			break;
		case Excel07:
			isSuccess = createExcelXFile(out, clazz, dataList, excludeFields, component);
			break;
		case TEXT:
			isSuccess = createTextFile(out, clazz, dataList, excludeFields);
			break;
		case CSV:
		default:
			isSuccess = createCSVFile(out, clazz, dataList, excludeFields);
			break;
		}
		return isSuccess;
	}
	
	/**
	 * 创建文件，数据写到out流中
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(OutputStream out, Class<?> clazz, List<? extends JSONSerializable> dataList, FileDownloadType fileType, String[] excludeFields) {
		boolean isSuccess = false;
		switch (fileType) {
		case Excel03:
			isSuccess = createExcelFile(out, clazz, dataList, excludeFields, null);
			break;
		case Excel07:
			isSuccess = createExcelXFile(out, clazz, dataList, excludeFields, null);
			break;
		case TEXT:
			isSuccess = createTextFile(out, clazz, dataList, excludeFields);
			break;
		case CSV:
		default:
			isSuccess = createCSVFile(out, clazz, dataList, excludeFields);
			break;
		}
		return isSuccess;
	}
	
	/**
	 * 创建文件，数据写到out流中
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(OutputStream out, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList, FileDownloadType fileType, Component component) {
		boolean isSuccess = false;
		switch (fileType) {
		case Excel03:
			isSuccess = createExcelFile(out, fields, dataList, null, component);
			break;
		case Excel07:
			isSuccess = createExcelXFile(out, fields, dataList,null, component);
			break;
		case TEXT:
			isSuccess = createTextFile(out, fields, dataList);
			break;
		case CSV:
		default:
			isSuccess = createCSVFile(out, fields, dataList);
			break;
		}
		return isSuccess;
	}
	
	/**
	 * 创建文件，数据写到out流中
	 * @param clazz
	 * @param dataList
	 * @param fileType
	 * @return
	 */
	public static boolean createFile(OutputStream out, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList, FileDownloadType fileType) {
		return createFile(out, fields, dataList, fileType, null);
	}

	/**
	 * 创建excel文件
	 * 
	 * @param os
	 * @param clazz
	 * @param dataList
	 * @return
	 */
	public static boolean createExcelFile(OutputStream os, Class<?> clazz, List<? extends JSONSerializable> dataList, String[] excludeFields, Component component) {
		try {
			ExcelWriter excelWriter = new ExcelWriter(FileDownloadType.Excel03);
			excelWriter.setExcludeFields(excludeFields);
			excelWriter.setComponent(component);
			excelWriter.write(os, clazz, dataList);
		}catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 创建excel文件
	 * 
	 * @param os
	 * @param clazz
	 * @param dataList
	 * @return
	 */
	public static boolean createExcelFile(OutputStream os, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList, String[] excludeFields, Component component) {
		try {
			ExcelWriter excelWriter = new ExcelWriter(FileDownloadType.Excel03);
			excelWriter.setExcludeFields(excludeFields);
			excelWriter.setComponent(component);
			excelWriter.write(os, fields, dataList);
		}catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 创建excel文件
	 * 
	 * @param os
	 * @param clazz
	 * @param dataList
	 * @return
	 */
	public static boolean createExcelFile(OutputStream os, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList) {
		return createExcelFile(os, fields, dataList, null, null);
	}
	
	/**
	 * 创建excel文件
	 * 
	 * @param os
	 * @param clazz
	 * @param dataList
	 * @return
	 */
	public static boolean createExcelFile(OutputStream os, Class<?> clazz, List<? extends JSONSerializable> dataList) {
		return createExcelFile(os, clazz, dataList, null, null);
	}
	
	/**
	 * 创建excel文件
	 * 
	 * @param os
	 * @param clazz
	 * @param dataList
	 * @return
	 */
	public static boolean createExcelXFile(OutputStream os, Class<?> clazz, List<? extends JSONSerializable> dataList, String[] excludeFields, Component component) {
		try {
			ExcelWriter excelWriter = new ExcelWriter(FileDownloadType.Excel07);
			excelWriter.setExcludeFields(excludeFields);
			excelWriter.setComponent(component);
			excelWriter.write(os, clazz, dataList);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 创建excel文件
	 * 
	 * @param os
	 * @param clazz
	 * @param dataList
	 * @return
	 */
	public static boolean createExcelXFile(OutputStream os, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList, String[] excludeFields , Component component) {
		try {
			ExcelWriter excelWriter = new ExcelWriter(FileDownloadType.Excel07);
			excelWriter.setExcludeFields(excludeFields);
			excelWriter.setComponent(component);
			excelWriter.write(os, fields, dataList);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean createExcelXFile(OutputStream os, Class<?> clazz, List<? extends JSONSerializable> dataList) {
		return createExcelXFile(os, clazz, dataList,null, null);
	}
	
	/**
	 * 支持多sheet
	 * @param os
	 * @param clazz
	 * @param dataList
	 * @return
	 */
	public static boolean createExcelXFile(OutputStream os, List<ExcelDataSet> datasetList, Component component) {
		try {
			ExcelWriter excelWriter = new ExcelWriter(FileDownloadType.Excel07);
			excelWriter.setComponent(component);
			excelWriter.write(os, datasetList);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean createExcelXFile(OutputStream os, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList) {
		return createExcelXFile(os, fields, dataList,null, null);
	}
	
	public static boolean createCSVFile(OutputStream out, Class<?> clazz, List<? extends JSONSerializable> dataList) {
		return createCSVFile(out, clazz, dataList, null);
	}
	
	public static boolean createCSVFile(OutputStream out, Class<?> clazz, List<? extends JSONSerializable> dataList, String[] excludeFields) {
		FileFieldParser parser = new FileFieldParser(clazz);
		parser.setExcludeFields(excludeFields);
		List<FileFieldItem> fields = parser.getFileFieldItems();
		return createCSVFile(out, fields, dataList);
	}
	
	public static boolean createCSVFile(OutputStream out, List<FileFieldItem> fields, List<? extends JSONSerializable> dataList) {
		CSVWriter writer = null;
		Writer oswriter = null;
		try {
			String sysEncoding = System.getProperty("file.encoding");
			//System.out.println("file.encoding:" + sysEncoding);
			FileFieldParser parser = new FileFieldParser(null);
			parser.parse(fields);
			String[] fieldNames = parser.getFieldNames();
			String[] fieldTitles = parser.getFieldTitles();
			
			oswriter =  new OutputStreamWriter(out);
			// 根据系统编码添加bom文件头，处理csv乱码问题
			if ("UTF-8".equalsIgnoreCase(sysEncoding)) {
				oswriter.write(0xFEFF);
				oswriter.flush();
				writer = new CSVWriter(oswriter, ',');
			} else {
				writer = new CSVWriter(oswriter, ',');
			}
			
			JSONObject jsonObj = null;
			writer.writeNext(fieldTitles);
			
			// 写数据
			String values[] = null;
			for (JSONSerializable item : dataList) {
				values = new String[fieldNames.length];
				jsonObj = item.toJSON();
				for (int i = 0; i < fieldNames.length; i++) {
					values[i] = jsonObj.getString(fieldNames[i]);
					if (values[i] == null) {
						values[i] = "";
					}else{
						//values[i] = "\t" + values[i];
						// values[i] = values[i];
					}
				}
				writer.writeNext(values);
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			log.error(e);
			return false;
		} finally {
			try {
				if (null != writer) {
					writer.close();
				}
				if (null != oswriter) {
					oswriter.close();
				}
			} catch (Exception e) {
				log.error(e, e);
				return false;
			}
		}
		return true;
	}

	public static boolean createTextFile(OutputStream out, Class<?> clazz, List<? extends JSONSerializable> dataList,  String[] excludeFields) {
		return createCSVFile(out, clazz, dataList, excludeFields);
	}
	public static boolean createTextFile(OutputStream out, Class<?> clazz, List<? extends JSONSerializable> dataList) {
		return createTextFile(out, clazz, dataList,null);
	}
	
	public static boolean createTextFile(OutputStream out, List<FileFieldItem> items, List<? extends JSONSerializable> dataList) {
		return createTextFile(out, items, dataList);
	}

	public static void main(String[] args) {
	}
}
