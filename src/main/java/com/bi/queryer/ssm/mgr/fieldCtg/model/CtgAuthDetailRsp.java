package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CtgAuthDetailRsp {

	/**
	 * 是否显示
	 */
	private Boolean isShow = false;

	/**
	 * 维度code
	 */
	private String dimCode;

	/**
	 * 已选择item列表
	 */
	private List<String> itemList = new ArrayList<>();
}
