package com.bi.queryer.sys.role.vo;

import java.sql.Timestamp;

import com.alibaba.fastjson.JSONObject;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;

/**
 * 系统角色
 *
 */
public class Role implements JSONSerializable{

	//角色ID
	private String roleId;
	//角色名称
	private String roleName;
	//角色描述
	private String roleDesc;
	//角色类型
	private Integer roleType;
	//是否可用
    private Integer isActive;
    //默认菜单
    private String roleHome;
    //创建时间
    private String createTime;
    //更新时间
    private String updateTime;
    
    private String updatedBy;
    
    private String createdBy;
    
    //起始有效时间
    private String activeStartDate;
    //截止有效时间
    private String activeEndDate;

	/**
	 * 是否改变可用状态
	 */
	private boolean changeVaild;

	/**
	 * 菜单名称
	 */
	private String menuName;

	/**
	 * 角色负责人
	 */
	private String roleOwner;

	/**
	 * 关联目录id
	 */
	private String roleMenuDirId;
    
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	public String getRoleDesc() {
		return roleDesc;
	}
	public void setRoleDesc(String roleDesc) {
		this.roleDesc = roleDesc;
	}
	
	public String getRoleHome() {
		return roleHome;
	}
	public void setRoleHome(String roleHome) {
		this.roleHome = roleHome;
	}
	
	public String getActiveStartDate() {
		return activeStartDate;
	}
	public void setActiveStartDate(String activeStartDate) {
		this.activeStartDate = activeStartDate;
	}
	public String getActiveEndDate() {
		return activeEndDate;
	}
	public void setActiveEndDate(String activeEndDate) {
		this.activeEndDate = activeEndDate;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((isActive == null) ? 0 : isActive.hashCode());
		result = prime * result + ((roleId == null) ? 0 : roleId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Role other = (Role) obj;
		if (isActive == other.isActive && roleId.equals(other.roleId)) {
			return true;
		}
		return false;
	}
	@Override
	public JSONObject toJSON() {
		return BIUtil.toJSONObject(this);
	}
	public String getRoleId() {
		return roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	public Integer getRoleType() {
		return roleType;
	}
	public void setRoleType(Integer roleType) {
		this.roleType = roleType;
	}
	public Integer getIsActive() {
		return isActive;
	}
	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}
	public String getCreateTime() {
		return createTime;
	}
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
	public String getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	public String getUpdatedBy() {
		return updatedBy;
	}
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public boolean isChangeVaild() {
		return changeVaild;
	}

	public void setChangeVaild(boolean changeVaild) {
		this.changeVaild = changeVaild;
	}

	public String getMenuName() {
		return menuName;
	}

	public void setMenuName(String menuName) {
		this.menuName = menuName;
	}

	public String getRoleOwner() {
		return roleOwner;
	}

	public void setRoleOwner(String roleOwner) {
		this.roleOwner = roleOwner;
	}

	public String getRoleMenuDirId() {
		if(BIUtil.isEmpty(roleMenuDirId)){
			return null;
		}
		return roleMenuDirId;
	}

	public void setRoleMenuDirId(String roleMenuDirId) {
		this.roleMenuDirId = roleMenuDirId;
	}
}
