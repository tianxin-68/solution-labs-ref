package com.bi.queryer.ssm.engine.acl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.FieldValueFilterType;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONArray;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限管理器
 * @author contributor
 */
public abstract class AclManager {

	private static List<MetaFieldAuthBlackList> aclFieldBlackList = new ArrayList<>();
	private static List<MetaFieldAuthWhiteList> aclFieldWhiteList = new ArrayList<>();

	public static void refresh(){
		QueryFieldService queryFieldService = (QueryFieldService) SpringContextUtil.getBean("queryFieldService");
		aclFieldBlackList = queryFieldService.getFieldAuthBlackList();
		aclFieldWhiteList = queryFieldService.getFieldAuthWhiteList();
	}

	/**
	 * 初始化查询权限控制列表
	 * @param config
	 * @param cxt
	 */
	public static void initQueryAcl(QueryConfigure config, QueryContext cxt) {
		// 避免重复加载
		if (BIUtil.isNotEmpty(cxt.getAclFields())) {
			return;
		}
		prepareFieldAcl(config, cxt);
		prepareRowAcl(config, cxt);
	}

	/**
	 * 字段权限
	 * @param config
	 * @param cxt
	 */
	public static void prepareFieldAcl(QueryConfigure config, QueryContext cxt) {
		SSDQueryService queryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");

		//List<String> aclFieldCodes = queryService.getAuthFieldCodes(cxt.getUser().getName(), SSDUtil.Field_Auth_Module, SSDUtil.Field_Auth_Dim);

		String datasetId = config.getSettings().getDatasetId();

		long t1 = System.currentTimeMillis();
		// 通过目录获取字段code权限列表
		List<String> aclCodes ;
		if (BIUtil.isApiUser() || BIUtil.isBossUser()) {
			aclCodes = SSDMetaCacheManager.getFieldsCache().values().stream().map(MetaField::getCode).distinct().collect(Collectors.toList());
		} else {
			aclCodes = queryService.getAuthFieldCodesByCtg(cxt.getUser().getName(), SSDUtil.Ctg_Auth_Module, SSDUtil.Ctg_Auth_Dim, datasetId);
		}

		// 黑名单
		Set<String> blacklistCodes =  getUnauthFieldCodeByBlackList(cxt.getUser().getName());//queryService.getUnauthFieldByBlackList(cxt.getUser().getName());

		aclCodes = aclCodes.stream()
				.filter(item -> !blacklistCodes.contains(item))
				.collect(Collectors.toList());

		long t2 = System.currentTimeMillis();
		System.out.printf("获取权限耗时"+(t2-t1)+"ms");

		// 合并字段和目录权限
		//List<String> aclCodes = queryService.mergeAclCodes(aclFieldCodes,aclCtgCodes);

		// 自定义字段
		appendCustomFieldAcl(config, aclCodes);

		// 分析字段：若原生字段有权限，则此分析字段也有权限，否则都无权限
		List<QueryField> fields = config.getResult().getFields();
		for (QueryField f : fields) {
			if (Enabled.isTrue(f.getIsAnalysis())) {
				if (aclCodes.contains(f.getAnalysisConfig().getMeasureCode())) {
					aclCodes.add(f.getCode());
				}
				//} else if(f.getCustomFieldConfigure() != null && CustomFieldType.isLod(f.getCustomFieldConfigure().getType())) {
			} else if(f.isCustom()) {
				// 自定义不需走下面兼容处理的逻辑
				continue;
			} else if(Enabled.isFalse(f.getIsShow()) || BIUtil.isEmpty(f.getModuleCtgId()) || (f.getMeta() != null && BIUtil.isEmpty(f.getMeta().getCategoryId()))){
				// 兼容处理
				aclCodes.add(f.getCode());
			}
		}

		// 添加常量字段
		aclCodes.add(BIConsts.GROUPING_VALUE);
		aclCodes.add(BIConsts.GROUPING_KEY);
		aclCodes.add(BIConsts.ROW_NUMBER_KEY);

		// 添加公共日期字段
		QueryField filterCommonDateField = config.getFilterCommonDateField();
		if(filterCommonDateField != null) {
			aclCodes.add(filterCommonDateField.getCode());
		}
		QueryField resultCommonDateField = config.getResultCommonDateField();
		if(resultCommonDateField != null){
			aclCodes.add(resultCommonDateField.getCode());
		}
		Map<String, String> aclMap = new HashMap<>(100);
		aclCodes.stream().forEach(c->aclMap.put(c, c));
		cxt.setAclFields(aclMap);


		// 查询时，暂时不支持敏感数据权限
		List<String> aclSensitiveCodes = new ArrayList<>(); // queryService.getAuthFieldCodes(cxt.getUser().getName(), SSDUtil.Field_Auth_Module, SSDUtil.Field_Sensitive_Auth_Dim);
		cxt.setAclSensitiveFields(aclSensitiveCodes);
	}

	/**
	 * 数据行级权限
	 * @param config
	 * @param cxt
	 */
	public static void prepareRowAcl(QueryConfigure config, QueryContext cxt) {
		// 用户的数据权限
		SSDQueryService queryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");

		List<MetaFieldDataAuth> dataAuthList = queryService.getDataAuthByUser(cxt.getUser().getName());

		cxt.setDataAuthList(dataAuthList);

		// 用户是否有城市行级权限管控
		if (CollUtil.isNotEmpty(dataAuthList)) {
			long count = dataAuthList.stream()
					.filter(f -> f.getFieldCode().equalsIgnoreCase(BIConsts.CITY_ROW_AUTH_CODE)
							&& !BIConsts.SSM_ALL.equalsIgnoreCase(f.getItemCode()))
					.count();
			cxt.setHasCityRowAuth(count > 0 ? Enabled.YES.getId() : Enabled.NO.getId());
		}
	}

	/**
	 * 注入数据权限过滤
	 * 此步骤在构建完模型之后，优化模型之前
	 */
	public static void injectRowAclFilterToConfig(QueryConfigure config, QueryContext cxt, List<StarModel> models) {
		if (BIUtil.isEmpty(models)) {
			return;
		}
		// 查询模型可触达的数据权限字段：包含已入模表字段，以及事实表可直接关联维表字段
		Map<String, MetaField> aclMetaFields = new HashMap<>();
		// 查询模型可触达的所有字段：用于判断用户配置的行权限字段是否应在当前查询中生效
		Map<String, MetaField> allModelMetaFields = new HashMap<>();
		for (StarModel starModel : models) {
			if (starModel.isOnlyJoinModel()) {
				continue;
			}
			// 已入模表字段优先，避免关联维表同编码字段覆盖当前模型字段
			collectModelTableAclFields(starModel, allModelMetaFields, aclMetaFields);

			// 事实表可直接关联维表字段作为补充，支持权限字段未被前端选择但可通过join过滤的场景
			collectRelationDimTableAclFields(starModel, allModelMetaFields, aclMetaFields);
		}

		// 所有维度行权限列表：无权限也在此列表中
		List<MetaFieldDataAuth> userDataAclList = cxt.getDataAuthList();
		if (BIUtil.isEmpty(userDataAclList) && BIUtil.isEmpty(aclMetaFields)) {
			return;
		}
		Map<String, List<MetaFieldDataAuth>> userDataAclMap = userDataAclList.stream().collect(Collectors.groupingBy(MetaFieldDataAuth::getFieldCode));

		// 特殊处理：附件城市其他同类字段的过滤
		appendCityAcl(userDataAclMap);

		// 最终需要过滤的字段
		Map<String, QueryField> aclFilterFields = new HashMap<>();
		for (String authFieldCode : userDataAclMap.keySet()) {
			// 不在模型中但用户配置了维度权限的，不处理
			if (!allModelMetaFields.containsKey(authFieldCode)) {
				continue;
			}
			// code在白名单且model事实表在白名单表中时，不处理
			if (isSkipRowAclByCodeAndTable(authFieldCode, models)) {
				continue;
			}
			List<MetaFieldDataAuth> itemAuthList = userDataAclMap.get(authFieldCode);
			boolean hasAllAuth = itemAuthList.stream().filter(d -> {
				return BIConsts.Ssm_Row_Acl_All_Dim_Value.equals(d.getItemCode());
			}).count() > 0;
			// 有所有权限，则不处理
			if (hasAllAuth) {
				continue;
			}
			MetaField mf = allModelMetaFields.get(authFieldCode);
			QueryField aclQueryField = createAclQueryField(mf, itemAuthList);
			if(aclQueryField != null) {
				aclFilterFields.put(aclQueryField.getCode(), aclQueryField);
			}
			/**
			if (mf != null) { // 避免重复添加
				QueryField qf = new QueryField(mf);
				qf.setId(mf.getId());
				qf.setFilter(true);
				// 添加过滤值
				for (DataAuth acl : itemAuthList) {
					FieldValue fv = new FieldValue();
					if (acl.getItemCode() == null) {
						fv.setId(acl.getItemValue());
					} else {
						fv.setId(acl.getItemCode());
					}
					fv.setTitle(acl.getItemValue());
					qf.getValues().add(fv);
				}
				aclFilterFields.put(qf.getCode(), qf);
			}
			 */
		}

		// 若用户无权限，但有需管控的维度
		if (BIUtil.isNotEmpty(aclMetaFields)) {
			for (MetaField aclField : aclMetaFields.values()) {
				if (userDataAclMap.containsKey(aclField.getCode())) {
					continue;
				}
				if(isVirtualFilterFieldCode(aclField.getCode())){
					continue;
				}
				// code在白名单且model事实表在白名单表中时，不处理
				if (isSkipRowAclByCodeAndTable(aclField.getCode(), models)) {
					continue;
				}
				QueryField qf = new QueryField(aclField);
				qf.setId(aclField.getId());
				qf.setFilter(true);
				FieldValue v = new FieldValue();
				v.setId("null");
				v.setTitle("--");
				v.setRealId("null");
				qf.getValues().add(v);
				aclFilterFields.put(qf.getCode(), qf);

				// TODO
				//  说明：exists过滤的针对需要管控但未配置任何维度值的场景并未处理，因为此场景的权限数据都是通过底表直接赋值，且都是未配置有全部权限
			}
		}

		// 与现有config中的过滤字段合并过滤值
		List<QueryField> configFilterFields = config.getFilter().getFields();//config.getFilter().getFields().stream().filter(f->BIUtil.isNotEmpty(f.getValues())).collect(Collectors.toList());
		Map<String, QueryField> configFilterFieldMap = configFilterFields.stream().collect(Collectors.toMap(QueryField::getCode, QueryField -> QueryField, (f1, f2) -> f1));

		// 添加过滤字段到已构建的模型中
		// 通过code查找是否已经在前端设置过滤条件
		for (QueryField aclField : aclFilterFields.values()) {
			if(isExistsFilterFieldCodes(aclField.getCode())){
				// 若是通过exists过滤的数据权限管控字段，则不做合并，因为最终是在where field_code exists子句中，不在 field_code in(xx)中
				cxt.addDataAuthExistsFilterField(new AclDataAuthExistsFilterField(aclField.getCode(), aclField.getMeta().getDataAuthDimCode(), aclField.getMeta().getDataAuthMode()));
				continue;
			}
			// 虚拟过滤字段：不合并
			if(isVirtualFilterFieldCode(aclField.getCode())){
				continue;
			}
			if (configFilterFieldMap.containsKey(aclField.getCode())) {
				QueryField configFilterField = configFilterFieldMap.get(aclField.getCode());
				mergeQueryFieldValues(aclField, configFilterField);
				// 合并过滤值
			} else { // 不存在则直接添加到过滤区中
				aclField.setIsFilter(true);
				aclField.setAppend(true);
				config.getFilter().getFields().add(aclField);
			}
		}
	}

	/**
	 * 补充城市管控的特殊逻辑：AAA or DMG or CVT，若AAA存在，则附加 DMG 和 CVT
	 * @param userDataAclMap
	 */
	protected static void appendCityAcl(Map<String, List<MetaFieldDataAuth>> userDataAclMap){
		String mainCityCode = BIConsts.CITY_ROW_AUTH_CODE;
		if(!userDataAclMap.containsKey(mainCityCode)){
			return;
		}
		List<MetaFieldDataAuth> cityDataAuthList = userDataAclMap.get(mainCityCode);
		if(BIUtil.isEmpty(cityDataAuthList)){
			return;
		}
		//String[] appendCityCodes = new String[]{"DMG", "CVT"};
		List<String> appendCityCodes = getVirtualFilterFieldCodes();
		for(String appendCityCode : appendCityCodes){
			MetaFieldDataAuth copy = cityDataAuthList.get(0).clone();
			copy.setFieldCode(appendCityCode);
			copy.setWhitePaperCode(appendCityCode);

			// 修改过滤值为特殊值：在where构建时将此值去掉，避免在正式where条件中添加，此次只是为了合并到config并在构建表时带出需要过滤的维度表
			copy.setItemCode(BIConsts.VIRTUAL_FILTER_VALUE);
			copy.setItemValue(BIConsts.VIRTUAL_FILTER_VALUE);
			List<MetaFieldDataAuth> copyDataAuthList = Arrays.asList(copy);
			userDataAclMap.put(appendCityCode, copyDataAuthList);
		}
	}

	protected static List<String> getVirtualFilterFieldCodes(){
		List<String> codes = Arrays.asList(SC.v("ssm.acl.virtual.filter.field.codes", "DMG,CVG"));
		return codes;
	}

	protected static boolean isVirtualFilterFieldCode(String fieldCode){
		List<String> vcodes = getVirtualFilterFieldCodes();
		return vcodes.contains(fieldCode);
	}

	/**
	 * 判断是否跳过行权限注入：字段code在白名单中，且model事实表表名在白名单中
	 * 场景1： 商品品牌在部分模板中，不注入权限管控
	 * https://ssd-admin.example.com/ssm/#/template/801c012bd0e041c4b76aa2a8028d15a3?viewId=49e68828b6ff4dc58f4f65f6aa6e159e&datasetId=68298a6d360c4c59ada307fd5fc0112c
	 */
	protected static boolean isSkipRowAclByCodeAndTable(String fieldCode, List<StarModel> models) {
		String skipCodes = SC.v("ssm.acl.skip.row.acl.field.codes", "ABC");
		if (StrUtil.isEmpty(skipCodes)) {
			return false;
		}
		List<String> skipCodeList = Arrays.asList(skipCodes.split(","));
		boolean codeMatched = false;
		for (String c : skipCodeList) {
			if (c.trim().equalsIgnoreCase(fieldCode.trim())) {
				codeMatched = true;
				break;
			}
		}
		if (!codeMatched) {
			return false;
		}
		String skipTables = SC.v("ssm.acl.skip.row.acl.table.names", "bi_olap.olap_shp_receive_cate_reco_check_order_dtl_di");
		if (StrUtil.isEmpty(skipTables)) {
			return false;
		}
		List<String> skipTableList = Arrays.asList(skipTables.split(","));
		for (StarModel m : models) {
			if (m.isOnlyJoinModel()) {
				continue;
			}
			if (m.getFactTable() == null || m.getFactTable().getMeta() == null) {
				continue;
			}
			if (skipTableList.contains(m.getFactTable().getMeta().getFullName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 创建数据权限管控的查询字段，用于后续合并到config中
	 * @param mf
	 * @param itemAuthList
	 * @return
	 */
	protected static QueryField createAclQueryField(MetaField mf, List<MetaFieldDataAuth> itemAuthList){
		if(mf == null){
			return null;
		}
		QueryField qf = new QueryField(mf);
		qf.setId(mf.getId());
		qf.setFilter(true);
		// 添加过滤值
		for (DataAuth acl : itemAuthList) {
			FieldValue fv = new FieldValue();
			if (acl.getItemCode() == null) {
				fv.setId(acl.getItemValue());
			} else {
				fv.setId(acl.getItemCode());
			}
			fv.setTitle(acl.getItemValue());
			qf.getValues().add(fv);
		}
		return qf;
	}

	/**
	 * 收集当前模型已入模表字段
	 * @param starModel 当前查询已构建出的星型模型
	 * @param allModelMetaFields 当前模型可触达的所有字段，key为字段code
	 * @param aclMetaFields 当前模型可触达且配置了数据权限维度的字段，key为字段code
	 */
	private static void collectModelTableAclFields(StarModel starModel, Map<String, MetaField> allModelMetaFields, Map<String, MetaField> aclMetaFields) {
		List<QueryTable> modelTables = starModel.getTables();
		if (BIUtil.isEmpty(modelTables)) {
			return;
		}
		for (QueryTable queryTable : modelTables) {
			if (queryTable.getMeta() == null) {
				continue;
			}
			collectAclCandidateFields(queryTable.getMeta().getId(), allModelMetaFields, aclMetaFields);
		}
	}

	/**
	 * 收集当前事实表可直接关联维表字段
	 * @param starModel 当前查询已构建出的星型模型
	 * @param allModelMetaFields 当前模型可触达的所有字段，key为字段code
	 * @param aclMetaFields 当前模型可触达且配置了数据权限维度的字段，key为字段code
	 */
	private static void collectRelationDimTableAclFields(StarModel starModel, Map<String, MetaField> allModelMetaFields, Map<String, MetaField> aclMetaFields) {
		QueryTable factTable = starModel.getFactTable();
		if (factTable == null || factTable.getMeta() == null) {
			return;
		}
		List<MetaTable> relationTables = SSDMetaCacheManager.getRelationTables(factTable.getMeta().getId());
		if (BIUtil.isEmpty(relationTables)) {
			return;
		}
		for (MetaTable relationTable : relationTables) {
			if (relationTable == null) {
				continue;
			}
			collectAclCandidateFields(relationTable.getId(), allModelMetaFields, aclMetaFields);
		}
	}

	/**
	 * 收集单表里的ACL候选字段
	 * @param tableId 元表ID，来源于当前模型表或当前事实表的直接关联维表
	 * @param allModelMetaFields 当前模型可触达的所有字段，key为字段code
	 * @param aclMetaFields 当前模型可触达且配置了数据权限维度的字段，key为字段code
	 */
	private static void collectAclCandidateFields(String tableId, Map<String, MetaField> allModelMetaFields, Map<String, MetaField> aclMetaFields) {
		if (BIUtil.isEmpty(tableId)) {
			return;
		}
		List<MetaField> fields = SSDMetaCacheManager.getTableFields(tableId);
		if (BIUtil.isEmpty(fields)) {
			return;
		}
		for (MetaField field : fields) {
			// 已入模字段优先；关联维表字段只在code不存在时补充
			allModelMetaFields.putIfAbsent(field.getCode(), field);
			if (BIUtil.isNotEmpty(field.getDataAuthDimCode())) {
				aclMetaFields.putIfAbsent(field.getCode(), field);
			}
		}
	}

	/**
	 * 合并过滤字段值
	 * @return
	 */
	protected static void mergeQueryFieldValues(QueryField aclField, QueryField configFilterField) {
		List<FieldValue> cfgValues = configFilterField.getValues();
		List<FieldValue> aclValues = (List<FieldValue>) JSONArray.parseArray(BIUtil.toJSONArray(aclField.getValues()).toJSONString(), FieldValue.class); //aclField.getValues();

		if (BIUtil.isNotEmpty(cfgValues)) {
			aclValues.retainAll(cfgValues); // 只保留有权限管控的值
		}
		if (FieldValueFilterType.isExclude(configFilterField.getFilterValueType())) {
			// 若是排除不包含，则需要你反向设置，同时修改值过滤类型为include
			List<FieldValue> aclAllValues = (List<FieldValue>) JSONArray.parseArray(BIUtil.toJSONArray(aclField.getValues()).toJSONString(), FieldValue.class);
			aclAllValues.removeAll(aclValues);
			// 删除已设置的值，然后设置为include
			configFilterField.setFilterValueType(FieldValueFilterType.include.toString());
			aclValues = aclAllValues;
		}

		// 若合并为空，表示存在本无权限的过滤值存在，则强行设置过滤值为null
		if (aclValues.isEmpty()) {
			FieldValue v = new FieldValue();
			v.setId("null");
			v.setTitle("--");
			v.setRealId("null");
			aclValues.add(v);
			configFilterField.setFilterValueType(FieldValueFilterType.include.toString());
		}
		configFilterField.setValues(aclValues);
	}

	/**
	 * 是否 exists 过滤的字段编码
	 */
	private static boolean isExistsFilterFieldCodes(String fieldCode) {
		String existsFilterFieldCodes = SC.v("ssm.data.auth.filter.by.exists.field.codes", "");
		if (BIUtil.isEmpty(fieldCode) || BIUtil.isEmpty(existsFilterFieldCodes)) {
			return false;
		}
		for (String existsFilterCode : existsFilterFieldCodes.split(",")) {
			if (existsFilterCode.trim().equalsIgnoreCase(fieldCode.trim())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取自定义字段的权限列表：自定义字段的原生字段，若有一个没有权限，则该字段都没有权限
	 * @param aclFieldCodes
	 * @return
	 */
	public static void appendCustomFieldAcl(QueryConfigure config, List<String> aclFieldCodes) {
		// 获取所有字段
		// 结果字段
		List<QueryField> configFields = config.getAllFields();
		List<QueryField> customFields = configFields.stream().filter(cf -> cf.getCustomFieldConfigure() != null && !cf.getCustomFieldConfigure().isEmpty())
				.sorted(Comparator.comparingInt(FieldUtil::getSortId)).collect(Collectors.toList());

		List<String> customAcl = new ArrayList<>();
		if (customFields != null) {
			for (QueryField cf : customFields) {
				// 已有权限则不用判断
				if (aclFieldCodes.contains(cf.getCode())) {
					continue;
				}

				// 度量自定义字段且有原生字段
				Set<QueryField> atomFields = getAtomFields(config, cf);

				// 原子字段去掉不显示的字段，但保留日均字段（因日均字段默认不显示）
				Set<QueryField> newAtomFields = new HashSet<>();
				for(QueryField atomField : atomFields){
					// 日均值
					if(isAvgByDay(atomField)){
						newAtomFields.add(atomField);
					}else if(atomField.getMeta() != null && Enabled.isTrue(atomField.getMeta().getIsShow())){
						newAtomFields.add(atomField);
					}
				}

				atomFields = newAtomFields;

				//挂载在数据模块里的字段
				/*
				Set<QueryField> frontVizFields = new HashSet<>();
				for (QueryField atomField : newAtomFields) {
					MetaField field = atomField.getMeta();
					if (field != null && Enabled.isTrue(field.getIsShow()) && BIUtil.isNotEmpty(field.getCategoryId())) {
						//过滤前台不展示字段
						frontVizFields.add(atomField);
					}
				}
				 */

				// atomFields = frontVizFields;

				if (BIUtil.isNotEmpty(atomFields)) {
					// 原子字段全部有权限则该计算字段有权限，其中只校验显示的元字段
					boolean hasAuth = atomFields.size() == atomFields.stream().filter(qf -> aclFieldCodes.contains(qf.getCode())).count();
					if (hasAuth) {
						aclFieldCodes.add(cf.getCode());
					}
				} else {
					String expression = cf.getCustomFieldConfigure().getExpression();
					if (expression != null && !expression.contains("[")) { // 是自定义常量表达式，没有引用具体字段
						aclFieldCodes.add(cf.getCode());
					}
				}
			}
		}
	}

	//获取依赖的字段, 分析指标计算字段的依赖字段特殊处理
	public static Set<QueryField> getAtomFields(QueryConfigure config, QueryField field) {
		/** 直接从依赖的原子字段出
		 if (field.isAnalysisCalc()) {
		 CustomFieldConfigure customFieldConfigure = field.getCustomFieldConfigure();
		 List<CustomFieldExpressionIdMapping> mappings = customFieldConfigure.getExpressionIdMapping();
		 Set<QueryField> res = new HashSet<>();
		 if (mappings == null) {
		 return res;
		 }
		 for (CustomFieldExpressionIdMapping mapping : mappings) {
		 AnalysisItemConfig itemConfig = mapping.getAnalysisItemConfig();
		 if (itemConfig == null) {
		 continue;
		 }
		 String id = itemConfig.getMeasureId();
		 QueryField qf = config.getResult().getFieldById(id);
		 if (qf == null) {
		 qf = config.getResult().getFieldByCode(itemConfig.getMeasureCode());
		 }

		 if (qf != null) {
		 res.add(qf);
		 }
		 }
		 return res;
		 }
		 */

		Set<QueryField> calcAtomFields  = CollUtil.isNotEmpty(field.getCusCalcDependFields())?
				field.getCusCalcDependFields() : field.getCalcAtomFields();

		Set<QueryField> reuslt = new HashSet<>();
		for(QueryField qf : calcAtomFields){

			//agent的调用，计算字段使用自定义时间段的字段，需要获取自定义时间段指标的原子字段
			if(qf.getCode().contains(BIConsts.DATE_RANGE_TYPE_IDENTIFIER)){
				Set<QueryField> dateCustomAtomFields = CollUtil.isNotEmpty(qf.getCusCalcDependFields())?
						qf.getCusCalcDependFields() : qf.getCalcAtomFields();
				reuslt.addAll(dateCustomAtomFields);
			}else{
				reuslt.add(qf);
			}
		}

		return reuslt;
	}

	/**
	 * 判断是不是日均
	 * @return
	 */
	public static boolean isAvgByDay(QueryField atomField) {

		//先判断聚合方式
		AggExpressionType aggExpressionType = AggExpressionType.get(atomField.getAggExpressionType());
		if (aggExpressionType.isAvgByDay()) {
			return true;
		}

		//通过id后缀判断是不是日均值
		if (atomField.getId().endsWith(AggExpressionType.Avg_By_Day.getCode()) ||
				atomField.getId().endsWith(AggExpressionType.Avg_By_Day_Real.getCode())) {
			return true;
		}

		return false;
	}




	/**
	 1、黑名单说明：
	 - 黑名单管控哪些用户不能有哪些字段code的权限。当用户名=all时，则代表所有用户不能有对应字段code的权限，其中字段code没有为all的场景。
	 - 黑名单中的fieldGroup只是用来对管控的字段进行分组，方便加白使用

	 2、白名单说明：
	 - 白名单管控哪些用户可以有黑名单中哪些字段code的权限
	 - 白名单中的userName不会出现=all的场景

	 3、修改AclManager.getUnauthFieldCodeByBlackList的逻辑
	 - 若白名单中fieldGroup=all 且 fieldCode=all ，则当前用户字段权限：所有黑名单字段
	 - 若白名单中fieldGroup不是all 且 fieldCode=all，则当前用户字段权限：对应分组下的所有黑名单字段
	 - 若白名单中fieldGroup不是all 且 fieldCode不是all，则当前用户字段权限：对应分组下的黑名单中对应的字段编码的字段
	 - 若白名单中fieldGroup是all 且 fieldCode不是all，则当前用户字段权限：对应分组下的黑名单中对应的字段编码的字段
	 *
	 * 返回：
	 * 用户在黑名单但不在白名单中的字段code
	 * @param userName
	 * @return
	 */
	public static Set<String> getUnauthFieldCodeByBlackList(String userName){
		Set<String> unauthFieldCodes = new HashSet<>();
		final String all = "all";

		// 获取该用户相关的黑名单记录（包括用户名为all的通用黑名单）
		List<MetaFieldAuthBlackList> userBlackList = aclFieldBlackList.stream()
				.filter(item -> all.equals(item.getUserName()) || userName.equals(item.getUserName()))
				.collect(Collectors.toList());
		if (CollUtil.isEmpty(userBlackList)) {
			return unauthFieldCodes;
		}

		// 获取该用户相关的白名单记录
		List<MetaFieldAuthWhiteList> userWhiteList = aclFieldWhiteList.stream()
				.filter(item -> userName.equals(item.getUserName()))
				.collect(Collectors.toList());

		for (MetaFieldAuthBlackList blackItem : userBlackList) {
			boolean isWhitelisted = false;
			for (MetaFieldAuthWhiteList whiteItem : userWhiteList) {
				String whiteFieldGroup = whiteItem.getFieldGroup();
				String whiteFieldCode = whiteItem.getFieldCode();

				// fieldGroup=all 且 fieldCode=all，则放行所有黑名单字段
				if (all.equals(whiteFieldGroup) && all.equals(whiteFieldCode)) {
					isWhitelisted = true;
					break;
				}

				// fieldGroup不是all 且 fieldCode=all，则放行对应分组全部黑名单字段
				if (!all.equals(whiteFieldGroup) && all.equals(whiteFieldCode)
						&& Objects.equals(whiteFieldGroup, blackItem.getFieldGroup())) {
					isWhitelisted = true;
					break;
				}

				// fieldGroup不是all 且 fieldCode不是all，则放行对应分组+字段编码
				if (!all.equals(whiteFieldGroup) && !all.equals(whiteFieldCode)
						&& Objects.equals(whiteFieldGroup, blackItem.getFieldGroup())
						&& Objects.equals(whiteFieldCode, blackItem.getFieldCode())) {
					isWhitelisted = true;
					break;
				}

				// fieldGroup=all 且 fieldCode不是all，则放行所有分组中对应字段编码
				if (all.equals(whiteFieldGroup) && !all.equals(whiteFieldCode)
						&& Objects.equals(whiteFieldCode, blackItem.getFieldCode())) {
					isWhitelisted = true;
					break;
				}
			}
			if (!isWhitelisted) {
				unauthFieldCodes.add(blackItem.getFieldCode());
			}
		}

		return unauthFieldCodes;
	}


}
