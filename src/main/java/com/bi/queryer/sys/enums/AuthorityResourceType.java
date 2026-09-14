package com.bi.queryer.sys.enums;

/**
 * 权限资源类型
 * @author contributor
 *
 */
public enum AuthorityResourceType {
	Menu(0, "菜单"),
	Function(1, "功能");
	
	private int id;
	
	private String desc;
	
	private AuthorityResourceType(int id, String desc){
		this.id = id;
		this.desc = desc;
	}
	
	public static AuthorityResourceType get(int id){
		for(AuthorityResourceType type : AuthorityResourceType.values()){
			if(type.getId() == id){
				return type;
			}
		}
		return Menu;
	}
	
	public static AuthorityResourceType get(String code){
		for(AuthorityResourceType type : AuthorityResourceType.values()){
			if(type.toString().equalsIgnoreCase(code)){
				return type;
			}
		}
		return Menu;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
}
