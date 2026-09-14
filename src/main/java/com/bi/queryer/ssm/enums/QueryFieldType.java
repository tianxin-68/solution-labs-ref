package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询字段类型
 * @author contributor
 *
 */
public enum QueryFieldType{
	
	Filter("过滤字段"),
	Result("结果字段"),
	All("既是过滤也是结果字段");
	
	private String desc;
	
	private QueryFieldType(String desc){
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	// 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
	public static List<Map<String, String>> toListMap() {
		List<Map<String, String>> listMap = new ArrayList<>();
		for (QueryFieldType i : QueryFieldType.values()) {
			Map<String, String> map = new HashMap<>();
			map.put("value", i.getDesc());
			map.put("label", i.getDesc());
			listMap.add(map);
		}
		return listMap;
	}
}
