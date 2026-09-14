package com.bi.queryer.sys.role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bi.queryer.sys.cache.CacheService;
import com.bi.queryer.sys.cache.CacheType;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.config.SystemConfig;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.interceptor.OperLog;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.RoleType;
import com.bi.queryer.sys.role.vo.Role;
import com.bi.queryer.sys.role.vo.RoleAuth;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import com.bi.queryer.util.component.text.Text;

/**
 * 角色管理service
 */
@Service
public class RoleService {

	@Autowired
	private BaseDao dao;

	/**
	 * 查询用户是否是某个角色
	 * 如果有值，那么返回true
	 *
	 * @return Boolean
	 */
	public Boolean queryRoleUserNameExist(String roleId, String userName) {
		Boolean flag = false;
		if(StringUtils.isBlank(roleId)||StringUtils.isBlank(userName)){
			return flag;
		}

		Map queryParamMap = new HashMap();
		queryParamMap.put("roleId",roleId);
		queryParamMap.put("userName",userName);
		Integer count = dao.queryCount("role.queryRoleUserName", queryParamMap, DataSourceType.Default);
		if(count>0){
			flag = true;
		}
		return flag;
	}

	/**
	 * 当前用户是否是管理员角色
	 * @return
	 */
	public Boolean isAdminRole(){
		User user = UserManager.get();
		return Enabled.isTrue(user.getIsAdmin()) ||  isAdminRole(user.getName());
	}

	public Boolean isAdminRole(String userName){
		//管理员和多维分析管理员可以又所有权限
		String roleId = SC.v("ssm.admin.roleId", "");
		return queryRoleUserNameExist(roleId, userName);
	}
	

}
