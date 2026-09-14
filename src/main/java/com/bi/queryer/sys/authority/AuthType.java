package com.bi.queryer.sys.authority;

public enum AuthType {
	LINE("line", "Line", "事业部"),
	DEPT("dept", "Dept", "部门"),
	PRODUCT("product", "Product", "产品线"),
	TRADER("trader", "Trader", "交易员");
	
	private String id = "";
	private String name = "";
	private String desc = "";
	
	private AuthType(String id, String name, String desc){
		this.id = id;
		this.name = name;
		this.desc = desc;
	}
	
	public static AuthType getType(String typeStr){
		for(AuthType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)){
				return type;
			}
		}
		return null;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
