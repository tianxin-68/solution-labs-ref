package com.bi.queryer.sys.role.vo;

public class RoleAuth {
	private String roleId;
	private String resId;
	private String resType;
	private Integer isAppend;
	public String getRoleId() {
		return roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	public String getResId() {
		return resId;
	}
	public void setResId(String resId) {
		this.resId = resId;
	}
	public Integer getIsAppend() {
		return isAppend;
	}
	public void setIsAppend(Integer isAppend) {
		this.isAppend = isAppend;
	}
	public String getResType() {
		return resType;
	}
	public void setResType(String resType) {
		this.resType = resType;
	}
}
