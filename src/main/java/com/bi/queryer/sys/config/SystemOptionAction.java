package com.bi.queryer.sys.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ReturnMsg;
import com.bi.queryer.util.UpperCaseMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 系统项配置管理的ACTION
 * 
 * @author contributor
 *
 */
@Controller
@Scope("prototype")
public class SystemOptionAction extends BaseController {

	@Autowired
	@Qualifier("systemOptionService")
	private SystemOptionService systemOptionService = null;

	/**
	 * 系统项管理页面
	 */
	@RequestMapping(value="sys/option")
	public void execute() {
		resultUrl = "/page/sys/option/option_setting.jsp";
		dispatch(resultUrl);
	}
	
	@RequestMapping(value="sys/queryAllOption")
	public void queryAllOption(){
		List<UpperCaseMap> result = systemOptionService.queryAllOption();
		writeJSON(JSONArray.fromObject(result).toString());
	}
	
	@RequestMapping(value="sys/saveOption")
	public void saveOption(){
		ReturnMsg<String> result = systemOptionService.saveOption(toStringMap());
		writeJSON(JSONObject.fromObject(result));
	}
	
}
