package com.bi.queryer.ssm.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum FieldUseType {
	Filter,
	Operator,
	Result,
	None;

	// 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
	public static List<Map<String, String>> toListMap() {
		List<Map<String, String>> listMap = new ArrayList<>();
		for (FieldUseType i : FieldUseType.values()) {
			Map<String, String> map = new HashMap<>();
			map.put("value", i.toString());
			map.put("label", i.toString());
			listMap.add(map);
		}
		return listMap;
	}
}
