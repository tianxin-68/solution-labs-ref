package com.bi.queryer.sys.user.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.UserAuthMode;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;

/**
 * 用户信息表
 * @author contributor
 *
 */
public class User implements JSONSerializable{
	private String id = null;
	
	private String name = "";
	
	private String realName = "";
	
	private Integer isAdmin = 0;

	private Integer hasSsmAuth = 0;

	private String deptId = "";
	
	private String deptName = "";
	
	// 部门上下级路径id
	private String deptPathId = "";
	
	// 部门上下级路径名称
	private String deptPathName = "";
	
	private Integer isActive = 0;
	
	private String createdTime = "";
	
	private String updatedTime = "";
	
	private String password = "";
	
	private String type = "";
	
	private Integer authMode = UserAuthMode.OA.getId();// 认证模式
	
	// 用户可见的tips 菜单id列表
	private List<Integer> tipMenus = new ArrayList<Integer>();
	
	// 用户属性
	private Map attributes = new HashMap();
	
	private String signCode = "";// 签名编码,用户创建时自动生成
	
	private String token = "";
	
	private String empAccount = ""; // 员工账号
	
	private String email = "";
	
	// 用户来源
	private String origin = "";
	
	// 首页路径
	private String homePath = "";
	
	// 是否已授权
	private boolean authorized = false;

	/**
	 * 是否改变可用状态
	 */
	private boolean changeVaild;

	/**
	 * 是否改变管理员
	 */
	private boolean changeAdmin;

	private String roleName;

	/**
	 * 设备指纹
	 */
	private String blackBox;
	
	public boolean isAuthorized() {
		return authorized;
	}

	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}

	public String getHomePath() {
		return homePath;
	}

	public void setHomePath(String homePath) {
		this.homePath = homePath;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public User(){
		
	}
	
	public User(String name){
		this.name = name;
	}
	
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public Integer getIsAdmin() {
		if(isAdmin == null){
			isAdmin = Enabled.NO.getId();
		}
		return isAdmin;
	}

	public Integer getHasSsmAuth() {
		if(hasSsmAuth == null){
			hasSsmAuth = Enabled.NO.getId();
		}
		return hasSsmAuth;
	}

	public void setHasSsmAuth(Integer hasSsmAuth) {
		this.hasSsmAuth = hasSsmAuth;
	}

	/**
	 * 是否是白名单用户
	 * @return
	 */
	public boolean isWhiteListUser(){
		if(Enabled.getType(this.getIsAdmin()) == Enabled.YES){
			return true;
		}
		return false;
	}

	public boolean isChangeVaild() {
		return changeVaild;
	}

	public void setChangeVaild(boolean changeVaild) {
		this.changeVaild = changeVaild;
	}

	public boolean isChangeAdmin() {
		return changeAdmin;
	}

	public void setChangeAdmin(boolean changeAdmin) {
		this.changeAdmin = changeAdmin;
	}

	public String getDeptId() {
		return deptId;
	}

	public void setDeptId(String deptId) {
		this.deptId = deptId;
	}

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public void setIsAdmin(Integer isAdmin) {
		this.isAdmin = isAdmin;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDeptPathId() {
		return deptPathId;
	}

	public void setDeptPathId(String deptPathId) {
		this.deptPathId = deptPathId;
	}

	public String getDeptPathName() {
		return deptPathName;
	}

	public void setDeptPathName(String deptPathName) {
		this.deptPathName = deptPathName;
	}

	public List<Integer> getTipMenus() {
		return tipMenus;
	}

	public void setTipMenus(List<Integer> tipMenus) {
		this.tipMenus = tipMenus;
	}

	public Map getAttributes() {
		return attributes;
	}

	public void setAttributes(Map attributes) {
		this.attributes = attributes;
	}

	public String getSignCode() {
		if(signCode == null) {
			signCode = "";
		}
		return signCode;
	}

	public void setSignCode(String signCode) {
		this.signCode = signCode;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getEmpAccount() {
		return empAccount;
	}

	public void setEmpAccount(String empAccount) {
		this.empAccount = empAccount;
	}

	public String getEmail() {
		if (BIUtil.isEmpty(email)) {
			email = this.name + "@example.com";
		}
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public Integer getAuthMode() {
		if(authMode == null){
			authMode = UserAuthMode.OA.getId();
		}
		return authMode;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public void setAuthMode(Integer authMode) {
		this.authMode = authMode;
	}

	public String getBlackBox() {
		return blackBox;
	}

	public void setBlackBox(String blackBox) {
		this.blackBox = blackBox;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jsonObject = BIUtil.toJSONObject(this);
		jsonObject.remove("token");
		jsonObject.remove("password");
		jsonObject.remove("signCode");
		jsonObject.remove("empAccount");
		return jsonObject;
	}

}
