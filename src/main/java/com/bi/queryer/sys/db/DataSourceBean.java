package com.bi.queryer.sys.db;

import com.bi.queryer.ssm.mgr.exportImport.SSDExcel;
import com.bi.queryer.sys.base.BaseModel;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

/**
 * 数据源bean构建器
 */
public class DataSourceBean extends BaseModel implements JSONSerializable {

	@SSDExcel(order = 1)
	private String name;

	@SSDExcel(order = 2)
	private String title;

	@SSDExcel(order = 3)
	private String dbType;

	@SSDExcel(order = 4)
	private String dbIp;

	@SSDExcel(order = 5)
	private String dbPort;

	@SSDExcel(order = 6)
	private String dbName;

	@SSDExcel(order = 7)
	private String dbShortName;

	@SSDExcel(order = 8)
	private String dbUser;

	@SSDExcel(order = 9)
	private String dbPassword;

	@SSDExcel(order = 10)
	private String description;

	private String id;
	private String key;

	/**
	 * 是否改变了密码
	 */
	private Integer isChangePwd;

	/**
	 * 应用平台
	 */
	private String dbPlatform;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDbType() {
		return dbType;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public String getDbIp() {
		return dbIp;
	}

	public void setDbIp(String dbIp) {
		this.dbIp = dbIp;
	}

	public String getDbPort() {
		return dbPort;
	}

	public void setDbPort(String dbPort) {
		this.dbPort = dbPort;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getDbUser() {
		return dbUser;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getDbShortName() {
		return dbShortName;
	}

	public void setDbShortName(String dbShortName) {
		this.dbShortName = dbShortName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Integer getIsChangePwd() {
		return isChangePwd;
	}

	public void setIsChangePwd(Integer isChangePwd) {
		this.isChangePwd = isChangePwd;
	}

	public String getDbPlatform() {
		return dbPlatform;
	}

	public void setDbPlatform(String dbPlatform) {
		this.dbPlatform = dbPlatform;
	}

	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}
}
