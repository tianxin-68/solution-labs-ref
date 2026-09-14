package com.bi.queryer.ssm.query.log;

import com.bi.queryer.ssm.engine.validator.QueryMessageType;
import com.bi.queryer.util.BIUtil;

import java.util.Date;

/**
 * 查询日志实体
 * @author contributor
 *
 */
public class SSDQueryLogEntity {
	protected String id;

	protected String templateId;

	protected String userName;

	protected String querySQL ;

	protected Date beginTime;

	protected Date endTime;

	protected Integer success = 1;

	protected String info;

	protected Integer rows = 0;

	protected String config = "";

	// 界面查询入参,方便定位
	private String queryParam = "";

	private String queryTables = null;

	// 查询环境
	private String env =  BIUtil.getRuntimeEnv().getCode();

	// 数据源key
	private String dsKey = null;

	private String querySource;

	private QueryMessageType messageType = QueryMessageType.SUCCESS;

	//查询会话id
	private String sessionId;

	private String uiBeginTime;

	private String uiEndTime;

	/**
	 * 查询视图id
	 */
	private String viewId;

	/**
	 * 数据库查询id（doris的query_id）
	 */
	private String dbQueryId;

	/**
	 * 查询过滤配置
	 */
	private String queryFilter;

	private String cacheType;

	public String getCacheType() {
		return cacheType;
	}

	public void setCacheType(String cacheType) {
		this.cacheType = cacheType;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getQuerySQL() {
		return querySQL;
	}

	public void setQuerySQL(String querySQL) {
		this.querySQL = querySQL;
	}

	public Date getBeginTime() {
		if(beginTime == null){
			beginTime = new Date();
		}
		return beginTime;
	}

	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		if(endTime == null){
			this.endTime = new Date();
		}
		this.endTime = endTime;
	}

	public Integer getSuccess() {
		return success;
	}

	public void setSuccess(Integer success) {
		this.success = success;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Integer getRows() {
		return rows;
	}

	public void setRows(Integer rows) {
		this.rows = rows;
	}

	public String getQueryParam() {
		return queryParam;
	}

	public void setQueryParam(String queryParam) {
		this.queryParam = queryParam;
	}

	public String getQueryTables() {
		return queryTables;
	}

	public void setQueryTables(String queryTables) {
		this.queryTables = queryTables;
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getDsKey() {
		return dsKey;
	}

	public void setDsKey(String dsKey) {
		this.dsKey = dsKey;
	}

	public QueryMessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(QueryMessageType messageType) {
		this.messageType = messageType;
	}

	public String getQuerySource() {
		return querySource;
	}

	public void setQuerySource(String querySource) {
		this.querySource = querySource;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getUiBeginTime() {
		return uiBeginTime;
	}

	public void setUiBeginTime(String uiBeginTime) {
		this.uiBeginTime = uiBeginTime;
	}

	public String getUiEndTime() {
		return uiEndTime;
	}

	public void setUiEndTime(String uiEndTime) {
		this.uiEndTime = uiEndTime;
	}

	public String getViewId() {
		return viewId;
	}

	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	public String getDbQueryId() {
		return dbQueryId;
	}

	public void setDbQueryId(String dbQueryId) {
		this.dbQueryId = dbQueryId;
	}

	public String getQueryFilter() {
		return queryFilter;
	}

	public void setQueryFilter(String queryFilter) {
		this.queryFilter = queryFilter;
	}
}
