package com.bi.queryer.sys.startup;

/**
 * 可被初始化的模块
 * @author contributor
 *
 */
public enum InitializableModule {
	/**
	 * 所有模块
	 */
	All("", ""),
	
	/**
	 * 系统配置
	 */
	SystemConfig("com.bi.queryer.sys.config.SystemConfig", "default"),
	
	DataSource("dynamicDataSource", "spring"),

	SSD("SSDMetaCacheManager", "spring"),

	HOT("com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager", "default"),

	QueryTemplate("com.bi.queryer.ssm.engine.accelerate.cache.QueryTemplateCacheManager", "default"),

	MultiSelect("multiSelectFilterCacheManager", "spring"),

	TargetValueCacheManager("targetValueCacheManager", "spring"),
	/**
	 * 未知模块
	 */
	Unknow("", "");
	
	private String className = "";
	private String constructer = "";
	
	private InitializableModule(String className, String constructer) {
		this.className = className;
		this.constructer = constructer;
	}
	
	public static InitializableModule get(String module){
		for(InitializableModule m : InitializableModule.values()){
			if(m.toString().equalsIgnoreCase(module)){
				return m;
			}
		}
		return Unknow;
	}
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getConstructer() {
		return constructer;
	}
	public void setConstructer(String constructer) {
		this.constructer = constructer;
	}
}
