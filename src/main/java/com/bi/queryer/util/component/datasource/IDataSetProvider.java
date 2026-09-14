package com.bi.queryer.util.component.datasource;

import java.util.List;
import java.util.Map;

import com.bi.queryer.util.JSONSerializable;

public interface IDataSetProvider{
	/**
	 * @param queryParamMap ibatis查询参数
	 * @return 获取数据集，如果分页，为分页数据集
	 */
	public List<? extends JSONSerializable> getDataSet(Map queryParamMap);
	

	/**
	 * @param queryParamMap ibatis查询参数
	 * @return 获取数据集总大小，用于分页
	 */
	public Integer getDataSetTotalSize(Map queryParamMap);
	
}
