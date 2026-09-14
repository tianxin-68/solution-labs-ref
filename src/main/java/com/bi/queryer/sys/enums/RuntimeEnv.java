package com.bi.queryer.sys.enums;

public enum RuntimeEnv {
	Product("prod", "正式环境", "40"),
	UT("uat", "UAT环境", "30"),
	Test("test", "测试环境", "20"),
	Dev("dev", "开发环境", "10"),
	ProductFms("prod_fms", "正式环境", "45"),
	UTFms("uat_fms", "UAT环境", "35"),
	TestFms("test_fms", "测试环境", "25"),
	DevFms("dev_fms", "开发环境", "15"),
	Unknow("", "未知环境", "-1");
	
	private String code = "";
	
	private String desc = "";
	
	private String value = "";
	
	private RuntimeEnv(String code, String desc, String value) {
		this.code = code;
		this.desc = desc;
		this.value = value;
	}
	
	public static RuntimeEnv getEnv(String code){
		for(RuntimeEnv re : RuntimeEnv.values()){
			if(re.getCode().equalsIgnoreCase(code)){
				return re;
			}
		}
		return Unknow;
	}
	
	public static RuntimeEnv getEnvByValue(String value){
		for(RuntimeEnv re : RuntimeEnv.values()){
			if(re.getValue().equalsIgnoreCase(value)){
				return re;
			}
		}
		return Unknow;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
