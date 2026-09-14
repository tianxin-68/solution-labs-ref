package com.bi.queryer.ssm.query.filter;

import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.ssm.query.log.SSDQueryLogService;
import com.bi.queryer.ssm.query.log.SSMFilterQueryLogEntity;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.QueryCancelException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.*;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 12:45
 * Description:下拉筛选数据集
 * 缓存策略：
 * 初次加载：
 * 1、计算总行数rows（不做数据权限过滤）
 * 2、rows <= 2000，则缓存，记录当前字段已缓存。当前配置的权限管控维度记录数均<2000，故都可缓存
 *    2.1 查询不分页的数据集，作为原始数据集缓存
 *    2.2 返回：原始数据集过滤掉有数据权限记录，再分页，total=分页前的数据集大小
 *
 * 3、rows > 2000，则不缓存，记录当前字段未缓存。因权限管控维度都可缓存，故此处不需在过滤数据权限记录
 *
 * 再次加载：
 * 1、判断当前字段是否已缓存数据
 * 2、若缓存，获取缓存数据集并过来数据权限，再分页
 * 3、若无缓存，则直接查询
 *
 * 关键字搜索：
 * 1、判断该字段是否已缓存
 * 2、若未缓存，则走sql过滤关键字
 * 3、若已缓存，则先从缓存数据集过滤关键字，再计算total
 *
 */
@Service
@Scope("prototype")
public class MultiSelectFilterDatasetProvider implements IFilterDatasetProvider {
	@Autowired
	private SSDQueryLogService queryLogService;
	/**
	public static final String cache_key_prefix = "filter_select@";

	public static final String cache_key_total_suffix = "@total";

	public static final String cache_space_key = "MultiSelectFilterDatasetProvider";
	 */

	protected MetaField metaField = null;
	protected Map<String, Object> params = null;
	protected boolean pagination = true;
	protected Integer currentPageNum = 1;

	protected boolean cacheEnabled = true;

	// 缓存的最大行数
	protected Integer cacheMaxRows = 2000;

	protected Integer total = 0;

	protected List<BIMap> rows = new ArrayList<>();

	protected String searchValue = "";

	protected Integer pageSize = 30;

	// 是否查询已重试：优先走元数据支持的引擎若查询报错，则降级走trino
	protected Boolean isRetry = false;

	protected DataSourceType dataSourceType = null;

	@Override
	public Object buildDataset(MetaField metaField, Map<String, Object> params) {

		MultiSelectFilterResult result = new MultiSelectFilterResult();

		this.metaField = metaField;
		this.params = params;
		//this.pagination = "true".equalsIgnoreCase(params.get("pagination") + "");
		if (params.containsKey("currentPageNum")) {
			this.currentPageNum = Integer.valueOf(params.get("currentPageNum") + "");
		}
		this.pageSize = Integer.valueOf(BIUtil.nvl(params.get("pageSize") + "", "30"));
		this.searchValue = (params.get("searchValue") + "").trim();
		this.cacheEnabled = "true".equalsIgnoreCase(SC.v("redis.cache.enable", "true"));
		this.cacheMaxRows = Integer.valueOf(SC.v("redis.cache.max.rows", cacheMaxRows + ""));

		this.dataSourceType = this.getDataSourceType();
		SSMFilterQueryLogEntity logEntity = new SSMFilterQueryLogEntity(getCurrentUserName(), metaField.getId(), metaField.getCode(),
				metaField.getTitle(), new Date(), this.dataSourceType.getKey());

		//String cacheRowsKey = cache_key_prefix +  metaField.getId();//metaField.getCode();
		//String cacheTotalKey = cacheRowsKey + cache_key_total_suffix;
		String cacheRowsValue =  MultiSelectFilterCacheManager.getRowsCache(metaField.getId());//RedisCacheManager.get(cache_space_key, cacheRowsKey);

		String sessionId = (params.get("sessionId") + "").trim();
		long expireMinute = BIUtil.isEmpty(params.get("cacheExpireMinute") + "") ? Long.valueOf(SC.v("redis.cache.expire", "120")) : Long.valueOf(params.get("cacheExpireMinute") + "");
		String spaceKey = BIUtil.isEmpty(params.get("cacheSpaceKey") + "") ? MultiSelectFilterCacheManager.cache_space_key : params.get("cacheSpaceKey") + "";

		logEntity.setSessionId(sessionId);

		try {

			/*** 不开启缓存 ***/
			if (!this.cacheEnabled) {
				result = this.buildResultByNoCache(sessionId);
				logQueryInfo(logEntity, result, true, "");
				resetSpecialFilterValue(result);
				return result;
			}

			/*** 开启缓存 ***/
			// 字段数据已缓存
			boolean isCached = BIUtil.isNotEmpty(cacheRowsValue);

			// 是否第一次初始化加载
			boolean isInit = !isCached && BIUtil.isEmpty(searchValue);

			if (isInit) {
				String sql = this.buildSQL();
				// 不加权限过滤
				this.total = this.queryTotal(sessionId);
				if (this.total <= this.cacheMaxRows && this.total > 0) {
					// 不加权限过滤 、不加分页
					this.rows = this.queryRows(sql, sessionId);
					MultiSelectFilterCacheManager.setRowsCache(spaceKey, metaField.getId(), JSON.toJSONString(this.rows), expireMinute);
					MultiSelectFilterCacheManager.setTotalCache(spaceKey, metaField.getId(), this.total + "", expireMinute);
					//RedisCacheManager.setAsyncCurrentDayActive(cache_space_key, cacheRowsKey, JSON.toJSONString(this.rows), expireMinute);
					//RedisCacheManager.setAsyncCurrentDayActive(cache_space_key, cacheTotalKey, total + "", expireMinute);

					result = new MultiSelectFilterResult(this.rows, this.total, sql);

					// 过滤数据权限
					this.filterResultByDataAuth(result);
					// 手动分页
					result.rows = this.paginate(result.rows);
				} else {
					// 此处不调用buildResultByNoCache，避免多查一次total
					sql = this.appendPaginationSql(sql);
					this.rows = this.queryRows(sql, sessionId);
					// 此处无需过滤数据权限，因为数据权限肯定会被缓存
					// do nothing
					// 分页
					result = new MultiSelectFilterResult(this.rows, this.total, sql);
					result.rows = this.paginate(result.rows);
				}
			} else {
				// 已缓存
				if (isCached && BIUtil.isNotEmpty(cacheRowsValue)) {
					this.rows = JSON.parseArray(cacheRowsValue, BIMap.class);
					String totalStr =  MultiSelectFilterCacheManager.getTotalCache(metaField.getId());//RedisCacheManager.get(cache_space_key, cacheTotalKey);
					if (BIUtil.isNotEmpty(totalStr)) {
						this.total = Integer.valueOf(totalStr);
					}

					result = new MultiSelectFilterResult(this.rows, this.total, "");
					// 过滤数据权限
					this.filterResultByDataAuth(result);
					// 过滤关键字
					this.filterResultBySearch(result);
					// 手动分页
					result.rows = this.paginate(result.rows);
					result.cache = true;
				} else {
					result = this.buildResultByNoCache(sessionId);
				}
			}
		}catch (Throwable e) {
			if (e instanceof QueryCancelException) {
				return result;
			}
		    //兼容处理：当时doris查询报表不存在错时，降级到trino重新查询
			String msg = e.getMessage();
//		    if(!isRetry && dataSourceType == DataSourceType.Doris_Master && msg != null && msg.toLowerCase().contains("unknown ")){
		    if(!isRetry && dataSourceType.isDoris() && msg != null && msg.toLowerCase().contains("unknown ")){
                isRetry = true;
				result = (MultiSelectFilterResult) buildDataset(metaField, params);
            }else {
				String owner = "";
				MetaTable metaTable = SSDMetaCacheManager.getTable(this.metaField.getTableId());
				if (metaTable != null) {
					owner = BIUtil.isNotEmpty(metaTable.getTableOwner()) ? metaTable.getTableOwner() : metaTable.getCreatedBy();
				}
				if (BIUtil.isEmpty(owner)) {
					owner = BIUtil.isNotEmpty(this.metaField.getUpdatedBy()) ? this.metaField.getUpdatedBy() : this.metaField.getCreatedBy();
				}

				logQueryInfo(logEntity, result, false, e.getMessage());
				SSDException exception = new SSDException(String.format("下拉筛选错误，请联系[%s]，原因：%s", owner, e.getMessage()));
				exception.getMailTo().add(owner);
				exception.setSql(this.buildSQL());
				throw exception;
			}
		}

		logQueryInfo(logEntity, result, true, "");
		resetSpecialFilterValue(result);
		return result;
	}

	private void resetSpecialFilterValue(MultiSelectFilterResult result) {
		if (result == null || BIUtil.isEmpty(result.getRows())) {
			return;
		}

		List<BIMap> rows = result.getRows();
		Map<String, String> specialDimFilterValueMap = BIConsts.SPECIAL_DIM_FILTER_VALUE_MAP;
		final Map<String, String> specialDimFilterValueKeyMap = specialDimFilterValueMap.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (x, y) -> x));
		rows.forEach(row -> {
			String value = row.get("id") + "";
			if (specialDimFilterValueKeyMap.containsKey(value)) {
				row.put("id", specialDimFilterValueKeyMap.get(value));
				row.put("name", BIConsts.NULL_VALUE);
			}else if(BIUtil.isEmpty(value)){
				row.put("id", BIConsts.NULL_VALUE);
				row.put("name", BIConsts.NULL_VALUE);
			}
		});
	}

	private void logQueryInfo(SSMFilterQueryLogEntity logEntity, MultiSelectFilterResult result, boolean success, String queryInfo) {
		try {
			logEntity.setIsRetry(isRetry ? Enabled.YES.getId() : Enabled.NO.getId());
			logEntity.setIsHitCache(result.cache ? Enabled.YES.getId() : Enabled.NO.getId());
			logEntity.setQuerySuccess(success ? "success" : "error");
			logEntity.setQueryRows(result.rows.size());
			logEntity.setQueryInfo(queryInfo);
			logEntity.setQuerySql(result.sql);
			logEntity.setQueryEndTime(new Date());
			result.sql = "";
			queryLogService.logFilterQuery(logEntity);
		} catch (Throwable ignored) {
		}
	}

	protected MultiSelectFilterResult buildResultByNoCache(String sessionId){
		String sql = this.buildSQL();
		sql = this.appendDataAuthFilter(sql);
		this.total = this.queryTotal(sessionId);

		sql = this.appendPaginationSql(sql);
		this.rows = this.queryRows(sql, sessionId);
		this.rows = this.paginate(this.rows);
		return new MultiSelectFilterResult(this.rows, this.total, sql);
	}

	protected String buildSQL() {
		String fieldKeyName = metaField.getFieldKeyName();
		String fieldName = metaField.getName();
		String tableName = "";
		StringBuilder querySQL = new StringBuilder();
		if (metaField != null) {
			String filterTableId = metaField.getFilterTableId();
			if (BIUtil.isEmpty(filterTableId)) {
				if (BIUtil.isNotEmpty(metaField.getDimTableId())) {
					filterTableId = metaField.getDimTableId();
				} else if (BIUtil.isNotEmpty(metaField.getFactTableId())) {
					filterTableId = metaField.getFactTableId();
				}
			}

			MetaTable t = SSDMetaCacheManager.getTable(filterTableId);
			if (t != null) {
				tableName = t.getFullName();
			}
			// 构建查询sql
			if(!StringUtil.isEmpty(metaField.getFilterSQL())) {
				querySQL = buildQuerySQLByFilterSQL(metaField);
			} else {
				querySQL = buildQuerySQLByFilterTable(tableName, fieldName, fieldKeyName);
			}
		}
		StringBuilder resultSQL = new StringBuilder();
		resultSQL.append(" select ");
		resultSQL.append("_tmp.id");
		resultSQL.append(",");
		resultSQL.append(" concat(_tmp.name ,'') AS name ");
		resultSQL.append(" from ").append("(").append(querySQL).append(") _tmp");
		resultSQL.append(" where 1=1 ")
				.append(" and _tmp.id is not null ");
		if (!StringUtil.isEmpty(searchValue)) {
			resultSQL.append(" and lower(_tmp.name) LIKE lower('%").append(searchValue).append("%')");
		}

		// 添加权限
		//this.addDataAuthFilter(resultSQL);

		return resultSQL.toString();
	}

	/**
	 * 根据过滤表构建查询sql
	 * @return
	 */
	protected StringBuilder buildQuerySQLByFilterTable(String tableName, String fieldName, String fieldKeyName) {
		StringBuilder sql = new StringBuilder();
		sql.append(" select ");
		sql.append(fieldKeyName).append(" AS ").append("id");
		sql.append(",");
		sql.append(fieldName).append(" AS ").append("name");
		sql.append(" from ").append(tableName).append(" group by ").append(fieldKeyName).append(",").append(fieldName);
		return sql;
	}

	/**
	 * 根据过滤sql构建查询sql
	 * @return
	 */
	protected StringBuilder buildQuerySQLByFilterSQL(MetaField metaField) {
		StringBuilder sql = new StringBuilder();
		sql.append(metaField.getFilterSQL());
		return sql;
	}

	public List<BIMap> queryRows(String sql, String sessionId) {
		String userName = getCurrentUserName();
//		String filterSQL = this.buildSQL();
		StringBuilder datasetSQL = new StringBuilder();
		datasetSQL.append("/* ssm-filter：").append(userName).append(" */ ");
		datasetSQL.append(sql);
//		datasetSQL.append(" order by name ");
		// 每页行大小
		String querySql = datasetSQL.toString();

		BaseDao dao = DBUtil.getBaseDao();
		List<BIMap> dataset = dao.queryObjectListBySQL(this.dataSourceType, querySql, sessionId);

		dataset = this.sortByFieldValueSort(dataset);

		/**
		// 若有分页，但db不支持页面，则截取分页记录数，只返回当前页条数
		DataSourceType dsType = DBUtil.getDataSourceType();
		DBType dbType = DBType.getType(dsType.getDialect());
		if (pagination && (!dbType.getCanPagination())) {
			try {
				int lastIndex = Math.min(dataset.size(), pageSize * currentPageNum);
				dataset = dataset.subList(pageSize * (currentPageNum - 1), lastIndex);
			} catch (Exception e) {
			}
		}
		 */

		return dataset;
	}

	// 根据自定义的排序构建返回数据集
	private List<BIMap> sortByFieldValueSort(List<BIMap> dataset) {
		List<MetaFieldValueSort> fieldValueSortList = SSDMetaCacheManager.getFieldValueSort(metaField.getCode());
		if(BIUtil.isEmpty(fieldValueSortList)){
			fieldValueSortList = Lists.newArrayList(new MetaFieldValueSort(metaField.getCode(), BIConsts.NULL_VALUE, "ZZZ"), new MetaFieldValueSort(metaField.getCode(), "", "ZZZ"));
		}
		/*
		if (CollectionUtil.isEmpty(fieldValueSortList)) {
			return dataset;
		}
		*/
		Map<String, MetaFieldValueSort> fieldValueSortMap = fieldValueSortList.stream().collect(Collectors.toMap(MetaFieldValueSort::getFieldValue, k -> k));
		dataset.sort(new Comparator<BIMap>() {
			@Override
			public int compare(BIMap o1, BIMap o2) {
				try {
					MetaFieldValueSort fieldValueSort1 = fieldValueSortMap.get(String.valueOf(o1.get("name")));
					MetaFieldValueSort fieldValueSort2 = fieldValueSortMap.get(String.valueOf(o2.get("name")));
					String sort1 = fieldValueSort1 == null ? "ZZ" : fieldValueSort1.getFieldValueSortNum();
					String sort2 = fieldValueSort2 == null ? "ZZ" : fieldValueSort2.getFieldValueSortNum();
					return sort1.compareTo(sort2);
				} catch (Exception e) {
					return 0;
				}
			}
		});
		return dataset;
	}

	public Integer queryTotal(String sessionId) {
		Integer count = 0;
		String filterSQL = buildSQL();
		StringBuilder countSQL = new StringBuilder();
		countSQL.append("/*ssm-filter-count：").append(getCurrentUserName()).append(" */");
		countSQL.append("select count(1) as f_count from (")
				.append(filterSQL)
				.append(") tmp_c");
		BaseDao dao = DBUtil.getBaseDao();
		List<BIMap> list = dao.queryObjectListBySQL(this.dataSourceType, countSQL.toString(), sessionId);
		if (BIUtil.isNotEmpty(list)) {
			BIMap m = list.get(0);
			count = Integer.valueOf(m.values().iterator().next() + "");
		}
		return count;
	}

	protected String getCurrentUserName() {
		// 添加权限
		User user = UserManager.get();
		String userName = user == null ? "_unknow_" : user.getName();
		return userName;
	}

	/**
	 * sql添加分页
	 * @param sql
	 * @return
	 */
	protected String appendPaginationSql(String sql) {
		String sortExpression = FieldUtil.getFieldValueSortExpression("tmp.name");

		String sortByAlis = "name" + BIConsts.ORDER_BY_ALIAS_SUFFIX;

		sql = String.format("select *, %s as %s from ( %s ) as tmp %s %s nulls last",
				sortExpression, sortByAlis, sql, BIConsts.ORDER_BY, sortByAlis);

		DataSourceType dsType = this.dataSourceType;
		DBType dbType = DBType.getType(dsType.getDialect());
		IFunction function = FunctionManager.getFunction(dbType);
		String paginationSql = function.appendPagination(sql, currentPageNum, pageSize);
		return paginationSql;
	}

	/**
	 * 结果集分页
	 * @param rows
	 * @return
	 */
	protected List<BIMap> paginate(List<BIMap> rows){
		if(BIUtil.isEmpty(rows)){
			return rows;
		}
		// 若有分页，但db不支持页面，则截取分页记录数，只返回当前页条数
		DataSourceType dsType = this.dataSourceType;
		DBType dbType = DBType.getType(dsType.getDialect());
		List<BIMap> newRows = new ArrayList<>();
		if (pagination && (!dbType.getCanPagination())) {
			try {
				int lastIndex = Math.min(rows.size(), pageSize * currentPageNum);
				newRows = rows.subList(pageSize * (currentPageNum - 1), lastIndex);
			} catch (Exception e) {
			}
		}else {
			newRows = rows;
		}

		return newRows;
	}

	/**
	 * 通过数据权限过滤结果集
	 * @return
	 */
	protected void filterResultByDataAuth(MultiSelectFilterResult result){
		boolean hasDataAuthCfg = SSDMetaCacheManager.hasDataAuthCfg(metaField.getCode());
		if (!hasDataAuthCfg || BIUtil.isEmpty(rows) || total == null || total <= 0) {
			return ;
		}
		String userName = getCurrentUserName();
		// 添加数据权限过滤
		SSDQueryService queryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");
		// 查询用户所有模块维度-字段权限
		List<MetaFieldDataAuth> dataAclList = queryService.getDataAuthByUser(userName);
		dataAclList = dataAclList.stream().filter(da -> {
			return da.getDimCode().equals(metaField.getCode());
		}).collect(Collectors.toList());

		// 是否有所有维度值的权限，若有，则不需要过滤此维度
		boolean hasAllAuth = dataAclList.stream().filter(d -> {
			return BIConsts.Ssm_Row_Acl_All_Dim_Value.equals(d.getItemCode());
		}).count() > 0;
		if (hasAllAuth) {
			return;
		}
		// 字段配置了权限，但用户无权限
		if (BIUtil.isEmpty(dataAclList)) {
			result.rows = new ArrayList<>();
			result.total = 0;
		} else {
			List<String> authValues = dataAclList.stream().map(da -> da.getItemCode()).collect(Collectors.toList());
			result.rows = this.rows.stream().filter(m ->{return authValues.contains(m.get("id"));}).collect(Collectors.toList());
			result.total = result.rows.size();
		}
	}

	/**
	 * 通过搜索关键字过来结果集
	 * @param result
	 */
	protected void filterResultBySearch(MultiSelectFilterResult result){
		if(BIUtil.isEmpty(this.searchValue)){
			return ;
		}
		result.rows = result.rows.stream().filter(m ->{
			return (m.get("id") + "").toLowerCase().contains(this.searchValue.toLowerCase());
		}).collect(Collectors.toList());
		result.total = result.rows.size();
	}

	/**
	 * 添加数据权限过滤
	 * @param
	 */
	protected String appendDataAuthFilter(String sql) {
		String userName = getCurrentUserName();
		boolean hasDataAuthCfg = SSDMetaCacheManager.hasDataAuthCfg(metaField.getCode());
		if (!hasDataAuthCfg) {
			return sql;
		}

		StringBuilder dataAuthFilterSql = new StringBuilder();
		dataAuthFilterSql.append(sql);
		// 添加数据权限过滤
		SSDQueryService queryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");
		// 查询用户所有模块维度-字段权限
		List<MetaFieldDataAuth> dataAclList = queryService.getDataAuthByUser(userName);
		dataAclList = dataAclList.stream().filter(da -> {
			return da.getDimCode().equals(metaField.getCode());
		}).collect(Collectors.toList());

		// 是否有所有维度值的权限，若有，则不需要过滤此维度
		boolean hasAllAuth = dataAclList.stream().filter(d -> {
			return BIConsts.Ssm_Row_Acl_All_Dim_Value.equals(d.getItemCode());
		}).count() > 0;
		if (hasAllAuth) {
			return sql;
		}
		// 字段配置了权限，但用户无权限
		if (BIUtil.isEmpty(dataAclList)) {
			dataAuthFilterSql.append(" AND 1 = 2");
		} else {
			String filterValues = "";
			List<String> authValues = new ArrayList<>();
			authValues = dataAclList.stream().map(da -> da.getItemCode()).collect(Collectors.toList());
			if (DataType.String != DataType.getType(metaField.getFieldKeyType())) {
				filterValues = BIUtil.listToStr(authValues, ",");
			} else {
				filterValues = BIUtil.listToStr(authValues, ",", "'");
			}
			dataAuthFilterSql.append(" AND _tmp.id in(").append(filterValues).append(")");
		}

		return dataAuthFilterSql.toString();
	}

	/**
	 * 清理全部缓存
	 */
	public static void clearCache(){
		//RedisCacheManager.delete(cache_space_key);
		MultiSelectFilterCacheManager.clearCache();
	}

	/**
	 * 清理单个字段缓存
	 * @param fieldId
	 */
	public static void clearCache(String fieldId){
		MultiSelectFilterCacheManager.clearCache(fieldId);
		/*
		String cacheRowsKey = cache_key_prefix +  fieldId;
		String cacheTotalKey = cacheRowsKey + cache_key_total_suffix;
		RedisCacheManager.delete(cache_space_key, cacheRowsKey);
		RedisCacheManager.delete(cache_space_key, cacheTotalKey);
		 */
	}

	protected DataSourceType getDataSourceType(){
		DataSourceType dsType = DataSourceType.Trino_Master; //DBUtil.getDataSourceType();
        if(isRetry){
            return dsType;
        }
		// 是支持doris的表字段，且过滤sql表也是支持doris
		MetaTable metaTable = SSDMetaCacheManager.getTable(this.metaField.getTableId());
		if(isUseDorisDs(metaTable)){
//			dsType = DataSourceType.Doris_Master;
			dsType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
		}
		/*
		if(metaTable != null && metaTable.getSupportQueryEngines().contains(DBType.Doris.toString().toLowerCase())){
			dsType = DataSourceType.Doris_Master;
		}
		 */
		return dsType;
	}

	/**
	 * 是否使用doris数据源
	 * @param metaTable
	 * @return
	 */
	public boolean isUseDorisDs(MetaTable metaTable) {

		if (metaTable != null && metaTable.getFullName().toLowerCase().contains("bi_olap.")) {
			return true;
		}

		if (metaTable != null && metaTable.getSupportQueryEngines().contains(DBType.Doris.toString().toLowerCase())) {
			return true;
		}

		return false;
	}

}
