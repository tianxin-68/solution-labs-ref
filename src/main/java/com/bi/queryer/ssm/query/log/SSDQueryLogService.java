package com.bi.queryer.ssm.query.log;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbConfig;
import com.bi.queryer.ssm.engine.config.QueryAnalysis;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.TextToZipUtil;
import com.bi.queryer.ssm.util.OssUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.WeekDateUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Qualifier("SSDQueryLogService")
public class SSDQueryLogService {


	private static final Integer MAX_THREAD_NUM = 3;

	private static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD_NUM);

	@Autowired
	protected BaseDao dao;

	/**
	 * sql日志
	 *
	 * @param entity
	 */
	public void log(SSDQueryLogEntity entity, QueryConfigure config) {
		try {
			threadPool.execute(()-> {
				//主体日志：独立线程单独处理，便于后续步骤可快速获取日志存储的字段id
				logQuerySql(entity, config);
			});

			threadPool.execute(()-> {
				// 查询配置详情
				logQueryDetail(entity, config);

				// 查询前端配置字段
				logQueryConfigFields(entity, config);

				//插入巡检查询日志
				insertInspectionQueryLog(entity, config);
			});

			/**
			SSDQueryLogWriter biSysLogWriter = new SSDQueryLogWriter(this.dao, "ssm.query.insertQueryLog", entity);
			biSysLogWriter.start();
			 */

			/**
			biSysLogWriter = new SSDQueryLogWriter(this.dao, "ssm.query.insertQueryDetailLog", detailLogEntity);
			biSysLogWriter.start();
			*/
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void insertInspectionQueryLog(SSDQueryLogEntity entity,QueryConfigure config){

		if (config.getTemplateEntity() == null) {
			return;
		}

		//非巡检类型的不处理
		String type = config.getTemplateEntity().getType();
		if(QuerySourceType.INSPECT != QuerySourceType.get(type)){
			return;
		}

		//查询数据才记录
		QueryModeType queryModeType = config.getSettings().getQueryModeType();
		if(QueryModeType.ALL != queryModeType){
			return;
		}

		SSMInspectionQueryLog inspectionQueryLog = new SSMInspectionQueryLog();

		inspectionQueryLog.setTplId(entity.getTemplateId());
		inspectionQueryLog.setViewId(entity.getViewId());
		inspectionQueryLog.setQuerySuccess(entity.getSuccess());
		inspectionQueryLog.setQueryRows(entity.getRows());
		inspectionQueryLog.setQueryBeginTime(entity.getBeginTime());
		inspectionQueryLog.setQueryEndTime(entity.getEndTime());
		inspectionQueryLog.setRemark(entity.getInfo());
		inspectionQueryLog.setCreatedBy(entity.getUserName());
		inspectionQueryLog.setQueryLogId(entity.getId());
		inspectionQueryLog.setDsKey(entity.getDsKey());

		dao.insert("ssm.query.inspectionQueryLog", inspectionQueryLog);

	}

	protected void logQuerySql(SSDQueryLogEntity entity, QueryConfigure config) {
		if (config.getTemplateEntity() != null) {
			String type = config.getTemplateEntity().getType();
			if (StrUtil.isEmpty(type)) {
				type = "template";
			}
			entity.setQuerySource(type);
		}

		String logId = entity.getId();
		try {
			// querySQL → zip 压缩上传 OSS，用 URL 替换（zip 内部文件后缀 .sql）
			entity.setQuerySQL(zipAndUploadToOss(entity.getQuerySQL(), logId + ".sql"));

			// queryParam → zip 压缩上传 OSS，用 URL 替换（zip 内部文件后缀 .json）
			entity.setQueryParam(zipAndUploadToOss(entity.getQueryParam(), logId + ".json"));
		}catch (Exception e){
			e.printStackTrace();
		}

		dao.insert("ssm.query.insertQueryLog", entity);
	}

	/**
	 * 将内容 zip 压缩后上传 OSS，返回 OSS URL；
	 * 上传失败时返回原始内容（不截断）。
	 *
	 * @param originalContent   原始字段内容（querySQL 或 queryParam）
	 * @param zipEntryFileName  zip 包内部文件名（含后缀，如 "logId.sql"、"logId.json"）
	 * @return OSS URL（上传成功）或原始内容（上传失败）
	 */
	private String zipAndUploadToOss(String originalContent, String zipEntryFileName) {
		if (originalContent == null) {
			return originalContent;
		}
		try {
			byte[] zippedBytes = TextToZipUtil.toZip(originalContent, zipEntryFileName);
			String ossFileUrl = OssUtil.upload(zippedBytes, "", ".zip", "bigdata/ssm/queryLog");
			if (StrUtil.isNotEmpty(ossFileUrl)) {
				return ossFileUrl;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return originalContent;
	}

	public void logExtend(final SSDQueryLogEntity entity) {
		threadPool.execute(() -> {
			try {
				// 因为log主体是异步存储，等待3s后在做更新
				Thread.sleep(3000);
				dao.update("ssm.query.updateQueryLog", entity);
			} catch (Exception ignored) {
			}
		});
	}

	/**
	 * 保存UI查询埋点日志
	 * @param entity
	 */
	public void saveQueryLogUi(SSDQueryLogUIEntity entity) {

		entity.setUserName(UserManager.get().getName());

		threadPool.execute(() -> {
			dao.insert("ssm.query.insertQueryLogUi", entity);
		});
	}

	/**
	 * 保存模板看板的加载性能日志
	 * @param entity
	 */
	public void addTplVisitLog(SSMQueryTplVisitEntity entity) {

		entity.setUserName(UserManager.get().getName());
		entity.setCreatedBy(UserManager.get().getName());
		entity.setEnv(BIUtil.getRuntimeEnv().getCode());
		threadPool.execute(() -> {
			dao.insert("ssm.query.insertQueryTplVisitLog", entity);
		});
	}

	/**
	 * 只记录最终真实查询的字段
	 * @param fields
	 * @param sqlLogEntity
	 * @param source
	 */
	public void logFinalQueryFields(Collection<QueryField> fields, SSDQueryLogEntity sqlLogEntity, String source) {
		try {
			if (fields == null || fields.isEmpty()) {
				return;
			}
			List<SSDQueryFieldLogEntity> fieldLogEntities = new ArrayList<>();
			for (QueryField f : fields) {
				SSDQueryFieldLogEntity fieldLogEntity = new SSDQueryFieldLogEntity();
				fieldLogEntity.setSqlLogId(sqlLogEntity.getId());
				fieldLogEntity.setTemplateId(sqlLogEntity.getTemplateId());
				fieldLogEntity.setUserName(sqlLogEntity.getUserName());
				fieldLogEntity.setViewId(sqlLogEntity.getViewId());
				fieldLogEntity.setModuleId(f.getModuleCtgId());
				fieldLogEntity.setCtgId(f.getCtgId());
				fieldLogEntity.setFieldId(f.getId());
				fieldLogEntity.setFieldName(f.getName());
				fieldLogEntity.setFieldCode(f.getCode());
				fieldLogEntity.setFieldTitle(f.getMeta().getTitle());
				fieldLogEntity.setFieldDesc(f.getMeta().getTitle());
				fieldLogEntity.setFieldSource(source);
				fieldLogEntity.setIsResult(f.getIsResult() ? Enabled.YES.getId() : Enabled.NO.getId());
				fieldLogEntity.setIsFilter(f.getIsFilter() && BIUtil.isNotEmpty(f.getValues()) ? Enabled.YES.getId() : Enabled.NO.getId());

				// 过滤值：最多10个
				String values = null;
				if(BIUtil.isNotEmpty(f.getValues())){
					values = BIUtil.listToStr(f.getValues().stream().map(FieldValue::getId).limit(10).collect(Collectors.toList()));
				}
				fieldLogEntity.setFilterValues(values);

				fieldLogEntities.add(fieldLogEntity);
			}
			/**
			 SSDQueryLogWriter biSysLogWriter = new SSDQueryLogWriter(this.dao, "ssm.query.insertFieldLog", params);
			 biSysLogWriter.start();
			 */
			threadPool.execute(()-> dao.insert( "ssm.query.insertFieldLog", fieldLogEntities));

			// 统计字段表达式长度
			logQueryTplFieldStats(fields, sqlLogEntity);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	// 记录模板字段粒度的统计日志
	public void logQueryTplFieldStats(Collection<QueryField> fields, SSDQueryLogEntity sqlLogEntity) {
		try {
			if (fields == null || fields.isEmpty() || BIUtil.isEmpty(sqlLogEntity.getTemplateId())) {
				return;
			}
			List<SSMQueryTplFieldLogEntity> entities = new ArrayList<>();
			for (QueryField f : fields) {
				int exprSize = getCustomFieldExprSize(f);
				if (exprSize <= 0) {
					continue;
				}

				SSMQueryTplFieldLogEntity entity = new SSMQueryTplFieldLogEntity();
				entity.setLastQueryLogId(sqlLogEntity.getId());
				entity.setTplId(sqlLogEntity.getTemplateId());
				entity.setViewId(sqlLogEntity.getViewId());
				entity.setFieldId(f.getId());
				entity.setFieldCode(f.getCode());
				entity.setFieldTitle(f.getTitle());
				entity.setFieldExprSize(exprSize);
				entity.setCreatedBy(sqlLogEntity.getUserName());
				entities.add(entity);
			}
			if (!entities.isEmpty()) {
				threadPool.execute(() -> dao.insert("ssm.query.insertQueryTplFieldLog", entities));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取自定义字段表达式的长度
	private int getCustomFieldExprSize(QueryField field) {
		if (field == null || !field.isCustom() || !field.isCalc()) {
			return 0;
		}

		Set<QueryField> dependFields = field.getCusCalcDependFields();

		if (BIUtil.isEmpty(dependFields)) {
			return 0;
		}

		String expr = field.getMeta().getAggExpression();
		// 替换字段名称, 用calcAtomFields替换
		for (QueryField f : field.getCalcAtomFields()) {
			expr = expr.replace("[" + f.getCode() + "]", f.getTitle());
		}

		return expr.length();
	}

	/**
	 * 只记录前端配置的字段
	 * @param sqlLogEntity
	 */
	public void logQueryConfigFields(SSDQueryLogEntity sqlLogEntity, QueryConfigure config) {
		try {
			if(sqlLogEntity == null || config == null || BIUtil.isEmpty(config.getConfig())){
				return;
			}
			UIQueryConfigure uiConfig = JSONObject.parseObject(config.getConfig(), UIQueryConfigure.class);
			Map<String, UIQueryField> resultFields = uiConfig.getResult().getAllFields().stream().filter(f->Enabled.isTrue(f.getIsShow())).collect(Collectors.toMap(UIQueryField::getId, f->f, (f1, f2) ->f1));
			Map<String, UIQueryField> filterFields = uiConfig.getFilter().stream().filter(f->Enabled.isTrue(f.getIsShow())).collect(Collectors.toMap(UIQueryField::getId, f->f, (f1, f2) ->f1));

			List<UIQueryField> allFields = new ArrayList<>(resultFields.values());
			allFields.addAll(filterFields.values());
			allFields.stream().forEach(f->{
				MetaField meta = SSDMetaCacheManager.getField(f.getId());
				if(meta != null){
					BeanUtil.copyProperties(meta, f, CopyOptions.create().ignoreCase().ignoreNullValue());
				}
				f.setInResultArea(resultFields.containsKey(f.getId()));
				f.setInFilterArea(filterFields.containsKey(f.getId()));
				if(filterFields.containsKey(f.getId())){
					// 过滤值重新赋值：避免后面去重时，导致过滤值丢失
					f.setValues(filterFields.get(f.getId()).getValues());
				}
			});

			// 计算字段、lod字段等，找对应模块id
			// 设置字段的模块id和所属目录id
			Map<String, UIQueryField> newRefFields = new HashMap<>();
			for(UIQueryField f : allFields){
				f.setCtgId(SSDMetaCacheManager.getCategoryIdByFieldIdAndModuleCtgId(f.getId(), f.getModuleCtgId()));
				// 若是计算字段，则先通过id mapping获取对应原子字段的模块id
				CustomFieldConfigure customFieldConfigure = f.getCustomFieldConfigure();
				if(customFieldConfigure != null && BIUtil.isNotEmpty(customFieldConfigure.getExpression())){
					List<CustomFieldExpressionIdMapping> mappings = customFieldConfigure.getExpressionIdMapping();
					for(CustomFieldExpressionIdMapping m : mappings){
						if(BIUtil.isEmpty(m.getId()) || BIUtil.isEmpty(m.getModuleCtgId())){
							continue;
						}
						MetaField metaField = SSDMetaCacheManager.getField(m.getId());
						// 结果和过滤中都不存在，则新增
						if(metaField != null && (!resultFields.containsKey(m.getId()) && !filterFields.containsKey(m.getId()))){
							UIQueryField refField = new UIQueryField();
							BeanUtil.copyProperties(metaField, refField, CopyOptions.create().ignoreCase().ignoreNullValue());
							refField.setModuleCtgId(m.getModuleCtgId());
							refField.setInResultArea(f.isInResultArea());
							refField.setInFilterArea(f.isInFilterArea());
							refField.setCtgId(SSDMetaCacheManager.getCategoryIdByFieldIdAndModuleCtgId(refField.getId(), refField.getModuleCtgId()));
							newRefFields.put(m.getId(), refField);
						}
					}
				}
			}

			allFields.addAll(newRefFields.values());

			// 按先后顺序去重
			List<SSDQueryCfgFieldLogEntity> fieldLogEntities = new ArrayList<>();
			Set<String> finalFieldIds = new HashSet<>();
			for(UIQueryField f : allFields){
				if(finalFieldIds.contains(f.getId())){
					continue;
				}
				if(BIConsts.ALL_MEASURE_CODE.equals(f.getCode())){
					continue;
				}
				SSDQueryCfgFieldLogEntity fieldEntity = new SSDQueryCfgFieldLogEntity(f);
				BeanUtil.copyProperties(sqlLogEntity, fieldEntity, CopyOptions.create().ignoreCase().ignoreNullValue());
				fieldEntity.setSqlLogId(sqlLogEntity.getId());
				fieldEntity.setFieldSource(sqlLogEntity.getQuerySource());
				fieldLogEntities.add(fieldEntity);
				finalFieldIds.add(f.getId());
			}
			this.dao.insert("ssm.query.insertCfgFieldLog", fieldLogEntities);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * 废弃：配置日志和最终查询日志分开存储
	 * 字段日志：配置字段 + 真实查询字段
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void logFields2(Collection<QueryField> fields, SSDQueryLogEntity entity, String source) {
		try {
			if (fields == null || fields.isEmpty()) {
				return;
			}
			List<Map> params = new ArrayList<Map>();
			for (QueryField f : fields) {
				Map item = new HashMap();

				item.put("templateId", entity.getTemplateId());
				item.put("viewId", entity.getViewId());
				item.put("userName", entity.getUserName());
				item.put("fieldId", f.getId());
				item.put("fieldCode", f.getCode());
				item.put("fieldName", f.getName());
				item.put("fieldDesc", f.getMeta().getTitle());
				item.put("fieldSource", source);
				item.put("isFilter", f.getIsFilter() && BIUtil.isNotEmpty(f.getValues()) ? Enabled.YES.getId() : Enabled.NO.getId());
				item.put("isResult", f.getIsResult() ? Enabled.YES.getId() : Enabled.NO.getId());
				item.put("logId", entity.getId());

				// 添加字段所属模块和目录id
				item.put("moduleId", f.getModuleCtgId());
				item.put("ctgId", f.getCtgId());

				// 过滤值：最多10个
				String values = null;
				if(BIUtil.isNotEmpty(f.getValues())){
					values = BIUtil.listToStr(f.getValues().stream().map(FieldValue::getId).limit(10).collect(Collectors.toList()));
				}
				item.put("filterValues", values);
				params.add(item);
			}
			/**
			SSDQueryLogWriter biSysLogWriter = new SSDQueryLogWriter(this.dao, "ssm.query.insertFieldLog", params);
			biSysLogWriter.start();
			 */
			threadPool.execute(()-> dao.insert( "ssm.query.insertFieldLog", params));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * 查询配置详情：同环比、日期范围、表等
	 * @param rawConfig
	 */
	public void logQueryDetail(SSDQueryLogEntity mainLogEntity, QueryConfigure rawConfig) {
		SSDQueryDetailLogEntity entity = new SSDQueryDetailLogEntity(mainLogEntity);

		QueryConfigure config = rawConfig.clone();
		//config.setLoadPost(false);
		config.load();
		// 配置开始时间
		entity.setConfigBeginTime(config.getSettings().getConfigBeginTime());

		QueryAnalysis analysis = config.getAnalysis();

		//贡献度设置
		Set<String> ctrInfos = new LinkedHashSet<>();

		// 同环比
		Set<String> thbInfos = new LinkedHashSet<>();
		AnalysisThbConfig thbConfig = analysis.getThb();
		if (thbConfig.isActive()) {

			for (AnalysisThbItemConfig analysisThbItemConfig : thbConfig.getItems()) {
				thbInfos.addAll(analysisThbItemConfig.getCalcModes());

				//是否含有贡献率配置
				if (analysisThbItemConfig.getCalcTypes().contains(AnalysisCalcType.CONTRIBUTION_RATE.getCode())) {

					if (analysisThbItemConfig.getCtr().getItems().contains(BIConsts.ALL_DIM)) {
						ctrInfos.addAll(analysisThbItemConfig.getCalcModes());
					} else {
						ctrInfos.addAll(analysisThbItemConfig.getCtr().getItems());
					}
				}

			}
		}
		if (BIUtil.isNotEmpty(thbInfos)) {
			entity.setConfigThb(BIUtil.listToStr(thbInfos));
		}

		// 占比
		Set<String> zbInfos = new LinkedHashSet<>();
		AnalysisZbConfig zbConfig = analysis.getZb();
		if (zbConfig.isActive()) {
			zbConfig.getItems().forEach(c -> {
				zbInfos.add(c.getCalcMode());
			});
		}
		if (BIUtil.isNotEmpty(zbInfos)) {
			entity.setConfigZb(BIUtil.listToStr(zbInfos));
		}

		// 汇总
		Set<String> totalInfos = new LinkedHashSet<>();
		AnalysisTotalConfig totalConfig = analysis.getTotal();
		if (totalConfig.isActive()) {
			totalConfig.getItems().forEach(c -> {
				totalInfos.add(c.getTotalType().getCode());
			});
		}
		if (BIUtil.isNotEmpty(totalInfos)) {
			entity.setConfigTotal(BIUtil.listToStr(totalInfos));
		}

		// 自定义对比
		Set<String> customCompareInfos = new LinkedHashSet<>();
		AnalysisCompareConfig compareConfig = analysis.getCompare();
		if (compareConfig.isActive()) {
			compareConfig.getItems().forEach(c -> {
				String info = c.getCalcMode() + c.getCompareIndex() + "[" + BIUtil.listToStr(c.getCompareDates()) + "]";
				customCompareInfos.add(info);

				//是否含有贡献率配置
				if (c.getCalcTypes().contains(AnalysisCalcType.CONTRIBUTION_RATE.getCode())) {
					ctrInfos.add(AnalysisCalcMode.CUSTOM_COMPARE.getCode());
				}
			});
		}

		// 贡献率
		if (BIUtil.isNotEmpty(ctrInfos)) {
			entity.setConfigCtr(BIUtil.listToStr(ctrInfos));
		}

		if (BIUtil.isNotEmpty(customCompareInfos)) {
			entity.setConfigCustomCompare(BIUtil.listToStr(customCompareInfos));
		}

		//设置查询公共日期查询范围
		QueryField commonDateFilter = config.getFilterCommonDateField();
		if (commonDateFilter != null) {
			List<String> filterValues = commonDateFilter.getValues().stream().map(FieldValue::getId).collect(Collectors.toList());
			entity.setCommonDateQueryRange(JSONObject.toJSONString(filterValues));
		}

		// 日期粒度
		entity.setDateGranularity(config.getSettings().getDateGranularity());

		//数据查询的范围
		List<String> queryDateRange = SSDUtil.getFilterDateRange(config);
		entity.setDataQueryRange(JSONObject.toJSONString(queryDateRange));

		//String querySql = entity.getQuerySQL();
		//entity.setDataQueryRange(JSONObject.toJSONString(getDataQueryRange(querySql, config.getSettings().getDateGranularity())));

		//热表查询引擎
		if(StrUtil.isNotEmpty(entity.getQueryTables())) {
			String[] queryTableArray = entity.getQueryTables().split(",");

			//热表数量
			int hotTableCount = 0;
			//所有查询表数量
			int allQueryTableCount = queryTableArray.length;

			for (String queryTable : queryTableArray) {
				if (queryTable.contains(BIConsts.HOT_TABLE_SCHEMA)) {
					hotTableCount++;
				}
			}

			if (hotTableCount > 0) {
				entity.setHotQueryEngine(entity.getDsKey());
			}

			if (hotTableCount == allQueryTableCount) {
				entity.setIsAllQueryTableHitHot(Enabled.YES.getId());
			}
		}

		entity.setUserAgent(config.getSettings().getUserAgent());
		entity.setTerminal(BIUtil.getUserTerminalType(config.getSettings().getUserAgent()));
		entity.setOlapApiKey(config.getSettings().getOlapApiKey());
		entity.setHostname(config.getSettings().getHostname());
		entity.setCacheType(mainLogEntity.getCacheType());

		dao.insert("ssm.query.insertQueryDetailLog", entity);
	}

	//正则匹配sql中between and 之间的多个时间片段
	public static List<String> getDataQueryRange(String sql, String dateGranularity) {
		if (BIUtil.isEmpty(sql)) {
			return Collections.emptyList();
		}
		String regex = "\\s*between\\s*'(20[\\d\\-]+.*?)'\\s*and\\s*'(20[\\d\\-]+.*?)'\\s*";
		List<Date> beginList = new ArrayList<>();
		List<Date> endList = new ArrayList<>();
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(sql);
		while (m.find()) {
			beginList.add(parseDate(m.group(1), dateGranularity, true));
			endList.add(parseDate(m.group(2), dateGranularity, false));
		}
		if (beginList.isEmpty() || endList.isEmpty()) {
			return Collections.emptyList();
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		List<String> res = new ArrayList<>(2);
		res.add(dateFormat.format(Collections.min(beginList)));
		res.add(dateFormat.format(Collections.max(endList)));
		return res;
	}

	private static Date parseDate(String date, String dateGranularity, boolean isBegin) {
		if (date == null) {
			return null;
		}

		try {
			if (date.trim().length() >= 10) {
				return DateUtil.parse(date.trim().substring(0, 10), "yyyy-MM-dd");
			}

			DateGranularity dg = DateGranularity.get(dateGranularity);
			Date resDate = null;
			switch (dg) {
				case YEAR:
					resDate = DateUtil.parse(date, "yyyy");
					if (!isBegin) {
						resDate = DateUtil.endOfYear(resDate);
					}
					break;
				case MONTH:
					resDate = DateUtil.parse(date, "yyyyMM");
					if (!isBegin) {
						resDate = DateUtil.endOfMonth(resDate);
					}
					break;
				case WEEK:
					if (isBegin) {
						resDate = WeekDateUtil.getWeekFirstDay(date);
					} else {
						resDate = WeekDateUtil.getWeekLastDay(date);
					}
					break;
			}

			if (isBegin || resDate == null) {
				return resDate;
			}

			Date yesterday = DateUtil.yesterday();
			if (resDate.after(yesterday)) {
				return yesterday;
			} else {
				return resDate;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public void logFilterQuery(SSMFilterQueryLogEntity entity) {
		try {
			if (entity == null) {
				return;
			}

			//20250102 因为需要记录ui_begin_time，ui_end_time 改为同步
			this.dao.insert( "ssm.query.insertFilterQueryLog", entity);
			//(new SSDQueryLogWriter(this.dao, "ssm.query.insertFilterQueryLog", entity)).start();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * 存储配置字段信息
	 * @param entity
	 * @param config
	 */
	public void logConfigFields(SSDQueryLogEntity entity, QueryConfigure config){

	}
}

/**
 * 启用单独的线程执行日志入库操作
 * @author contributor
 */
class SSDQueryLogWriter extends Thread {
	private BaseDao dao = null;
	private Object params = null;
	private String sqlId = "";

	public SSDQueryLogWriter(BaseDao dao, String sqlId, Object params) {
		this.dao = dao;
		this.sqlId = sqlId;
		this.params = params;
	}

	public void run() {
		try {
			// 如果是UT环境的相关操作，则不记录日志。
			/*if (SC.appName.toUpperCase().contains("UT")) {
				return;
			}*/
			dao.insert(sqlId, params);
		} catch (Exception e) {
			System.out.println("SSD日志添加出错！！");
			e.printStackTrace();
		}

	}

}
