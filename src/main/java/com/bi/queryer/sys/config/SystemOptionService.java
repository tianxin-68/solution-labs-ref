package com.bi.queryer.sys.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bi.queryer.sys.base.BaseService;
import com.bi.queryer.sys.common.ReturnMsg;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.UpperCaseMap;

public class SystemOptionService extends BaseService<SystemOption>{

	public List<SystemOption> queryAll(){
		return queryList("option.queryAll", new HashMap());
	}
	
	public List<UpperCaseMap> queryAllOption(){
		List<UpperCaseMap> result = (List<UpperCaseMap>) dao.queryObjectList("option.queryAllOption", null, DataSourceType.Default);
		return result;
	}
	
	public ReturnMsg<String> saveOption(Map<String,String> params){
		ReturnMsg<String> result = new ReturnMsg<>();
		try {
			dao.update("option.saveOption", params, DataSourceType.Default);
			SystemConfig.initialize();
			result.setMsg("保存成功");
		} catch (Exception e) {
			// TODO: handle exception
			result =  new ReturnMsg<>(e);
		}
		return result;
	}
}
