package com.bi.queryer.sys.enums;

public enum TokenErrorType {
	None("404", "无token"),
	Expired("501", "已过期"),
	None_User("502", "无此账号"),
	Invalid_User("503", "账号无效"),
	Check_Error("504", "校验失败"),
	Unknow("-1", "未知");
	
	private String desc;
	
	private String code;
	
	private TokenErrorType(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}
	
	public static TokenErrorType get(String code) {
		for(TokenErrorType t : values()) {
			if(t.getCode().equalsIgnoreCase(code)) {
				return t;
			}
		}
		return Unknow;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
