package com.bi.queryer.ssm.query.log;

import com.bi.queryer.sys.enums.Enabled;

import java.util.Date;

/**
 * 查询日志实体
 * @author contributor
 *
 */
public class SSDQueryDetailLogEntity {
	protected String id;

	protected String templateId;

	protected String userName;

	protected Date beginTime;

	protected Date endTime;

	private String queryTables = null;

	private String configBeginTime = null;

	private String configThb = null;

	private String configZb = null;

	private String configTotal = null;

	private String configCtr = null;

	private String configCustomCompare = null;

	private String commonDateQueryRange = null;

	/** 日期粒度，来自 QuerySettings.dateGranularity */
	private String dateGranularity = null;

	private String dataQueryRange = null;

	private final String querySQL;

	//热表查询引擎
	private String hotQueryEngine;

	//是否所有的查询表都命中热表
	private Integer isAllQueryTableHitHot = Enabled.NO.getId();

	private String dsKey;

    //查询会话id
    private String sessionId;

	/**
	 * 视图id
	 */
	private String viewId;

	private String userAgent;

	/**
	 * pc/mobile
	 */
	private String terminal;

	/**
	 * 提供给外部调用的api的鉴权码
	 */
	private String olapApiKey;

	/**
	 * 客户端机器名
	 */
	private String hostname;

	private String cacheType;

	public SSDQueryDetailLogEntity(SSDQueryLogEntity logEntity) {
		this.id = logEntity.getId();
		this.templateId = logEntity.getTemplateId();
		this.viewId = logEntity.getViewId();
		this.userName = logEntity.getUserName();
		this.beginTime = logEntity.getBeginTime();
		this.endTime = logEntity.getEndTime();
		this.queryTables = logEntity.getQueryTables();
		this.querySQL = logEntity.getQuerySQL();
		this.dsKey = logEntity.getDsKey();
		this.sessionId = logEntity.getSessionId();
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

	public String getQueryTables() {
		return queryTables;
	}

	public void setQueryTables(String queryTables) {
		this.queryTables = queryTables;
	}

	public String getConfigBeginTime() {
		return configBeginTime;
	}

	public void setConfigBeginTime(String configBeginTime) {
		this.configBeginTime = configBeginTime;
	}

	public String getConfigThb() {
		return configThb;
	}

	public void setConfigThb(String configThb) {
		this.configThb = configThb;
	}

	public String getConfigZb() {
		return configZb;
	}

	public void setConfigZb(String configZb) {
		this.configZb = configZb;
	}

	public String getConfigTotal() {
		return configTotal;
	}

	public void setConfigTotal(String configTotal) {
		this.configTotal = configTotal;
	}

	public String getConfigCtr() {
		return configCtr;
	}

	public void setConfigCtr(String configCtr) {
		this.configCtr = configCtr;
	}

	public String getConfigCustomCompare() {
		return configCustomCompare;
	}

	public void setConfigCustomCompare(String configCustomCompare) {
		this.configCustomCompare = configCustomCompare;
	}

	public String getCommonDateQueryRange() {
		return commonDateQueryRange;
	}

	public void setCommonDateQueryRange(String commonDateQueryRange) {
		this.commonDateQueryRange = commonDateQueryRange;
	}

	public String getDateGranularity() {
		return dateGranularity;
	}

	public void setDateGranularity(String dateGranularity) {
		this.dateGranularity = dateGranularity;
	}

	public String getDataQueryRange() {
		return dataQueryRange;
	}

	public void setDataQueryRange(String dataQueryRange) {
		this.dataQueryRange = dataQueryRange;
	}

	public String getQuerySQL() {
		return querySQL;
	}

	public String getHotQueryEngine() {
		return hotQueryEngine;
	}

	public void setHotQueryEngine(String hotQueryEngine) {
		this.hotQueryEngine = hotQueryEngine;
	}

	public Integer getIsAllQueryTableHitHot() {
		return isAllQueryTableHitHot;
	}

	public void setIsAllQueryTableHitHot(Integer isAllQueryTableHitHot) {
		this.isAllQueryTableHitHot = isAllQueryTableHitHot;
	}

	public String getDsKey() {
		return dsKey;
	}

	public void setDsKey(String dsKey) {
		this.dsKey = dsKey;
	}

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

	public String getViewId() {
		return viewId;
	}

	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getTerminal() {
		return terminal;
	}

	public void setTerminal(String terminal) {
		this.terminal = terminal;
	}

	public String getOlapApiKey() {
		return olapApiKey;
	}

	public void setOlapApiKey(String olapApiKey) {
		this.olapApiKey = olapApiKey;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getCacheType() {
		return cacheType;
	}

	public void setCacheType(String cacheType) {
		this.cacheType = cacheType;
	}
}
