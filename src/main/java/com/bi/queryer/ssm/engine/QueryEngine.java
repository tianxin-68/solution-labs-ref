package com.bi.queryer.ssm.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.OlapApiManager;
import com.bi.queryer.ssm.engine.accelerate.cache.LocalSqlResultCacheManager;
import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.acl.AclManager;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.chart.ChartQueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryResult;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.function.impl.model.QueryProcess;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.model.creator.InvalidModel;
import com.bi.queryer.ssm.engine.model.creator.JoinModelCreator;
import com.bi.queryer.ssm.engine.model.creator.ModelValidateResult;
import com.bi.queryer.ssm.engine.pivot.PivotFactory;
import com.bi.queryer.ssm.engine.result.*;
import com.bi.queryer.ssm.engine.session.QuerySessionManager;
import com.bi.queryer.ssm.engine.session.QuerySessionProperty;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.engine.validator.QueryEngineValidator;
import com.bi.queryer.ssm.engine.validator.QueryMessage;
import com.bi.queryer.ssm.engine.validator.QueryMessageType;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.mail.MailItem;
import com.bi.queryer.ssm.mail.MailServer;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.monitor.ServiceMonitor;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.query.SSDQueryManager;
import com.bi.queryer.ssm.query.log.SSDQueryLogEntity;
import com.bi.queryer.ssm.query.log.SSDQueryLogService;
import com.bi.queryer.ssm.query.risk.RiskEngine;
import com.bi.queryer.ssm.query.risk.RiskQueueService;
import com.bi.queryer.ssm.query.risk.enums.UserBehaviorType;
import com.bi.queryer.ssm.query.risk.model.RiskInfo;
import com.bi.queryer.ssm.util.CsvUtil;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.OssUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.*;
import com.bi.queryer.util.period.DateUtil;
import com.bi.queryer.util.period.WeekDateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * 模板查询引擎:构造查询SQL并返回DataSet
 * @author contributor
 */
public class QueryEngine {

	private static final long serialVersionUID = 1L;

	/**
	 * 查询配置
	 */
	protected QueryConfigure config = null;

	/**
	 * 上下文环境
	 */
	protected QueryContext cxt = null;

	protected SSDQueryLogEntity log = new SSDQueryLogEntity();

	protected boolean isBuilt = false;

	/**
	 * 星型模型列表
	 */
	protected List<StarModel> models = null;

	protected MultiModelQuerySqlBuilder sqlBuilder = null;

	protected QuerySqlFragments sqlFragments = null;

	protected IFunction fx = null;

	protected ResultDataSetRowBuilder rowBuilder = null;

	private boolean hasThb = false;

	protected boolean isShowLunarDate= false;

	private boolean isChartQuery = false;

	/**
	 * 对比日期列表Map，key=dt value = 对比日期
	 * 2026-01-03  值= key = calcMode value = 对比日期
	 */
	protected Map<String, LinkedHashMap<String,String>> compareDateMappings = new HashMap<>();

	/**
	 * 对比期标题Map，key=dt value = 对比期标题
	 */
	protected Map<String,String> compareTitleMapping = new HashMap<>();

	public QueryEngine(QueryConfigure configure, QueryContext cxt) {
		this.config = configure;
		this.cxt = cxt;
		this.rowBuilder = new ResultDataSetRowBuilder(this);
		this.prepare();
		this.hasThb = config.getAnalysis() != null && config.getAnalysis().getThb().isActive();
		this.isShowLunarDate = config.isShowLunarDate();
		this.isChartQuery = config instanceof ChartQueryConfigure;
	}

	/**
	 * 构建SQL前准备
	 * @return
	 */
	public void prepare() {
		// 字段权限
		if (this.config.getSettings().getAclCheck()) {
			AclManager.initQueryAcl(this.config, this.cxt);
		}
		// 构建模型
		this.models = this.createModels();

		//如果展示对比期时间说明，需要记录同环比的配置相关信息
		if (Enabled.value(config.getSettings().getShowDateRemark())) {
			this.compareDateMappings = DateUtil.buildCompareDateMapping(config,compareTitleMapping);
		}

		//设置查询的目录数据类型
		initCtgDataType();

		//设置数据最大可用时间
		initLastDateAvailableTime();
	}

	/**
	 * 初始化查询的目录数据类型
	 */
	public void initCtgDataType() {

		QuerySettings settings = config.getSettings();

		//避免重复查询
		if (StrUtil.isNotEmpty(settings.getCtgDataType())) {
			return;
		}

		Set<String> ctgDataTypeList = new HashSet<>();
		for (QueryField qf : config.getAllFields()) {

			String ctgId = qf.getModuleCtgId();
			//前端没有值，从元数据获取
			if(StrUtil.isEmpty(ctgId)){
				MetaField metaField = qf.getMeta();
				if(metaField == null || CollUtil.isEmpty(metaField.getCategoryIdList())){
					continue;
				}
				ctgId = metaField.getCategoryIdList().get(0);
			}

			MetaFieldCategory ctg = SSDMetaCacheManager.getCategoryById(ctgId);
			if (ctg == null) {
				continue;
			}

			if (StrUtil.isEmpty(ctg.getDataType())) {
				continue;
			}

			//只处理指标
			if (!qf.isMeasure()) {
				continue;
			}

			ctgDataTypeList.add(ctg.getDataType());
		}

		String ctgDataType = CtgDataType.OFFLINE.getCode();

		//模块包含实时，则查询目录数据类型为实时
		if (ctgDataTypeList.contains(CtgDataType.RT.getCode())) {
			ctgDataType = CtgDataType.RT.getCode();
		}

		//模块包含预测，则查询目录数据类型为预测
		if (ctgDataTypeList.contains(CtgDataType.PREDICT.getCode())) {
			ctgDataType = CtgDataType.PREDICT.getCode();
		}

		settings.setCtgDataType(ctgDataType);

	}

	/**
	 * 初始化数据可用时间
	 */
	public void initLastDateAvailableTime() {

		//避免重复查询
		if (StrUtil.isNotEmpty(config.getSettings().getLastDateAvailableTime())) {
			return;
		}

		//设置当前模型最大可用日期
		Map<String, Date> etlJobUpdateTimeMap = new HashMap<>();
		String lastDateAvailableTime = SSDUtil.getLastDateAvailableTime(this,etlJobUpdateTimeMap);
		config.getSettings().setLastDateAvailableTime(lastDateAvailableTime);
		config.getSettings().setEtlJobUpdateTimeMap(etlJobUpdateTimeMap);

	}

	/**
	 * 创建星型模型
	 * @return
	 */
	public List<StarModel> createModels() {

		// 创建模型
		JoinModelCreator creator = new JoinModelCreator(config, cxt);// new StarModelCreator(config, cxt);
		ModelValidateResult validateResult = creator.validate(true);
		if (!validateResult.isSuccess()) {
			//throw new SSDException("查询失败：无对应的查询规则，请联系模块owner");
			String invalidMessage = QueryEngineValidator.getInvalidModelMessage(config, validateResult);
			if(BIUtil.isEmpty(invalidMessage)) {
				invalidMessage = "查询失败：查询模型表中有部分字段不存在，请联系模块负责人。";
			}
			if(config.getTemplateEntity() != null && config.getTemplateEntity().getId() != null){
				invalidMessage = invalidMessage + String.format(";模板id[%s]", config.getTemplateEntity().getId());
			}
			SSDException exception = new SSDException(invalidMessage);
			exception.setMailTo(validateResult.getInvalidModelOwners());

			// 收集无效模型信息，供前端展示模型名列表、负责人列表、模板id
			List<String> modelNames = new ArrayList<>();
			for (InvalidModel im : validateResult.getInvalidModels()) {
				StarModel invalidStarModel = im.getModel();
				if (invalidStarModel.getFactTable() == null || invalidStarModel.getFactTable().getMeta() == null) {
					continue;
				}
				// 模型名取物理表名
				modelNames.add(invalidStarModel.getFactTable().getMeta().getFullName());
			}
			Map<String, Object> invalidModelInfo = new HashMap<>();
			invalidModelInfo.put("modelNames", modelNames);
			invalidModelInfo.put("modelOwners", validateResult.getInvalidModelOwners());
			String tplId = config.getTemplateEntity() != null ? config.getTemplateEntity().getId() : null;
			invalidModelInfo.put("tplId", tplId);
			exception.setInvalidModelInfo(invalidModelInfo);

			// 添加服务监控
			ServiceMonitor.monitorQuery(exception);
			throw exception;
		}

		List<StarModel> modelList = creator.create();

		if (this.config.getSettings().getAclCheck()) {
			// 注入行级权限过滤信息到config中
			AclManager.injectRowAclFilterToConfig(config, cxt, modelList);

			// 再次再最新的模型创建：此处需重新new JoinModelCreator，避免其内置属性冲突
			creator = new JoinModelCreator(config, cxt);//new JoinModelCreator(config, cxt);
			modelList = creator.create();
		}

		return modelList;
	}

	/**
	 * 构建可执行SQL
	 * @return
	 */
	public String buildSql() throws BIException {
		this.fx = FunctionManager.getFunction();

		MultiModelQuerySqlBuilder builder = this.createSqlBuilder();
		String sql = builder.build();
		if (BIUtil.isEmpty(sql)) {
			return "";
		}
		String tips = getSqlTips();
		sql = tips + sql;

		// 语法修正：若是doris热引擎（从trino -> doris)时，会出现语法不兼容问题：因后台配置的自定义表达式或自定义字段可能存在，如：try(xx)，需要替换为doris语法
		DataSourceType dsType = this.getExecuteDataSourceType();
		sql = SSDUtil.rectifySqlBySyntaxRule(sql, dsType);

		/*
		if(DBType.Doris == DBType.getType(dsType.getDialect())) {
			String rectifySyntaxRule = SC.v("hot.table.doris.syntax.rectify.rule", "try\\(=\\(");
			sql = HotUtil.rectifySyntax(sql, rectifySyntaxRule);
		}
		*/
		// 设置sql片段
		this.sqlFragments = new QuerySqlFragments(builder.getSelectFragments());
		return sql;
	}

	public String getSqlTips() {
		String tips = "";
		if (null != cxt) {
			User user = cxt.getUser();
			if (user != null) {
				//String configId = cxt.getParamValue("configId");
				String viewId = "";
//				String tplName = "";
				String source = "";
				if (config.getTemplateEntity() != null) {
//					tplName = ":" + config.getTemplateEntity().getName();
//					if (StringUtil.isEmpty(configId)) {
//						configId = config.getTemplateEntity().getId() + "";
//					}
					viewId = config.getTemplateEntity().getViewId();
					if (BIUtil.isNotEmpty(config.getTemplateEntity().getType())) {
						source = config.getTemplateEntity().getType();
					}
				}
				DataSourceType dsType = getCurrentDataSource();
				String dsDialect = dsType != null ? dsType.getDialect() : "";
				if (StringUtil.isEmpty(viewId)) {
					tips = String.format(" /* ssm:%s|%s|%s */ ", user.getName(), dsDialect, source);
//					tips = " /* ssm-" + user.getName() + " */ ";
				} else {
					tips = String.format(" /* ssm:%s|%s|%s|%s */ ", user.getName(), viewId, dsDialect, source);
//					tips = " /* ssm-" + user.getName() + "(" + configId + tplName + ") */ ";
				}

				// 华为云支持tips添加动态内容命中缓存
				boolean isDorisTipsAddUserInfoEnable = dsType.isHWDoris() && "true".equalsIgnoreCase(SC.v("doris.tips.add.user.info.enable", "true"));

				// 若是doris，只保留引擎和来源，便于提升sql缓存命中率
				if(DBType.getType(dsDialect) == DBType.Doris && !isDorisTipsAddUserInfoEnable){
					tips = String.format(" /* ssm:%s|%s */ ", dsDialect, source);
				}
			}
		}

		return tips;
	}

	/**
	 * 获取查询的表id列表
	 * @return
	 */
	public List<String> getQueryTableIdList() {
		List<StarModel> models = this.createModels();
		List<String> idList = new ArrayList<>();
		Set<String> idSet = new HashSet();
		if (BIUtil.isEmpty(models)) {
			return idList;
		}
		for (StarModel m : models) {
			// 事实表id
			if (m.getFactTable() != null) {
				idSet.add(m.getFactTable().getId());
			}
		}
		idList = idSet.stream().collect(Collectors.toList());
		return idList;
	}
	/*
	public ResultDataSet execute(String sql, Integer maxRowCount, String sessionId) {
		return this.execute(sql, maxRowCount, sessionId, this.buildColumns());
	}
	*/

	public ResultDataSet execute(String sql, QueryExecuteParameter executeParameter) {
		this.fx = FunctionManager.getFunction();

		BIException biException = null;
		ResultDataSet dataSet = new ResultDataSet();
		dataSet.setSessionId(config.getSessionId());

		long tx1 = System.currentTimeMillis();

		// 通过路由器获取负载最低的数据源
		DataSourceType dsType = this.getExecuteDataSourceType();

		long tx2 = System.currentTimeMillis();

		System.out.println("====================Cluster.getBesetDataSourceType:" + dsType.getDesc() + ", Consume " + ((tx2 - tx1)/1000.0) + "s====================");

		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;

		System.out.println("====================SSM Engine Query Data Begin...====================");

		Long t1 = System.currentTimeMillis();
		log.setBeginTime(Calendar.getInstance().getTime());
		log.setQueryParam(cxt.getQueryParamString());
		log.setDsKey(dsType.getKey());

		String sessionId = BIUtil.isNotEmpty(executeParameter.getSessionId()) ? executeParameter.getSessionId() : config.getSessionId();
		log.setSessionId(sessionId);
		QueryProcess queryProcess = null;
		String dbQueryId = null;
		try {
			if (BIUtil.isEmpty(sql)) {
				return dataSet;
			}
			QuerySessionManager.add(sessionId, "", dsType.getKey(), this);
			// 语法修正：若是doris热引擎（从trino -> doris)时，会出现语法不兼容问题：因后台配置的自定义表达式或自定义字段可能存在，如：try(xx)，需要替换为doris语法
			sql = SSDUtil.rectifySqlBySyntaxRule(sql, dsType);

			//order by 使用开窗函数，存在语法问题，需要进行sql修正
			sql = SSDUtil.rectifyOrderBySql(sql,this.getSqlTips());

			// sessionId重赋值：便于运行监控
			config.setSessionId(sessionId);

			String templateId = config.getTemplateEntity() == null ? null : config.getTemplateEntity().getId();
			List<QuerySessionProperty> sessionProperties = QuerySessionSettingManager.getSessionProperties(UserManager.get().getName(), templateId);

			// 添加附加sql片段：limit、hint等
			sql = appendSqlLimit(executeParameter.getMaxRowCount(), sql);
			sql = fx.appendHint(sessionProperties, config.getSettings(), sql);
			log.setQuerySQL(sql);

			ResultDataSet localCacheDataSet = LocalSqlResultCacheManager.getCache(config, cxt, sessionId, dsType, sql);
			if (localCacheDataSet != null) {
				log.setCacheType("local");
				return localCacheDataSet;
			}

			conn = DBUtil.getConn(dsType);
			stmt = conn.createStatement();

			// session查询属性参数
			fx.setSessionProperties(conn, sessionProperties, sessionId, sql);

			// 监控，超时kill
			config.getSettings().setQueryDatasourceKey(dsType.getKey());
			queryProcess = fx.monitor(conn, stmt, config.getSettings());

			rs = stmt.executeQuery(sql);

			// 更新queryId，便于后面kill
			dbQueryId = fx.getDbEngineQueryId(queryProcess, sessionId, dsType);
			QuerySessionManager.update(sessionId, dbQueryId);

			// columns
			List<ResultDataSetColumn> columns = executeParameter.getColumns();
			if (columns == null) {
				columns = this.buildColumnsByResultSet(rs);
			}
			dataSet.setColumns(columns);

			this.buildDataSet(rs, dataSet);

			// 隐藏不前端设置不显示的字段
			List<String> hideFieldCodes = this.config.getResult().getFields()
					.stream().filter(f -> Enabled.isFalse(f.getIsShow()))
					.map(QueryField::getCode).collect(Collectors.toList());
			columns = dataSet.getColumns().stream().filter(c -> !hideFieldCodes.contains(c.getRawCode())).collect(Collectors.toList());
			dataSet.setColumns(columns);

			// 无列数据时，则清空数据集
			if (BIUtil.isEmpty(dataSet.getColumns())) {
				dataSet.clear();
			}

			Long t2 = System.currentTimeMillis();
			dataSet.getProperties().put("consumeTime", (t2 - t1));
			dataSet.getProperties().put("cluster", dsType.getDesc());

			// 添加到缓存
			LocalSqlResultCacheManager.addCache(config, cxt, sessionId, dsType, sql, dataSet);
		} catch (Throwable e) {
			//验证字段时，输出实际的异常信息
			QueryModeType queryModeType = config.getSettings().getQueryModeType();
			if(QueryModeType.VAILD_COLUMNS == queryModeType){
				biException = new BIException(e);
			}else{
				biException = this.createErrorException(e);
			}
			/** 存在executeQuery时抛异常，导致无法获取dbQueryId*/
			dbQueryId = fx.getDbEngineQueryId(queryProcess, sessionId, dsType);
			QuerySessionManager.update(sessionId, dbQueryId);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				log.setSuccess(Enabled.NO.getId());
				log.setInfo(e.getMessage());
				e.printStackTrace();
				biException = new BIException(BIConsts.QUERY_ERROR_TIP, e);
			}

			// 删除sessionId记录
			QuerySessionManager.delete(sessionId);

		}
		Long t2 = System.currentTimeMillis();
		System.out.println("====================SSM Engine Query Data End, Consume " + ((t2 - t1) / 1000.0) + "s====================");

		// 记录数据库查询id
		log.setDbQueryId(dbQueryId);

		if (biException != null) {
			throw biException;
		}

		return dataSet;
	}

	/**
	 * 迭代resultset并封装dataset
	 * @param rs
	 * @param dataSet
	 */
	public void buildDataSet(ResultSet rs, ResultDataSet dataSet) throws SQLException {
		DefaultDataSetBuilder builder = DataSetBuilderFactory.createDataSetBuilder(rs, dataSet, this);
		builder.buildDataSet();
	}

	/**
	 * 执行
	 * @return
	 */
	public ResponseMessage execute() {
		SSDQueryManager.setQueryEngine(this);
		ResponseMessage result = new ResponseMessage();
		BIException biException = null;
		ResultDataSet dataSet = new ResultDataSet();
		try {
			String sql = this.buildSql();
			if (BIUtil.isEmpty(sql)) {
				return result;
			}
			//只查表头和模拟数据，插入默认条件 1=2
			QueryModeType queryModeType = config.getSettings().getQueryModeType();
			if (QueryModeType.COLUMNS == queryModeType
					|| QueryModeType.MOCK_DATA == queryModeType
					|| QueryModeType.VAILD_COLUMNS == queryModeType) {

				//order by 使用开窗函数，存在语法问题，需要进行sql修正
				sql = SSDUtil.rectifyOrderBySql(sql, this.getSqlTips());

				sql = String.format(" select * from ( %s ) tmp where 1=2 ", sql);
			}

			int limitRow = this.getLimitRow();
			//前置查询列维度的值时，从settings中获取
			//前置查询列维度，会设置limit = 100 ，提高查询效率
			if (StrUtil.isNotEmpty(config.getSessionId()) && config.getSessionId().startsWith(BIConsts.HEADER_PREPARE_SESSIONID_PREFIX)) {
				limitRow = config.getSettings().getQueryRowLimit();
			}

			dataSet = this.execute(sql, new QueryExecuteParameter(limitRow, config.getSessionId(), this.buildColumns())); //this.execute(sql, config.getSessionId());

			//模拟数据
			if (QueryModeType.MOCK_DATA == queryModeType) {
				dataSet.setRows(mockData(dataSet.getColumns()));
			}

			//进行行列转置变换
			dataSet = PivotFactory.createPivotEngine(this.getConfig(), dataSet, this).pivot(false);
		} catch (BIException e) {
			if (BIUtil.isEmpty(log.getInfo())) {
				log.setInfo(e.getMessage());
			}
			biException = e;
		}

		this.post(dataSet);

		//构建查询使用的表名
		dataSet.setQueryTableNames(buildQueryTableNames());
		//构建查询数据更新时间
		dataSet.setDataUpdateTime(buildQueryDataUpdateTime());
		dataSet.setIsRtDataSet(config.getSettings().isRtDataset() ? Enabled.YES.getId() : Enabled.NO.getId());

		SSDQueryManager.remove();

		// 此处不外抛异常，避免重复发送邮件
		if (biException != null) {
			return new ResponseMessage(false, biException.getMessage(), null, biException.getCode());
		}

		if (QueryResponseFormat.LIST.getCode().equals(config.getSettings().getResponseFormat())) {
			result.setData(buildRowList(dataSet));
		} else {
			result.setData(dataSet);
		}
		return result;
	}

	/**
	 * 构建查询使用的表名
	 * @return
	 */
	public List<String> buildQueryTableNames() {
		List<String> queryTableNames = new ArrayList<>();

		for (StarModel model : models) {
			for (QueryTable table : model.getTables()) {
				if (table.getMeta() == null) {
					continue;
				}
				queryTableNames.add(table.getMeta().getFullName());
			}
		}

		//出重
		queryTableNames = queryTableNames.stream().distinct().collect(Collectors.toList());
		return queryTableNames;
	}

	/**
	 * 构建查询数据更新时间
	 * @return
	 */
	public String buildQueryDataUpdateTime() {
		String dataUpdateTime = "";

		if (!config.getSettings().isRtDataset()) {
			return dataUpdateTime;
		}

		for (StarModel model : models) {
			dataUpdateTime = DateUtil.getMaxDate(dataUpdateTime, model.getDataUpdateTime());
		}

		return dataUpdateTime;
	}

	// 行从map转为list
	private ResultDataSet buildRowList(ResultDataSet dataSet) {
		if (dataSet == null || CollUtil.isEmpty(dataSet.getRows())) {
			return dataSet;
		}

		List<Map<String, Object>> rows = dataSet.getRows();
		Set<String> codeSet = new HashSet<>(rows.get(0).size());
		for (Map<String, Object> rowMap : rows) {
			codeSet.addAll(rowMap.keySet());
		}

		List<String> codes = new ArrayList<>(codeSet);
		List<List<Object>> rowList = new ArrayList<>(rows.size());
		for (Map<String, Object> rowMap : rows) {
			List<Object> row = new ArrayList<>(codes.size());
			for (String code : codes) {
				row.add(rowMap.get(code));
			}
			rowList.add(row);
		}

		Map<String, Integer> columnIndex = new HashMap<>(codes.size());
		int i = 0;
		for (String code : codes) {
			columnIndex.put(code, i++);
		}

		dataSet.setColumnIndex(columnIndex);
		dataSet.setRowList(rowList);
		dataSet.setRows(null);
		return dataSet;
	}

	/**
	 *模拟数据
	 * @return
	 */
	public List<Map<String, Object>>  mockData(List<ResultDataSetColumn> columns) {

		List<Map<String, Object>> rows = new ArrayList<>();
		if (CollUtil.isEmpty(columns)) {
			return rows;
		}

		String mockColumnValue = SC.v("ssm.querytemplate.mock.column.value"," ");
		Map<String, Object> row = new HashMap<>();
		for (ResultDataSetColumn column : columns) {
			buildColumnMockValue(column, row, mockColumnValue);
		}

		rows.add(row);
		return rows;
	}

	public void buildColumnMockValue(ResultDataSetColumn column,Map<String, Object> row,String mockColumnValue) {

		//叶子节点
		if (CollUtil.isEmpty(column.getChildren())) {
			row.put(column.getCode(), mockColumnValue);
		} else {

			for (ResultDataSetColumn child : column.getChildren()) {
				buildColumnMockValue(child, row, mockColumnValue);
			}

		}

	}

	public void post(ResultDataSet dataSet) {

		// 修正 row total里指标的raw code, 是指标的hint在前端展示出来
		amendRowTotalRawCode(this.getConfig(), dataSet.getColumns());
		amendRawCode(dataSet.getColumns());

		//如果模板是使用在AI配置中
		//需要将结果转为csv，并上传到文件服务器，供后续agent使用
		if(Enabled.value(config.getSettings().getIsTplUseInAIConfig())){
			String csvContent = CsvUtil.toCsv(dataSet).csvContent;
			String csvDatasetUrl = OssUtil.upload(csvContent.getBytes(StandardCharsets.UTF_8), "",".csv", "bigdata/agent_new/dataset");
			dataSet.setCsvDatasetUrl(csvDatasetUrl);

			//构建
			// "columnsInfo": [
			//                        {
			//                            "code": "dt",
			//                            "title": "日期"
			//                        }
			//                    ]
			//提供给agent将编码转化为名称
			List<CsvUtil.FlatColumn> flatColumns =	CsvUtil.flattenColumns(dataSet.getColumns());
			JSONArray columnsInfo = new JSONArray();
			for (CsvUtil.FlatColumn flatColumn : flatColumns) {
				JSONObject columnInfo = new JSONObject();
				columnInfo.put("code", flatColumn.getCode());
				columnInfo.put("title", flatColumn.getFlatTitle());
				columnsInfo.add(columnInfo);
			}

			dataSet.getProperties().put("columnsInfo", columnsInfo);
		}

		log.setRows(dataSet.getSize());
		log.setEndTime(Calendar.getInstance().getTime());
		if (config.getTemplateEntity() != null) {
			log.setTemplateId(config.getTemplateEntity().getId());
			log.setViewId(config.getTemplateEntity().getViewId());
		}
		User user = UserManager.get();
		if (user != null) {
			log.setUserName(user.getName());
		}

		String logId = Guid.id();
		log.setId(logId);
		if(BIUtil.isEmpty(log.getSessionId())) {
			log.setSessionId(config.getSessionId());
		}

		dataSet.setLogId(logId);

		// 设置查询表：便于后期判断是否跨模块查询
		List<String> queryTables = new ArrayList<>();
		this.models.forEach(m -> {
			m.getTables().forEach(t -> {
				if (t.getMeta() != null) {
					queryTables.add(t.getMeta().getFullName());
				}
			});
		});
		log.setQueryTables(BIUtil.listToStr(queryTables));

		QueryMessageType msgType = log.getMessageType();

		// 查询字段日志记录
		List<QueryField> allFields = SSDUtil.getFinalQueryAllFields(this.getConfig(), this.getModels()); //ssdQueryService.loadAllFieldList(this.getConfig(), this.getModels());
		// 如果查询字段有在测试开发目录中，则归为UAT环境查询
		Boolean hasDevTestField = checkHasDevTestField(allFields);
		if (hasDevTestField && RuntimeEnv.Product == BIUtil.getRuntimeEnv()) {
			log.setEnv(RuntimeEnv.UT.getCode());
		}

		// 过滤值
		log.setQueryFilter(SSDUtil.getQueryConfigFilterDesc(config));

		// 查询SQL日志记录
		if(msgType.isWriteLog()) {
			SSDQueryLogService logService = (SSDQueryLogService) SpringContextUtil.getBean("SSDQueryLogService");
			logService.log(log, config);

			logService.logFinalQueryFields(allFields, log, "query");
			/**
			//读取合并前的字段，存入字段日志表
			if (CollUtil.isNotEmpty(allFields) && allFields.size() > 0) {
				logService.logFields(allFields, log, "query");
			} else {
				logService.logFields(cxt.getUsedFields(), log, "query");
			}
			 */
		}

		// 发送邮件
		if (!Enabled.value(log.getSuccess()) || msgType == QueryMessageType.ERROR_BLOCK) { // 查询失败或熔断，发送邮件
			if(msgType.isSendMail()){
				sendErrorMail(log);
			}
		} else {
			//推送安全kafka日志
			RiskQueueService riskQueueService = (RiskQueueService) SpringContextUtil.getBean("riskQueueService");
			RiskInfo riskInfo = new RiskInfo();
			riskInfo.setUsername(user.getName());
			riskInfo.setUserBehavior(UserBehaviorType.QUERY.getName());
			riskInfo.setDataNumber(dataSet.getSize());
			riskInfo.setClientLogId(logId);
			riskInfo.setBlackBox(user.getBlackBox());
			riskInfo.setQueryClient(BIUtil.isNotEmpty(config.getSettings().getOlapApiKey()) ? "mcp" : "web");
			RiskEngine riskEngine = new RiskEngine(riskInfo, this.getConfig());
			riskQueueService.addQueue(riskEngine);
		}

		// 添加服务监控
		ServiceMonitor.monitorQuery(log,msgType);
	}

	// 纯粹为前端需要转换逻辑
	private void amendRawCode(List<ResultDataSetColumn> columns) {
		if (BIUtil.isEmpty(columns)) {
			return;
		}
		try {
			for (ResultDataSetColumn column : columns) {
				if (column.getTargetConfig() != null) {
					column.setRawCode(column.getTargetConfig().getRawMeasureCode() + "_target_analysis");
				}

				amendRawCode(column.getChildren());
			}
		} catch (Exception e) {
			//ignore
		}
	}

	private void amendRowTotalRawCode(QueryConfigure queryConfigure, List<ResultDataSetColumn> columns) {
		if (BIUtil.isEmpty(columns)) {
			return;
		}

		QueryResult result = queryConfigure.getResult();
		if (result == null || result.getPivotConfig() == null) {
			return;
		}

		if (result.getPivotConfig().isMeasureOnRow() || BIUtil.isEmpty(result.getColDimensions())) {
			return;
		}

		try {
			for (ResultDataSetColumn column : columns) {
				if (!BIConsts.ROW_TOTAL_COLUMN_CODE.equals(column.getRawCode())) {
					continue;
				}

				if (BIUtil.isEmpty(column.getChildren())) {
					return;
				}

				for (ResultDataSetColumn child : column.getChildren()) {
					child.setRawCode(child.getRawCode().replace("_" + AnalysisCalcMode.ROW_TOTAL.getCode(), ""));
				}
			}
		} catch (Exception e) {
			//ignore
		}
	}

	// 校验是否包含正在开发测试阶段的字段
	private Boolean checkHasDevTestField(List<QueryField> allFields) {
		for (QueryField field : allFields) {
			if (CollUtil.isNotEmpty(field.getMeta().getCategoryIdList())) {
				for (String ctgId : field.getMeta().getCategoryIdList()) {
					MetaFieldCategory category = SSDMetaCacheManager.getCategories().get(ctgId);
					if (category != null && Enabled.isTrue(category.getIsTestCtg())) {
						return true;
					}
				}
			}

		}
		return false;
	}

	protected String appendSqlLimit(Integer maxRowCount, String sql) {
		if (maxRowCount >= 0) {
			IFunction function = FunctionManager.getFunction();
			sql = function.appendLimit(sql, maxRowCount);
		}

		return sql;
	}

	/**
	 * 构建列
	 * @return
	 */
	public List<ResultDataSetColumn> buildColumns() {


	   List<QueryField> resultFields = config.getResult().getFields();

	   String aggTypeDesc = config.getAnalysis().getTotal().getAggConfig().getAggTypeDesc();
		List<ResultDataSetColumn> columns = new ArrayList<>();
		for (QueryField field : resultFields) {
			if (field.isAppend() || Enabled.isFalse(field.getIsShow())) { // 附加字段不显示
				continue;
			}

			ResultDataSetColumn col = this.createDataSetColumn(field);
			col.setTotalAggTypeDesc(aggTypeDesc);
			columns.add(col);
		}

		return columns;
	}

	public ResultDataSetColumn createDataSetColumn(QueryField field) {
		return this.createDataSetColumn(field, field.getCode());
	}

	public ResultDataSetColumn createDataSetColumn(QueryField field, String columnCode) {
		if (BIUtil.isEmpty(columnCode)) {
			columnCode = field.getCode();
		}
		String fieldTitle = FieldUtil.getTitle(field); //BIUtil.isEmpty(field.getDisplayTitle()) ? field.getMeta().getTitle() : field.getDisplayTitle();

		//设置了日均，修改字段显示
		/*
		AggExpressionType aggExpressionType = AggExpressionType.get(field.getAggExpressionType());
		if (aggExpressionType.isAvgByDay()) {
			fieldTitle = String.format("%s（%s）", fieldTitle, aggExpressionType.getTitle());
		}
		 */

		Map<String, String> aclCodes = cxt.getAclFields();
		boolean hasAuth = aclCodes.containsKey(field.getRawCode());
		/*
		if(!hasAuth){
			fieldTitle = fieldTitle + "(无权限)";
		}
		 */
		ResultDataSetColumn col = new ResultDataSetColumn(columnCode, fieldTitle);
		col.setId(field.getId());
		col.setRawCode(field.getRawCode());
		col.setRawTitle(fieldTitle);
		col.setRawQueryArea(field.getQueryArea().toString());
		col.setHasAuth(hasAuth);
		DataType dataType = DataType.getType(field.getMeta().getDataType());
		FieldFilterType filterType = FieldUtil.getFilterType(field);
		if (FieldFilterType.BooleanSelect == filterType) {
			// 行维度统一为字符串
			dataType = DataType.String;
		}
		col.setDataType(dataType.toString());

		String exportAppendString = field.getMeta().getExportAppendString();
		if (StringUtils.isNotBlank(exportAppendString)) {
			ExportAppendStringType exportAppendStringType = ExportAppendStringType.get(exportAppendString);
			if (ExportAppendStringType.TAB.getId().equalsIgnoreCase(exportAppendStringType.getId())) {
				exportAppendString = "\t";
			}
		}
		col.setExportAppendString(exportAppendString);

		String fieldFormatString = FieldUtil.getFieldFormatString(field);
		col.setDataFormat(fieldFormatString);

		// 设置排序
		col.setOrderType(field.getSortType().toString());

		// 设置预警
		AnalysisItemConfig analysisConfig = field.getAnalysisConfig();
		if (analysisConfig != null && BIUtil.isNotEmpty(analysisConfig.getMeasureId())) {
			col.setHasAlarm(AnalysisCalcType.get(analysisConfig.getCalcType()) == AnalysisCalcType.RATIO && AnalysisCalcMode.get(analysisConfig.getCalcMode()).isCompare());
			col.setZb(AnalysisCalcMode.get(analysisConfig.getCalcMode()).isZb());
		}

		// 设置是否需要显示农历日期
		QueryField dateField = config.getResultCommonDateField();
		boolean showLunarDate = isShowLunarDate && field.getCode().equalsIgnoreCase(dateField.getCode());
		col.setShowLunarDate(showLunarDate);
		if (col.getShowLunarDate()) {

			if(Enabled.value(config.getSettings().getShowDateRemark())){
				col.setTitle("日期-公历 周几(农历)");
				col.setRawTitle("日期-公历 周几(农历)");
			}

			col.setDataFormat(IFunction.Format_Lunar_Date);
		}

		// 布尔类型格式化字符串
		if(filterType == FieldFilterType.BooleanSelect){
			col.setDataFormat(IFunction.Format_Boolean);
		}

		// 枚举值映射类型
		if(CollUtil.isNotEmpty(SSDMetaCacheManager.getFieldValueMapByFieldCode(col.getRawCode()))) {
			col.setDataFormat(IFunction.Format_Map);
		}

		return col;
	}

	public List<ResultDataSetColumn> buildColumnsByResultSet(ResultSet rs) throws SQLException {
		ResultSetMetaData rsMeta = rs.getMetaData();
		int columnCount = rsMeta.getColumnCount();
		List<ResultDataSetColumn> columnList = Lists.newArrayList();
		QueryField dateField = config.getResultCommonDateField();
		for (int i = 1; i <= columnCount; i++) {
			String columnName = rsMeta.getColumnLabel(i);
			columnName = columnName.substring(columnName.lastIndexOf(".") + 1);
			ResultDataSetColumn columnInfo = new ResultDataSetColumn();
			String type = rsMeta.getColumnTypeName(i).toLowerCase();
			if (type.contains("(")) {
				type = type.substring(0, type.indexOf("("));
			}
			columnInfo.setCode(columnName);
			columnInfo.setTitle(columnName);
			columnInfo.setDataType(DBUtil.getDataType(type).toString());

			// 设置是否需要显示农历日期
			boolean showLunarDate = isShowLunarDate && columnInfo.getCode().equalsIgnoreCase(dateField.getCode());
			columnInfo.setShowLunarDate(showLunarDate);

			if (columnInfo.getShowLunarDate()) {
				columnInfo.setDataFormat(IFunction.Format_Lunar_Date);
			}

			columnList.add(columnInfo);
		}
		return columnList;
	}

	/**
	 * @param log
	 */
	protected void sendErrorMail(SSDQueryLogEntity log) {
		MailItem item = new MailItem(log);
		if (this.getModels() != null) {
			Set<String> tableOwners = this.getModels().stream().map(StarModel::getFactTable)
					.map(k -> k.getMeta().getTableOwner())
					.collect(Collectors.toSet());
			item.setMailTo(tableOwners);
		}
		MailServer.send(item);
	}

	public Integer getDataSetTotalSize(String sql) {
//		String sql = this.buildSql();
		Integer count = 0;
		if (BIUtil.isEmpty(sql)) {
			return count;
		}

		//order by 使用开窗函数，存在语法问题，需要进行sql修正
		sql = SSDUtil.rectifyOrderBySql(sql,this.getSqlTips());

		String countSQL = this.getSqlTips() + " select count(1) as f_count from (" + sql + ") _count_tmp";
		BIException biException = null;
		Statement stmt = null;
		ResultSet res = null;
		Connection conn = null;
		Long t1 = System.currentTimeMillis();

		try {
            fx = FunctionManager.getFunction();
			conn = DBUtil.getConn(DBUtil.getDataSourceType());
			List<QuerySessionProperty> sessionProperties = QuerySessionSettingManager.getSessionProperties(UserManager.get().getName(), "");
			fx.setSessionProperties(conn, sessionProperties, Guid.id(), countSQL);
			stmt = conn.createStatement();
			//stmt.execute("SET SESSION use_mark_distinct = false");
			res = stmt.executeQuery(countSQL);
			if (res.next()) {
				count = res.getInt(1);
			}
		} catch (Throwable e) {
			e.printStackTrace();
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
			} catch (Exception e) {
				e.printStackTrace();
				biException = new BIException(BIConsts.QUERY_ERROR_TIP);
			}
		}
		if (biException != null) {
			throw biException;
		}
		return count;
	}

	public Object formatValue(Object value, String dataType, String formatStr, String fieldCode) {

		try {

			if (value == null) {
				return value;
			}

			//业务日期的公共日期格式化处理
			if (config.getSettings().isBusinessCalendar() && BIConsts.DATE_CODE.equalsIgnoreCase(fieldCode) && !isChartQuery) {
				value = PromotionManager.getLunarFormatPromotionName(value.toString(), compareDateMappings, isShowLunarDate,config.getSettings().getShowDateRemark());
				return value;
			} else if (hasThb && BIConsts.DATE_CODE.equalsIgnoreCase(fieldCode) && !isChartQuery) {
				value = formatDateValue(value);
			}

			if (BIUtil.isEmpty(formatStr)) {
				return value;
			}

			//日期周显示全称
			if (IFunction.Format_Week.equalsIgnoreCase(formatStr)) {
				value = WeekDateUtil.getWeekFullnameById(value.toString());
				return value;
			}

			//月展示yyyy-mm
			if (IFunction.Format_Month.equalsIgnoreCase(formatStr)) {

				SimpleDateFormat inputFormat = new SimpleDateFormat(IFunction.Format_Month);
				Date date = inputFormat.parse(value.toString());
				SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM");
				return outputFormat.format(date);

			}

			// 日：农历
//			if (IFunction.Format_Lunar_Date.equalsIgnoreCase(formatStr)) {
//				//return String.format("%s(农历%s)",value,DateUtil.toLunar(value.toString()));
//				return DateUtil.formatDateToLunar(value, dataType, formatStr, this.compareDateMappings);
//			}

			// boolean：0=否、1=是、null=其他
			if (IFunction.Format_Boolean.equalsIgnoreCase(formatStr)) {
				if (fx == null) {
					fx = FunctionManager.getFunction();
				}
				return fx.formatBoolean(value, formatStr);
			}

			/* 按东哥的要求，查底表中是啥，查询结果就展示啥, 先不转
			if (IFunction.Format_Map.equalsIgnoreCase(formatStr)) {
				Map<String, String> dimItemMap = SSDMetaCacheManager.getFieldValueMapByFieldCode(fieldCode);
				if (CollUtil.isNotEmpty(dimItemMap)) {
					String mapValue = dimItemMap.get(value + "");
					return mapValue == null ? value : mapValue;
				} else {
					return value;
				}
			}
			*/

			DataType dType = DataType.getType(dataType);
			if (dType != DataType.String) {
				if (value instanceof Double) {
					// 处理NaN数字，避免查询格式化后出现乱码
					Double d = (Double) value;
					if (Double.isNaN(d) || Double.isInfinite(d)) {
						return null;
					}
				}
				if (value != null && dType.isDecimal()) {
					// 是否是日均值格式化
					formatStr = this.getDecimalFormatString(value, dType, formatStr);
					DecimalFormat df = new DecimalFormat(formatStr);
					value = df.format(value);
					if (BIUtil.isNotEmpty(value + "")) {
						// 去掉百分比指标格式化后多余字符串
						value = value.toString().replace("%pt", "pt");
					}
				}
			}
		} catch (Exception e) {
			//System.out.println("格式化错误数据：value=" + value + ",format=" + formatStr);
		}
		return value;
	}

	/**
	 * 格式化日期
	 * 若关闭对比期时间说明：
	 *         环比：2026-01-03 周六
	 *         农历年同：2026-01-03 周六(农2025-11-15)
	 *
	 * 若开启对比期时间说明：
	 *         环比：2026-01-03 周六 - 环比 2026-01-02 周五
	 *         农历年同：2026-01-03 周六(农2025-11-15) - 农年同(-1)2024-12-15 周日(农2024-11-15)
	 * --注：若勾选多个对比类型，对比期注释排列顺序为先农历再公历（农年同-1、农年同-2、农年周同-1、农年周同-2、环比、周同比...）
	 * @param value
	 * @return
	 */
	public String formatDateValue(Object value) {

		String result = value.toString();

		//非日粒度不处理
		if(DateGranularity.DAY != DateGranularity.get(config.getSettings().getDateGranularity())) {
			return result;
		}

		//不展示对比期不处理
		if (!Enabled.value(config.getSettings().getShowDateRemark())) {
			return result;
		}

		String weekName = DateUtil.getWeekNameWithSpace(result);
		result = result.replaceAll("-","") + weekName;

		if(isShowLunarDate){
			result = String.format("%s (%s)", result, DateUtil.toLunarDate(value.toString()));
		}

		//是否展示对比日期
		if (Enabled.value(config.getSettings().getShowDateRemark())) {

			Map<String, String> compareDateMap = this.compareDateMappings.get(value.toString());
			if (compareDateMap == null) {
				return result;
			}

			List<String> dateRemarkList = new ArrayList<>();
			for (String calcMode : compareDateMap.keySet()) {
				AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);
				String compareDate = compareDateMap.get(calcMode);

				String analysisCalcModeTitle = analysisCalcMode.getDesc();
				if(calcMode.contains(AnalysisCalcMode.CUSTOM_COMPARE.getCode())){
					analysisCalcModeTitle = this.compareTitleMapping.get(calcMode);
				}

				String compareDateRemark= String.format("%s(无)", analysisCalcModeTitle);
				if(StrUtil.isNotEmpty(compareDate)) {
					compareDateRemark = String.format("%s %s%s"
							, analysisCalcModeTitle
							, compareDate.replaceAll("-",""),
							DateUtil.getWeekNameWithSpace(compareDate));

					if (analysisCalcMode.isTblny() || analysisCalcMode.isTblnyw()) {
						compareDateRemark += String.format(" (%s)", DateUtil.toLunarDate(compareDate));
					}
				}

				dateRemarkList.add(compareDateRemark);
			}

			result = String.format("%s || %s"
					, result
					, String.join(" || ", dateRemarkList));

		}
		return result;
	}

	/**
	 * 获取农历年同比
	 * @return
	 */
	public List<String> getLunarCalcModeList() {
		List<String> calcModeList = new ArrayList<>();

		AnalysisThbConfig thbConfig = this.config.getAnalysis().getThb();
		if (!thbConfig.isActive()) {
			return calcModeList;
		}
		for (AnalysisThbItemConfig item : thbConfig.getItems()) {
			for (String calcMode : item.getCalcModes()) {
				AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);
				if (analysisCalcMode.isTblny()) {
					calcModeList.add(calcMode);
				}
			}
		}


		List<String> result = new ArrayList<>();
		//去重
		if (CollUtil.isNotEmpty(calcModeList)) {

			calcModeList = calcModeList.stream().distinct().collect(Collectors.toList());

			//农历年同比按顺序返回
			for (AnalysisCalcMode calcMode : AnalysisCalcMode.values()) {
				if (!calcMode.isTblny()) {
					continue;
				}

				if (calcModeList.contains(calcMode.getCode())) {
					result.add(calcMode.getCode());
				}

			}

		}

		return result;
	}

	/**
	 * 通过值获取该值的数字格式化字符串
	 * 根据阈值（如：100）判断设置日均值格式化
	 * 若数据类型=integer，值小阈值，保留2位小数，值大于阈值，不保留小数位
	 * 若数据类型=double，不处理
	 * @param value
	 * @param dataType
	 * @param rawFormatString
	 * @return
	 */
	protected String getDecimalFormatString(Object value, DataType dataType, String rawFormatString){
		String newFormatString = rawFormatString;
		// 是否是日均值格式化
//		boolean isAvgValueFormat = BIUtil.isNotEmpty(rawFormatString) && rawFormatString.endsWith(BIConsts.AVG_FORMAT_SUFFIX);
//		String valueString = value + "";
//		Integer avgIntegerFormatThreshold = Integer.valueOf(SC.v("avg.integer.format.threshold", "100"));
//		if(dataType.isInteger() && isAvgValueFormat && BIUtil.isNotEmpty(valueString) && Double.valueOf(valueString) < avgIntegerFormatThreshold){
//			if(!rawFormatString.contains(".")){
//				newFormatString = newFormatString.replace(BIConsts.AVG_FORMAT_SUFFIX, "");
//				// 最后一位替换为0，避免出现.00问题
//				newFormatString = newFormatString.substring(0, newFormatString.length() - 1) + "0.00";
//			}
//		}
		newFormatString = newFormatString.replace(BIConsts.AVG_FORMAT_SUFFIX, "");
		return newFormatString;
	}

	public boolean killQuery() {
		String killMessage = SSDException.Manual_Kill_Error + "：查询被" + cxt.getUser().getName() + "手动kill";
		String sessionId = config.getSessionId();
		IFunction function = FunctionManager.getFunction();
		boolean success = true;
		try{
			function.killQuery(sessionId, killMessage, config.getSettings(), this.getCurrentDataSource());
		}catch (Exception e){
			e.printStackTrace();
		}
		QuerySessionManager.delete(sessionId);

		return success;
	}

	/**
	 * 创建SQL构建器
	 * @return
	 */
	public MultiModelQuerySqlBuilder createSqlBuilder() {
		if (sqlBuilder != null) {
			return sqlBuilder;
		}
		this.sqlBuilder = QuerySqlBuilderFactory.createSqlBuilder(this); // new MultiModelQuerySqlBuilder(this.config, this.cxt, this.models);
		return this.sqlBuilder;
	}

	public int getLimitRow() {
		Integer searchLimit = QuerySessionSettingManager.getQueryRowLimit(); //Integer.parseInt(SC.v("ssm.search.limit", "20000"));

		//列维度不处理
		Optional<QueryField> opt = config.getResult().getColDimensions().stream().filter(f -> Enabled.value(f.getIsShow())).findAny();
		if (!opt.isPresent()) {
			//如果是外部olap-api查询，从配置中获取
			//外部api需要查询更多的数据，使用单独的配置限制
			if (StrUtil.isNotEmpty(config.getSettings().getOlapApiKey())) {
				Integer olapApiRowLimit = OlapApiManager.getUserMaxQueryRowNum(config.getSettings().getOlapApiKey());
				Integer customRowLimit = config.getSettings().getApiQueryRowLimit();
				if (customRowLimit != null && customRowLimit > 0) {
					searchLimit = Math.min(olapApiRowLimit, customRowLimit);
				} else {
					searchLimit = olapApiRowLimit;
				}
			}

			//如果是来自AI问题，按配置的最大数据量限制
			if (this.getConfig().getTemplateEntity() != null && QuerySourceType.CHAT == QuerySourceType.get(this.getConfig().getTemplateEntity().getType())) {
				searchLimit = Integer.parseInt(SC.v("ssm.olap.chat.search.limit", "500000"));
			}

		}

		return searchLimit;
		//return Integer.valueOf(BIUtil.nvl(this.config.getSettings().getQueryRowLimit(), "20000"));
	}

	public BIException createErrorException(Throwable e){
		e.printStackTrace();
		log.setSuccess(Enabled.NO.getId());
		log.setInfo(e.getMessage());

		QueryMessage errorMessage = QueryEngineValidator.validateErrorMessage(e.getMessage(), this.models, this.config);

		log.setMessageType(errorMessage.getType());

		BIException biException = new BIException(errorMessage.getMessage(), e);
		if(e instanceof BIException){
			biException.setCode(((BIException)e).getCode());
		}

		// 查询熔断：不作为查询错误
		if(errorMessage.getType() == QueryMessageType.ERROR_BLOCK){
			log.setSuccess(Enabled.YES.getId());
		}

		// 查询超时
		if(errorMessage.getType() == QueryMessageType.ERROR_TIMEOUT){
			log.setInfo(errorMessage.getMessage());
		}

		// 手动kill，biException = null 不处理错误：不发邮件
		if(errorMessage.getType() == QueryMessageType.ERROR_MANUAL_KILL){
			biException = null;
			log.setSuccess(Enabled.YES.getId());
		}

		return biException;
	}

	/**
	 * 统一结果数据集列的格式化字符串
	 * 若有日均值的同环对比实际值、差值的都和日均值本期值格式化一致
	 * @param columns
	 */
	public void unifyColumnFormat(List<ResultDataSetColumn> columns) {
		if (BIUtil.isEmpty(columns)) {
			return;
		}
		// 日均值字段
		Map<String, ResultDataSetColumn> avgColumns = new HashMap<>();
		// 均值标识符
		String[] avgFlags = new String[]{
				AggExpressionType.Avg_By_Day.getCode(),
				AggExpressionType.Avg_By_Day_Real.getCode()
		};

		// 同环对比字段
		Map<String, ResultDataSetColumn> compareValueColumns = new HashMap<>();
		// 同环对比标识符
		List<String> compareFlags = new ArrayList<>();
		for (AnalysisCalcMode calcMode : AnalysisCalcMode.values()) {
			if (calcMode.isTarget()) {
				continue;
			}
			if (calcMode == AnalysisCalcMode.CUSTOM_COMPARE) {
				for (int i = 0; i < 10; i++) {
					compareFlags.add(String.format("%s_%s_%s", calcMode.getCode(), i, AnalysisCalcType.VALUE.getCode()));
					compareFlags.add(String.format("%s_%s_%s", calcMode.getCode(), i, AnalysisCalcType.REAL_VALUE.getCode()));
				}
			} else {
				compareFlags.add(String.format("%s_%s", calcMode.getCode(), AnalysisCalcType.VALUE.getCode()));
				compareFlags.add(String.format("%s_%s", calcMode.getCode(), AnalysisCalcType.REAL_VALUE.getCode()));
			}
		}
		Map<String, String> avgFormatStrings = new HashMap<>();
		for (ResultDataSetColumn column : columns) {
			// 均值
			for (String flag : avgFlags) {
				String rawCode = column.getCode().split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
				if (rawCode.endsWith(flag)) {
					String avgFormatString = column.getDataFormat();
					if (BIUtil.isNotEmpty(avgFormatString)) {
						avgFormatStrings.put(rawCode, avgFormatString);
						break;
					}
				}
			}

			// 同环对比实际值、差值
			String columnCode = column.getCode();
			for (String compareFlag : compareFlags) {
				for (String avgFlag : avgFlags) {
					String flag = avgFlag + "_" + compareFlag;
					if (columnCode.contains(flag)) {
						compareValueColumns.put(column.getCode(), column);
						break;
					}
				}
			}
		}

		for (String avgRawCode : avgFormatStrings.keySet()) {
			String avgFormatString = avgFormatStrings.get(avgRawCode);
			for (String compareFlag : compareFlags) {
				String compareFieldCodePrefix = String.format("%s_%s", avgRawCode, compareFlag);

				for (ResultDataSetColumn compareValueColumn : compareValueColumns.values()) {
					if (compareValueColumn.getCode().startsWith(compareFieldCodePrefix)) {

						String finalFormatString = avgFormatString;
						//比率的同环比差值格式为pt，不能直接使用当期值的格式
						if (compareValueColumn.getDataFormat().contains("pt")) {
							finalFormatString = avgFormatString.replace("pt", "");
							finalFormatString = finalFormatString + "pt";
						}

						compareValueColumn.setDataFormat(finalFormatString);
					}
				}
			}
		}
	}

	// 获取查询受到的行级权限控制的code
	public Set<String> getDataRowAuthFieldCodes() {
		List<StarModel> starModels = this.getModels();
		if (BIUtil.isEmpty(starModels)) {
			return Collections.emptySet();
		}

		Set<String> fieldCodes = new HashSet<>();
		for (StarModel starModel : starModels) {
			List<QueryField> fields = starModel.getFields();
			for (QueryField field : fields) {
				MetaField metaField = field.getMeta();
				if (metaField != null && BIUtil.isNotEmpty(metaField.getDataAuthDimCode())) {
					fieldCodes.add(field.getCode());
				}
			}
		}
		return fieldCodes;
	}

	public static void main(String[] args) {
		System.out.println(System.currentTimeMillis());
		String sql = "select x, try(x/y) as z";
		sql = HotUtil.rectifySyntax(sql, "try\\(=\\(");
		System.out.println(sql);
	}

	public QueryContext getCxt() {
		return cxt;
	}

	public void setCxt(QueryContext cxt) {
		this.cxt = cxt;
	}

	public QueryConfigure getConfig() {
		return config;
	}

	public void setConfig(QueryConfigure config) {
		this.config = config;
	}

	public SSDQueryLogEntity getLog() {
		return log;
	}

	public void setLog(SSDQueryLogEntity log) {
		this.log = log;
	}

	public MultiModelQuerySqlBuilder getSqlBuilder() {
		if (sqlBuilder == null) {
			return createSqlBuilder();
		}
		return sqlBuilder;
	}

	public void setSqlBuilder(MultiModelQuerySqlBuilder sqlBuilder) {
		this.sqlBuilder = sqlBuilder;
	}

	public List<StarModel> getModels() {
		return models;
	}

	public void setModels(List<StarModel> models) {
		this.models = models;
	}

	public QuerySqlFragments getSqlFragments() {
		return sqlFragments;
	}

	public void setSqlFragments(QuerySqlFragments sqlFragments) {
		this.sqlFragments = sqlFragments;
	}

	public QueryEngineType getType() {
		return QueryEngineType.Common;
	}

	/**
	 * 获取当前数据源
	 * @return
	 */
	public DataSourceType getCurrentDataSource(){
		return DataSourceRouter.getCurrentDataSourceType();
	}

	/**
	 * 获取执行时的数据源
	 * @return
	 */
	protected DataSourceType getExecuteDataSourceType(){
		DataSourceType dsType = DataSourceRouter.getBestDataSourceType();
		return dsType;
	}

	public ResultDataSetRowBuilder getRowBuilder() {
		return rowBuilder;
	}

	public void setRowBuilder(ResultDataSetRowBuilder rowBuilder) {
		this.rowBuilder = rowBuilder;
	}

	public Map<String, LinkedHashMap<String, String>> getCompareDateMappings() {
		return compareDateMappings;
	}

	public boolean isShowLunarDate() {
		return isShowLunarDate;
	}

	public void setShowLunarDate(boolean showLunarDate) {
		isShowLunarDate = showLunarDate;
	}
}
