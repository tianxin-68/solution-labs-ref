package com.bi.queryer.ssm.engine.config;

import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.ui.normalizer.UIQueryConfigureNormalizer;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.QueryConfigureType;
import com.bi.queryer.ssm.meta.MetaDataset;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * 模板查询配置
 * @author contributor
 *
 */
public class QueryConfigure implements JSONSerializable {
	
	// 查询模板
	private SSDQueryTemplate templateEntity ;

	//全局筛选，对lod生效
	private QueryFilter globalFilter = new QueryFilter();
	
	private QueryFilter filter = new QueryFilter();
	
	private QueryResult result = new QueryResult();

	private QueryAnalysis analysis = new QueryAnalysis();

	private QueryMesoscopic meso = new QueryMesoscopic();

	private QuerySettings settings = new QuerySettings();

	private String sessionId = "";

	private String config = "";

	/**
	 * 是否加载后置处理
	 */
	private boolean isLoadPost = true;

	/**
	 * 查询配置标准化处理器
	 */
	private QueryConfigureNormalizer normalizer = null;

	//慎用，因为该类有继承类ChartQueryConfigure，他们的load行为不一样
	public QueryConfigure(SSDQueryTemplate templateEntity){
		this();
		this.templateEntity = templateEntity;
	}
	
	public QueryConfigure(){
		this.normalizer = new QueryConfigureNormalizer();
	}
	
	/**
	 * 加载
	 */
	public void load(){
		if (templateEntity == null) {
			return;
		}
		String config = templateEntity.getConfig();
		if(!StringUtil.isEmpty(config)){
			UIQueryConfigureNormalizer uiQueryConfigureNormalizer = new UIQueryConfigureNormalizer(config, this.getType());
			config = uiQueryConfigureNormalizer.normalize();
			templateEntity.setConfig(config);
			load(config);
		}
		/*
		Integer queryTimeoutSec = settings.getQueryTimeoutSec();
		if(queryTimeoutSec == null || queryTimeoutSec <= 0) {
			queryTimeoutSec = templateEntity.getQueryTimeoutSec();
		}
		if(queryTimeoutSec == null || queryTimeoutSec <= 0) {
			queryTimeoutSec = BIConsts.QUERY_TIME_OUT_SEC;
		}
		settings.setQueryTimeoutSec(queryTimeoutSec);
		 */
	}
	
	public void load(String config) {
		this.config = config;
		JSONObject configJSON = JSONObject.parseObject(config);
		load(configJSON);
	}
	
	public void reload(){
		load(this.config);
	}
	
	public void load(JSONObject root) {

		sessionId = root.getString("sessionId");

		meso = new QueryMesoscopic();
		JSONArray mesoJson = root.getJSONArray("meso");
		if(mesoJson != null){
			meso.load(mesoJson);
		}

		filter = new QueryFilter();
		filter.load(root.getJSONArray("filter"));

		result = new QueryResult();
		result.setCustomMeasureFilterIds(filter.getCustomMeasureFilterIds());
		result.load(root.getJSONObject("result"), meso);

		analysis = new QueryAnalysis();
		JSONObject analysisJson = root.getJSONObject("analysis");
		if(analysisJson != null){
			analysis = JSONObject.parseObject(analysisJson.toJSONString(), QueryAnalysis.class);
		}

		settings = new QuerySettings();
		if(BIUtil.isNotEmpty(root.getJSONObject("setting"))) {
			settings = JSONObject.parseObject(root.getJSONObject("setting").toJSONString(), QuerySettings.class);
		}
        settings.setQueryTimeoutSec(QuerySessionSettingManager.getQueryTimeoutSec());
		buildDatasetType();

		this.post();
	}

	/**
	 * 加载完后置处理
	 */
	public void post() {
		if(!isLoadPost){
			return;
		}
		normalizer.normalize(this);
	}

	@Override
	public JSONObject toJSON() {
		JSONObject result =  new JSONObject();

		result.put("filter", this.filter);
		result.put("result", this.result);
		result.put("setting", this.settings);

		return result;
	}

	public QueryFilter getFilter() {
		return filter;
	}

	public void setFilter(QueryFilter filter) {
		this.filter = filter;
	}

	public QueryFilter getGlobalFilter() {
		return globalFilter;
	}

	public void setGlobalFilter(QueryFilter globalFilter) {
		this.globalFilter = globalFilter;
	}

	public QueryResult getResult() {
		return result;
	}

	public void setResult(QueryResult result) {
		this.result = result;
	}

	public SSDQueryTemplate getTemplateEntity() {
		return templateEntity;
	}

	public void setTemplateEntity(SSDQueryTemplate templateEntity) {
		this.templateEntity = templateEntity;
	}

	public String getConfig() {
		return config;
	}

	public void setConfigXML(String config) {
		this.config = config;
	}

	public List<QueryField> getAllFields(){
		QueryResult result = this.getResult();

		// 结果字段
		List<QueryField> allFields = new ArrayList<>();
		if(result != null && BIUtil.isNotEmpty(result.getFields())) {
			allFields.addAll(result.getFields());
		}

		// 过滤字段
		QueryFilter filter = this.getFilter();
		if(filter != null && BIUtil.isNotEmpty(filter.getFields())) {
			allFields.addAll(filter.getFields());
		}

		return allFields;
	}

	public List<QueryField> getAllQueryOriginFields() {
		List<QueryField> allFields = new ArrayList<>();
		try {
			if (BIUtil.isEmpty(this.config)) {
				return allFields;
			}
			JSONObject root = JSONObject.parseObject(this.config);
			QueryFilter filter = new QueryFilter();
			filter.load(root.getJSONArray("filter"));

			QueryResult result = new QueryResult();
			result.setCustomMeasureFilterIds(filter.getCustomMeasureFilterIds());
			result.load(root.getJSONObject("result"));

			allFields.addAll(result.getFields());
			allFields.addAll(filter.getFields());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return allFields;
	}

	/**
	 * 是否有分析
	 * @return
	 */
	public Boolean hasAnalysis(){
		// 只分析且不是分类聚合字段
		long count = this.getResult().getFields().stream().filter(f -> Enabled.value(f.getIsAnalysis()) && !f.isCategoryAggregation()).count();
		boolean hasTotalAnalysis = this.getAnalysis().isActive(); //this.getAnalysis().total.isActive();
		return count > 0 || hasTotalAnalysis;
	}

	public boolean hasTargetAnalysis() {
		return this.getResult().getFields().stream().anyMatch(QueryField::isTargetValue) || getAnalysis().hasTargetAnalysis();
	}

	public QueryField getFilterCommonDateField(){
		QueryField commonDateField = null;
		List<QueryField> fields = filter.getFields();
		for(QueryField field : fields){
			if(field.isCommonDate()){
				commonDateField = field;
				break;
			}
		}
		return commonDateField;
	}

	public QueryField getResultCommonDateField() {
		QueryField commonDateField = null;
		List<QueryField> fields = result.getFields();
		for (QueryField field : fields) {

			//某些指标计算表达式含有dt，在汇总场景isAggQuery值不对，此处过滤掉
			if (field.isAppend()) {
				continue;
			}

			if (field.isCommonDate()) {
				commonDateField = field;
				break;
			}
		}
		return commonDateField;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public QueryAnalysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(QueryAnalysis analysis) {
		this.analysis = analysis;
	}

	public QuerySettings getSettings() {
		return settings;
	}

	public void setSettings(QuerySettings settings) {
		this.settings = settings;
	}

	public boolean isAggQuery() {
		return Enabled.value(settings.getIsAggQuery());
	}

	public QueryMesoscopic getMeso() {
		return meso;
	}

	public void setMeso(QueryMesoscopic meso) {
		this.meso = meso;
	}

	public List<QueryField> getLodFields(){
		List<QueryField> fields = this.getResult().getMeasures();
		List<QueryField> lodFields = new ArrayList<>();
		if(BIUtil.isEmpty(fields)) {
			return lodFields;
		}
		for(QueryField f : fields){
			if(f.getCustomFieldConfigure() != null &&
					CustomFieldType.isLod(f.getCustomFieldConfigure().getType())) {
				lodFields.add(f);
			}
		}
		return lodFields;
	}

	public boolean isLodConfig(){
		return BIUtil.isNotEmpty(this.getLodFields());
	}

	/**
	 * 是否显示农历日期
	 * @return
	 */
	public boolean isShowLunarDate() {
		QueryField dateField = this.getResultCommonDateField();

		boolean showLunarDate = false;
		if (!settings.isBusinessCalendar()) {
			showLunarDate = Enabled.isTrue(this.getSettings().getShowLunarDate())
					&& (dateField != null)
					&& !dateField.isAggQuery()
					&& DateGranularity.get(dateField.getQueryDateGranularity()) == DateGranularity.DAY;
		} else {

			//业务日历不判断日期是否聚合
			showLunarDate = Enabled.isTrue(this.getSettings().getShowLunarDate())
					&& (dateField != null)
					&& DateGranularity.get(dateField.getQueryDateGranularity()) == DateGranularity.DAY;
		}

		return showLunarDate;
	}

	/**
	 * 是否只配置了汇总
	 * @return
	 */
	public boolean isConfigTotalOnly() {

		if (!hasAnalysis()) {
			return false;
		}

		if (this.analysis.getThb().isActive() ||
				this.analysis.getZb().isActive() ||
				this.analysis.getCompare().isActive() ||
				this.analysis.getTarget().isActive()
		) {
			return false;
		}

		if (this.analysis.getTotal().isActive()) {
			return true;
		}

		return false;
	}

	/**
	 * 构建数据集类型,标记是实时还是离线
	 */
	public void buildDatasetType() {
		String datasetId = settings.getDatasetId();
		MetaDataset dataset = SSDMetaCacheManager.getDataset(datasetId);
		if (dataset != null) {
			DataTypeEnum datasetType = DataTypeEnum.codeOf(dataset.getDatasetType());
			settings.setDatasetType(datasetType);
		}
	}

	public boolean isLoadPost() {
		return isLoadPost;
	}

	public void setLoadPost(boolean loadPost) {
		isLoadPost = loadPost;
	}

	public QueryConfigure clone() {
		return new QueryConfigure(this.getTemplateEntity());
	}

	public QueryConfigureType getType(){
		return QueryConfigureType.Table;
	}

	// 是否是实时数据集的趋势图查询
	public boolean isRtDatasetChartQuery() {

		if (QueryConfigureType.Chart == this.getType() && this.getSettings().isRtDataset()) {
			return true;
		}

		return false;
	}

}
