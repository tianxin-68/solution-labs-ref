package com.bi.queryer.ssm.engine;

import com.bi.queryer.ssm.engine.acl.AclDataAuthExistsFilterField;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.prepare.PrepareQueryResult;
import com.bi.queryer.ssm.meta.MetaFieldDataAuth;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 查询上下文环境
 * @author contributor
 *
 */
public class QueryContext {
	
	private Map<String, String> queryParams = new HashMap<String, String>();

	private String queryParamString = "";
	
	private User user = null;

	private Integer decryptSensitiveField = 0;
	
	private HttpServletRequest request = null;

	// list -> map 提升匹配性能
	private Map<String, String> aclFields = new HashMap(100); // 有权限的字段code列表

	private List<String> aclSensitiveFields = new ArrayList<>(); // 有权限敏感字段code列表

	private List<String> sensitiveApplyFields = new ArrayList<>(); // 解密申请敏感字段code列表
	
	private Set<QueryField> usedFields = new HashSet<QueryField>(); // 查询用到的字段


	private Map<String, Set<QueryField>> modelUsedFields = new HashMap<>(); // 每个模型查询用到的字段

	private List<MetaFieldDataAuth> dataAuthList = new ArrayList<>(); // 用户数据权限

	private boolean isExport = false; // 是否是导出的上下文

	private StringBuilder whereSqlBuilder = new StringBuilder();

	/**
	 * 设备指纹
	 */
	private String blackBox;

	/**
	 * 导出日志id
	 */
	private String exportLogId;

	/**
	 * 预查询结果：在查询正式sql查询做的前置查询结果
	 */
	private PrepareQueryResult prepareQueryResult;

	// 每个列维值下的列数
	private Integer columnCountPerDimValue = 0;

	// 用户是否有城市行级权限管控
	private Integer hasCityRowAuth = Enabled.NO.getId();

	// 行级数据权限 exists过滤字段
	private List<AclDataAuthExistsFilterField> dataAuthExistsFilterFields = new ArrayList<>();

	public Set<QueryField> getUsedFields(){
		return usedFields;
	}
	
	public QueryContext addUsedField(QueryField f){
		usedFields.add(f);
		return this;
	}
	
	public Map<String, String> getAclFields() {
		return aclFields;
	}

	public void setAclFields(Map<String, String> aclFields) {
		this.aclFields = aclFields;
	}

	public QueryContext(){
		
	}
	
	public QueryContext(HttpServletRequest request){
		this.request = request;
	}
	
	public String getParamValue(String paramKey){
		if(request != null){
			return request.getParameter(paramKey);
		}else{
			return null;
		}
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(Map<String, String> queryParams) {
		this.queryParams = queryParams;
	}

	public User getUser() {
		/*
		if(request != null) {
			user = (User) request.getSession().getAttribute(Consts.SESSION_KEY_USER);
		}
		return user;
		 */
		User user = UserManager.get();
		if(user == null) {
			user = new User("unkonw-user");
		}
		return user;
	}

	public QueryContext clone(){
		QueryContext cp = new QueryContext();
		cp.request = this.request;
		cp.decryptSensitiveField = this.decryptSensitiveField;
		cp.queryParamString = this.queryParamString;

		cp.queryParams = new HashMap<>();
		cp.queryParams.putAll(this.queryParams);

		cp.aclFields = new HashMap<>(100);
		cp.aclFields.putAll(this.aclFields);

		cp.aclSensitiveFields = new ArrayList<>(); // 有权限敏感字段code列表
		cp.aclSensitiveFields.addAll(this.aclSensitiveFields);

		cp.sensitiveApplyFields = new ArrayList<>(); // 解密申请敏感字段code列表
		cp.sensitiveApplyFields.addAll(this.sensitiveApplyFields);

		cp.usedFields = new HashSet<QueryField>(); // 查询用到的字段
		cp.usedFields.addAll(this.usedFields);

		cp.dataAuthList = new ArrayList<>(); // 用户数据权限
		cp.dataAuthList.addAll(this.dataAuthList);

		cp.isExport = isExport;
		cp.prepareQueryResult = prepareQueryResult;

		cp.columnCountPerDimValue = this.columnCountPerDimValue;

		cp.hasCityRowAuth = this.hasCityRowAuth;

		cp.dataAuthExistsFilterFields = new ArrayList<>();
		cp.dataAuthExistsFilterFields.addAll(this.dataAuthExistsFilterFields);

		return cp;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public List<String> getAclSensitiveFields() {
		return aclSensitiveFields;
	}

	public void setAclSensitiveFields(List<String> aclSensitiveFields) {
		this.aclSensitiveFields = aclSensitiveFields;
	}

	public List<String> getSensitiveApplyFields() {
		return sensitiveApplyFields;
	}

	public void setSensitiveApplyFields(List<String> sensitiveApplyFields) {
		this.sensitiveApplyFields = sensitiveApplyFields;
	}

	public Integer getDecryptSensitiveField() {
		return decryptSensitiveField;
	}

	public void setDecryptSensitiveField(Integer decryptSensitiveField) {
		this.decryptSensitiveField = decryptSensitiveField;
	}

	public List<MetaFieldDataAuth> getDataAuthList() {
		return dataAuthList;
	}

	public void setDataAuthList(List<MetaFieldDataAuth> dataAuthList) {
		this.dataAuthList = dataAuthList;
	}

	public String getQueryParamString() {
		return queryParamString;
	}

	public void setQueryParamString(String queryParamString) {
		this.queryParamString = queryParamString;
	}

	public boolean isExport() {
		return isExport;
	}

	public void setExport(boolean export) {
		isExport = export;
	}

	public StringBuilder getWhereSqlBuilder() {
		return whereSqlBuilder;
	}

	public void setWhereSqlBuilder(StringBuilder whereSqlBuilder) {
		this.whereSqlBuilder = whereSqlBuilder;
	}

	public String getBlackBox() {
		return blackBox;
	}

	public void setBlackBox(String blackBox) {
		this.blackBox = blackBox;
	}

	public Set<QueryField> getModelUsedFields(String modelAlias) {
		return modelUsedFields.getOrDefault(modelAlias, new HashSet<>());
	}

	public void addModelUsedFields(String modelAlias, QueryField field) {
		modelUsedFields.computeIfAbsent(modelAlias, k -> new HashSet<>()).add(field);
	}

	public String getExportLogId() {
		return exportLogId;
	}

	public void setExportLogId(String exportLogId) {
		this.exportLogId = exportLogId;
	}

	public PrepareQueryResult getPrepareQueryResult() {
		return prepareQueryResult;
	}

	public void setPrepareQueryResult(PrepareQueryResult prepareQueryResult) {
		this.prepareQueryResult = prepareQueryResult;
	}

	public Integer getColumnCountPerDimValue() {
		return columnCountPerDimValue;
	}

	public void setColumnCountPerDimValue(Integer columnCountPerDimValue) {
		this.columnCountPerDimValue = columnCountPerDimValue;
	}

	public Integer getHasCityRowAuth() {
		return hasCityRowAuth;
	}

	public void setHasCityRowAuth(Integer hasCityRowAuth) {
		this.hasCityRowAuth = hasCityRowAuth;
	}

	public List<AclDataAuthExistsFilterField> getDataAuthExistsFilterFields() {
		return this.dataAuthExistsFilterFields == null ? new ArrayList<>() : this.dataAuthExistsFilterFields;
	}

	public void addDataAuthExistsFilterField(AclDataAuthExistsFilterField filterField) {
		if (filterField == null) {
			return;
		}
		// exists 行级权限注入可能随模型重建被重复触发；DataAuthExistsFilter 自身定义唯一性，这里只保持追加幂等。
		if (!this.dataAuthExistsFilterFields.contains(filterField)) {
			dataAuthExistsFilterFields.add(filterField);
		}
	}
}
