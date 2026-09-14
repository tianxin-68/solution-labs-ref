package com.bi.queryer.ssm.meta;

import java.util.Date;

public class SSDAcl {
	
	private Integer roleId;
	
	private String fieldCode;
	
	private Date crateTime;
	
	private Integer isAppend;   //IS_APPEND


	public Integer getRoleId() {
		return roleId;
	}

	public void setRoleId(Integer roleId) {
		this.roleId = roleId;
	}

	public Integer getIsAppend() {
		return isAppend;
	}

	public void setIsAppend(Integer isAppend) {
		this.isAppend = isAppend;
	}

	public String getFieldCode() {
		return fieldCode;
	}

	public void setFieldCode(String fieldCode) {
		this.fieldCode = fieldCode;
	}

	public Date getCrateTime() {
		return crateTime;
	}

	public void setCrateTime(Date crateTime) {
		this.crateTime = crateTime;
	}



}
