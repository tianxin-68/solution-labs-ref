package com.bi.queryer.ssm.meta;

import java.util.Date;

public class RoleInfo {
	
	private int roleId;
	
	private String roleName;
	
	private String roleDes;
	
	private int roleType;
	
	private int isActive;
	
	private Date createdTime;
	
	private Date updatedTime;
	
	private String roleHome;

	public int getRoleId() {
		return roleId;
	}

	public void setRoleId(int roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getRoleDes() {
		return roleDes;
	}

	public void setRoleDes(String roleDes) {
		this.roleDes = roleDes;
	}

	public int getRoleType() {
		return roleType;
	}

	public void setRoleType(int roleType) {
		this.roleType = roleType;
	}

	public int getIsActive() {
		return isActive;
	}

	public void setIsActive(int isActive) {
		this.isActive = isActive;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getRoleHome() {
		return roleHome;
	}

	public void setRoleHome(String roleHome) {
		this.roleHome = roleHome;
	}
	
}
