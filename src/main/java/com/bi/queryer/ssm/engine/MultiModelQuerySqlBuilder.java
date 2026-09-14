package com.bi.queryer.ssm.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.analysis.AnalysisMultiModelSqlBuilder;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.ssm.util.AnalysisTotalUtil;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL构建器：多模型查询sql构建器
 */
public class MultiModelQuerySqlBuilder {
	/**
	 * 查询配置
	 */
	protected QueryConfigure config = null;

	protected QueryContext cxt = null;

	protected List<StarModel> models = null;

	protected IFunction fx = null;

	protected SingleModelSqlBuilder singleModelSQLBuilder = null;

	private List<String> selectFragments = null;

	private List<String> fromFragments = null;

	//记录计算字段的表达式 和 指标编码的映射关系
	private Map<String,String> customMeasureCodeExpressionMap = new HashMap<>();

	public MultiModelQuerySqlBuilder(QueryContext cxt){
		this.cxt = cxt;
	}

	public MultiModelQuerySqlBuilder(QueryConfigure config, QueryContext cxt, List<StarModel> models){
		this(cxt);
		this.config = config;
		this.models = models;
		this.fx = FunctionManager.getFunction();
		// 单模型sql构建器
		this.singleModelSQLBuilder =  new SingleModelBuilderAvgWrapper(new SingleModelSqlBuilder(this.config, this.cxt));
	}

	public String build() {
		StringBuilder sql = new StringBuilder();
		if (BIUtil.isEmpty(models)) {
			return sql.toString();
		}

		/**select*/
		sql.append(this.buildSelectClause());

		/**from*/
		sql.append(this.buildFromClause());

		/**where*/
		sql.append(this.buildWhereClause());

		/**order by*/
		// 常规查询使用order by子句，lod表达式的order by 在select子句中
		if(this.config.getSettings().getSortMode() == SortMode.NORMAL) {
			sql.append(this.buildOrderByClause());
		}

		// 替换系统变量占位符
		String resultSQL = BIUtil.replaceEnvVariables(sql.toString());
		return resultSQL;
	}

	public StringBuilder buildSelectClause(){
		selectFragments = this.buildSelectFragments();
		StringBuilder sql = new StringBuilder();
		sql.append(" select ").append(BIUtil.listToStr(selectFragments));
		return sql;
	}

	protected List<QueryField> getSelectFields(){
		List<QueryField> dimFields = config.getResult().getRowDimensions();
		List<QueryField> measureFields = config.getResult().getMeasures();

		List<QueryField> selectFields = new ArrayList<>();
		selectFields.addAll(dimFields);
		selectFields.addAll(measureFields);

		return selectFields;
	}

	/**
	 * 按自定义计算字段依赖排序：{@link QueryField#getCusCalcDependFields()} 中的字段必须在 SELECT 中先于当前字段出现。
	 * 无依赖约束时保持 {@link FieldUtil#getSortId} 与入参列表中的先后次序。
	 */
	private List<QueryField> sortSelectFieldsByCusCalcDependencies(List<QueryField> selectFields) {
		if (selectFields == null || selectFields.size() <= 1) {
			return selectFields;
		}
		try {
			Set<QueryField> inSelect = new HashSet<>(selectFields);
			Map<QueryField, Integer> stableIndex = new HashMap<>(selectFields.size() * 2);
			for (int i = 0; i < selectFields.size(); i++) {
				stableIndex.put(selectFields.get(i), i);
			}
			Map<QueryField, Integer> inDegree = new LinkedHashMap<>(selectFields.size() * 2);
			Map<QueryField, List<QueryField>> successors = new HashMap<>(selectFields.size() * 2);
			for (QueryField f : selectFields) {
				int deg = 0;
				for (QueryField dep : f.getCusCalcDependFields()) {
					if (!inSelect.contains(dep) || dep.equals(f)) {
						continue;
					}
					deg++;
					successors.computeIfAbsent(dep, k -> new ArrayList<>()).add(f);
				}
				inDegree.put(f, deg);
			}
			Comparator<QueryField> tieBreak = Comparator
					.comparingInt(FieldUtil::getSortId)
					.thenComparingInt(stableIndex::get);
			PriorityQueue<QueryField> ready = new PriorityQueue<>(tieBreak);
			for (QueryField f : selectFields) {
				if (inDegree.get(f) == 0) {
					ready.add(f);
				}
			}
			List<QueryField> ordered = new ArrayList<>(selectFields.size());
			while (!ready.isEmpty()) {
				QueryField u = ready.poll();
				ordered.add(u);
				for (QueryField v : successors.getOrDefault(u, Collections.emptyList())) {
					int next = inDegree.merge(v, -1, Integer::sum);
					if (next == 0) {
						ready.add(v);
					}
				}
			}
			if (ordered.size() < selectFields.size()) {
				Set<QueryField> placed = new HashSet<>(ordered);
				for (QueryField f : selectFields) {
					if (!placed.contains(f)) {
						ordered.add(f);
					}
				}
			}
			return ordered;
		} catch (Exception e) {
			System.out.println("Error in sortSelectFieldsByCusCalcDependencies: " + e.getMessage());
			return selectFields;
		}
	}

	protected List<String> buildSelectFragments() {
		List<QueryTable> queryTables = models.stream().map(StarModel::getFactTable).collect(Collectors.toList());

		List<String> selectFragments = new ArrayList<>();
		List<String> orderByFragments = new ArrayList<>();

		List<QueryField> selectFields = this.getSelectFields().stream()
				.filter(f -> !f.isAppend() || SSDUtil.isAggFilter(f))
				.collect(Collectors.toList());
		selectFields = sortSelectFieldsByCusCalcDependencies(selectFields);

		// 获取groupValue和维度分组的关系
		Function<String, String> fieldSqlSupplier = fieldCode -> FieldUtil.getModelFiledExpression(models, fx, fieldCode);
		Map<Integer, List<String>> groupingPartitionMap = AnalysisTotalUtil.getGroupingPartitionMap(config, fieldSqlSupplier);

		IFunction fx = FunctionManager.getFunction();
		Map<String,String> expressionMap = new HashMap<>(16);
		for (QueryField selectField : selectFields) {
			if (selectField.isAnalysisCalc()) {
				continue;
			}
			String selectExpression = selectField.getCode();
			if (selectField.isCustomMeasure()) {
				// 若是用户自定义计算字段，不在QueryTable中，需单独处理：使用子查询中已聚合的字段进行计算
				selectExpression = this.getCustomCalcMeasureFieldName(queryTables, selectField, expressionMap);

				customMeasureCodeExpressionMap.put(selectField.getCode(), selectExpression);
			}
			if (!selectField.isCustomMeasure()) {
				List<String> coalesceFields = new ArrayList<>();
				for (StarModel model : models) {
					if (model.getFieldByCode(selectField.getCode()) != null) {
						coalesceFields.add(model.getAlias() + "." + selectExpression);
					}
				}
				selectExpression = fx.coalesce(coalesceFields);
			}

			if (Enabled.isTrue(selectField.getIsAnalysis())) {
				// 此处不处理分析字段，分析字段在下面统一处理
				continue;
			}
			//if(selectField.isMeasure() && Enabled.value(selectField.getIsShow())){ // 去掉隐藏字段
			//	measureExpressions.add(selectExpression);
			//}

			if (StringUtils.isNotEmpty(selectField.getTotalAggType())) {
				// 处理汇总指标的自定义聚合方式
				List<String> tableAlias = models.stream().map(StarModel::getAlias).collect(Collectors.toList());
				fieldSqlSupplier = field -> FieldUtil.getTableFiledExpression(tableAlias, fx, field);
				selectExpression = AnalysisTotalUtil.buildTotalMeasureAggExpression(selectField, selectExpression, groupingPartitionMap, fieldSqlSupplier, BIConsts.GROUPING_VALUE);
				if (!AnalysisTotalAggType.isDefault(selectField.getTotalAggType())) {
					expressionMap.put(selectField.getCode(), String.format("(%s)", selectExpression));
				}
			}

			StringBuilder selectFragment = new StringBuilder();
			selectFragment.append(selectExpression).append(" AS ").append(selectField.getCode());

			//兼容selectExpression 为空的场景
			if(StrUtil.isNotEmpty(selectExpression)){
				selectFragments.add(selectFragment.toString());
			}

			// 排序字段
			if (selectField.getSortType() != FieldSortType.NONE) {
				String sortExpression = FieldUtil.getFieldValueSortExpression(selectField.getCode(), selectField);
				sortExpression = sortExpression.replace(selectField.getCode(), selectExpression);
				//orderByFragments.add(sortExpression + " " + selectField.getSortType().getCode());
				orderByFragments.add(String.format("%s %s nulls last", sortExpression, selectField.getSortType().getCode()));
			}
		}

		/*
		// 添加分析字段
		List<String> analysisFragments = this.buildAnalysisSelectFragments(selectFields, measureExpressions);
		selectFragments.addAll(analysisFragments);
		 */

		// 排序：row_number order by，用于支持lod子查询排序
		if (BIUtil.isNotEmpty(orderByFragments) && config.getSettings().getNeedSort() && config.getSettings().getSortMode() == SortMode.ROW_NUMBER) {
			String orderByClause = String.format("%s %s", BIConsts.ORDER_BY, BIUtil.listToStr(orderByFragments));
			selectFragments.add(String.format(String.format("row_number() over(%s) as %s", orderByClause, BIConsts.ROW_NUMBER_KEY)));
		}

		//去重
		selectFragments = selectFragments.stream().distinct().collect(Collectors.toList());
		return selectFragments;
	}

	public StringBuilder buildFromClause(){
		List<QueryField> dimFields = config.getResult().getRowDimensions();
		fromFragments = new ArrayList<>();
		StarModel previousModel = null;

		boolean hasTotalAnalysis = config.getAnalysis().getTotal().isActive();

		Map<String, List<String>> previousJoinFields = new LinkedHashMap<>();
		List<String> previousGroupingExpressions = new ArrayList<>();
		for (StarModel model : models) {
			StringBuilder fromFragment = new StringBuilder();
			String currentSubQueryAlias = model.getAlias();

			// 单模型sql构建器
			String subQuerySql = singleModelSQLBuilder.build(model);

			// full join
			if (previousModel == null) {
				// 第一个子查询
				fromFragment.append("(").append(subQuerySql).append(")")
						.append(" ").append(currentSubQueryAlias);
				fromFragments.add(fromFragment.toString());
				previousModel = model;
				continue;
			}

			String previousQueryTableAlias = previousModel.getAlias();
			previousGroupingExpressions.add(String.format("%s.%s", previousQueryTableAlias, BIConsts.GROUPING_VALUE));

			fromFragment.append(" FULL JOIN ")
					.append(" (").append(subQuerySql).append(") ").append(currentSubQueryAlias);
			fromFragment.append(" ON (");
			List<String> joinExpressions = new ArrayList<>();
			if (BIUtil.isEmpty(dimFields)) {
				joinExpressions.add(" 1=1 ");
			} else {
				for (QueryField dimField : dimFields) {
					if (dimField.isAppend()) {
						continue;
					}

					String dimCode = dimField.getCode();
					List<String> previousFieldCodes = previousJoinFields.get(dimCode);
					if(previousFieldCodes == null){
						previousFieldCodes = new ArrayList<>();
						previousJoinFields.put(dimCode, previousFieldCodes);
					}

					String constValue =  "'" + BIConsts.SSM_ALL + "'";
					String currentFieldExpression = fx.coalesce(currentSubQueryAlias + "." + dimField.getCode(), constValue);

					// 常量值：先删再插入到最后
					previousFieldCodes.remove(constValue);
					previousFieldCodes.add(previousQueryTableAlias + "." + dimField.getCode());
					previousFieldCodes.add(constValue);


					String joinExpression = String.format("%s = %s", fx.coalesce(previousFieldCodes), currentFieldExpression);
					joinExpressions.add(joinExpression);
				}
			}

			// 若有汇总分析：需要关联汇总分组值，避免多层总计/小计后null无法关联的问题
			if(hasTotalAnalysis){
				String groupingJoinExpression = String.format("%s = %s.%s", fx.coalesce(previousGroupingExpressions), currentSubQueryAlias, BIConsts.GROUPING_VALUE);
				joinExpressions.add(groupingJoinExpression);
			}

			fromFragment.append(BIUtil.listToStr(joinExpressions, " and "));
			fromFragment.append(" )");

			fromFragments.add(fromFragment.toString());

			previousModel = model;
		}

		StringBuilder sql = new StringBuilder();
		sql.append(" FROM ").append(BIUtil.listToStr(fromFragments, " "));
		return sql;
	}

	/**
	 * 构建where子句
	 * 将结果筛选的过滤放在最外层处理
	 * @return
	 */
	protected StringBuilder buildWhereClause() {

		StringBuilder whereSql = new StringBuilder();

		//分析场景，在最外层处理指标结果过滤
		if(this instanceof AnalysisMultiModelSqlBuilder){
			return whereSql;
		}

		List<String> whereFragments = new ArrayList<>();
		List<QueryField> filterFields = config.getFilter().getFields();
		for (QueryField field : filterFields) {

			if (!field.isActive()) {
				continue;
			}

			//不可度量字段直接跳过
			if (!field.isMeasure()) {
				continue;
			}

			//由于模板可能存在没有FilterValueMode的情况
			if (FieldFilterMode.detail == FieldFilterMode.get(field.getFilterValueMode())) {
				continue;
			}

			//************正式开始装配sql****************
			List<FieldValue> values = field.getValues();
			if (values == null || values.isEmpty()) {
				continue;
			}
			values.forEach(fv -> {
				fv.setId(EnvVariableManager.value(fv.getId()));
			});
			// 字段数据类型
			FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());
			if (values.isEmpty()) {
				continue;
			}

			String whereFieldName = getWhereFieldFullName(field);

			//计算指标的表达式从映射关系中获取
			if(FieldType.CUSTOM_MEASURE == FieldType.get(field.getFieldType())){
				String expression = customMeasureCodeExpressionMap.get(field.getCode());

				if(StrUtil.isEmpty(expression)){
					continue;
				}

				whereFieldName = String.format("(%s)", expression);
			}

			FieldFilterType filterType = FieldUtil.getFilterType(field);

			if (filterType.isRange()) {
				if (dataType == FieldDataType.Integer || dataType == FieldDataType.Double) {
					if (StringUtil.isEmpty(values.get(0).getId())) {
						values.get(0).setId("0");
					}
					if (StringUtil.isEmpty(values.get(1).getId())) {
						values.get(1).setId("100000000");
					}
					String v1 = values.get(0).getId();
					String v2 = values.get(1).getId();
					whereFragments.add(String.format("%s between %s and %s", whereFieldName, v1, v2));
				}
			}

		}

		if (BIUtil.isEmpty(whereFragments)) {
			return whereSql;
		}

		whereSql.append(" WHERE ").append(BIUtil.listToStr(whereFragments, " AND ", "(", ")"));
		return whereSql;
	}

	public String getWhereFieldFullName(QueryField field) {
		String whereFieldExpression = field.getCode();
		List<String> coalesceFields = new ArrayList<>();
		for (StarModel model : models) {
			if (model.getFieldByCode(field.getCode()) != null) {
				coalesceFields.add(model.getAlias() + "." + whereFieldExpression);
			}
		}
		whereFieldExpression = fx.coalesce(coalesceFields);
		return whereFieldExpression;
	}


	/**
	 * 构建order by子句
	 *
	 * @return
	 */
	protected StringBuilder buildOrderByClause() {
		StringBuilder orderBySQL = new StringBuilder();

		if(!config.getSettings().getNeedSort()) {
			return orderBySQL;
		}

		// 获取排序字段
		List<QueryField> resultFields = config.getResult().getFields();
		List<String> orderByFragments = new ArrayList<String>();

		List<String> resultFieldCodes = new ArrayList<>();
		resultFieldCodes.addAll(config.getResult().getFields().stream()
				.filter(f->Enabled.value(f.getIsShow())&&!f.isAppend())
				.map(QueryField::getCode).collect(Collectors.toList()));

		orderByFragments.addAll(FieldUtil.getOrderByFragments(config,resultFieldCodes));
		List<String> sortFieldCodes = this.config.getSettings().getQuerySortFieldCodes(this.config);

		for (QueryField field : resultFields) {
			if (field.getSortType() == FieldSortType.NONE) {
				continue;
			}

			//已排序，不再处理
			if(sortFieldCodes.contains(field.getCode())){
				continue;
			}

			String sortExpression = FieldUtil.getFieldValueSortExpression(field.getCode(), field);
			//orderByFragments.add(sortExpression + " " + field.getSortType().getCode());
			orderByFragments.add(String.format("%s %s nulls last", sortExpression, field.getSortType().getCode()));
		}

		if (orderByFragments.isEmpty()) {
			return orderBySQL;
		}

		orderBySQL.append(" ").append(BIConsts.ORDER_BY).append(" ");
		orderBySQL.append(BIUtil.listToStr(orderByFragments, ","));
		return orderBySQL;
	}


	/**
	 * 获取用户自定义计算指标的字段名（表达式）
	 * 示例：[field1Id] + [field2Id]
	 * 查询表：T1、T2
	 * 场景1：T1/T2包含2个字段，转换为 coalesce(T1.field1, T2.field1) + coalesce(T1.field2, T2.field2)
	 * 场景2：T1包含2个字段，但T2只包含field1，转换为 coalesce(T1.field1, T2.field1) + coalesce(T1.field2, null)
	 * 场景3：T1包含2个字段，但T2不包含任何字段，转换为 coalesce(T1.field1, null) + coalesce(T1.field2, null)
	 *
	 * @param calcField
	 * @return
	 */
	protected String getCustomCalcMeasureFieldName(List<QueryTable> tables, QueryField calcField, Map<String,String> expressionMap) {
		// 注：此处需用原始表达式，因为在最外层select不需要再做聚合
		// 示例：[field1Id] + [field2Id]
		String expression = calcField.getCustomFieldConfigure().getExpression();

		//20260402兼容用户输入表达式前后没有空格，导致查询报错的问题
		//例子：(case when [65bc46563dbf43e7b654c989aa121112_avg_by_d]= 0 then [11a42db1bb5e4f4493adee093e1d564a_avg_by_d]else [65bc46563dbf43e7b654c989aa121112_avg_by_d]end)
		if(BIUtil.isNotEmpty(expression)){
			expression = expression.replaceAll("\\["," \\[");
			expression = expression.replaceAll("\\]","\\] ");
		}

		String commonDateFieldName = getCommonDateFieldName();
		expression = FieldUtil.rectifytCustomCalcExpression(expression,this.config,commonDateFieldName);

		//若表达式中包含“/”运算符，且不包含“*1.0”,则需将度量字段都乘于1.0000,避免查询结果丢失精确度
		Boolean hasDivisionWithoutDecimal = expression.indexOf("/") > -1 && expression.replaceAll("\\s*", "").indexOf("*1.0") < 0;
		if (!calcField.isCalc() || !calcField.isMeasure()) {
			return expression;
		}
		if (BIUtil.isEmpty(expression)) {
			return calcField.getTable().getModel().getAlias() + "." + calcField.getCode(); //calcField.getAlias();
		}

		Set<String> refFieldIds = new HashSet<String>();
		Pattern pattern = Pattern.compile("(?<=\\[)(.+?)(?=\\])");
		Matcher matcher = pattern.matcher(expression);
		while (matcher.find()) {
			refFieldIds.add(matcher.group());
		}

		if (refFieldIds.isEmpty()) {
			return expression;
		}
		IFunction function = FunctionManager.getFunction();

		// 只有加减运算需要添加coalesce，数字加减null时作为0处理
		String operator = expression.replaceAll("\\$\\{.*?\\}", "");
		boolean isOperatorCoalesce = (operator.contains("+") || operator.contains("-")) && !operator.contains("*") && !operator.contains("/");

		//用户字段权限
		Map<String, String> aclCodes = new HashMap<>();
		if (cxt != null) {
			aclCodes = cxt.getAclFields();
		}

		boolean hasAuth = true;
		for (String refFieldId : refFieldIds) {
			MetaField mf = SSDMetaCacheManager.getField(refFieldId);
			if (mf == null) {
				continue;
			}

			if (aclCodes != null && !aclCodes.containsKey(mf.getCode())) {
				hasAuth = false;
			}

			String refFieldCode = mf.getCode();
			List<String> tableFieldFullNames = new ArrayList<>();
			for (QueryTable table : tables) {
				QueryField tableField = table.getFieldByCode(refFieldCode);
				String tableFieldFullName = "null";
				if (tableField != null) {
					tableFieldFullName = table.getModel().getAlias() + "." + refFieldCode;
				}
				tableFieldFullNames.add(tableFieldFullName);
			}

			String replacement = function.coalesce(tableFieldFullNames);

			//维度为null时，说明在事实表中不存在，从关联的维表中再找一次
			if(!Enabled.value(mf.getIsMeasure()) && "null".equalsIgnoreCase(replacement)) {

				List<String> dimTableFieldFullNames = new ArrayList<>();
				for (StarModel model : models) {
					if (CollUtil.isEmpty(model.getDimTables())) {
						continue;
					}

					for (QueryTable dimTable : model.getDimTables()) {
						QueryField tableField = dimTable.getFieldByCode(refFieldCode);
						String tableFieldFullName = "null";
						if (tableField != null) {
							tableFieldFullName = dimTable.getModel().getAlias() + "." + refFieldCode;
						}
						dimTableFieldFullNames.add(tableFieldFullName);
					}
				}

				replacement = function.coalesce(dimTableFieldFullNames);
			}

			if (isOperatorCoalesce) {
				replacement = function.coalesce(replacement, "0");
			}

			if (AnalysisTotalAggType.isDefault(calcField.getTotalAggType())) {
				replacement = expressionMap.getOrDefault(refFieldCode, replacement);
			}

			if (hasDivisionWithoutDecimal && Enabled.value(mf.getIsMeasure())) {
				expression = expression.replaceAll("\\[" + refFieldId + "\\]", "\\[" + refFieldId + "\\] * " + BIConsts.INT_TO_DOUBLE_PRECISION);
			}
			expression = expression.replaceAll("\\[" + refFieldId + "\\]", replacement);
		}
		expression = " " + function.tryCatch(expression) + " ";
		//" try(" + expression + ") ";

		//判断权限，引用的字段有一个没有权限，则计算字段无权限
		// 权限不在sql层处理，在最后结果输出时做掩码，避免后续sql构建时数据类型不一致导致sql查询错误
		/*
		if (!hasAuth) {
			return "'***'";
		}
		 */

		return expression;
	}

	/**
	 * 获取公共日期字段名
	 * @return
	 */
	public String getCommonDateFieldName() {

		try {
			QueryField selectField = config.getFilterCommonDateField();
			String selectExpression = selectField.getCode();
			List<String> coalesceFields = new ArrayList<>();
			for (StarModel model : models) {
				if (model.getFieldByCode(selectField.getCode()) != null) {
					coalesceFields.add(model.getAlias() + "." + selectExpression);
				}
			}
			return fx.coalesce(coalesceFields);
		} catch (Exception e) {
			return "";
		}
	}

	public QueryConfigure getConfig() {
		return config;
	}

	public void setConfig(QueryConfigure config) {
		this.config = config;
	}

	public List<StarModel> getModels() {
		return models;
	}

	public void setModels(List<StarModel> models) {
		this.models = models;
	}

	public QueryContext getCxt() {
		return cxt;
	}

	public void setCxt(QueryContext cxt) {
		this.cxt = cxt;
	}

	public SingleModelSqlBuilder getSingleModelSQLBuilder() {
		return singleModelSQLBuilder;
	}

	public void setSingleModelSQLBuilder(SingleModelSqlBuilder singleModelSQLBuilder) {
		this.singleModelSQLBuilder = singleModelSQLBuilder;
	}

	public List<String> getSelectFragments() {
		return selectFragments;
	}

	public void setSelectFragments(List<String> selectFragments) {
		this.selectFragments = selectFragments;
	}

	public List<String> getFromFragments() {
		return fromFragments;
	}

	public void setFromFragments(List<String> fromFragments) {
		this.fromFragments = fromFragments;
	}
}



