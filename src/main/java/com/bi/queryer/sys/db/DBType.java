package com.bi.queryer.sys.db;

public enum DBType {
	MySQL("com.mysql.jdbc.Driver", "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF8&useSSL=false", "select 1", true),
	Doris("com.mysql.jdbc.Driver", "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF8&useSSL=false", "select 1", false),
	Oracle("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@//%s:%s/%s", "select 1 from dual", true),
	Presto("com.facebook.presto.jdbc.PrestoDriver", "jdbc:presto://%s:%s/%s",  "select 1", false),
	Trino("io.trino.jdbc.TrinoDriver", "jdbc:trino://%s:%s/%s?SSL=true&SSLVerification=NONE",  "select 1", false),
	SQLServer("com.microsoft.sqlserver.jdbc.SQLServerDriver" ,"jdbc:sqlserver://%s:%s;DatabaseName=%s", "select 1", true),
	Kylin("org.apache.kylin.jdbc.Driver", "jdbc:kylin://%s:%s/%s", "select 1 ", true),
	Hive("org.apache.hive.jdbc.HiveDriver", "jdbc:hive2://%s:%s/%s", "select 1", false);

	private String driverClass = "";
	private String url = "";
	private String validationQuery = "";
	private Boolean canPagination = true;

	private DBType(String driverClass, String url, String validationQuery, Boolean canPagination) {
		this.driverClass = driverClass;
		this.url = url;
		this.validationQuery = validationQuery;
		this.canPagination = canPagination;
	}

	public static DBType getType(String typeStr){
		for(DBType type : DBType.values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return MySQL;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getValidationQuery() {
		return validationQuery;
	}

	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	public Boolean getCanPagination() {
		return canPagination;
	}

	public void setCanPagination(Boolean canPagination) {
		this.canPagination = canPagination;
	}
}
