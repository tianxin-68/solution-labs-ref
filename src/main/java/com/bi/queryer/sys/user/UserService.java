package com.bi.queryer.sys.user;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.sensitive.model.DownloadWorkOrderInfoRV;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import com.tx.cache.redis.RedisCacheClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

	@Autowired
	private BaseDao dao;

	/**
	 * 查询指定用户名的用户信息
	 *
	 * @param userName 查询参数（用户名：username）
	 * @return User
	 */
	public User queryByName(String userName) {
		//User user = (User) CacheManager.getObject(CacheType.User, userName, User.class);
		User user = null;
		String cacheKey = BIConsts.USER_INFO_REDIS_KEY + userName;

		try {
			RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
			Object cacheValue = redisCacheClient.get(cacheKey);
			user = cacheValue == null ? null : JSONObject.parseObject(cacheValue.toString(), User.class);

			if (user == null) {
				user = queryByNameFromDB(userName);

				if(StrUtil.isNotEmpty(userName)){
					redisCacheClient.set(cacheKey, JSONObject.toJSONString(user), 30*60, TimeUnit.SECONDS);
				}
				//	CacheManager.set(CacheType.User, userName, user);
			}

		} catch (Exception e) {
			e.printStackTrace();
			//兜底从数据库查询
			user = queryByNameFromDB(userName);
		}

		return user;
	}

	public User queryByNameFromDB(String userName) {
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("userName", userName);
		User user = (User) dao.queryObject("user.queryUserByName", paramMap);
		return user;
	}

	/**
	 * 查询与指定用户同部门(最小部门单位)的所有用户名
	 *
	 * @param userName 用户名
	 * @return 同部门用户名列表
	 */
	public List<String> queryDeptColleagueUserNames(String userName) {
		if (StringUtils.isBlank(userName)) {
			return new ArrayList<>();
		}
		return (List<String>) dao.queryObjectList("user.queryDeptColleagueUserNames", userName);
	}

	/**
	 * 获取用户模板公共模块编辑数据权限
	 *
	 * @return
	 */
	public Integer queryTemplateCtgDataAuthByUserCount(String userName) {
		if (StringUtils.isBlank(userName)) {
			return 0;
		}
		return dao.queryCount("ssm.query.queryTemplateCtgDataAuthByUserCount", userName);
	}

	/**
	 * 获取用户所在部门
	 *
	 * @return
	 */
	public List<DownloadWorkOrderInfoRV> getUserDeptInfo(String userName) {
		if (StringUtils.isBlank(userName)) {
			return new ArrayList<>();
		}
		List<DownloadWorkOrderInfoRV> deptList = (List<DownloadWorkOrderInfoRV>) dao.queryObjectList("user.getUserDeptInfo", userName);

		return deptList;
	}

	/**
	 * 查询用户用于穿梭框
	 *
	 * @param paramMap
	 * @return
	 */
	public ResponseMessage queryAllUser(Map<String, Object> paramMap) {
		ResponseMessage resultMsg = new ResponseMessage();

		Integer pageRowLower = Integer.parseInt(paramMap.get("pageRowLower") + "");
		Integer prePageSize = Integer.parseInt(paramMap.get("prePageSize") + "");

		if (pageRowLower > 0) {
			pageRowLower = (pageRowLower - 1) * prePageSize;
		} else {
			pageRowLower = 0;
		}

		paramMap.put("pageRowLower", pageRowLower);

		// 兼容邮箱格式：searchText 若为邮箱则取 @ 前的域账号部分
		Object searchTextObj = paramMap.get("searchText");
		if (searchTextObj != null) {
			paramMap.put("searchText", BIUtil.resolveUserName(searchTextObj.toString()));
		}

		// 兼容邮箱格式：selectUserList 中每项若为邮箱则取 @ 前的域账号部分
		Object selectUserListObj = paramMap.get("selectUserList");
		if(selectUserListObj != null){
			if (selectUserListObj instanceof List) {
				List<?> rawList = (List<?>) selectUserListObj;
				List<String> resolvedList = new java.util.ArrayList<>();
				for (Object item : rawList) {
					if (item == null) {
						continue;
					}
					resolvedList.add(BIUtil.resolveUserName(item.toString()));
				}
				paramMap.put("selectUserList", resolvedList);
			}
		}

		paramMap.put("isActive", 1);
		List<User> list = (List<User>) dao.queryObjectList("user.queryAllUser", paramMap);
		int totalCount = dao.queryCount("user.queryAllUserCount", paramMap);

		//封装结果集
		Map<String, Object> mapResult = new HashMap<>();
		mapResult.put("list", list);
		mapResult.put("totalCount", totalCount);

		resultMsg.setData(mapResult);
		return resultMsg;
	}

	/**
	 * 获取当前用户信息
	 *
	 * @return
	 */
	public ResponseMessage getCurrentUser() {

		ResponseMessage result = new ResponseMessage();
		try {
			User user = UserManager.get();
			//封装结果集
			Map<String, Object> map = new HashMap<>();
			map.put("name", user.getName());
			map.put("realName", user.getRealName());
			map.put("deptName", user.getDeptName());
			map.put("email", user.getEmail());
			map.put("isAdmin", user.getIsAdmin());
			result.setData(map);

		} catch (Exception e) {
			System.out.println("获取当前用户异常" + e.getMessage());
			return new ResponseMessage(e);
		}
		return result;
	}

}
