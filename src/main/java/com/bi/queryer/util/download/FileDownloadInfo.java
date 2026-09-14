package com.bi.queryer.util.download;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.StringUtil;

/**
 * 文件下载信息表
 * @author contributor
 *
 */
public class FileDownloadInfo implements Serializable{
	
	private static final long serialVersionUID = 1L;

	/**
	 * request的下载列表对应的属性key
	 */
	public static final String REQUEST_DOWNLOAD_KEY = "__req_download_list";
	
	/**
	 * 文件存放目录
	 */
	public static final String FILE_DIR = "bi_download";
	
	public static final int LOADING_FLAG = 0;// 正在加载
	public static final int LOADED_FLAG = 1;// 加载完毕
	public static final int ERROR_FLAG = 2;// 加载错误
	public static final int NO_DATA_FLAG = 3;// 加载无数据
	
	private String id;
	private String title;
	private String funcId;
	private String funcName;
	private String userId;
	private String userName;
	private String fileInfo ;
	private String filePath;
	protected Date beginTime;
	protected Date endTime; 
	private Integer success = Enabled.NO.getId();
	private String serverBaseUrl;
	private String remark;
	private String fileRealPath = "";
	
	private boolean formatData = true; // 格式化数据
	private String  fileExtName = FileDownloadType.CSV.getExtName();
	private String rmiServerIP = "";
	private Integer async = Enabled.NO.getId();
	private Integer rows = 0;
	
	public JSONObject toJSON(){
		JSONObject obj = JSONObject.parseObject(JSONObject.toJSONString(this));
		return obj;
	}
	
	@Override
	public String toString() {
		return "id:" + id + "\t title:" + title + "\t userName:" + userName;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getFuncId() {
		if(StringUtil.isEmpty(funcId)){
			return funcName;
		}
		return funcId;
	}
	public void setFuncId(String funcId) {
		this.funcId = funcId;
	}
	public String getUserId() {
		if(StringUtil.isEmpty(userId)){
			return userName;
		}
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getFileInfo() {
		return fileInfo;
	}
	public void setFileInfo(String fileInfo) {
		this.fileInfo = fileInfo;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getServerBaseUrl() {
		return serverBaseUrl;
	}
	public void setServerBaseUrl(String serverBaseUrl) {
		this.serverBaseUrl = serverBaseUrl;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		if(remark == null){
			remark = "";
		}
		this.remark = remark;
	}
	public String getFuncName() {
		if(null == funcName) {
			funcName = "";
		}
		return funcName;
	}
	public void setFuncName(String funcName) {
		this.funcName = funcName;
	}
	public String getUserName() {
		if(null == userName){
			userName = "";
		}
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFileRealPath() {
		return fileRealPath;
	}

	public void setFileRealPath(String fileRealPath) {
		this.fileRealPath = fileRealPath;
	}

	public boolean isFormatData() {
		return formatData;
	}

	public void setFormatData(boolean formatData) {
		this.formatData = formatData;
	}

	public String getFileExtName() {
		return fileExtName;
	}

	public void setFileExtName(String fileExtName) {
		this.fileExtName = fileExtName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRmiServerIP() {
		return rmiServerIP;
	}

	public void setRmiServerIP(String rmiServerIP) {
		this.rmiServerIP = rmiServerIP;
	}

	public Integer getAsync() {
		return async;
	}

	public void setAsync(Integer async) {
		this.async = async;
	}

	public Integer getRows() {
		return rows;
	}

	public void setRows(Integer rows) {
		this.rows = rows;
	}

	public Date getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Integer getSuccess() {
		return success;
	}

	public void setSuccess(Integer success) {
		this.success = success;
	}

}
