package com.bi.queryer.sys.enums;

public enum UserAuthMode {
	OA(0, "oa域账号认证"),
	Inner(1, "系统内部密码认证");
	
	private Integer id;
	
	private String desc;
	
	public static UserAuthMode get(Integer id){
		for(UserAuthMode m : UserAuthMode.values()){
			if(m.getId().equals(id)){
				return m;
			}
		}
		return OA;
	}
	
	public static UserAuthMode get(String code){
		for(UserAuthMode m : UserAuthMode.values()){
			if(m.toString().equalsIgnoreCase(code)){
				return m;
			}
		}
		return OA;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	private UserAuthMode(Integer id, String desc) {
		this.id = id;
		this.desc = desc;
	}
}
