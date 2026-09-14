package com.bi.queryer.sys.user;

import com.bi.queryer.sys.authapply.SsmAuthApplyService;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.user.vo.SsmAuthApplyVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 用户管理
 * 
 */
@Controller
@Scope("prototype")
@RequestMapping("user")
public class UserController extends BaseController {

	@Autowired
	private UserService userService = null;

	@Autowired
	private SsmAuthApplyService ssmAuthApplyService;

	/**
	 * 查询所有用户
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="queryAllUser")
	public ResponseMessage queryAllUser(@RequestBody Map<String,Object> map){
		return userService.queryAllUser(map);
	}

	/**
	 * 获取当前用户信息
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="getCurrentUser")
	public ResponseMessage getCurrentUser(){
		return userService.getCurrentUser();
	}

	/**
	 * 多维分析权限申请
	 */
	@ResponseBody
	@RequestMapping(value = "ssmAuthApply")
	public ResponseMessage ssmAuthApply(@RequestBody SsmAuthApplyVo ssmAuthApplyVo) {
		return ssmAuthApplyService.ssmAuthApply(ssmAuthApplyVo);
	}

}
