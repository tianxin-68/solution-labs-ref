package com.bi.queryer.util.component;

public class ComponentSQLLog {
	private String logId;
	private String userName;
	private String entityId;
	private String entityName;
	private String componentType;
	private String componentId;
	private String componentName;
	private String componentSql;
	private Double runSecond;
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getComponentId() {
		return componentId;
	}
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	public String getComponentName() {
		return componentName;
	}
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	public String getComponentSql() {
		return componentSql;
	}
	public void setComponentSql(String componentSql) {
		this.componentSql = componentSql;
	}
	public String getLogId() {
		return logId;
	}
	public void setLogId(String logId) {
		this.logId = logId;
	}
	public String getEntityId() {
		return entityId;
	}
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	public String getComponentType() {
		return componentType;
	}
	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}
	public Double getRunSecond() {
		return runSecond;
	}
	public void setRunSecond(Double runSecond) {
		this.runSecond = runSecond;
	}
}
