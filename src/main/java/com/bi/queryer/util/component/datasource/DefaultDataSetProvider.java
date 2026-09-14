package com.bi.queryer.util.component.datasource;

import java.util.List;
import java.util.Map;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.UpperCaseMap;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.ComponentUtil;

public class DefaultDataSetProvider extends BaseDao implements IDataSetProvider {
	@Override
	public List<UpperCaseMap> getDataSet(Map queryParamMap) {
		String sqlId = getSQLID(queryParamMap);
		long t1 = System.currentTimeMillis();
		List<UpperCaseMap> data = this.queryMapList(sqlId, queryParamMap, DataSourceType.Default);
		ComponentUtil.log("读取数据（未读取缓存）[" + sqlId + "]", "耗费时间" + (System.currentTimeMillis() - t1) + "毫秒");
		return data;
	}

	@Override
	public Integer getDataSetTotalSize(Map queryParamMap) {
		String sqlId = getSQLID(queryParamMap) + "Count";
		return (Integer) this.queryObject(sqlId, queryParamMap, DataSourceType.Default);
	}

	public String getSQLID(Map queryParamMap) {
		String namespace = queryParamMap.get(ComponentConstants.Parameter_Component_Namespace) + "";
		String componentCode = queryParamMap.get(ComponentConstants.Parameter_Component_Code) + "";
		String sqlId = namespace + "." + componentCode;
		return sqlId;
	}
}
