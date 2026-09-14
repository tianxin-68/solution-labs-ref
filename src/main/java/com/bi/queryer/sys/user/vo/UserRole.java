package com.bi.queryer.sys.user.vo;

import java.sql.Timestamp;

/**
 * 用户角色信息
 * 
 * @author contributor
 *
 */
public class UserRole {

	private Integer userId;
	
	private Integer roleId;
	
	private Timestamp created_time;
	
	private Timestamp updated_time;

	public UserRole() {
		
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getRoleId() {
		return roleId;
	}

	public void setRoleId(Integer roleId) {
		this.roleId = roleId;
	}

	public Timestamp getCreated_time() {
		return created_time;
	}

	public void setCreated_time(Timestamp created_time) {
		this.created_time = created_time;
	}

	public Timestamp getUpdated_time() {
		return updated_time;
	}

	public void setUpdated_time(Timestamp updated_time) {
		this.updated_time = updated_time;
	}
}
