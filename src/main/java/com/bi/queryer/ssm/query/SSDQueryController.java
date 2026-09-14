package com.bi.queryer.ssm.query;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.enums.QueryModeType;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.meta.dateRangeProgress.DateRangeProgressReq;
import com.bi.queryer.ssm.meta.dateRangeProgress.DateRangeProgressRsp;
import com.bi.queryer.ssm.mgr.tableDef.TableDefService;
import com.bi.queryer.ssm.query.filter.IFilterDatasetProvider;
import com.bi.queryer.ssm.query.filter.MultiSelectFilterResult;
import com.bi.queryer.ssm.query.rt.SSMRTQueryService;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.KillRemoteQueryService;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import com.bi.queryer.sys.role.RoleService;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserService;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.render.VueDataGridRender;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * User: contributor
 * Date: 2020/2/3
 * Time: 12:16
 * Description:
 * 处理多维分析主界面所有请求
 */
@Controller
@Scope("prototype")
@RequestMapping("ssd")
public class SSDQueryController extends QueryController {

	@Autowired
	protected SSDQueryService service = null;

	@Autowired
	private TableDefService tableDefService = null;

	@Autowired
	private UserService userService = null;

	@Autowired
	private RoleService roleService;

	@Autowired
	private SSMRTQueryService ssmrtQueryService = null;

	public static final String Cookies_Token = "_token";

	/** 业务线类筛选字段编码配置项，逗号分隔；未配置则不处理 */
	private static final String BUSINESSLINE_FILTER_FIELD_CODES_KEY = "ssm.filter.businessline.field.codes";

	/** 业务线类筛选需追加的下拉值 */
	private static final List<String> BUSINESSLINE_FILTER_APPEND_VALUES = Arrays.asList(
			"保养", "保养油液", "保养配件",
			"改装超市", "改装升级与车品超市", "电子改装", "电瓶车");


	/**
	 * 获取查询结果
	 */
	@RequestMapping("result/grid")
	@ResponseBody
	public ResponseMessage buildQueryResultGrid() {
		QueryEngine engine = createEngine(true);
		List<QueryField> measures = engine.getConfig().getResult().getMeasures();
		long measureCount = measures.stream()
							.filter(a -> Enabled.isTrue(a.getIsShow()))
							.filter(a -> Enabled.isFalse(a.getIsAnalysis())).count();
		int measureCountLimit = Integer.parseInt(SC.v("ssm.query.measure.count.limit", "50"));
		checkState(measureCount <= measureCountLimit, "受资源限制，大查询消耗过多资源影响其他同学使用体验，单次查询指标数量限制%s个，当前查询有%s个指标，请减少指标数量。", measureCountLimit, measureCount);
		return engine.execute();
	}

	/**
	 * 获取查询结果SQL
	 * @return
	 */
	@RequestMapping("result/sql")
	@ResponseBody
	public ResponseMessage buildResultSQL() {
		long startTime = System.currentTimeMillis();
		ResponseMessage result = new ResponseMessage();
		QueryEngine engine = createEngine(true);
		String sql = engine.buildSql();

		//order by 使用开窗函数，存在语法问题，需要进行sql修正
		sql = SSDUtil.rectifyOrderBySql(sql,engine.getSqlTips());

		result.setData(sql);
		long endTime = System.currentTimeMillis();
		System.out.println("buildResultSQL程序运行时间：" + (endTime - startTime) + "ms");
		return result;
	}

	/**
	 * 获取查询受到的行级权限字段code
	 * @return
	 */
	@RequestMapping("query/getRowAuthFieldCodes")
	@ResponseBody
	public ResponseMessage getQueryRowAuthFieldCodes() {
		ResponseMessage result = new ResponseMessage();
		QueryEngine engine = createEngine(true);
		Set<String> codes = engine.getDataRowAuthFieldCodes();
		result.setData(codes);
		return result;
	}

	/**
	 * 获取查询结果数量
	 * @return
	 */
	@RequestMapping("result/queryCount")
	@ResponseBody
	public ResponseMessage buildResultQueryCount() {
		long startTime = System.currentTimeMillis();
		ResponseMessage result = new ResponseMessage();
		QueryEngine engine = createEngine(true);
		Integer queryCount = engine.getDataSetTotalSize(engine.buildSql());
		result.setData(queryCount);
		long endTime = System.currentTimeMillis();
		System.out.println("queryCount程序运行时间：" + (endTime - startTime) + "ms");
		return result;
	}

	/**
	 * 查询模板sql供外部系统使用
	 * @return
	 */
	@RequestMapping("queryResultSqlForExternal")
	@ResponseBody
	@FreeCheckAuthority
	public ResponseMessage queryResultSqlForExternal() {
		ResponseMessage result = new ResponseMessage();

		//设置当前线程的用户，构造sql需要用户权限
		String userName = this.stringValue("userName");
		User user = userService.queryByName(userName);
		UserManager.set(user);

		QueryEngine engine = createEngine(true);
		String sql = engine.buildSql();
		result.setData(sql);
		return result;
	}

	/**
	 * 查询模板字段
	 * @return
	 */
	@RequestMapping("queryTemplateFields")
	@ResponseBody
	@FreeCheckAuthority
	public ResponseMessage queryTemplateFields() {

		ResponseMessage result = new ResponseMessage();
		JSONArray jsonArray = new JSONArray();

		//设置当前线程的用户，构造sql需要用户权限
		String userName = this.stringValue("userName");
		User user = userService.queryByName(userName);
		UserManager.set(user);

		QueryEngine engine = createEngine(true);

		List<QueryField> fieldList = new ArrayList<>();
		//获取模板字段
		Optional<List<QueryField>> opQf = Optional.ofNullable(engine)
				.map(a -> a.getConfig())
				.map(b -> b.getResult())
				.map(c -> c.getFields());

		if (opQf.isPresent()) {
			fieldList = opQf.get();
		}

		if (BIUtil.isNotEmpty(fieldList)) {

			for (QueryField queryField : fieldList) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("fieldName", queryField.getName());
				jsonObject.put("fieldTitle", queryField.getTitle());

				jsonArray.add(jsonObject);
			}
		}


		result.setData(jsonArray);
		return result;
	}

	/**
	 * 查询模板etljobs供外部系统使用
	 * @return
	 */
	@RequestMapping("queryEtlJobsForExternal")
	@ResponseBody
	@FreeCheckAuthority
	public ResponseMessage queryEtlJobsForExternal() {

		ResponseMessage result = new ResponseMessage();

		//设置当前线程的用户，构造sql需要用户权限
		String userName = this.stringValue("userName");
		User user = userService.queryByName(userName);
		UserManager.set(user);

		QueryEngine engine = createEngine(true);

		//关联的etl依赖
		List<String> tableIds = new ArrayList<>();
		Optional<List<String>> ops = Optional.ofNullable(engine).map(a -> a.getQueryTableIdList());
		if (ops.isPresent()) {
			tableIds = ops.get();
		}

		List<String> etlJobs = tableDefService.selectEtlJobByTableIdList(tableIds);
		result.setData(etlJobs);

		return result;
	}

	/**
	 * 过滤器数据
	 * @return
	 */
	@RequestMapping("filter/data")
	@ResponseBody
	public ResponseMessage buildFilterData() {
		ResponseMessage result = new ResponseMessage();
		String fieldId = this.stringValue("fieldId"); // 字段id
		String fieldCode = this.stringValue("fieldCode");

		Object filterData = null;
		String providerBeanName = "";
		MetaField metaField = null;

		//特殊处理计算维度下拉数据获取
		String fieldType = this.stringValue("fieldType");
		if (FieldType.CUSTOM_DIM == FieldType.get(fieldType)) {
			providerBeanName = "customDimFilterDatasetProvider";
		} else {
			metaField = getFilterMetaField(fieldId, fieldCode);
			providerBeanName = FieldFilterType.get(metaField.getFilterShowType()).toString() + "FilterDatasetProvider";
			providerBeanName = providerBeanName.substring(0, 1).toLowerCase() + providerBeanName.substring(1);
		}

		// 字段Code
		IFilterDatasetProvider filterDatasetProvider = (IFilterDatasetProvider) SpringContextUtil.getBean(providerBeanName);
		filterData = filterDatasetProvider.buildDataset(metaField, this.params);

		String effectiveFieldCode = resolveFilterFieldCode(fieldCode, metaField);
		appendBusinesslineFilterValues(filterData, effectiveFieldCode);

		result.setData(filterData);
		return result;
	}

	/**
	 * 解析筛选字段编码，优先使用请求参数 fieldCode。
	 *
	 * @param fieldCode 请求参数中的字段编码
	 * @param metaField 元数据字段
	 * @return 有效字段编码
	 */
	private String resolveFilterFieldCode(String fieldCode, MetaField metaField) {
		if (StringUtils.isNotEmpty(fieldCode)) {
			return fieldCode;
		}
		if (metaField != null) {
			return metaField.getCode();
		}
		return "";
	}

	/**
	 * 业务线类筛选字段在下拉数据中追加固定维值，并按字段值排序规则重排。
	 *
	 * @param filterData 筛选数据集
	 * @param fieldCode  字段编码
	 */
	private void appendBusinesslineFilterValues(Object filterData, String fieldCode) {
		if (!isBusinesslineFilterField(fieldCode) || !(filterData instanceof MultiSelectFilterResult)) {
			return;
		}

		MultiSelectFilterResult filterResult = (MultiSelectFilterResult) filterData;
		List<BIMap> rows = filterResult.getRows();
		if (rows == null) {
			rows = new ArrayList<>();
			filterResult.setRows(rows);
		}

		Set<String> existingIds = rows.stream()
				.map(row -> row.get("id") + "")
				.collect(Collectors.toSet());

		int addedCount = 0;
		for (String value : BUSINESSLINE_FILTER_APPEND_VALUES) {
			if (existingIds.contains(value)) {
				continue;
			}
			BIMap row = new BIMap();
			row.put("id", value);
			row.put("name", value);
			rows.add(row);
			existingIds.add(value);
			addedCount++;
		}

		if (addedCount > 0) {
			if (filterResult.getTotal() != null) {
				filterResult.setTotal(filterResult.getTotal() + addedCount);
			}
			sortBusinesslineFilterRows(rows, fieldCode);
		}
	}

	/**
	 * 按字段值自定义排序重排下拉数据，逻辑与 MultiSelectFilterDatasetProvider 保持一致。
	 * 追加维值未配置排序号时，按 {@link #BUSINESSLINE_FILTER_APPEND_VALUES} 顺序兜底。
	 *
	 * @param rows      下拉数据行
	 * @param fieldCode 字段编码
	 */
	private void sortBusinesslineFilterRows(List<BIMap> rows, String fieldCode) {
		if (CollUtil.isEmpty(rows) || StringUtils.isEmpty(fieldCode)) {
			return;
		}

		List<MetaFieldValueSort> fieldValueSortList = SSDMetaCacheManager.getFieldValueSort(fieldCode);
		if (BIUtil.isEmpty(fieldValueSortList)) {
			fieldValueSortList = Lists.newArrayList(
					new MetaFieldValueSort(fieldCode, BIConsts.NULL_VALUE, "ZZZ"),
					new MetaFieldValueSort(fieldCode, "", "ZZZ"));
		}

		Map<String, MetaFieldValueSort> fieldValueSortMap = fieldValueSortList.stream()
				.collect(Collectors.toMap(MetaFieldValueSort::getFieldValue, item -> item, (left, right) -> left));

		Map<String, Integer> appendValueOrderMap = new HashMap<>();
		for (int index = 0; index < BUSINESSLINE_FILTER_APPEND_VALUES.size(); index++) {
			appendValueOrderMap.put(BUSINESSLINE_FILTER_APPEND_VALUES.get(index), index);
		}

		rows.sort((row1, row2) -> {
			try {
				String name1 = String.valueOf(row1.get("name"));
				String name2 = String.valueOf(row2.get("name"));
				String sortNum1 = resolveBusinesslineFilterSortNum(fieldValueSortMap, appendValueOrderMap, name1);
				String sortNum2 = resolveBusinesslineFilterSortNum(fieldValueSortMap, appendValueOrderMap, name2);
				int compare = sortNum1.compareTo(sortNum2);
				if (compare != 0) {
					return compare;
				}
				return name1.compareTo(name2);
			} catch (Exception ignored) {
				return 0;
			}
		});
	}

	/**
	 * 解析下拉维值排序号：优先元数据配置，追加维值使用固定列表顺序兜底。
	 *
	 * @param fieldValueSortMap  字段值排序配置
	 * @param appendValueOrderMap 追加维值顺序
	 * @param fieldValue         维值名称
	 * @return 排序号
	 */
	private String resolveBusinesslineFilterSortNum(Map<String, MetaFieldValueSort> fieldValueSortMap,
													Map<String, Integer> appendValueOrderMap,
													String fieldValue) {
		MetaFieldValueSort fieldValueSort = fieldValueSortMap.get(fieldValue);
		if (fieldValueSort != null) {
			return fieldValueSort.getFieldValueSortNum();
		}
		Integer appendOrder = appendValueOrderMap.get(fieldValue);
		if (appendOrder != null) {
			return String.format(Locale.ROOT, "ZZ%03d", appendOrder);
		}
		return "ZZ";
	}

	/**
	 * 判断是否为业务线类筛选字段，字段编码来自配置 {@link #BUSINESSLINE_FILTER_FIELD_CODES_KEY}。
	 *
	 * @param fieldCode 字段编码
	 * @return 命中时返回 true；配置为空时不处理
	 */
	private boolean isBusinesslineFilterField(String fieldCode) {
		if (StringUtils.isEmpty(fieldCode)) {
			return false;
		}
		Set<String> fieldCodes = getBusinesslineFilterFieldCodes();
		if (fieldCodes.isEmpty()) {
			return false;
		}
		return fieldCodes.contains(fieldCode.toLowerCase(Locale.ROOT));
	}

	/**
	 * 从配置读取业务线类筛选字段编码集合（大小写不敏感）。
	 *
	 * @return 配置为空时返回空集合
	 */
	private Set<String> getBusinesslineFilterFieldCodes() {
		String configValue = SC.v(BUSINESSLINE_FILTER_FIELD_CODES_KEY);
		if (StringUtils.isEmpty(configValue)) {
			return Collections.emptySet();
		}
		return Arrays.stream(configValue.split(","))
				.map(String::trim)
				.filter(StringUtils::isNotEmpty)
				.map(code -> code.toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());
	}


	private MetaField getFilterMetaField(String fieldId, String fieldCode) {
		MetaField metaField = SSDMetaCacheManager.getField(fieldId);
		if (metaField == null || FieldFilterType.None == FieldFilterType.get(metaField.getFilterShowType())) {
			if (StringUtils.isNotEmpty(fieldCode)) {
				List<MetaField> metaFields = SSDMetaCacheManager.getFieldByCode(fieldCode);
				if (CollUtil.isNotEmpty(metaFields)) {
					metaField = metaFields.stream().filter(v -> FieldFilterType.None != FieldFilterType.get(v.getFilterShowType()))
							.findFirst().orElse(null);
					if (metaField != null) {
						return metaField;
					}
				}
			}
		} else {
			return metaField;
		}
		throw new BIException("字段维值设置未找到：" + fieldId);
	}

	@RequestMapping("filter/query/kill")
	@ResponseBody
	public ResponseMessage killFilterValueQuery() {
		ResponseMessage result = new ResponseMessage();
		String sessionId = this.stringValue("sessionId");
		result.setData(KillRemoteQueryService.killQuery(sessionId));
		return result;
	}

	/**
	 * 查询字段filterValueMode
	 * @return 默认返回agg
	 */
	@RequestMapping("filter/getFilterValueMode")
	@ResponseBody
	public ResponseMessage getFilterValueMode() {
		ResponseMessage result = new ResponseMessage();
		String fieldId = this.stringValue("fieldId"); // 字段id
		MetaField metaField = SSDMetaCacheManager.getField(fieldId);
		if (metaField == null) {
			throw new BIException("字段不存在：" + fieldId);
		}
		Map<String, Object> data = new HashMap<String, Object>();
		String aggExpression = metaField.getAggExpression();
		if (!StringUtil.isEmpty(aggExpression) && (aggExpression.indexOf("[") != -1 || "count".equals(aggExpression))) {
			data.put("readOnly", 1);
			data.put("value", "agg");
			result.setData(data);
			return result;
		}
		data.put("readOnly", 0);
		data.put("value", StringUtils.isNotBlank(metaField.getFilterValueMode()) ? metaField.getFilterValueMode() : "agg");
		result.setData(data);
		return result;
	}

	/**
	 * 获取查询结果
	 */
	@RequestMapping("my/auth/grid")
	@ResponseBody
	public ResponseMessage buildDataAuthGrid() {
		ResponseMessage result = new ResponseMessage();
		this.params.put("userName", this.getUser() == null ? "" : this.getUser().getName());
		this.params.put("deptId", this.getUser() == null ? "" : this.getUser().getDeptId());
		this.params.put("pageQuery", "true");
		DataGrid grid = service.buildDataAuthGrid(toStringMap());
		grid.setRenderClassName(VueDataGridRender.class.getName());
		JSONObject dataset = grid.toJSON();
		result.setData(dataset);
		return result;
	}

	/**
	 * 获取系统配置
	 * @return
	 */
	@RequestMapping("getSystemConfig")
	@ResponseBody
	public ResponseMessage getSystemConfig() {
		return service.getSystemConfig();
	}

	/**
	 * 发送申请敏感数据邮件
	 * @return
	 */
	@RequestMapping("sendApplySensitiveDataMail")
	@ResponseBody
	public ResponseMessage sendApplySensitiveDataMail() {
		return service.sendApplySensitiveDataMail(this.params);
	}

	/**
	 * 获取用户拥有的所有行级权限
	 * @return
	 */
	@RequestMapping("getUserAllSSDRowRole")
	@ResponseBody
	public ResponseMessage getUserAllSSDRowRole() {
		long startTime1 = System.currentTimeMillis();
		ResponseMessage temp = service.getUserAllSSDRowRole();
		long endTime1 = System.currentTimeMillis();
		System.out.println("getUserAllSSDRowRole:程序运行时间：" + (endTime1 - startTime1) + "ms");
		return temp;
	}

	/**
	 * 邮件订阅-保存
	 * @return
	 */
	@RequestMapping("saveMailSubscription")
	@ResponseBody
	public ResponseMessage saveMailSubscription() {

		Map<String, Object> map = new HashMap<>();
		String sql = "";
		JSONArray jsonArray = new JSONArray();

		SSDQueryTemplate queryTemplate = this.createTemplateFromRequest();

		if (queryTemplate == null) {
			return new ResponseMessage(false, "当前模板不存在，无法订阅");
		}

		QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
		queryConfigure.load();

		//判断字段是否为空
		if (BIUtil.isNotEmpty(queryConfigure.getResult().getFields())) {

			QueryEngine engine = QueryFactory.createEngine(queryConfigure, new QueryContext(getRequest())); //new QueryEngine(queryConfigure, new QueryContext(getRequest()));
			sql = engine.buildSql();

			List<QueryField> fieldList = new ArrayList<>();
			//获取模板字段
			Optional<List<QueryField>> opQf = Optional.ofNullable(engine)
					.map(a -> a.getConfig())
					.map(b -> b.getResult())
					.map(c -> c.getFields());

			if (opQf.isPresent()) {
				fieldList = opQf.get();
			}

			if (BIUtil.isNotEmpty(fieldList)) {

				for (QueryField queryField : fieldList) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("fieldName", queryField.getName());
					jsonObject.put("fieldTitle", queryField.getTitle());

					jsonArray.add(jsonObject);
				}
			}

			//关联的etl依赖
			List<String> tableIds = new ArrayList<>();
			Optional<List<String>> ops = Optional.ofNullable(engine).map(a -> a.getQueryTableIdList());
			if (ops.isPresent()) {
				tableIds = ops.get();
			}

			List<String> etlJobs = tableDefService.selectEtlJobByTableIdList(tableIds);
			if (BIUtil.isNotEmpty(etlJobs)) {
				map.put("etlJobs", etlJobs);
			}
		}

		//主题
		map.put("emailSubject", this.stringValue("emailSubject"));
		//触发规则
		map.put("triggerRule", this.stringValue("triggerRule"));
		//来源id
		map.put("sourceId", this.stringValue("templateId"));

		// 1 先从request的参数中获取
		String token = request.getParameter(BIConsts.User_Token);

		//2 再从request的header中获取
		if (BIUtil.isEmpty(token)) {
			token = request.getHeader(BIConsts.User_Token);
		}

		//3 最后从cookies中获取
		if (BIUtil.isEmpty(token)) {
			token = BIUtil.getCookie(BIConsts.Cookies_Prefix + Cookies_Token, request);
		}

		return service.saveMailSubscription(map, sql, token, jsonArray);
	}


	/**
	 * 字段合法性检测
	 * @return
	 */
	@RequestMapping("checkField")
	@ResponseBody
	@Deprecated
	public ResponseMessage checkField() {

		String ctgId = this.stringValue("ctgId");
		ResponseMessage result = new ResponseMessage();

		//1. 查询目录下所有可查询的字段
		List<MetaField> metaFieldList = service.queryFieldsByCtgId(ctgId);

		Map<String, MetaField> fieldMap = SSDMetaCacheManager.getFieldsCache();
		List<MetaField> commonDateFields = fieldMap.values().stream().filter(item -> item.getIsCommonDate() == 1).collect(Collectors.toList());


		if (BIUtil.isNotEmpty(metaFieldList)) {

			//2. 构建查询引擎config
			List<JSONObject> measuresList = new ArrayList<>();
			List<JSONObject> rowDimensionsList = new ArrayList<>();
			MetaField commonDate = null;
			String metaFieldCtgId = SSDMetaCacheManager.getCategories().get(ctgId).getTopParentCtgId();
			for (int i = 0; i < metaFieldList.size(); i++) {

				MetaField metaField = metaFieldList.get(i);
				for (MetaField commonDateField : commonDateFields) {
					if (commonDate != null) {
						break;
					}
					MetaFieldCategory commonDateFieldCtg = SSDMetaCacheManager.getCategories().get(commonDateField.getCategoryId());
					if (commonDateFieldCtg != null && metaFieldCtgId.equals(commonDateFieldCtg.getTopParentCtgId())) {
						commonDate = commonDateField;
					}
				}

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("code", metaField.getCode());
				jsonObject.put("displayTitle", "");
				jsonObject.put("id", metaField.getId());
				jsonObject.put("name", metaField.getName());
				jsonObject.put("showOrder", (i + 1));
				jsonObject.put("sortType", "NONE");
				jsonObject.put("title", metaField.getTitle());

				if (Enabled.value(metaField.getIsMeasure())) {
					measuresList.add(jsonObject);
				} else {
					rowDimensionsList.add(jsonObject);
				}
			}

			if (commonDate == null) {
				commonDate = commonDateFields.get(0);
			}

			JSONObject resultJson = new JSONObject();
			resultJson.put("measures", measuresList);
			resultJson.put("rowDimensions", rowDimensionsList);

			JSONObject config = new JSONObject();
			config.put("result", resultJson);
			List<JSONObject> filterList = new ArrayList<>();
			filterList.add(commonDate.toJSON());
			config.put("filter", filterList);
			config.put("setting", new JSONObject());
			this.params.put("config", config);

			QueryEngine engine = createEngine(true);
			String sql = engine.buildSql();

			result = service.checkField(sql);
		}

		return result;
	}

	/**
	 * 从请求中创建问题回答类
	 * @return
	 */
	protected SSDQuestionAndAnswer createQuestionAndAnswerRequest() {
		SSDQuestionAndAnswer entity = new SSDQuestionAndAnswer();
		Integer pageSize = this.intValue("pageSize");
		if (pageSize != null) {
			entity.setPageSize(pageSize);
		}
		Integer pageNum = this.intValue("pageNum");
		if (pageNum != null) {
			entity.setPageNum(pageNum);
		}
		Long questionId = this.longValue("questionId");
		if (questionId != null) {
			entity.setQuestionId(questionId);
		}
		String answerOwner = this.stringValue("answerOwner");
		if (BIUtil.isNotEmpty(answerOwner)) {
			entity.setAnswerOwner(answerOwner);
		}
		String questionOwner = this.stringValue("questionOwner");
		if (BIUtil.isNotEmpty(questionOwner)) {
			entity.setQuestionOwner(questionOwner);
		}
		String objectType = this.stringValue("objectType");
		if (BIUtil.isNotEmpty(objectType)) {
			entity.setObjectType(objectType);
		}
		String objectId = this.stringValue("objectId");
		if (BIUtil.isNotEmpty(objectId)) {
			entity.setObjectId(objectId);
		}
		String objectTitle = this.stringValue("objectTitle");
		if (BIUtil.isNotEmpty(objectTitle)) {
			entity.setObjectTitle(objectTitle);
		}
		String questionTimeBegin = this.stringValue("questionTimeBegin");
		if (BIUtil.isNotEmpty(questionTimeBegin)) {
			entity.setQuestionTimeBegin(questionTimeBegin);
		}
		String questionTimeEnd = this.stringValue("questionTimeEnd");
		if (BIUtil.isNotEmpty(questionTimeEnd)) {
			entity.setQuestionTimeEnd(questionTimeEnd);
		}
		String questionContent = this.stringValue("questionContent");
		if (BIUtil.isNotEmpty(questionContent)) {
			entity.setQuestionContent(questionContent);
		}
		String questionTime = this.stringValue("questionTime");
		if (BIUtil.isNotEmpty(questionTime)) {
			entity.setQuestionTime(questionTime);
		}
		String answerContent = this.stringValue("answerContent");
		if (BIUtil.isNotEmpty(answerContent)) {
			entity.setAnswerContent(answerContent);
		}
		Integer isActive = this.intValue("isActive");
		if (isActive != null) {
			entity.setIsActive(isActive);
		}
		Integer isAnswer = this.intValue("isAnswer");
		if (isAnswer != null) {
			entity.setIsAnswer(isAnswer);
		}
		Double sortId = this.doubleValue("sortId");
		if (sortId != null) {
			entity.setSortId(sortId);
		}
		String orderBy = this.stringValue("orderBy");
		if (BIUtil.isNotEmpty(orderBy)) {
			entity.setOrderBy(orderBy);
		}

		return entity;
	}


	public SSDQueryService getService() {
		return service;
	}

	public void setService(SSDQueryService service) {
		this.service = service;
	}

	@Override
	public JSONObject toJSON() {
		return null;
	}

	@Override
	protected QueryConfigure createQueryConfigure(SSDQueryTemplate queryTemplate) {
		return new QueryConfigure(queryTemplate);
	}

	/**
	 * 校验是否是SSD管理员
	 * @return
	 */
	@RequestMapping("checkIsSSDAdmin")
	@ResponseBody
	public ResponseMessage checkIsSSDAdmin() {
		ResponseMessage result = new ResponseMessage();

		try {
			/**
			 User user = UserManager.get();
			 //管理员和多维分析管理员可以又所有权限
			 String roleId = SC.v("ssm.admin.roleId", "");
			 Boolean flag = roleService.queryRoleUserNameExist(roleId, user.getName());
			 if (Enabled.value(user.getIsAdmin()) || flag) {
			 result.setData(true);
			 } else {
			 result.setData(false);
			 }
			 */
			boolean isAdminRole = roleService.isAdminRole();
			result.setData(isAdminRole);

		} catch (Exception e) {
			return new ResponseMessage(e);
		}

		return result;
	}

	/**
	 * 校验是否能够编辑公共模板权限
	 * 废弃：迁移到TemplateController中
	 * @return
	 */
	@RequestMapping("checkIsEditPublicTemplateCtg")
	@ResponseBody
	@Deprecated
	public ResponseMessage checkIsEditPublicTemplateCtg() {
		ResponseMessage result = new ResponseMessage();

		try {
			User user = UserManager.get();
			Boolean adminflag = (Boolean) checkIsSSDAdmin().getData();
			if (adminflag) {
				result.setData(true);
			} else {
				Integer count = userService.queryTemplateCtgDataAuthByUserCount(user.getName());
				if (count > 0) {
					result.setData(true);
				} else {
					result.setData(false);
				}
			}

		} catch (Exception e) {
			return new ResponseMessage(e);
		}

		return result;
	}

	/**
	 * 校验查询时的规范
	 * @return
	 */
	@RequestMapping("checkQueryRules")
	@ResponseBody
	public ResponseMessage checkQueryRules() {

		String config = stringValue("config");
		QueryConfigure queryConfigure = new QueryConfigure();
		queryConfigure.load(config);

		ResponseMessage result = new ResponseMessage();
		Map<String, Object> data = new HashMap<>();
		try {
			data = service.checkQueryRules(queryConfigure);
		} catch (Exception e) {
			data.put("flag", false);
			data.put("message", e.getMessage());

		}
		result.setData(data);
		return result;
	}

	/**
	 * 校验数据就绪时间
	 * @return
	 */
	@RequestMapping("checkDataAvailableTime")
	@ResponseBody
	public ResponseMessage checkDataAvailableTime() {
		String id = stringValue("id");
		String config = stringValue("config");
		QueryConfigure queryConfigure = new QueryConfigure();
		queryConfigure.load(config);

		DataAvailableTimeResp dataAvailableTimeResp = new DataAvailableTimeResp();

		//只查表头、查询模拟数据，不校验数据就绪时间
		QueryModeType queryModeType = queryConfigure.getSettings().getQueryModeType();
		if(QueryModeType.COLUMNS == queryModeType || QueryModeType.MOCK_DATA == queryModeType) {
			return new ResponseMessage(dataAvailableTimeResp);
		}

		//20250114 日粒度不再校验数据可用日期
		DateGranularity dateGranularity = DateGranularity.get(queryConfigure.getSettings().getDateGranularity());
		if(DateGranularity.DAY == dateGranularity){
			if(!"true".equalsIgnoreCase(SC.v("ssm.day.data.available.time.check.enable", "false"))){
				return new ResponseMessage(dataAvailableTimeResp);
			}
		}

		ResponseMessage result = new ResponseMessage();
		QueryEngine engine = createEngine(false);

		dataAvailableTimeResp = service.checkDataAvailableTime(engine);
		result.setData(dataAvailableTimeResp);

		return result;
	}

	/**
	 * 校验查询时的规范
	 * @return
	 */
	@RequestMapping("checkQueryAuth")
	@ResponseBody
	public ResponseMessage checkQueryAuth() {

		//20240628 此处校验的字段id，查询时校验的字段code，存在误导，暂时不校验
		Map<String, Object> data = new HashMap<>();
		data.put("needApply",false);
		data.put("message","");
		data.put("success",true);
		return  new ResponseMessage(data);

//		String config = stringValue("config");
//		QueryConfigure queryConfigure = new QueryConfigure();
//		queryConfigure.load(config);
//
//		ResponseMessage result = new ResponseMessage();
//		Map<String, Object> data = new HashMap<>();
//		QueryEngine engine = createEngine(false);
//
//		try {
//			data = service.checkQueryAuth(engine);
//			if (CollectionUtil.isNotEmpty(data.keySet())) {
//				data.put("needApply", true);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			data.put("message", e.getMessage());
//		}
//		result.setData(data);
//		return result;
	}


	@RequestMapping("kill/query")
	@ResponseBody
	public ResponseMessage killQuery() {
		ResponseMessage result = new ResponseMessage();
		try {
			QueryEngine engine = this.createEngine(false);
			engine.killQuery();
		} catch (Exception e) {
			// 不处理：避免前台查询grid时报错
		}
		return result;
	}

	/**
	 * 获取查询结果SQL+原始查询表
	 * 说明：仅用于第三方应用，不可内部调用
	 * @return
	 */
	@RequestMapping("result/sqlAndEtlJobs")
	@ResponseBody
	public ResponseMessage buildResultSQLAndEtlJobs() {
		ResponseMessage result = new ResponseMessage();
		try {
			QueryEngine engine = createEngine(true);
			String sql = engine.buildSql();
			Map<String, Object> data = new HashMap<>();
			data.put("sql", sql);
			List<String> etlJobs = new ArrayList<>();
			// 获取表
			List<String> tableIdList = engine.getQueryTableIdList();
			if (BIUtil.isNotEmpty(tableIdList)) {
				etlJobs = tableDefService.selectEtlJobByTableIdList(tableIdList);
			}
			data.put("datasourceType", DataSourceRouter.getCurrentDataSourceType().getDialect());
			data.put("etlJobs", etlJobs);
			result.setData(data);
		}catch (Exception e){
			e.printStackTrace();
		}finally {
			// 禁用数据源路由
			DataSourceRouter.removeEnable();
		}
		return result;
	}

	/**
	 * 获取查询配置对应的依赖作业
	 * @return
	 */
	@RequestMapping("result/etlJobs")
	@ResponseBody
	public SSMResponseMessage<List<String>> buildResultEtlJobs() {
		List<String> etlJobs = new ArrayList<>();

		String config = stringValue("config");
		List<String> configList = new ArrayList<>();
		configList.add(config);

		Integer isNeedNormalize = intValue("isNeedNormalize");
		String datasetId = stringValue("datasetId");

		etlJobs = SSDUtil.getEtlJobByConfig(configList, datasetId, isNeedNormalize)
				.stream().flatMap(t -> t.getEtlJobs().stream()).distinct().collect(Collectors.toList());
		return SSMResponseMessage.success("获取查询配置对应的依赖作业", etlJobs);
	}

	/**
	 * 获取指标名称
	 * @param metricCode
	 * @return
	 */
	@RequestMapping(value = "getMetricNameByMetricCode", method = RequestMethod.GET)
	@ResponseBody
	public SSMResponseMessage<String> getMetricNameByMetricCode(String metricCode){
		return SSMResponseMessage.success("获取指标名称成功", service.getMetricNameByMetricCode(metricCode));
	}

	/**
	 * 获取日期进度
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "getDateRangeProgress", method = RequestMethod.POST)
	@ResponseBody
	public SSMResponseMessage<DateRangeProgressRsp> getDateRangeProgress(@RequestBody DateRangeProgressReq req) {
		return SSMResponseMessage.success("获取日期进度成功", service.getDateRangeProgress(req));
	}

	/**
	 * 获取指标支持的切片粒度列表
	 * @return
	 */
	@RequestMapping(value = "getMetricSupportDataSliceGranularityList", method = RequestMethod.POST)
	@ResponseBody
	public SSMResponseMessage<List<MetricSupportDataSliceGranularityRsp>> getMetricSupportDataSliceGranularityList() {
		QueryEngine createEngine = createEngine(false);
		return SSMResponseMessage.success("获取指标支持的切片粒度列表成功", ssmrtQueryService.getMetricSupportDataSliceGranularityList(createEngine));
	}

}
