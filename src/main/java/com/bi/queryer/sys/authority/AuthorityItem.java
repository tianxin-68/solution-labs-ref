package com.bi.queryer.sys.authority;

import java.sql.Date;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.ResourceType;

public class AuthorityItem {
	
	private String roleId = null;
	
	private String resourceId = "";
	
	private String resourceType = ResourceType.Menu.toString();
	
	private Date createdTime = null;
	
	private Integer isAppend = Enabled.NO.getId();
	
	public Integer getIsAppend() {
		return isAppend;
	}

	public void setIsAppend(Integer isAppend) {
		this.isAppend = isAppend;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(resourceId == null) return false;
		if(resourceType == null) return false;
		AuthorityItem a = (AuthorityItem) obj;
		return resourceId.equals(a.resourceId) && this.resourceType.equals(a.resourceType);
	}

	@Override
	public int hashCode() {
		int result = (resourceId + resourceType).hashCode();
		return result;
	}
}
